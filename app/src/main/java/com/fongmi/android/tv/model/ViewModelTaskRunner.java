package com.fongmi.android.tv.model;

import com.fongmi.android.tv.utils.Task;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * ViewModel 任务调度器（对齐 FongMi 上游 5.8.8 的健壮性设计，GS-82）。
 *
 * <p>我们的 Task API 与上游不同（com.fongmi.android.tv.utils.Task 无 Task.submit(callable, timeoutMs)），
 * 因此 execute 的提交方式保留本地写法；但「取消时递增 taskId 使在途回调失效」与
 * 「可注入 callbackExecutor」两个机制按上游 backport：旧 cancelAll 只 cancel future + clear map，
 * 不递增 taskId，导致已经排入 executor 但尚未执行的回调依旧满足 taskId.get()==currentId，
 * 把过期结果投递出去（切源/退出后旧结果覆盖新页面）。
 */
final class ViewModelTaskRunner<T extends Enum<T>> {

    private final Map<T, ListenableFuture<?>> futures;
    private final Map<T, AtomicInteger> taskIds;
    private final Executor callbackExecutor;

    ViewModelTaskRunner(Class<T> typeClass) {
        this(typeClass, MoreExecutors.directExecutor());
    }

    ViewModelTaskRunner(Class<T> typeClass, Executor callbackExecutor) {
        futures = new EnumMap<>(typeClass);
        taskIds = new EnumMap<>(typeClass);
        this.callbackExecutor = callbackExecutor;
        for (T type : typeClass.getEnumConstants()) taskIds.put(type, new AtomicInteger(0));
    }

    <R> void execute(T type, long timeoutMs, Callable<R> callable, Consumer<R> onSuccess, Consumer<Throwable> onError) {
        AtomicInteger taskId = Objects.requireNonNull(taskIds.get(type));
        int currentId = taskId.incrementAndGet();
        ListenableFuture<?> old = futures.get(type);
        if (old != null) old.cancel(true);
        FluentFuture<R> future = FluentFuture.from(Task.executor().submit(callable)).withTimeout(timeoutMs, TimeUnit.MILLISECONDS, Task.scheduler());
        futures.put(type, future);
        future.addCallback(Task.callback(
                result -> {
                    if (taskId.get() == currentId) onSuccess.accept(result);
                },
                error -> {
                    if (error instanceof CancellationException) return;
                    if (taskId.get() == currentId) onError.accept(error);
                }
        ), callbackExecutor);
    }

    /** 取消单个类型的任务：递增 taskId 让已排队的回调失效，再移除并 cancel future。 */
    void cancel(T type) {
        Objects.requireNonNull(taskIds.get(type)).incrementAndGet();
        ListenableFuture<?> future = futures.remove(type);
        if (future != null) future.cancel(true);
    }

    void cancelAll() {
        for (T type : taskIds.keySet()) cancel(type);
    }
}
