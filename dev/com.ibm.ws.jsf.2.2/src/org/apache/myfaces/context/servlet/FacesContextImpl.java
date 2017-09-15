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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FactoryFinder;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextFactory;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.context.ReleaseableFacesContextFactory;
import org.apache.myfaces.shared.util.NullIterator;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 * @version $Revision: 1524497 $ $Date: 2013-09-18 17:25:54 +0000 (Wed, 18 Sep 2013) $
 */
public class FacesContextImpl extends FacesContextImplBase
{

    private static final String METHOD_RESPONSEWRITER = "responseWriter";
    static final String RE_SPLITTER = "[\\s\\t\\r\\n]*\\,[\\s\\t\\r\\n]*";
    
    // ~ Instance fields ----------------------------------------------------------------------------
    
    private Map<String, List<FacesMessage>> _messages = null;
    private List<FacesMessage> _orderedMessages = null;
    private PhaseId _currentPhaseId;
    private ResponseStream _responseStream = null;
    private ResponseWriter _responseWriter = null;
    private FacesMessage.Severity _maximumSeverity = null;
    private boolean _renderResponse = false;
    private boolean _responseComplete = false;
    private boolean _validationFailed = false;
    private PartialViewContext _partialViewContext = null;
    private ReleaseableFacesContextFactory _facesContextFactory = null;
    
    private PartialViewContextFactory _partialViewContextFactory = null;
    private RenderKitFactory _renderKitFactory = null;

    // ~ Constructors -------------------------------------------------------------------------------
    
    /**
     * Creates a FacesContextImpl with a ServletExternalContextImpl.
     */
    public FacesContextImpl(final ServletContext servletContext, final ServletRequest servletRequest,
                            final ServletResponse servletResponse)
    {
        this(new ServletExternalContextImpl(servletContext, servletRequest, servletResponse));
    }
    
    /**
     * Private constructor used in internal construtor chain.
     * @param externalContext
     */
    private FacesContextImpl(ServletExternalContextImpl externalContext)
    {
        this(externalContext, externalContext, null);
    }
    
    /**
     * Creates a FacesContextImpl with the given ExternalContext,
     * ReleaseableExternalContext and ReleaseableFacesContextFactory.
     * @param externalContext
     * @param defaultExternalContext
     * @param facesContextFactory
     */
    public FacesContextImpl(final ExternalContext externalContext,
            final ReleaseableExternalContext defaultExternalContext , 
            final ReleaseableFacesContextFactory facesContextFactory)
    {
        // setCurrentInstance is called in constructor of super class
        super(externalContext, defaultExternalContext);
        
        _facesContextFactory = facesContextFactory;
    }
    
    public FacesContextImpl(final ExternalContext externalContext,
            final ReleaseableExternalContext defaultExternalContext , 
            final ReleaseableFacesContextFactory facesContextFactory,
            final ApplicationFactory applicationFactory,
            final RenderKitFactory renderKitFactory,
            final PartialViewContextFactory partialViewContextFactory)
    {
        // setCurrentInstance is called in constructor of super class
        super(externalContext, defaultExternalContext, applicationFactory, 
                renderKitFactory);
        
        _facesContextFactory = facesContextFactory;
        _renderKitFactory = renderKitFactory;
        _partialViewContextFactory = partialViewContextFactory;
    }

    // ~ Methods ------------------------------------------------------------------------------------
    
    @Override
    public final void release()
    {
        assertNotReleased();

        _messages = null;
        _orderedMessages = null;
        _currentPhaseId = null;
        _responseStream = null;
        _responseWriter = null;
        _maximumSeverity = null;
        _partialViewContext = null;
        _renderKitFactory = null;
        _partialViewContextFactory = null;

        if (_facesContextFactory != null)
        {
            _facesContextFactory.release();
            _facesContextFactory = null;
        }

        // release FacesContextImplBase (sets current instance to null)
        super.release();
    }
    
    @Override
    public final FacesMessage.Severity getMaximumSeverity()
    {
        assertNotReleased();

        return _maximumSeverity;
    }
    
    @Override
    public final void addMessage(final String clientId, final FacesMessage message)
    {
        assertNotReleased();

        if (message == null)
        {
            throw new NullPointerException("message");
        }

        if (_messages == null)
        {
            _messages = new LinkedHashMap<String, List<FacesMessage>>();
            _orderedMessages = new ArrayList<FacesMessage>();
        }
        
        List<FacesMessage> lst = _messages.get(clientId); 
        if (lst == null)
        {
            lst = new ArrayList<FacesMessage>();
            _messages.put(clientId, lst);
        }
        
        lst.add(message);
        _orderedMessages.add (message);
        
        FacesMessage.Severity serSeverity = message.getSeverity();
        if (serSeverity != null)
        {
            if (_maximumSeverity == null)
            {
                _maximumSeverity = serSeverity;
            }
            else if (serSeverity.compareTo(_maximumSeverity) > 0)
            {
                _maximumSeverity = serSeverity;
            }
        }
    }

