/**
 * Copyright (c) 2013-2020 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.pubsub;

import org.redisson.PubSubEntry;
import org.redisson.api.RFuture;
import org.redisson.client.BaseRedisPubSubListener;
import org.redisson.client.ChannelName;
import org.redisson.client.RedisPubSubListener;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.pubsub.PubSubType;
import org.redisson.misc.RPromise;
import org.redisson.misc.RedissonPromise;
import org.redisson.misc.TransferListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Nikita Koksharov
 *
 */
abstract class PublishSubscribe<E extends PubSubEntry<E>> {

    private final PublishSubscribeService service;

    PublishSubscribe(PublishSubscribeService service) {
        super();
        this.service = service;
    }

    private final ConcurrentMap<String, E> entries = new ConcurrentHashMap<>();

    public void unsubscribe(E entry, String entryName, String channelName) {
        AsyncSemaphore semaphore = service.getSemaphore(new ChannelName(channelName));
        semaphore.acquire(new Runnable() {
            @Override
            public void run() {
                if (entry.release() == 0) {
                    // just an assertion
                    boolean removed = entries.remove(entryName) == entry;
                    if (!removed) {
                        throw new IllegalStateException();
                    }
                    service.unsubscribe(new ChannelName(channelName), semaphore);
                } else {
                    semaphore.release();
                }
            }
        });

    }

    //entryName = 机器uuid + ":" + lockName;
    //channelName = redisson_lock__channel_lockName
    public RFuture<E> subscribe(String entryName, String channelName) {
        AtomicReference<Runnable> listenerHolder = new AtomicReference<Runnable>();
        AsyncSemaphore            semaphore      = service.getSemaphore(new ChannelName(channelName));
        RPromise<E> newPromise = new RedissonPromise<E>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                System.out.println(Thread.currentThread().getName()+" 执行cancel不为空");
                return semaphore.remove(listenerHolder.get());
            }
        };

        Runnable listener = new Runnable() {

            @Override
            public void run() {
                E entry = entries.get(entryName);
                if (entry != null) {
                    entry.acquire();
                    semaphore.release();
                    entry.getPromise().onComplete(new TransferListener<E>(newPromise));
                    System.out.println(Thread.currentThread().getName()+" 执行entry不为空");
                    return;
                }
                // 订阅监听redis消息，并且创建RedissonLockEntry，其中RedissonLockEntry中比较关键的是一个 Semaphore属性对象用来控制本地的锁请求的信号量同步，返回的是netty框架的Future实现。
                E value = createEntry(newPromise);
                value.acquire();

                E oldValue = entries.putIfAbsent(entryName, value);
                if (oldValue != null) {
                    oldValue.acquire();
                    semaphore.release();
                    oldValue.getPromise().onComplete(new TransferListener<E>(newPromise));
                    System.out.println(Thread.currentThread().getName()+" 执行oldValue不为空");
                    return;
                }

                RedisPubSubListener<Object> listener = createListener(channelName, value);
                System.out.println(Thread.currentThread().getName()+" 订阅成功.");
                service.subscribe(LongCodec.INSTANCE, channelName, semaphore, listener);
            }
        };
        semaphore.acquire(listener);
        //将listener保存到listenerHolder容器当中、等到newPromise完成之后、从semaphore中删除这个listener
        listenerHolder.set(listener);

        return newPromise;
    }

    protected abstract E createEntry(RPromise<E> newPromise);

    protected abstract void onMessage(E value, Long message);

    private RedisPubSubListener<Object> createListener(String channelName, E value) {
        RedisPubSubListener<Object> listener = new BaseRedisPubSubListener() {

            @Override
            public void onMessage(CharSequence channel, Object message) {
                if (!channelName.equals(channel.toString())) {
                    return;
                }

                PublishSubscribe.this.onMessage(value, (Long) message);
            }

            @Override
            public boolean onStatus(PubSubType type, CharSequence channel) {
                if (!channelName.equals(channel.toString())) {
                    return false;
                }

                //此处执行到这里会表明连接建立完成、并且订阅完成、然后调用value.getPromise().trySuccess(value);表明promise完成
                //表明订阅完成
                if (type == PubSubType.SUBSCRIBE) {
                    value.getPromise().trySuccess(value);
                    return true;
                }
                return false;
            }

        };
        return listener;
    }

}
