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
//  PI63633     embreijo    THREAD-SAFETY ISSUE IN THE UNDERLYING (APACHE) JSF 2.0 CODE
package org.apache.myfaces.view.facelets.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.application.ViewResource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.UniqueIdVendor;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import org.apache.myfaces.shared.config.MyfacesConfig;

import org.apache.myfaces.view.facelets.AbstractFacelet;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.compiler.EncodingHandler;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;


/**
 * Default Facelet implementation.
 * 
 * @author Jacob Hookom
 * @version $Id: DefaultFacelet.java 1641481 2014-11-24 21:42:00Z lu4242 $
 */
final class DefaultFacelet extends AbstractFacelet
{

    //private static final Logger log = Logger.getLogger("facelets.facelet");
    private static final Logger log = Logger.getLogger(DefaultFacelet.class.getName());

    private final static String APPLIED_KEY = "org.apache.myfaces.view.facelets.APPLIED";

    private final String _alias;
    
    private final String _faceletId;

    private final ExpressionFactory _elFactory;

    private final DefaultFaceletFactory _factory;

    private final long _createTime;

    private final long _refreshPeriod;

    private final Map<String, URL> _relativePaths;

    private final FaceletHandler _root;

    private final URL _src;

    private final boolean _isBuildingCompositeComponentMetadata; 
    
    private final boolean _encodingHandler;

    public DefaultFacelet(DefaultFaceletFactory factory, ExpressionFactory el, URL src, String alias,
                          String faceletId, FaceletHandler root)
    {
        _factory = factory;
        _elFactory = el;
        _src = src;
        _root = root;
        _alias = alias;
        _faceletId = faceletId;
        _createTime = System.currentTimeMillis();
        _refreshPeriod = _factory.getRefreshPeriod();
        // PI63633 start
        _relativePaths = Collections.synchronizedMap(new WeakHashMap());
        // PI63633 end
        _isBuildingCompositeComponentMetadata = false;
        _encodingHandler = (root instanceof EncodingHandler);
    }

    public DefaultFacelet(DefaultFaceletFactory factory, ExpressionFactory el, URL src, String alias,
            String faceletId, FaceletHandler root, boolean isBuildingCompositeComponentMetadata)
    {
        _factory = factory;
        _elFactory = el;
        _src = src;
        _root = root;
        _alias = alias;
        _faceletId = faceletId;
        _createTime = System.currentTimeMillis();
        _refreshPeriod = _factory.getRefreshPeriod();
        // PI63633 start
        _relativePaths = Collections.synchronizedMap(new WeakHashMap());
        // PI63633 end
        _isBuildingCompositeComponentMetadata = isBuildingCompositeComponentMetadata;
        _encodingHandler = (root instanceof EncodingHandler);
    }    

    /**
     * @see org.apache.myfaces.view.facelets.Facelet#apply(javax.faces.context.FacesContext,
     *      javax.faces.component.UIComponent)
     */
    public void apply(FacesContext facesContext, UIComponent parent) throws IOException, FacesException,
            FaceletException, ELException
    {
        FaceletCompositionContext myFaceletContext = null;
        boolean faceletCompositionContextInitialized = false;
        boolean recordUniqueIds = false;
        myFaceletContext = FaceletCompositionContext.getCurrentInstance(facesContext);
        if (myFaceletContext == null)
        {
            myFaceletContext = new FaceletCompositionContextImpl(_factory, facesContext);
            myFaceletContext.init(facesContext);
            faceletCompositionContextInitialized = true;
            if (_encodingHandler && !myFaceletContext.isBuildingViewMetadata()
                    && MyfacesConfig.getCurrentInstance(
                    facesContext.getExternalContext()).isViewUniqueIdsCacheEnabled() && 
                    _refreshPeriod <= 0)
            {
                List<String> uniqueIdList = ((EncodingHandler)_root).getUniqueIdList();
                if (uniqueIdList == null)
                {
                    myFaceletContext.initUniqueIdRecording();
                    recordUniqueIds = true;
                }
                else
                {
                    myFaceletContext.setUniqueIdsIterator(uniqueIdList.iterator());
                }
            }
            if (parent instanceof UIViewRoot)
            {
                myFaceletContext.setViewRoot((UIViewRoot)parent);
                ComponentSupport.setCachedFacesContext((UIViewRoot)parent, facesContext);
            }
        }
        DefaultFaceletContext ctx = new DefaultFaceletContext(facesContext, this, myFaceletContext);
        
        //Set FACELET_CONTEXT_KEY on FacesContext attribute map, to 
        //reflect the current facelet context instance
        FaceletContext oldCtx = (FaceletContext) 
                facesContext.getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctx);
        
