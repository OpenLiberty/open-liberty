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
package org.apache.myfaces.shared.context;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.SystemEvent;

/**
 * This wrapper is a switch to choose in a lazy way between ajax and
 * normal exceptionHandler wrapping, because FacesContext is initialized after
 * ExceptionHandler, so it is not safe to get it when
 * ExceptionHandlerFactory.getExceptionHandler() is called.
 */
public class SwitchAjaxExceptionHandlerWrapperImpl extends ExceptionHandlerWrapper
{
    private ExceptionHandler _requestExceptionHandler;
    private ExceptionHandler _ajaxExceptionHandler;
    private Boolean _isAjaxRequest;
    
    public SwitchAjaxExceptionHandlerWrapperImpl(
            ExceptionHandler requestExceptionHandler,
            ExceptionHandler ajaxExceptionHandler)
    {
        _requestExceptionHandler = requestExceptionHandler;
        _ajaxExceptionHandler = ajaxExceptionHandler;
    }
    
    @Override
    public void processEvent(SystemEvent exceptionQueuedEvent)
            throws AbortProcessingException
    {
        //Check if this is an ajax request, but take advantage of exceptionQueuedEvent facesContext
        isAjaxRequest(exceptionQueuedEvent);
        super.processEvent(exceptionQueuedEvent);
    }

    protected boolean isAjaxRequest(SystemEvent exceptionQueuedEvent)
    {
        if (_isAjaxRequest == null)
        {
            if (exceptionQueuedEvent instanceof ExceptionQueuedEvent)
            {
                ExceptionQueuedEvent eqe = (ExceptionQueuedEvent)exceptionQueuedEvent;
                ExceptionQueuedEventContext eqec = eqe.getContext();
                if (eqec != null)
                {
                    FacesContext facesContext = eqec.getContext();
                    if (facesContext != null)
                    {
                        return isAjaxRequest(facesContext);
                    }
                }
            }
            return isAjaxRequest();
        }
        return _isAjaxRequest;
    }
    
    protected boolean isAjaxRequest(FacesContext facesContext)
    {
        if (_isAjaxRequest == null)
        {
            facesContext = (facesContext == null) ? FacesContext.getCurrentInstance() : facesContext;
            PartialViewContext pvc = facesContext.getPartialViewContext();
            if (pvc == null)
            {
                return false;
            }
            _isAjaxRequest = pvc.isAjaxRequest();
        }
        return _isAjaxRequest;
    }
    
    protected boolean isAjaxRequest()
    {
        if (_isAjaxRequest == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            PartialViewContext pvc = facesContext.getPartialViewContext();
            if (pvc == null)
            {
                return false;
            }
            _isAjaxRequest = pvc.isAjaxRequest();
        }
        return _isAjaxRequest;
    }
    
    @Override
    public ExceptionHandler getWrapped()
    {
        if (isAjaxRequest())
        {
            return _ajaxExceptionHandler;
        }
        else
        {
            return _requestExceptionHandler;
        }
    }
}
