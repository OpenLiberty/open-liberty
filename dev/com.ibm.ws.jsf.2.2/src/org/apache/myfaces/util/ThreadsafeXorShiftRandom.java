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
package org.apache.myfaces.util;

import java.util.Random;

/**
 * A threadsafe implementation of {@link XorShiftRandom}.
 * We use a ThreadLocal to give each thread it's own implementation.
 */
public class ThreadsafeXorShiftRandom extends XorShiftRandom
{
    /**
     * Only used for creating the initial seed for each thread!
     *
     * We use a threadsafe java.util.Random for making sure that
     * each thread get it's very own initial seed value.
     *
     * If we would not do this then starting dozen Threads at the same time
     * would result in all having the same seed and thus generate
     * the same random numbers.
     */
    private static Random seedRandom = new Random(System.nanoTime());

    public ThreadsafeXorShiftRandom()
    {
        // seed is not used but created by the ThreadLocal
        super(0);
    }

    private static final ThreadLocal<XorShiftRandom> randomThreadLocal = new ThreadLocal<XorShiftRandom>()
    {
        @Override
        protected XorShiftRandom initialValue()
        {
            return new XorShiftRandom(seedRandom.nextLong());
        }
    };

    /**
     * We use the random generator for this very thread.
     * This method is perfectly threadsafe. It is also guaranteed
     * that each thread will get own values.
     */
    @Override
    public long random()
    {
        return randomThreadLocal.get().random();
    }
}
