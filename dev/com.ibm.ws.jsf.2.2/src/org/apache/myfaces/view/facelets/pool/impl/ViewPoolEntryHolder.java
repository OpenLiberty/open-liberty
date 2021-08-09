/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.pool.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.myfaces.view.facelets.pool.ViewEntry;

/**
 * Fast pool using ConcurrentLinkedQueue, with uses an AtomicInteger as 
 * count limit. The reasons of design this pool in this way are:
 * 
 * <ol>
 * <li>There is no need to put a hard limit about the max number of views stored
 * in the pool. Remember ViewEntry internally has a Soft or Weak reference over
 * the view. The maxCount is just a way to limit the max footprint fo the pool
 * in memory, but if the limit is exceed, the vm can always reclaim the memory space.</li>
 * <li>View creation is quite fast, so according to previous tests done,
 * include any syncronized method in this code will produce worse performance.</li>
 * </ol>
 *
 * @author Leonardo Uribe
 */
public class ViewPoolEntryHolder
{
    private Queue<ViewEntry> queue;
    
    private AtomicInteger count;
    
    private int maxCount;
    
    public ViewPoolEntryHolder(int maxCount)
    {
        this.queue = new ConcurrentLinkedQueue<ViewEntry>();
        this.count = new AtomicInteger();
        this.maxCount = maxCount;
    }
    
    public boolean add(ViewEntry entry)
    {
        if (count.get() < maxCount)
        {
            queue.add(entry);
            count.incrementAndGet();
            return true;
        }
        return false;
    }
    
    public ViewEntry poll()
    {
        ViewEntry entry = queue.poll();
        count.decrementAndGet();
        return entry;
    }
    
    public boolean isFull()
    {
        return count.get() >= maxCount;
    }
    
    public int getCount()
    {
        return count.get();
    }
}