    @Override
    public List<FacesMessage> getMessageList()
    {
        assertNotReleased();
        
        if (_messages == null)
        {
            return Collections.unmodifiableList(Collections.<FacesMessage>emptyList());
        }
        
        return Collections.unmodifiableList(_orderedMessages);
    }

    @Override
    public List<FacesMessage> getMessageList(String clientId)
    {
        assertNotReleased();
        
        if (_messages == null || !_messages.containsKey(clientId))
        {
            return Collections.unmodifiableList(Collections.<FacesMessage>emptyList());
        }
        
        return _messages.get(clientId);
    }

    @Override
    public final Iterator<FacesMessage> getMessages()
    {
        assertNotReleased();

        if (_messages == null)
        {
            return NullIterator.instance();
        }
        
        return _orderedMessages.iterator();
    }

    @Override
    public final Iterator<FacesMessage> getMessages(final String clientId)
    {

        assertNotReleased();

        if (_messages == null || !_messages.containsKey(clientId))
        {
            return NullIterator.instance();
        }
        
        return _messages.get(clientId).iterator();        
    }
    
    @Override
    public final Iterator<String> getClientIdsWithMessages()
    {
        assertNotReleased();

        if (_messages == null || _messages.isEmpty())
        {
            return NullIterator.instance();
        }
        
        return _messages.keySet().iterator();
    }
    
    @Override
    public PhaseId getCurrentPhaseId()
    {
        assertNotReleased();

        return _currentPhaseId;
    }
    
    @Override
    public void setCurrentPhaseId(PhaseId currentPhaseId)
    {
        assertNotReleased();

        _currentPhaseId = currentPhaseId;
    }
    
    @Override
    public PartialViewContext getPartialViewContext()
    {
        assertNotReleased();

        if (_partialViewContext == null)
        {
            //Get through factory finder
            if (_partialViewContextFactory == null)
            {
                _partialViewContextFactory = (PartialViewContextFactory)
                    FactoryFinder.getFactory(FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY);
            }
            // Put actual facesContext as param, not this - this can be wrapped
            _partialViewContext = _partialViewContextFactory.getPartialViewContext(getCurrentFacesContext());
        }
        return _partialViewContext;
    }

    @Override
    public final boolean getRenderResponse()
    {
        assertNotReleased();

        return _renderResponse;
    }
    
    @Override
    public final void renderResponse()
    {
        assertNotReleased();

        _renderResponse = true;
    }

    @Override
    public final boolean getResponseComplete()
    {
        assertNotReleased();

        return _responseComplete;
    }
    
    @Override
    public final void responseComplete()
    {
        assertNotReleased();

        _responseComplete = true;
    }

    @Override
    public final void setResponseStream(final ResponseStream responseStream)
    {
        assertNotReleased();

        if (responseStream == null)
        {
            throw new NullPointerException("responseStream");
        }
        _responseStream = responseStream;
    }

    @Override
    public final ResponseStream getResponseStream()
    {
        assertNotReleased();

        return _responseStream;
    }

    @Override
    public final void setResponseWriter(final ResponseWriter responseWriter)
    {
        assertNotReleased();

        if (responseWriter == null)
        {
            throw new NullPointerException(METHOD_RESPONSEWRITER);
        }
        _responseWriter = responseWriter;
    }

    @Override
    public final ResponseWriter getResponseWriter()
    {
        assertNotReleased();

        return _responseWriter;
    }

    @Override
    public boolean isPostback()
    {
        assertNotReleased();

        RenderKit renderKit = getRenderKit();
        FacesContext facesContext = getCurrentFacesContext();
        if (renderKit == null)
        {
            // NullPointerException with StateManager, because
            // to restore state it first restore structure,
            // then fill it and in the middle of the two previous
            // process there is many calls from _ComponentChildrenList.childAdded
            // to facesContext.isPostback, and getViewRoot is null.
            // 
            // Setting a "phantom" UIViewRoot calling facesContext.setViewRoot(viewRoot)
            // to avoid it is bad, because this is work of RestoreViewExecutor,
            // and theorically ViewHandler.restoreView must return an UIViewRoot
            // instance.
            //
            // The problem with this is if the user changes the renderkit directly
            // using f:view renderKitId param, the ResponseStateManager returned
            // will be the one tied to faces-config selected RenderKit. But the usual 
            // method to check if a request is a postback, is always detect the param
            // javax.faces.ViewState, so there is no problem after all.
            String renderKitId = facesContext.getApplication().getViewHandler().calculateRenderKitId(facesContext);
            if (_renderKitFactory == null)
            {
                _renderKitFactory = (RenderKitFactory) 
                    FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            }
            renderKit = _renderKitFactory.getRenderKit(facesContext, renderKitId);
        }
        return renderKit.getResponseStateManager().isPostback(facesContext);
    }

    @Override
    public void validationFailed()
    {
        assertNotReleased();
        
        _validationFailed=true;
    }

    @Override
    public boolean isValidationFailed()
    {
        assertNotReleased();
        
        return _validationFailed;
    }
    
}
