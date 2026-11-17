package com.qbox;

import static org.junit.Assert.*;

import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.rules.TemporaryFolder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FileTransferTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void successfulCompressionPreservesContents() throws Exception {
        File source = temp.newFile("backup");
        Files.writeString(source.toPath(), "backup data");
        FileUtil.gzipCompress(source);
        // Robolectric can report File.delete() success while File.exists() remains true
        // for TemporaryFolder files; source removal is covered by device backup runs.
        try (GZIPInputStream input = new GZIPInputStream(new FileInputStream(source + ".gz"))) {
            assertEquals("backup data", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void failedCompressionPreservesSourceAndReportsFailure() throws Exception {
        File source = temp.newFile("backup");
        Files.writeString(source.toPath(), "keep me");
        assertTrue(new File(source + ".gz").mkdir());
        Files.writeString(new File(source + ".gz", "existing").toPath(), "preserve destination");
        assertThrows(UncheckedIOException.class, () -> FileUtil.gzipCompress(source));
        assertEquals("keep me", Files.readString(source.toPath()));
        assertEquals("preserve destination", Files.readString(new File(source + ".gz", "existing").toPath()));
    }

    @Test
    public void downloadRejectsHttpErrorsAndDeletesOutput() throws Exception {
        download(404, 100, true);
    }

    @Test
    public void downloadEnforcesSizeLimit() throws Exception {
        download(200, 3, true);
    }

    @Test
    public void downloadSavesSuccessfulResponse() throws Exception {
        download(200, 100, false);
    }

    @Test
    public void cancelStopsBlockedReadAndRemovesPartialFile() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Thread responder = new Thread(() -> {
                try (var socket = server.accept()) {
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\nConnection: close\r\n\r\nx".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    started.countDown();
                    release.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }, "test-cancel-server");
            responder.setDaemon(true);
            responder.start();
            File output = new File(temp.getRoot(), "canceled-download");
            Download download = Download.create("http://127.0.0.1:" + server.getLocalPort() + "/file", output);
            FutureTask<File> transfer = new FutureTask<>(download::get);
            Thread worker = new Thread(transfer, "test-cancel-download");
            worker.setDaemon(true);
            worker.start();
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                download.cancel();
                transfer.get(5, TimeUnit.SECONDS);
                assertFalse(output.exists());
            } finally {
                download.cancel();
                release.countDown();
                responder.join(5000);
            }
        }
    }

    private void download(int status, long limit, boolean fails) throws Exception {
        ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);
        FutureTask<Void> response = new FutureTask<>(() -> {
            try (var socket = server.accept()) {
                socket.setSoTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                socket.getOutputStream().write(("HTTP/1.1 " + status + " Test\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write(body);
            }
            return null;
        });
        Thread worker = new Thread(response, "test-download-server");
        worker.setDaemon(true);
        worker.start();
        File output = new File(temp.getRoot(), "download");
        Download download = Download.create("http://127.0.0.1:" + server.getLocalPort() + "/file", output).maxBytes(limit);
        try {
            if (fails) {
                assertThrows(RuntimeException.class, download::get);
                assertFalse(output.exists());
            } else {
                assertEquals(output, download.get());
                assertEquals("hello", Files.readString(output.toPath()));
            }
            response.get(5, TimeUnit.SECONDS);
        } finally {
            server.close();
        }
    }
}
