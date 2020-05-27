/**
 * Copyright (c) 2013-2020 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson.api;

import java.util.List;
import java.util.concurrent.TransferQueue;

/**
 * Redis based implementation of {@link java.util.concurrent.TransferQueue}
 *
 * @author Nikita Koksharov
 *
 */
public interface RTransferQueue<V> extends TransferQueue<V>, RBlockingQueue<V>, RTransferQueueAsync<V> {

    /**
     * Returns all queue elements at once
     *
     * @return elements
     */
    List<V> readAll();

}
