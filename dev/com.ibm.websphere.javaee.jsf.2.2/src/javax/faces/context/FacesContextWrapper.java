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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.faces.FacesWrapper;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;

/**
 * @since 2.0
 */
public abstract class FacesContextWrapper extends FacesContext implements FacesWrapper<FacesContext>
{
    @Override
    public void addMessage(String clientId, FacesMessage message)
    {
        getWrapped().addMessage(clientId, message);
    }

    @Override
    public Application getApplication()
    {
        return getWrapped().getApplication();
    }

    @Override
    public Map<Object, Object> getAttributes()
    {
        return getWrapped().getAttributes();
    }

    @Override
    public Iterator<String> getClientIdsWithMessages()
    {
        return getWrapped().getClientIdsWithMessages();
    }

    @Override
    public PhaseId getCurrentPhaseId()
    {
        return getWrapped().getCurrentPhaseId();
    }

    @Override
    public ELContext getELContext()
    {
        return getWrapped().getELContext();
    }

    @Override
    public ExceptionHandler getExceptionHandler()
    {
        return getWrapped().getExceptionHandler();
    }

    @Override
    public ExternalContext getExternalContext()
    {
        return getWrapped().getExternalContext();
    }

    @Override
    public Severity getMaximumSeverity()
    {
        return getWrapped().getMaximumSeverity();
    }

    @Override
    public List<FacesMessage> getMessageList()
    {
        return getWrapped().getMessageList();
    }

    @Override
    public List<FacesMessage> getMessageList(String clientId)
    {
        return getWrapped().getMessageList(clientId);
    }

    @Override
    public Iterator<FacesMessage> getMessages()
    {
        return getWrapped().getMessages();
    }

    @Override
    public Iterator<FacesMessage> getMessages(String clientId)
    {
        return getWrapped().getMessages(clientId);
    }

    @Override
    public PartialViewContext getPartialViewContext()
    {
        return getWrapped().getPartialViewContext();
    }

    @Override
    public RenderKit getRenderKit()
    {
        return getWrapped().getRenderKit();
    }

    @Override
    public boolean getRenderResponse()
    {
        return getWrapped().getRenderResponse();
    }

    @Override
    public boolean getResponseComplete()
    {
        return getWrapped().getResponseComplete();
    }

    @Override
    public ResponseStream getResponseStream()
    {
        return getWrapped().getResponseStream();
    }

    @Override
    public ResponseWriter getResponseWriter()
    {
        return getWrapped().getResponseWriter();
    }

    @Override
    public boolean isValidationFailed()
    {
        return getWrapped().isValidationFailed();
    }

    @Override
    public UIViewRoot getViewRoot()
    {
        return getWrapped().getViewRoot();
    }

    public abstract FacesContext getWrapped();

    @Override
    public boolean isPostback()
    {
        return getWrapped().isPostback();
    }

    @Override
    public boolean isProcessingEvents()
    {
        return getWrapped().isProcessingEvents();
    }

    @Override
    public void release()
    {
        getWrapped().release();
    }

    @Override
    public void renderResponse()
    {
        getWrapped().renderResponse();
    }

    @Override
    public void responseComplete()
    {
        getWrapped().responseComplete();
    }

    @Override
    public void setCurrentPhaseId(PhaseId currentPhaseId)
    {
        getWrapped().setCurrentPhaseId(currentPhaseId);
    }

    @Override
    public void setExceptionHandler(ExceptionHandler exceptionHandler)
    {
        getWrapped().setExceptionHandler(exceptionHandler);
    }

    @Override
    public void setProcessingEvents(boolean processingEvents)
    {
        getWrapped().setProcessingEvents(processingEvents);
    }

    @Override
    public void setResponseStream(ResponseStream responseStream)
    {
        getWrapped().setResponseStream(responseStream);
    }

    @Override
    public void setResponseWriter(ResponseWriter responseWriter)
    {
        getWrapped().setResponseWriter(responseWriter);
    }

    @Override
    public void setViewRoot(UIViewRoot root)
    {
        getWrapped().setViewRoot(root);
    }

    @Override
    public void validationFailed()
    {
        getWrapped().validationFailed();
    }

    @Override
    public boolean isProjectStage(ProjectStage stage)
    {
        return getWrapped().isProjectStage(stage);
    }

    @Override
    public boolean isReleased()
    {
        return getWrapped().isReleased();
    }

    @Override
    public char getNamingContainerSeparatorChar()
    {
        return getWrapped().getNamingContainerSeparatorChar();
    }

    @Override
    public void setResourceLibraryContracts(List<String> contracts)
    {
        getWrapped().setResourceLibraryContracts(contracts);
    }

    @Override
    public List<String> getResourceLibraryContracts()
    {
        return getWrapped().getResourceLibraryContracts();
    }

}
