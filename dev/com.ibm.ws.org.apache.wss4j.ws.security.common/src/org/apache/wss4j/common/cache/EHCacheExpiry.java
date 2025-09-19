/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.ehcache.expiry.ExpiryPolicy;

/**
 * A custom Expiry implementation for EhCache. It uses the supplied expiry which is part of the cache value.
 * If it doesn't exist, it falls back to the default value (3600 seconds).
 */
public class EHCacheExpiry implements ExpiryPolicy<String, EHCacheValue> {

    /**
     * The default time to live in seconds (60 minutes)
     */
    public long DEFAULT_TTL = 3600L;  // Liberty Change

    /**
     * The max time to live in seconds (12 hours)
     */
    public long MAX_TTL = DEFAULT_TTL * 12L;   // Liberty Change


    @Override
    public Duration getExpiryForCreation(String s, EHCacheValue ehCacheValue) {
        Instant expiry = ehCacheValue.getExpiry();
        Instant now = Instant.now();

        if (expiry == null || expiry.isBefore(now) || expiry.isAfter(now.plusSeconds(MAX_TTL))) {
            return Duration.of(DEFAULT_TTL, ChronoUnit.SECONDS);
        }

        return Duration.of(expiry.toEpochMilli() - now.toEpochMilli(), ChronoUnit.MILLIS);
    }

    @Override
    public Duration getExpiryForAccess(String s, Supplier<? extends EHCacheValue> supplier) {
        return null;
    }

    @Override
    public Duration getExpiryForUpdate(String s, Supplier<? extends EHCacheValue> supplier, EHCacheValue ehCacheValue) {
        return null;
    }

 // Liberty Change
    public void setDefaultTTL(long ttl) {
        DEFAULT_TTL = ttl;
    }
    public void setMaxTTL(long ttl) {
        MAX_TTL = ttl;
    }
 // End Liberty Change

}