        ctx.pushPageContext(new PageContextImpl());
        
        try
        {
            // push the parent as a UniqueIdVendor to the stack here,
            // if there is no UniqueIdVendor on the stack yet
            boolean pushedUniqueIdVendor = false;
            if (parent instanceof UniqueIdVendor
                && ctx.getFaceletCompositionContext().getUniqueIdVendorFromStack() == null)
            {
                ctx.getFaceletCompositionContext().pushUniqueIdVendorToStack((UniqueIdVendor) parent);
                pushedUniqueIdVendor = true;
            }
            
            this.refresh(parent);
            myFaceletContext.markForDeletion(parent);
            _root.apply(ctx, parent);
            if (faceletCompositionContextInitialized &&
                parent instanceof UIViewRoot)
            {
                UIComponent metadataFacet = parent.getFacet(UIViewRoot.METADATA_FACET_NAME);
                if (metadataFacet != null)
                {
                    // Ensure metadata facet is removed from deletion, so if by some reason
                    // is not refreshed, its content will not be removed from the component tree.
                    // This behavior is preferred, even if the spec suggest to include it using
                    // a trick with the template client.
                    myFaceletContext.removeComponentForDeletion(metadataFacet);
                }
                if (myFaceletContext.isRefreshingTransientBuild())
                {
                    myFaceletContext.finalizeRelocatableResourcesForDeletion((UIViewRoot) parent);
                }
            }
            myFaceletContext.finalizeForDeletion(parent);
            this.markApplied(parent);
            
            // remove the UniqueIdVendor from the stack again
            if (pushedUniqueIdVendor)
            {
                ctx.getFaceletCompositionContext().popUniqueIdVendorToStack();
            }
        }
        finally
        {
            ctx.popPageContext();
            
            if (faceletCompositionContextInitialized)
            {
                if (parent instanceof UIViewRoot)
                {
                    ComponentSupport.setCachedFacesContext((UIViewRoot)parent, null);
                }
                myFaceletContext.release(facesContext);
                List<String> uniqueIdList = ((EncodingHandler)_root).getUniqueIdList();
                if (recordUniqueIds &&  uniqueIdList == null)
                {
                    uniqueIdList = Collections.unmodifiableList(
                            myFaceletContext.getUniqueIdList());
                    ((EncodingHandler)_root).setUniqueIdList(uniqueIdList);
                }
            }
            
            if (oldCtx != null)
            {
                facesContext.getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, oldCtx);
            }
        }
    }
    
    public void applyDynamicComponentHandler(FacesContext facesContext, 
            UIComponent parent, String baseKey)
         throws IOException, FacesException, FaceletException, ELException
    {
        FaceletCompositionContext fcctx = null;
        boolean faceletCompositionContextInitialized = false;
        fcctx = FaceletCompositionContext.getCurrentInstance(facesContext);
        boolean pushDynCompSection = false;
        if (fcctx == null)
        {
            fcctx = new FaceletCompositionContextImpl(_factory, facesContext, 
                baseKey);
            fcctx.init(facesContext);
            faceletCompositionContextInitialized = true;
        }
        else
        {
            pushDynCompSection = true;
            fcctx.pushDynamicComponentSection(baseKey);
        }
        // Disable dynamic component top level if the parent is a
        // dynamic wrapper, to allow the content to be reorganized properly under
        // a refresh.
        if (parent.getAttributes().containsKey("oam.vf.DYN_WRAPPER"))
        {
            fcctx.setDynamicComponentTopLevel(false);
        }
        
        FaceletContext oldCtx = (FaceletContext) facesContext.getAttributes().get(
            FaceletContext.FACELET_CONTEXT_KEY);
        DefaultFaceletContext ctx = new DefaultFaceletContext(facesContext, this, fcctx);
        
        //Set FACELET_CONTEXT_KEY on FacesContext attribute map, to 
        //reflect the current facelet context instance
        facesContext.getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctx);
        
        ctx.pushPageContext(new PageContextImpl());
        
        try
        {
            // push the parent as a UniqueIdVendor to the stack here,
            // if there is no UniqueIdVendor on the stack yet
            boolean pushedUniqueIdVendor = false;
            if (parent instanceof UniqueIdVendor
                && ctx.getFaceletCompositionContext().getUniqueIdVendorFromStack() == null)
            {
                ctx.getFaceletCompositionContext().pushUniqueIdVendorToStack((UniqueIdVendor) parent);
                pushedUniqueIdVendor = true;
            }
            
            //this.refresh(parent);
            //myFaceletContext.markForDeletion(parent);
            _root.apply(ctx, parent);
            //myFaceletContext.finalizeForDeletion(parent);
            //this.markApplied(parent);
            
            // remove the UniqueIdVendor from the stack again
            if (pushedUniqueIdVendor)
            {
                ctx.getFaceletCompositionContext().popUniqueIdVendorToStack();
            }
        }
        finally
        {
            ctx.popPageContext();
            facesContext.getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, oldCtx);
            
            if (pushDynCompSection)
            {
                fcctx.popDynamicComponentSection();
            }
            if (faceletCompositionContextInitialized)
            {
                fcctx.release(facesContext);
            }
        }
    }    

    private void refresh(UIComponent c)
    {
        if (_refreshPeriod > 0)
        {

            // finally remove any children marked as deleted
            int sz = c.getChildCount();
            if (sz > 0)
            {
                UIComponent cc = null;
                List<UIComponent> cl = c.getChildren();
                ApplyToken token;
                while (--sz >= 0)
                {
                    cc = cl.get(sz);
                    if (!cc.isTransient())
                    {
                        token = (ApplyToken) cc.getAttributes().get(APPLIED_KEY);
                        if (token != null && token._time < _createTime && token._alias.equals(_alias))
                        {
                            if (log.isLoggable(Level.INFO))
                            {
                                DateFormat df = SimpleDateFormat.getTimeInstance();
                                log.info("Facelet[" + _alias + "] was modified @ "
                                        + df.format(new Date(_createTime)) + ", flushing component applied @ "
                                        + df.format(new Date(token._time)));
                            }
                            cl.remove(sz);
                        }
                    }
                }
            }

            // remove any facets marked as deleted
            if (c.getFacetCount() > 0)
            {
                Collection<UIComponent> col = c.getFacets().values();
                UIComponent fc;
                ApplyToken token;
                for (Iterator<UIComponent> itr = col.iterator(); itr.hasNext();)
                {
                    fc = itr.next();
                    if (!fc.isTransient())
                    {
                        token = (ApplyToken) fc.getAttributes().get(APPLIED_KEY);
                        if (token != null && token._time < _createTime && token._alias.equals(_alias))
                        {
                            if (log.isLoggable(Level.INFO))
                            {
                                DateFormat df = SimpleDateFormat.getTimeInstance();
                                log.info("Facelet[" + _alias + "] was modified @ "
                                        + df.format(new Date(_createTime)) + ", flushing component applied @ "
                                        + df.format(new Date(token._time)));
                            }
                            itr.remove();
                        }
                    }
                }
            }
        }
    }

    private void markApplied(UIComponent parent)
    {
        if (this._refreshPeriod > 0)
        {
            int facetCount = parent.getFacetCount();
            int childCount = parent.getChildCount();
            if (childCount > 0 || facetCount > 0)
            {
                ApplyToken token = new ApplyToken(_alias, System.currentTimeMillis() + _refreshPeriod);

                if (facetCount > 0)
                {
                    for (UIComponent facet : parent.getFacets().values())
                    {
                        markApplied(token, facet);
                    }
                }
                for (int i = 0; i < childCount; i++)
                {
                    UIComponent child = parent.getChildren().get(i);
                    markApplied(token, child);
                }
            }
        }
    }

    private void markApplied(ApplyToken token, UIComponent c)
    {
        if (!c.isTransient())
        {
            Map<String, Object> attr = c.getAttributes();
            if (!attr.containsKey(APPLIED_KEY))
            {
                attr.put(APPLIED_KEY, token);
            }
        }
    }

    /**
     * Return the alias name for error messages and logging
     * 
     * @return alias name
     */
    public String getAlias()
    {
        return _alias;
    }
    
    public String getFaceletId()
    {
        return _faceletId;
    }

    /**
     * Return this Facelet's ExpressionFactory instance
     * 
     * @return internal ExpressionFactory instance
     */
    public ExpressionFactory getExpressionFactory()
    {
        return _elFactory;
    }

    /**
     * The time when this Facelet was created, NOT the URL source code
     * 
     * @return final timestamp of when this Facelet was created
     */
    public long getCreateTime()
    {
        return _createTime;
    }

    /**
     * Delegates resolution to DefaultFaceletFactory reference. Also, caches URLs for relative paths.
     * 
     * @param path
     *            a relative url path
     * @return URL pointing to destination
     * @throws IOException
     *             if there is a problem creating the URL for the path specified
     */
    private URL getRelativePath(FacesContext facesContext, String path) throws IOException
    {
        URL url = (URL) _relativePaths.get(path);
        if (url == null)
        {
            url = _factory.resolveURL(facesContext, _src, path);
            if (url != null)
            {
                ViewResource viewResource = (ViewResource) facesContext.getAttributes().get(
                    FaceletFactory.LAST_RESOURCE_RESOLVED);
                if (viewResource != null)
                {
                    // If a view resource has been used to resolve a resource, the cache is in
                    // the ResourceHandler implementation. No need to cache in _relativeLocations.
                }
                else
                {
                    _relativePaths.put(path, url);
                }
            }
        }
        return url;
    }

    /**
     * The URL this Facelet was created from.
     * 
     * @return the URL this Facelet was created from
     */
    public URL getSource()
    {
        return _src;
    }

    /**
     * Given the passed FaceletContext, apply our child FaceletHandlers to the passed parent
     * 
     * @see FaceletHandler#apply(FaceletContext, UIComponent)
     * @param ctx
     *            the FaceletContext to use for applying our FaceletHandlers
     * @param parent
     *            the parent component to apply changes to
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    private void include(AbstractFaceletContext ctx, UIComponent parent) throws IOException, FacesException,
            FaceletException, ELException
    {
        ctx.pushPageContext(new PageContextImpl());
        try
        {
            this.refresh(parent);
            DefaultFaceletContext ctxWrapper = new DefaultFaceletContext((DefaultFaceletContext)ctx, this, false);
            ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctxWrapper);
            _root.apply(ctxWrapper, parent);
            ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctx);
            this.markApplied(parent);
        }
        finally
        {
            ctx.popPageContext();
        }
    }

    /**
     * Used for delegation by the DefaultFaceletContext. First pulls the URL from {@link #getRelativePath(String)
     * getRelativePath(String)}, then calls
     * {@link #include(org.apache.myfaces.view.facelets.AbstractFaceletContext,
     * javax.faces.component.UIComponent, java.net.URL)}.
     * 
     * @see FaceletContext#includeFacelet(UIComponent, String)
     * @param ctx
     *            FaceletContext to pass to the included Facelet
     * @param parent
     *            UIComponent to apply changes to
     * @param path
     *            relative path to the desired Facelet from the FaceletContext
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public void include(AbstractFaceletContext ctx, UIComponent parent, String path)
            throws IOException, FacesException, FaceletException, ELException
    {
        URL url = this.getRelativePath(ctx.getFacesContext(), path);
        this.include(ctx, parent, url);
    }

    /**
     * Grabs a DefaultFacelet from referenced DefaultFaceletFacotry
     * 
     * @see DefaultFaceletFactory#getFacelet(URL)
     * @param ctx
     *            FaceletContext to pass to the included Facelet
     * @param parent
     *            UIComponent to apply changes to
     * @param url
     *            URL source to include Facelet from
     * @throws IOException
     * @throws FacesException
     * @throws FaceletException
     * @throws ELException
     */
    public void include(AbstractFaceletContext ctx, UIComponent parent, URL url) throws IOException, FacesException,
            FaceletException, ELException
    {
        DefaultFacelet f = (DefaultFacelet) _factory.getFacelet(ctx, url);
        f.include(ctx, parent);
    }
    
    public void applyCompositeComponent(AbstractFaceletContext ctx, UIComponent parent, Resource resource)
            throws IOException, FacesException, FaceletException, ELException
    {
        // Here we are creating a facelet using the url provided by the resource.
        // It works, but the Resource API provides getInputStream() for that. But the default
        // implementation wraps everything that could contain ValueExpression and decode so
        // we can't use it here.
        //DefaultFacelet f = (DefaultFacelet) _factory.getFacelet(resource.getURL());
        //f.apply(ctx.getFacesContext(), parent);
        DefaultFacelet f = (DefaultFacelet) _factory.getFacelet(resource.getURL());
        
        ctx.pushPageContext(new PageContextImpl());
        try
        {
            // push the parent as a UniqueIdVendor to the stack here,
            // if there is no UniqueIdVendor on the stack yet
            boolean pushedUniqueIdVendor = false;
            FaceletCompositionContext mctx = ctx.getFaceletCompositionContext();
            if (parent instanceof UniqueIdVendor
                && ctx.getFaceletCompositionContext().getUniqueIdVendorFromStack() == null)
            {
                mctx.pushUniqueIdVendorToStack((UniqueIdVendor) parent);
                pushedUniqueIdVendor = true;
            }
            
            f.refresh(parent);
            mctx.markForDeletion(parent);
            DefaultFaceletContext ctxWrapper = new DefaultFaceletContext( (DefaultFaceletContext)ctx, f, true);
            //Update FACELET_CONTEXT_KEY on FacesContext attribute map, to 
            //reflect the current facelet context instance
            ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctxWrapper);
            f._root.apply(ctxWrapper, parent);
            ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctx);
            mctx.finalizeForDeletion(parent);
            f.markApplied(parent);
            
            // remove the UniqueIdVendor from the stack again
            if (pushedUniqueIdVendor)
            {
                ctx.getFaceletCompositionContext().popUniqueIdVendorToStack();
            }
        }
        finally
        {
            ctx.popPageContext();
        }
    }

    private static class ApplyToken implements Externalizable
    {
        public String _alias;

        public long _time;

        public ApplyToken()
        {
        }

        public ApplyToken(String alias, long time)
        {
            _alias = alias;
            _time = time;
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
        {
            _alias = in.readUTF();
            _time = in.readLong();
        }

        public void writeExternal(ObjectOutput out) throws IOException
        {
            out.writeUTF(_alias);
            out.writeLong(_time);
        }
    }

    public String toString()
    {
        return _alias;
    }

    @Override
    public boolean isBuildingCompositeComponentMetadata()
    {
        return _isBuildingCompositeComponentMetadata;
    }
}
