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
package org.apache.myfaces.lifecycle;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author lu4242
 */
class TokenGenerator
{
    private final AtomicLong _count;
    
    public TokenGenerator()
    {
        _count = new AtomicLong(_getSeed());
    }
    
    private static long _getSeed()
    {
        SecureRandom rng;
        try
        {
            // try SHA1 first
            rng = SecureRandom.getInstance("SHA1PRNG");
        }
        catch (NoSuchAlgorithmException e)
        {
            // SHA1 not present, so try the default (which could potentially not be
            // cryptographically secure)
            rng = new SecureRandom();
        }

        // use 48 bits for strength and fill them in
        byte[] randomBytes = new byte[6];
        rng.nextBytes(randomBytes);

        // convert to a long
        return new BigInteger(randomBytes).longValue();
    }
    
    /**
     * Get the next token to be assigned to this request
     * 
     * @return
     */
    String _getNextToken()
    {
        // atomically increment the value
        long nextToken = _count.incrementAndGet();

        // convert using base 36 because it is a fast efficient subset of base-64
        return Long.toString(nextToken, 36);
    }
}
