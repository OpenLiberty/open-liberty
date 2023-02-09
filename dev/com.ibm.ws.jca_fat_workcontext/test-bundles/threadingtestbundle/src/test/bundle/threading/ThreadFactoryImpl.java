package test.bundle.threading;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryImpl implements ThreadFactory {
    private final AtomicInteger createdThreadCount = new AtomicInteger();
    private final ThreadGroup threadGroup;
    private final ClassLoader contextClassLoader;

    public ThreadFactoryImpl() {
        this.threadGroup = AccessController.doPrivileged(new PrivilegedAction<ThreadGroup>() {
            @Override
            public ThreadGroup run() {
                return new ThreadGroup("testThreadGroup");
            }
        });
        this.contextClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        int threadId = createdThreadCount.incrementAndGet();
        final String name = "com.ibm.ws.threading_fat_ThreadFactoryImpl-thread-" + threadId;
        // The AccessControlContext is implicitly copied from the creating
        // thread, so use doPrivileged to prevent that.
        return AccessController.doPrivileged(new PrivilegedAction<Thread>() {
            @Override
            public Thread run() {
                Thread thread = new Thread(threadGroup, runnable, name);
                // The daemon, priority, and context class loader are implicitly
                // copied from the creating thread, so reset them all.
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.setContextClassLoader(contextClassLoader);
                return thread;
            }
        });
    }
}