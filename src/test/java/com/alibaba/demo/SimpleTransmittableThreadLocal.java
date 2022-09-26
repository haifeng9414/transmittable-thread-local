package com.alibaba.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

/**
 * @author donghaifeng <donghaifeng@kuaishou.com>
 * Created on 2022-09-22
 */
public class SimpleTransmittableThreadLocal {
    public static class SimpleThreadLocal {
        private static final ThreadLocal<Map<String, Object>> copyOnThreadLocal = new ThreadLocal<>();

        public static Map<String, Object> get() {
            return copyOnThreadLocal.get();
        }

        public static void put(Map<String, Object> val) {
            copyOnThreadLocal.set(val);
        }

        public static void remove() {
            copyOnThreadLocal.remove();
        }
    }

    public static class SimpleRunnable implements Runnable {

        private final Runnable runnable;
        private static Map<String, Object> threadLocal;

        public SimpleRunnable(Runnable runnable) {
            this.runnable = runnable;
            threadLocal = SimpleThreadLocal.get();
        }

        @Override
        public void run() {
            SimpleThreadLocal.put(threadLocal);
            runnable.run();
            SimpleThreadLocal.remove();
        }
    }

    @Test
    public void testSimpleThreadLocal() throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        map.put("UUID", UUID.randomUUID().toString().replace("-", ""));
        SimpleThreadLocal.put(map);
        System.out.println("主线程打印————" + Thread.currentThread().getName() + "：" + SimpleThreadLocal.get().get("UUID"));

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 4; i++) {
            executorService.execute(new SimpleRunnable(new Runnable() {
                @Override
                public void run() {
                    System.out.println(
                            "线程池打印————" + Thread.currentThread().getName() + "：" + SimpleThreadLocal.get().get("UUID"));
                }
            }));
        }

        Thread.sleep(3000L);
        System.out.println("主线程修改ThreadLocal的值");
        map = new HashMap<>();
        map.put("UUID", UUID.randomUUID().toString().replace("-", ""));
        SimpleThreadLocal.put(map);
        System.out.println("主线程打印————" + Thread.currentThread().getName() + "：" + SimpleThreadLocal.get().get("UUID"));
        try {
            for (int i = 0; i < 4; i++) {
                executorService.execute(new SimpleRunnable(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(
                                "线程池打印————" + Thread.currentThread().getName() + "：" + SimpleThreadLocal.get()
                                        .get("UUID"));
                    }
                }));
            }
        } finally {
            executorService.shutdown();
        }
    }
}
