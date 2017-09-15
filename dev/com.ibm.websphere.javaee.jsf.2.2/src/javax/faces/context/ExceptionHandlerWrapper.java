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
package javax.faces.context;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.SystemEvent;

/**
 * @since 2.0
 */
public abstract class ExceptionHandlerWrapper extends ExceptionHandler implements FacesWrapper<ExceptionHandler>
{
    @Override
    public ExceptionQueuedEvent getHandledExceptionQueuedEvent()
    {
        return getWrapped().getHandledExceptionQueuedEvent();
    }

    @Override
    public Iterable<ExceptionQueuedEvent> getHandledExceptionQueuedEvents()
    {
        return getWrapped().getHandledExceptionQueuedEvents();
    }

    @Override
    public Throwable getRootCause(Throwable t)
    {
        return getWrapped().getRootCause(t);
    }

    @Override
    public Iterable<ExceptionQueuedEvent> getUnhandledExceptionQueuedEvents()
    {
        return getWrapped().getUnhandledExceptionQueuedEvents();
    }

    public abstract ExceptionHandler getWrapped();

    @Override
    public void handle() throws FacesException
    {
        getWrapped().handle();
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        return getWrapped().isListenerForSource(source);
    }

    @Override
    public void processEvent(SystemEvent exceptionQueuedEvent) throws AbortProcessingException
    {
        getWrapped().processEvent(exceptionQueuedEvent);
    }
}
