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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;

import org.apache.myfaces.context.ReleaseableExternalContext;
import org.apache.myfaces.el.unified.FacesELContext;

/**
 * Provides a base implementation of the FacesContext for the use
 * in FacesContextImpl and StartupFacesContextImpl.
 * 
 * @author Jakob Korherr (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public abstract class FacesContextImplBase extends FacesContext
{
    public final static String SKIP_CLEAR_VIEW_MAP_HINT = "oam.fc.skipClearViewMapHint";

    private Application _application;
    private ExternalContext _externalContext;
    private ReleaseableExternalContext _defaultExternalContext;
    private UIViewRoot _viewRoot;
    private RenderKitFactory _renderKitFactory;
    private ELContext _elContext;
    private Map<Object, Object> _attributes = null;
    private boolean _processingEvents = true;
    private ExceptionHandler _exceptionHandler = null;
    
    // Variables used to cache values
    private RenderKit _cachedRenderKit = null;
    private String _cachedRenderKitId = null;
    
    protected boolean _released = false;
    
    private ApplicationFactory _applicationFactory = null;
    
    private List<String> _resourceLibraryContracts;
    private Character _separatorChar;
    private FacesContext _currentFacesContext;

    /**
     * Base constructor.
     * Calls FacesContext.setCurrentInstance(this);
     */
    public FacesContextImplBase(final ExternalContext externalContext,
            final ReleaseableExternalContext defaultExternalContext)
    {
        _externalContext = externalContext;
        _defaultExternalContext = defaultExternalContext;
        
        // this FacesContext impl is now the current instance
        // note that because this method is protected, it has to be called from here
        FacesContext.setCurrentInstance(this);
    }
    
    public FacesContextImplBase(final ExternalContext externalContext,
            final ReleaseableExternalContext defaultExternalContext,
            final ApplicationFactory applicationFactory,
            final RenderKitFactory renderKitFactory)
    {
        _externalContext = externalContext;
        _defaultExternalContext = defaultExternalContext;
        
        _applicationFactory = applicationFactory;
        _renderKitFactory = renderKitFactory;
        
        // this FacesContext impl is now the current instance
        // note that because this method is protected, it has to be called from here
        FacesContext.setCurrentInstance(this);
    }
    
    /**
     * Releases the instance fields on FacesContextImplBase.
     * Must be called by sub-classes, when overriding it!
     */
    @Override
    public void release()
    {
        _applicationFactory = null;
        _currentFacesContext = null;
        if (_defaultExternalContext != null)
        {
            _defaultExternalContext.release();
            _defaultExternalContext = null;
        }
        
        _application = null;
        _externalContext = null;
        _viewRoot = null;
        _renderKitFactory = null;
        _elContext = null;
        _exceptionHandler = null;
        _cachedRenderKit = null;
        _cachedRenderKitId = null;
        _separatorChar = null;
        
        // Spec JSF 2 section getAttributes when release is called the attributes map
        // must!!! be cleared! (probably to trigger some clearance methods on possible
        // added entries before nullifying everything)
        if (_attributes != null)
        {
            _attributes.clear();
            _attributes = null;
        }
        
        _released = true;
        FacesContext.setCurrentInstance(null);
    }
    
    @Override
    public boolean isReleased()
    {
        return _released;
    }

    @Override
    public final ExternalContext getExternalContext()
    {
        assertNotReleased();

        return _externalContext;
    }
    
    @Override
    public final Application getApplication()
    {
        assertNotReleased();
        
        if (_application == null)
        {
            if (_applicationFactory == null)
            {
                _applicationFactory = (ApplicationFactory) FactoryFinder.getFactory(
                    FactoryFinder.APPLICATION_FACTORY);
            }
            _application = _applicationFactory.getApplication();
        }
        
        return _application;
    }
    
    /**
     * 
     */
    public void purgeFacesContext()
    {
        _application = null;
        _renderKitFactory = null;
        _cachedRenderKit = null;
        _cachedRenderKitId = null;
    }
    
    @Override
    public final ExceptionHandler getExceptionHandler()
    {
        assertNotReleased();
        
        return _exceptionHandler;
    }
    
    @Override
    public final void setExceptionHandler(ExceptionHandler exceptionHandler)
    {
        assertNotReleased();
        
        _exceptionHandler = exceptionHandler;
    }
    
    @Override
    public final boolean isProcessingEvents()
    {
        assertNotReleased();
        
        return _processingEvents;
    }
    
    @Override
    public final void setProcessingEvents(boolean processingEvents)
    {
        assertNotReleased();
        
        _processingEvents = processingEvents;
    }
    
    @Override
    public final ELContext getELContext()
    {
        assertNotReleased();

        if (_elContext != null)
        {
            return _elContext;
        }

        _elContext = new FacesELContext(getApplication().getELResolver(), getCurrentFacesContext());

        ELContextEvent event = new ELContextEvent(_elContext);
        for (ELContextListener listener : getApplication().getELContextListeners())
        {
            listener.contextCreated(event);
        }

        return _elContext;
    }

    /**
     * Returns a mutable map of attributes associated with this faces context when
     * {@link javax.faces.context.FacesContext#release()} is called the map must be cleared!
     * 
     * Note this map is not associated with the request map the request map still is accessible via the
     * {@link javax.faces.context.ExternalContext#getRequestMap()} method!
     * 
     * Also the scope is different to the request map, this map has the scope of the context, and is cleared once the
     * release method on the context is called!
     * 
     * Also the map does not cause any events according to the spec!
     * 
     * @since JSF 2.0
     * 
     * @throws IllegalStateException
     *             if the current context already is released!
     */
    @Override
    public final Map<Object, Object> getAttributes()
    {
        assertNotReleased();

        if (_attributes == null)
        {
            _attributes = new HashMap<Object, Object>();
        }
        return _attributes;
    }
    
    @Override
    public UIViewRoot getViewRoot()
    {
        assertNotReleased();

        return _viewRoot;
    }
    
    @Override
    public final void setViewRoot(final UIViewRoot viewRoot)
    {
        assertNotReleased();

        if (viewRoot == null)
        {
            throw new NullPointerException("viewRoot");
        }
        // If the current UIViewRoot is non-null, and calling equals() on the argument root,
        // passing the current UIViewRoot returns false
        // the clear method must be called on the Map returned from UIViewRoot.getViewMap().
        if (_viewRoot != null && !_viewRoot.equals(viewRoot))
        {
            if (!Boolean.TRUE.equals(getAttributes().get(SKIP_CLEAR_VIEW_MAP_HINT)))
            {
                //call getViewMap(false) to prevent unnecessary map creation
                Map<String, Object> viewMap = _viewRoot.getViewMap(false);
                if (viewMap != null)
                {
                    viewMap.clear();
                }
            }
        }
        _viewRoot = viewRoot;
    }
    
    @Override
    public final RenderKit getRenderKit()
    {
        assertNotReleased();

        if (getViewRoot() == null)
        {
            return null;
        }

        String renderKitId = getViewRoot().getRenderKitId();

        if (renderKitId == null)
        {
            return null;
        }
        
        if (_cachedRenderKitId == null || !renderKitId.equals(_cachedRenderKitId))
        {
            _cachedRenderKitId = renderKitId;
            if (_renderKitFactory == null)
            {
                _renderKitFactory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            }
            _cachedRenderKit = _renderKitFactory.getRenderKit(getCurrentFacesContext(), renderKitId);
        }
        
        return _cachedRenderKit;
    }

    @Override
    public List<String> getResourceLibraryContracts()
    {
        assertNotReleased();
        
        if (_resourceLibraryContracts == null)
        {
            return Collections.emptyList();
        }
        else
        {
            return _resourceLibraryContracts;
        }
    }
    
    @Override
    public void setResourceLibraryContracts(List<String> contracts)
    {
        assertNotReleased();
     
        if (contracts == null)
        {
            _resourceLibraryContracts = null;
        }
        else if (contracts.isEmpty())
        {
            _resourceLibraryContracts = null;
        }
        else
        {
            _resourceLibraryContracts = new ArrayList<String>(contracts);
        }
    }

    @Override
    public char getNamingContainerSeparatorChar()
    {
        if (_separatorChar == null)
        {
            _separatorChar = UINamingContainer.getSeparatorChar(this);
        }
        return _separatorChar;
    }
    
    /**
     * has to be thrown in many of the methods if the method is called after the instance has been released!
     */
    protected final void assertNotReleased()
    {
        if (_released)
        {
            throw new IllegalStateException("Error the FacesContext is already released!");
        }
    }
    
    protected FacesContext getCurrentFacesContext()
    {
        if (_currentFacesContext == null)
        {
            _currentFacesContext = FacesContext.getCurrentInstance();
        }
        return _currentFacesContext;
    }
}
