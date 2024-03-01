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

package org.apache.cxf.workqueue;

import java.util.concurrent.Executor;

/**
 * Used to mimic Executor semantics by hijacking calling thread.
 * <b>Must</b> be used for Service.getExecutor() return unless a real
 * (i.e. truely asynchronous) Executor is available.
 */
public final class SynchronousExecutor implements Executor {

    private static final SynchronousExecutor INSTANCE = new SynchronousExecutor();

    private SynchronousExecutor() {
    }

    public void execute(Runnable command) {
        command.run();
    }

    public static SynchronousExecutor getInstance() {
        return INSTANCE;
    }

    public static boolean isA(Executor executor) {
        return executor == INSTANCE;
    }
}
