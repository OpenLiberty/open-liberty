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
package org.apache.cxf.jaxrs.sse.client;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

public class SseEventSourceBuilderImpl extends SseEventSource.Builder {
    private long delay = SseEvent.RECONNECT_NOT_SET;
    private TimeUnit unit = TimeUnit.MILLISECONDS;
    private WebTarget target;
    
    @Override
    public SseEventSource build() {
        return new SseEventSourceImpl(target, delay, unit);
    }

    @Override
    public Builder reconnectingEvery(long tdelay, TimeUnit tunit) {
        this.delay = tdelay;
        this.unit = tunit;
        return this;
    }

    @Override
    protected Builder target(WebTarget tgt) {
        this.target = tgt;
        return this;
    }
}
