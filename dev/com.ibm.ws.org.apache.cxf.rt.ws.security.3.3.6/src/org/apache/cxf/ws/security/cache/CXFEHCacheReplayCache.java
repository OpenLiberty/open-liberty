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

package org.apache.cxf.ws.security.cache;

import java.nio.file.Path;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.wss4j.common.cache.EHCacheReplayCache;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * Wrap the default WSS4J EHCacheReplayCache in a BusLifeCycleListener, to make sure that
 * the cache is shutdown correctly.
 */
public class CXFEHCacheReplayCache extends EHCacheReplayCache implements BusLifeCycleListener {
    private final Bus bus;

    public CXFEHCacheReplayCache(String key, Bus bus, Path diskstorePath) throws WSSecurityException {
        super(key, diskstorePath);
        this.bus = bus;
        if (bus != null) {
            bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }
    }

    @Override
    public void close() {
        super.close();

        if (bus != null) {
            bus.getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
        }
    }
}
