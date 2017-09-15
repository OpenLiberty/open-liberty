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
package org.apache.myfaces.context.servlet;

import java.util.Iterator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;

import org.apache.myfaces.context.ReleaseableExternalContext;

/**
 * A FacesContext implementation which will be set as the current instance
 * during container startup and shutdown and which provides a basic set of
 * FacesContext functionality.
 * 
 * @author Jakob Korherr (latest modification by $Author: jakobk $)
 * @version $Revision: 963629 $ $Date: 2010-07-13 09:29:07 +0000 (Tue, 13 Jul 2010) $
 */
public class StartupFacesContextImpl extends FacesContextImplBase
{
    
    public static final String EXCEPTION_TEXT = "This method is not supported during ";

    private boolean _startup;
    
    public StartupFacesContextImpl(
            ExternalContext externalContext, 
            ReleaseableExternalContext defaultExternalContext,
            ExceptionHandler exceptionHandler,
            boolean startup)
    {
        // setCurrentInstance is called in constructor of super class
        super(externalContext, defaultExternalContext);
        
        _startup = startup;
        setExceptionHandler(exceptionHandler);
    }

    // ~ Methods which are valid by spec to be called during startup and shutdown------
    
    // public final UIViewRoot getViewRoot() implemented in super-class
    // public void release() implemented in super-class
    // public final ExternalContext getExternalContext() implemented in super-class
    // public Application getApplication() implemented in super-class
    // public boolean isProjectStage(ProjectStage stage) implemented in super-class
    
    // ~ Methods which can be called during startup and shutdown, but are not
    //   officially supported by the spec--------------------------------------
    
    // all other methods on FacesContextImplBase
    
    // ~ Methods which are unsupported during startup and shutdown-------------

    @Override
    public final FacesMessage.Severity getMaximumSeverity()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
    
    @Override
    public List<FacesMessage> getMessageList()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public List<FacesMessage> getMessageList(String clientId)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final Iterator<FacesMessage> getMessages()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
    
    
    @Override
    public final Iterator<String> getClientIdsWithMessages()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final Iterator<FacesMessage> getMessages(final String clientId)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final void addMessage(final String clientId, final FacesMessage message)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public PartialViewContext getPartialViewContext()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
    
    @Override
    public boolean isPostback()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
    
    @Override
    public void validationFailed()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public boolean isValidationFailed()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final void renderResponse()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final void responseComplete()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public PhaseId getCurrentPhaseId()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
   
    @Override
    public void setCurrentPhaseId(PhaseId currentPhaseId)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
        
    @Override
    public final boolean getRenderResponse()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final boolean getResponseComplete()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final void setResponseStream(final ResponseStream responseStream)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final ResponseStream getResponseStream()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final void setResponseWriter(final ResponseWriter responseWriter)
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }

    @Override
    public final ResponseWriter getResponseWriter()
    {
        assertNotReleased();
        throw new UnsupportedOperationException(EXCEPTION_TEXT + _getTime());
    }
    
    // ~ private Methods ------------------------------------------------------
    
    /**
     * Returns startup or shutdown as String according to the field _startup.
     * @return
     */
    private String _getTime()
    {
        return _startup ? "startup" : "shutdown";
    }
    
}
