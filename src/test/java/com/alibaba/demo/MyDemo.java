package com.alibaba.demo;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;

/**
 * @author donghaifeng <donghaifeng@kuaishou.com>
 * Created on 2022-09-18
 */
public class MyDemo {
    private static final TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();


    @Test
    public void testInheritableThreadLocal() throws InterruptedException {
        ThreadLocal<String> threadLocal = new InheritableThreadLocal<>();
        threadLocal.set(UUID.randomUUID().toString().replace("-", ""));
        System.out.println("主线程打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("子线程打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
            }
        }).start();

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 4; i++) {
            executorService.execute(() -> {
                System.out.println("线程池打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
            });
        }

        Thread.sleep(3000L);
        System.out.println("主线程修改ThreadLocal的值");
        threadLocal.remove();
        threadLocal.set(UUID.randomUUID().toString().replace("-", ""));
        System.out.println("主线程打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("子线程打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
            }
        }).start();
        try {
            for (int i = 0; i < 4; i++) {
                executorService.execute(() -> {
                    System.out.println("线程池打印————" + Thread.currentThread().getName() + "：" + threadLocal.get());
                });
            }
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testTtlRunnable() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Runnable task = new RunnableTask();

        // 在父线程中设置
        context.set("init-value");
        executorService.submit(TtlRunnable.get(task));

        // 在父线程中设置
        context.set("value-set-in-parent");
        executorService.submit(TtlRunnable.get(task));
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testTtlRunnableCapture() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // 在父线程中设置
        context.set("init-value");
        executorService.submit(TtlRunnable.get(() -> {
            System.out.println("Runnable A");
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        executorService.submit(TtlRunnable.get(() -> {
            System.out.println("Runnable B");
            System.out.println(context.get());
        }));
        // 提交Runnable A后马上提交Runnable B，再更新父线程的值，Runnable B拿到的还是submit的那一时刻的值
        context.set("value-set-in-parent");
        // Runnable C能拿到submit那一时刻的值
        executorService.submit(TtlRunnable.get(() -> {
            System.out.println("Runnable C");
            System.out.println(context.get());
        }));
        System.out.println("await termination");
        executorService.awaitTermination(11, TimeUnit.SECONDS);
    }

    @Test
    public void testExecutorService() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Runnable task = new RunnableTask();

        // 在父线程中设置
        context.set("init-value");
        executorService.submit(task);

        // 在父线程中设置
        context.set("value-set-in-parent");
        // 普通的runnable拿不到父进程更新的值
        executorService.submit(task);
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testTtlExecutors() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService = TtlExecutors.getTtlExecutorService(executorService);
        Runnable task = new RunnableTask();

        // 在父线程中设置
        context.set("init-value");
        executorService.submit(task);

        // 在父线程中设置
        context.set("value-set-in-parent");
        executorService.submit(task);
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void test() throws InterruptedException {
        TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService = TtlExecutors.getTtlExecutorService(executorService);
        // 在父线程中设置
        context.set("init-value");
        executorService.submit(() -> {
            System.out.println("context: " + context.get());
        });

        // 在父线程中设置
        context.set("value-set-in-parent");
        context.remove();
        TimeUnit.SECONDS.sleep(1);
        executorService.submit(() -> {
            System.out.println("context: " + context.get());
        });
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    public static class RunnableTask implements Runnable {
        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(context.get());
        }
    }
}
