/*******************************************************************************
 * Copyright (c) 2018, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
//This file is based on https://github.com/eclipse/microprofile-reactive-streams-operators/blob/3.0/tck/src/main/java/org/eclipse/microprofile/reactive/streams/operators/tck/arquillian/ReactiveStreamsCdiTck.java
//It has been modified to
//- allow the ScheduledExecutorService to be injected
//- use the TestEnvironment(boolean) constructor to work around a bug with default polling timeouts

package org.eclipse.microprofile.reactive.streams.operators.tck.arquillian;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.tck.ReactiveStreamsTck;
import org.reactivestreams.tck.TestEnvironment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;

@ApplicationScoped
public class ReactiveStreamsCdiTck extends ReactiveStreamsTck<ReactiveStreamsEngine> {

    public ReactiveStreamsCdiTck() {
        super(new TestEnvironment(false));
    }

    @Inject
    private ReactiveStreamsEngine engine;
    
    @Resource
    private ScheduledExecutorService executorService;

    @Override
    protected ReactiveStreamsEngine createEngine() {
        return engine;
    }
    
    @Override
    protected ScheduledExecutorService createExecutorService() {
        return executorService;
    }
    
    @Override
    protected void shutdownExecutorService(ScheduledExecutorService executorService) {
        //no-op
    }
}