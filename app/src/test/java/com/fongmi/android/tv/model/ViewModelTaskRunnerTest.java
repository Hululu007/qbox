package com.fongmi.android.tv.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对齐 FongMi 上游 5.8.8 的 ViewModelTaskRunner 健壮性（GS-82）。
 *
 * <p>用「收集但不自动执行」的 executor 确定性控制回调投递时机：验证
 * cancel/cancelAll 会递增 taskId，使「已经排队但尚未投递」的过期回调被丢弃，
 * 不会把旧结果覆盖到新页面。旧实现 cancelAll 只 cancel future + clear map，不递增
 * taskId，pending 回调仍满足 taskId.get()==currentId 而投递过期结果。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ViewModelTaskRunnerTest {

    private enum TaskType { A, B }

    private static final class ManualExecutor implements Executor {
        private final BlockingQueue<Runnable> pending = new LinkedBlockingQueue<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
        }

        Runnable await() throws InterruptedException {
            Runnable runnable = pending.poll(5, TimeUnit.SECONDS);
            assertNotNull("callback should have been queued", runnable);
            return runnable;
        }
    }

    @Test
    public void normalCallbackDeliversResult() throws Exception {
        ManualExecutor executor = new ManualExecutor();
        ViewModelTaskRunner<TaskType> runner = new ViewModelTaskRunner<>(TaskType.class, executor);
        AtomicReference<String> received = new AtomicReference<>();
        runner.execute(TaskType.A, 5000, () -> "value", received::set, error -> received.set("error"));
        executor.await().run();
        assertEquals("value", received.get());
    }

    /** 核心：cancelAll 后，已排队未投递的过期回调不得投递。 */
    @Test
    public void cancelAllInvalidatesPendingCallback() throws Exception {
        ManualExecutor executor = new ManualExecutor();
        ViewModelTaskRunner<TaskType> runner = new ViewModelTaskRunner<>(TaskType.class, executor);
        AtomicReference<String> received = new AtomicReference<>();
        runner.execute(TaskType.A, 5000, () -> "stale", received::set, error -> received.set("error"));
        Runnable pending = executor.await();
        runner.cancelAll();
        pending.run();
        assertNull("stale result must be dropped after cancelAll", received.get());
    }

    /** cancel(单类型) 只让该类型失效，不影响其它类型正常投递。 */
    @Test
    public void cancelOneTypeKeepsOther() throws Exception {
        ManualExecutor executor = new ManualExecutor();
        ViewModelTaskRunner<TaskType> runner = new ViewModelTaskRunner<>(TaskType.class, executor);
        AtomicReference<String> receivedA = new AtomicReference<>();
        AtomicReference<String> receivedB = new AtomicReference<>();
        runner.execute(TaskType.A, 5000, () -> "a", receivedA::set, error -> receivedA.set("error"));
        Runnable pendingA = executor.await();
        runner.execute(TaskType.B, 5000, () -> "b", receivedB::set, error -> receivedB.set("error"));
        Runnable pendingB = executor.await();
        runner.cancel(TaskType.A);
        pendingA.run();
        pendingB.run();
        assertNull("type A stale result must be dropped", receivedA.get());
        assertEquals("b", receivedB.get());
    }
}
