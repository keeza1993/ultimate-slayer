package com.imth3keeza0.ultimateslayer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** One bounded serial queue across controller lifetimes; queued clears precede later loads. */
final class LocalSnapshotIo
{
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(64), action -> {
            Thread thread = new Thread(action, "ultimate-slayer-local-snapshots"); thread.setDaemon(true); return thread;
        });
    static { WORKER.allowCoreThreadTimeOut(true); }
    private LocalSnapshotIo() {}
    static boolean submit(Runnable action)
    {
        try { WORKER.execute(action); return true; }
        catch (RejectedExecutionException e) { return false; }
    }
}
