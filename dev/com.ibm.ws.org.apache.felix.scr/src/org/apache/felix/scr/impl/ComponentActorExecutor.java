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
package org.apache.felix.scr.impl;


import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.logger.ScrLogger;


/**
 * The <code>ComponentActorExecutor</code> is the thread used to act upon registered
 * components of the service component runtime.
 * This is also used by the ComponentRegistry to schedule service.changecount updates.
 */
class ComponentActorExecutor extends ScheduledThreadPoolExecutor
{

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = new Thread(r, "SCR Component Actor");
            thread.setDaemon(true);
            return thread;
        }
    };

    private final ScrLogger logger;

    ComponentActorExecutor(final ScrLogger log )
    {
        super( 1, THREAD_FACTORY );
        logger = log;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r)
    {
        logger.log(Level.DEBUG, "Running task: {0}", null, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t)
    {
        if (t != null)
        {
            logger.log(Level.ERROR, "Unexpected problem executing task {0}", t, r);
        }
    }
}
