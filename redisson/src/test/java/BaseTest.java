import io.netty.util.concurrent.*;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;

/**
 * @author chengzhengzheng
 * @date 2020/5/27
 */
public class BaseTest implements Runnable {
    private static RedissonClient redisson;


    private static void init() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setDatabase(10);
        redisson = Redisson.create(config);
    }

    @Override
    public void run() {
        RLock lock = redisson.getLock("anyLock");
        lock.lock();
        try {

            System.out.println(Thread.currentThread().getName() + " hello world");
            Thread.sleep(100000000);
            lock.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException {
//        testPromise();
        init();
        for (int i = 0; i < 1; i++) {
            new Thread(new BaseTest()).start();
        }
        System.in.read();


    }

    private static void testPromise() {
        // 构造线程池
        EventExecutor executor = new DefaultEventExecutor();
        // 创建 DefaultPromise 实例
        Promise promise = executor.newPromise();


        // 下面给这个 promise 添加两个 listener
        promise.addListener(new GenericFutureListener<Future<Integer>>() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("任务结束，结果：" + future.get());
                } else {
                    System.out.println("任务失败，异常：" + future.cause());
                }
            }
        }).addListener(new GenericFutureListener<Future<Integer>>() {
            @Override
            public void operationComplete(Future future) throws Exception {
                System.out.println("任务结束，balabala...");
            }
        });

        // 提交任务到线程池，五秒后执行结束，设置执行 promise 的结果
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                // 设置 promise 的结果
                // promise.setFailure(new RuntimeException());
                promise.setSuccess(123456);
//                promise.setFailure(new RuntimeException("出错啦"));
            }
        });

        // main 线程阻塞等待执行结果
        try {
            promise.sync();
        } catch (InterruptedException e) {
        }
    }

}
