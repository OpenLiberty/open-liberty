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


import java.util.LinkedList;

import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.logger.ScrLogger;


/**
 * The <code>ComponentActorThread</code> is the thread used to act upon registered
 * components of the service component runtime.
 */
class ComponentActorThread implements Runnable
{

    // sentinel task to terminate this thread
    private static final Runnable TERMINATION_TASK = new Runnable()
    {
        @Override
        public void run()
        {
        }


        @Override
        public String toString()
        {
            return "Component Actor Terminator";
        }
    };

    // the queue of Runnable instances  to be run
    private final LinkedList<Runnable> tasks = new LinkedList<>();

    private final ScrLogger logger;


    ComponentActorThread( final ScrLogger log )
    {
        logger = log;
    }


    // waits on Runnable instances coming into the queue. As instances come
    // in, this method calls the Runnable.run method, logs any exception
    // happening and keeps on waiting for the next Runnable. If the Runnable
    // taken from the queue is this thread instance itself, the thread
    // terminates.
    @Override
    public void run()
    {
        logger.log(Level.DEBUG, "Starting ComponentActorThread", null);

        for ( ;; )
        {
            final Runnable task;
            synchronized ( tasks )
            {
                while ( tasks.isEmpty() )
                {
                    boolean interrupted = Thread.interrupted();
                    try
                    {
                        tasks.wait();
                    }
                    catch ( InterruptedException ie )
                    {
                        interrupted = true;
                        // don't care
                    }
                    finally
                    {
                        if (interrupted)
                        { // restore interrupt status
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                task = tasks.removeFirst();
            }

            try
            {
                // return if the task is this thread itself
                if ( task == TERMINATION_TASK )
                {
                    logger.log(Level.DEBUG, "Shutting down ComponentActorThread",
                        null);
                    return;
                }

                // otherwise execute the task, log any issues
                logger.log(Level.DEBUG, "Running task: " + task, null);
                task.run();
            }
            catch ( Throwable t )
            {
                logger.log(Level.ERROR, "Unexpected problem executing task " + task,
                    t);
            }
            finally
            {
                synchronized ( tasks )
                {
                    tasks.notifyAll();
                }
            }
        }
    }


    // cause this thread to terminate by adding this thread to the end
    // of the queue
    void terminate()
    {
        schedule( TERMINATION_TASK );
        synchronized ( tasks )
        {
            while ( !tasks.isEmpty() )
            {
                boolean interrupted = Thread.interrupted();
                try
                {
                    tasks.wait();
                }
                catch ( InterruptedException e )
                {
                    interrupted = true;
                    logger.log(Level.ERROR,
                        "Interrupted exception waiting for queue to empty", e);
                }
                finally
                {
                    if (interrupted)
                    { // restore interrupt status
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }


    // queue the given runnable to be run as soon as possible
    void schedule( Runnable task )
    {
        synchronized ( tasks )
        {
            // append to the task queue
            tasks.add( task );

            logger.log(Level.DEBUG, "Adding task [{0}] as #{1} in the queue", null,
                    task, tasks.size());

            // notify the waiting thread
            tasks.notifyAll();
        }
    }
}
