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
            System.out.println("hello world");
            System.in.read();
//            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        lock.unlock();
    }

    public static void main(String[] args) throws IOException {
        init();
        for (int i = 0; i < 2; i++) {
            new Thread(new BaseTest()).start();
        }
        System.in.read();
    }

}
