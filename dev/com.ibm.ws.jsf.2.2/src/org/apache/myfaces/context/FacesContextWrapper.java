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
package org.apache.myfaces.context;

import javax.el.ELContext;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Convenient class to wrap the current FacesContext.
 * 
 * @author Manfred Geiler (latest modification by $Author: bommel $)
 * @author Anton Koinov
 * @version $Revision: 1187701 $ $Date: 2011-10-22 12:21:54 +0000 (Sat, 22 Oct 2011) $
 */
public class FacesContextWrapper extends FacesContext
{
    // ~ Instance fields ----------------------------------------------------------------------------

    private FacesContext _facesContext;

    // ~ Constructors -------------------------------------------------------------------------------

    public FacesContextWrapper(FacesContext facesContext)
    {
        _facesContext = facesContext;
    }

    // ~ Methods ------------------------------------------------------------------------------------

    @Override
    public Application getApplication()
    {
        return _facesContext.getApplication();
    }

    @Override
    public Map<Object,Object> getAttributes()
    {
        return _facesContext.getAttributes();
    }

    @Override
    public Iterator<String> getClientIdsWithMessages()
    {
        return _facesContext.getClientIdsWithMessages();
    }

    @Override
    public PhaseId getCurrentPhaseId()
    {
        return _facesContext.getCurrentPhaseId();
    }
    
    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return _facesContext.getExceptionHandler();
    }

    @Override
    public ExternalContext getExternalContext()
    {
        return _facesContext.getExternalContext();
    }

    @Override
    public Severity getMaximumSeverity()
    {
        return _facesContext.getMaximumSeverity();
    }

    @Override
    public List<FacesMessage> getMessageList()
    {
        return _facesContext.getMessageList();
    }

    @Override
    public List<FacesMessage> getMessageList(String clientId)
    {
        return _facesContext.getMessageList(clientId);
    }

    @Override
    public Iterator<FacesMessage> getMessages()
    {
        return _facesContext.getMessages();
    }

    @Override
    public Iterator<FacesMessage> getMessages(String clientId)
    {
        return _facesContext.getMessages(clientId);
    }

    @Override
    public PartialViewContext getPartialViewContext()
    {
        return _facesContext.getPartialViewContext();
    }

    @Override
    public RenderKit getRenderKit()
    {
        return _facesContext.getRenderKit();
    }

    @Override
    public boolean getRenderResponse()
    {
        return _facesContext.getRenderResponse();
    }

    @Override
    public boolean getResponseComplete()
    {
        return _facesContext.getResponseComplete();
    }

    @Override
    public void setExceptionHandler(ExceptionHandler exceptionHandler)
    {
        _facesContext.setExceptionHandler(exceptionHandler);
    }

    @Override
    public void setResponseStream(ResponseStream responsestream)
    {
        _facesContext.setResponseStream(responsestream);
    }

    @Override
    public ResponseStream getResponseStream()
    {
        return _facesContext.getResponseStream();
    }

    @Override
    public void setResponseWriter(ResponseWriter responsewriter)
    {
        _facesContext.setResponseWriter(responsewriter);
    }

    @Override
    public ResponseWriter getResponseWriter()
    {
        return _facesContext.getResponseWriter();
    }

    @Override
    public void setViewRoot(UIViewRoot viewRoot)
    {
        _facesContext.setViewRoot(viewRoot);
    }

    @Override
    public UIViewRoot getViewRoot()
    {
        return _facesContext.getViewRoot();
    }
    
    @Override
    public boolean isPostback()
    {
        return _facesContext.isPostback();
    }

    @Override
    public void addMessage(String clientId, FacesMessage message)
    {
        _facesContext.addMessage(clientId, message);
    }

    @Override
    public void release()
    {
        _facesContext.release();
    }

    @Override
    public void renderResponse()
    {
        _facesContext.renderResponse();
    }

    @Override
    public void responseComplete()
    {
        _facesContext.responseComplete();
    }
    
    @Override
    public ELContext getELContext()
    {
        return _facesContext.getELContext();
    }
    
    @Override
    public void setCurrentPhaseId(PhaseId currentPhaseId)
    {
        _facesContext.setCurrentPhaseId(currentPhaseId);
    }
}
