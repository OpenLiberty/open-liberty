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

/**
 * A simple XORShift Random generator.
 * This class is NOT synchronized, you need to do that yourself if needed!
 */
public class XorShiftRandom
{
    private long value;

    public XorShiftRandom( long initialSeed )
    {
        value = initialSeed;
    }

    /**
     * Calculate a random value based on the previous value stored in this instance.
     * If you use this method in a concurring way, you need to synchronize the access!
     * @return
     */
    public long random()
    {
        value = random(value);
        return value;
    }

    /**
     * Calculate a random value based on the given start Value.
     * @param randomStart the start value. Usually the result of the previous invocation
     * @return the new random value.
     */
    public static long random(final long randomStart)
    {
        long randomValue = randomStart;
        randomValue ^= (randomValue << 21);
        randomValue ^= (randomValue >>> 35);
        randomValue ^= (randomValue << 4);

        return randomValue;
    }
}
