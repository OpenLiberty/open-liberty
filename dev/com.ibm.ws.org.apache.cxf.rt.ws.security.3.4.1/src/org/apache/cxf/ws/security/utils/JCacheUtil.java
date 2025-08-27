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
package org.apache.cxf.ws.security.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public final class JCacheUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(JCacheUtil.class);
    private static final boolean JCACHE_INSTALLED;

    static {
        boolean jcacheInstalled = false;
        try {
            final Class<?> caching = Class.forName("javax.cache.Caching");
            final Class<?> cachingProvider = Class.forName("javax.cache.spi.CachingProvider");
            if (caching != null) {
                jcacheInstalled = MethodHandles
                    .publicLookup()
                    .findStatic(caching, "getCachingProvider", MethodType.methodType(cachingProvider))
                    .invoke() != null;
            }
        } catch (Throwable e) {
            LOG.fine("No JCache SPIs detected on classpath: " + e.getMessage());
        }
        JCACHE_INSTALLED = jcacheInstalled;
    }

    private JCacheUtil() {
    }

    public static boolean isJCacheInstalled() {
        return JCACHE_INSTALLED;
    }

}
