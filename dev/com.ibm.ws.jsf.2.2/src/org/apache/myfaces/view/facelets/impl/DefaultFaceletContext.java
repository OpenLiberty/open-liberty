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
package org.apache.myfaces.view.facelets.impl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

import org.apache.myfaces.view.facelets.AbstractFacelet;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.ELExpressionCacheMode;
import org.apache.myfaces.view.facelets.FaceletCompositionContext;
import org.apache.myfaces.view.facelets.PageContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.TemplateContext;
import org.apache.myfaces.view.facelets.TemplateManager;
import org.apache.myfaces.view.facelets.el.DefaultVariableMapper;
import org.apache.myfaces.view.facelets.el.VariableMapperBase;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;

/**
 * Default FaceletContext implementation.
 * 
 * A single FaceletContext is used for all Facelets involved in an invocation of
 * {@link org.apache.myfaces.view.facelets.Facelet#apply(FacesContext, UIComponent)
 * Facelet#apply(FacesContext, UIComponent)}. This
 * means that included Facelets are treated the same as the JSP include directive.
 * 
 * @author Jacob Hookom
 * @version $Id: DefaultFaceletContext.java 1576792 2014-03-12 15:57:38Z lu4242 $
 */
final class DefaultFaceletContext extends AbstractFaceletContext
{
    private final FacesContext _faces;

    private final ELContext _ctx;

    private final AbstractFacelet _facelet;
    private final List<AbstractFacelet> _faceletHierarchy;

    private VariableMapper _varMapper;
    private final DefaultVariableMapper _defaultVarMapper;
    private VariableMapperBase _varMapperBase;

    private FunctionMapper _fnMapper;

    //private final Map<String, Integer> _ids;
    //private final Map<Integer, Integer> _prefixes;
    private String _prefix;

    private StringBuilder _uniqueIdBuilder;

    //private final LinkedList<TemplateManager> _clients;
    
    private final FaceletCompositionContext _mctx;
    
    private LinkedList<AjaxHandler> _ajaxHandlerStack;
    
    private final List<TemplateContext> _isolatedTemplateContext;
    
    private int _currentTemplateContext;
    
    private ELExpressionCacheMode _elExpressionCacheMode;
    
    private boolean _isCacheELExpressions;

    private final List<PageContext> _isolatedPageContext;
    
    public DefaultFaceletContext(DefaultFaceletContext ctx,
            AbstractFacelet facelet, boolean ccWrap)
    {
        _ctx = ctx._ctx;
        //_ids = ctx._ids;
        //_prefixes = ctx._prefixes;
        //_clients = ctx._clients;
        _faces = ctx._faces;
        _fnMapper = ctx._fnMapper;
        _varMapper = ctx._varMapper;
        _defaultVarMapper = ctx._defaultVarMapper;
        _varMapperBase = ctx._varMapperBase;
        _faceletHierarchy = new ArrayList<AbstractFacelet>(ctx._faceletHierarchy
                .size() + 1);
        _faceletHierarchy.addAll(ctx._faceletHierarchy);
        _faceletHierarchy.add(facelet);
        _facelet = facelet;
        _mctx = ctx._mctx;
        
        if (ccWrap)
        {
            // Each time a composite component is being applied, a new
            // ajax stack should be created, and f:ajax tags outside the
            // composite component should be ignored.
            _ajaxHandlerStack = null;
        }
        else
        {
            // It is a template include, the current ajax stack should be
            // preserved.
            _ajaxHandlerStack = ctx._ajaxHandlerStack;
        }
        
        // It is not necessary to create a copy of this array, because we always use
        // pushTemplateContext(TemplateContext) / popTemplateContext() and
        // pushCompositeComponentClient(final TemplateClient client) / popCompositeComponentClient()
        // in pairs.
        //_isolatedTemplateContext = new ArrayList<TemplateContext>(ctx._isolatedTemplateContext.size()+1);
        //for (int i = 0; i <= ctx._currentTemplateContext; i++)
        //{
        //    _isolatedTemplateContext.add(ctx._isolatedTemplateContext.get(i));
        //}
        _isolatedTemplateContext = ctx._isolatedTemplateContext;
        _currentTemplateContext = ctx._currentTemplateContext;
        
        _isolatedPageContext = ctx._isolatedPageContext;
        
        _elExpressionCacheMode = ctx._elExpressionCacheMode;
        _isCacheELExpressions = ctx._isCacheELExpressions;

    }

    public DefaultFaceletContext(FacesContext faces, AbstractFacelet facelet, FaceletCompositionContext mctx)
    {
        _ctx = faces.getELContext();
        //_ids = new HashMap<String, Integer>();
        //_prefixes = new HashMap<Integer, Integer>();
        //_clients = new LinkedList<TemplateManager>();
        _faces = faces;
        _fnMapper = _ctx.getFunctionMapper();
        _varMapper = _ctx.getVariableMapper();
        if (_varMapper == null)
        {
            _defaultVarMapper = new DefaultVariableMapper();
            _varMapper = _defaultVarMapper;
            _varMapperBase = _defaultVarMapper;
        }
        else
        {
            _defaultVarMapper = new DefaultVariableMapper(_varMapper);
            _varMapper = _defaultVarMapper;
            _varMapperBase = _defaultVarMapper;
        }
        
        _faceletHierarchy = new ArrayList<AbstractFacelet>(1);
        _faceletHierarchy.add(facelet);
        _facelet = facelet;
        _mctx = mctx;
        
        _isolatedTemplateContext = new ArrayList<TemplateContext>(1);
        _isolatedTemplateContext.add(new TemplateContextImpl());
        _currentTemplateContext = 0;
        _defaultVarMapper.setTemplateContext(_isolatedTemplateContext.get(_currentTemplateContext));
        
        _isolatedPageContext = new ArrayList<PageContext>(8);
        
        _elExpressionCacheMode = mctx.getELExpressionCacheMode();
        _isCacheELExpressions = !ELExpressionCacheMode.noCache.equals(_elExpressionCacheMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FacesContext getFacesContext()
    {
        return _faces;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExpressionFactory getExpressionFactory()
    {
        return _facelet.getExpressionFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVariableMapper(VariableMapper varMapper)
    {
        // Assert.param("varMapper", varMapper);
        _varMapper = varMapper;
        _varMapperBase = (_varMapper instanceof VariableMapperBase) ? (VariableMapperBase) varMapper : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFunctionMapper(FunctionMapper fnMapper)
    {
        // Assert.param("fnMapper", fnMapper);
        _fnMapper = fnMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void includeFacelet(UIComponent parent, String relativePath)
            throws IOException
    {
        _facelet.include(this, parent, relativePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FunctionMapper getFunctionMapper()
    {
        return _fnMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VariableMapper getVariableMapper()
    {
        return _varMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getContext(Class key)
    {
        return _ctx.getContext(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void putContext(Class key, Object contextObject)
    {
        _ctx.putContext(key, contextObject);
    }

    private void initPrefix()
    {
        if (_prefix == null)
        {
            _uniqueIdBuilder = new StringBuilder(
                    _faceletHierarchy.size() * 30);
            for (int i = 0; i < _faceletHierarchy.size(); i++)
            {
                AbstractFacelet facelet = _faceletHierarchy.get(i);
                _uniqueIdBuilder.append(facelet.getFaceletId());
            }

            // Integer prefixInt = new Integer(builder.toString().hashCode());
            // -= Leonardo Uribe =- if the previous formula is used, it is possible that
            // negative values are introduced. The presence of '-' char causes problems
            // with htmlunit 2.4 or lower, so in order to prevent it it is better to use
            // only positive values instead.
            // Take into account CompilationManager.nextTagId() uses Math.abs too.
            Integer prefixInt = new Integer(Math.abs(_uniqueIdBuilder.toString().hashCode()));
            _prefix = prefixInt.toString();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String generateUniqueId(String base)
    {
        initPrefix();

        _uniqueIdBuilder.setLength(0);
        // getFaceletCompositionContext().generateUniqueId() is the one who ensures
        // the final id will be unique, but prefix and base ensure it will be unique
        // per facelet because prefix is calculated from faceletHierarchy and base is
        // related to the tagId, which depends on the location.
        //_uniqueIdBuilder.append(getFaceletCompositionContext().generateUniqueId());
        
        String uniqueIdFromIterator = getFaceletCompositionContext().getUniqueIdFromIterator();
        if (uniqueIdFromIterator == null)
        {
            getFaceletCompositionContext().generateUniqueId(_uniqueIdBuilder);
            // Since two different facelets are used to build the metadata, it is necessary
            // to trim the "base" part from the returned unique id, to ensure the components will be
            // refreshed properly. Note the "base" part is the one that allows to ensure
            // uniqueness between two different facelets with the same <f:metadata>, but since by 
            // spec view metadata sections cannot live on template client facelets, this case is
            // just not possible. 
            // MYFACES-3709 It was also noticed that in some cases, the prefix should also
            // be excluded from the id. The prefix is included if the metadata section is
            // applied inside an included section (by ui:define and ui:insert for example).
            if (!getFaceletCompositionContext().isInMetadataSection())
            {
                _uniqueIdBuilder.append("_");
                _uniqueIdBuilder.append(_prefix);
                _uniqueIdBuilder.append("_");
                _uniqueIdBuilder.append(base);
            }
            uniqueIdFromIterator = _uniqueIdBuilder.toString();
            getFaceletCompositionContext().addUniqueId(uniqueIdFromIterator);
            return uniqueIdFromIterator;
        }
        else
        {
            getFaceletCompositionContext().incrementUniqueId();
            return uniqueIdFromIterator;
        }
    }
    
    public String generateUniqueFaceletTagId(String count, String base)    
    {
        initPrefix();
        _uniqueIdBuilder.setLength(0);
        _uniqueIdBuilder.append(count);
        _uniqueIdBuilder.append("_");
        _uniqueIdBuilder.append(_prefix);
        _uniqueIdBuilder.append("_");
        _uniqueIdBuilder.append(base);
        return _uniqueIdBuilder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String name)
    {
        if (_varMapper != null)
        {
            ValueExpression ve = _varMapper.resolveVariable(name);
            if (ve != null)
            {
                return ve.getValue(this);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(String name, Object value)
    {
        if (_varMapper != null)
        {
            if (value == null)
            {
                _varMapper.setVariable(name, null);
            }
            else
            {
                _varMapper.setVariable(name, _facelet.getExpressionFactory()
                        .createValueExpression(value, Object.class));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void includeFacelet(UIComponent parent, URL absolutePath)
            throws IOException, FacesException, ELException
    {
        _facelet.include(this, parent, absolutePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ELResolver getELResolver()
    {
        return _ctx.getELResolver();
    }

    //Begin methods from AbstractFaceletContext

    @Override
    public TemplateManager popClient(TemplateClient client)
    {
        //if (!this._clients.isEmpty())
        //{
        //    Iterator<TemplateManager> itr = this._clients.iterator();
        //    while (itr.hasNext())
        //    {
        //        if (itr.next().equals(client))
        //        {
        //            itr.remove();
        //            return;
        //        }
        //    }
        //}
        //throw new IllegalStateException(client + " not found");
        //return _clients.removeFirst();
        return _isolatedTemplateContext.get(_currentTemplateContext).popClient(this);
    }

    @Override
    public void pushClient(final TemplateClient client)
    {
        //this._clients.add(0, new TemplateManager(this._facelet, client, true));
        //_clients.addFirst(new TemplateManagerImpl(this._facelet, client, true));
        _isolatedTemplateContext.get(_currentTemplateContext).pushClient(this, this._facelet, client);
    }

    public TemplateManager popExtendedClient(TemplateClient client)
    {
        //return _clients.removeLast();
        return _isolatedTemplateContext.get(_currentTemplateContext).popExtendedClient(this);
    }
    
    @Override
    public void extendClient(final TemplateClient client)
    {
        //this._clients.add(new TemplateManager(this._facelet, client, false));
        //_clients.addLast(new TemplateManagerImpl(this._facelet, client, false));
        _isolatedTemplateContext.get(_currentTemplateContext).extendClient(this, this._facelet, client);
    }

    @Override
    public boolean includeDefinition(UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException
    {
        //boolean found = false;
        //TemplateManager client;
        //for (int i = 0, size = this._clients.size(); i < size && !found; i++)
        //{
        //    client = ((TemplateManager) this._clients.get(i));
        //    if (client.equals(this._facelet))
        //        continue;
        //    found = client.apply(this, parent, name);
        //}
        //return found;
        return _isolatedTemplateContext.get(_currentTemplateContext).includeDefinition(
                this, this._facelet, parent, name);
    }

    /*
    private final static class TemplateManagerImpl extends TemplateManager implements TemplateClient
    {
        private final DefaultFacelet _owner;

        private final TemplateClient _target;

        private final boolean _root;

        private final Set<String> _names = new HashSet<String>();

        public TemplateManagerImpl(DefaultFacelet owner, TemplateClient target,
                boolean root)
        {
            this._owner = owner;
            this._target = target;
            this._root = root;
        }

        public boolean apply(FaceletContext ctx, UIComponent parent, String name)
                throws IOException, FacesException, FaceletException,
                ELException
        {
            String testName = (name != null) ? name : "facelets._NULL_DEF_";
            if (this._names.contains(testName))
            {
                return false;
            }
            else
            {
                this._names.add(testName);
                boolean found = false;
                found = this._target
                        .apply(new DefaultFaceletContext(
                                (DefaultFaceletContext) ctx, this._owner, false),
                                parent, name);
                this._names.remove(testName);
                return found;
            }
        }

        public boolean equals(Object o)
        {
            // System.out.println(this.owner.getAlias() + " == " +
            // ((DefaultFacelet) o).getAlias());
            return this._owner == o || this._target == o;
        }

        public boolean isRoot()
        {
            return this._root;
        }
    }*/

    /*
    @Override
    public TemplateManager popCompositeComponentClient(boolean cleanClientStack)
    {
        //if (!this._compositeComponentClients.isEmpty())
        //{
            //if (cleanClientStack)
            //{
            //    _clientsStack.get(_currentClientStack).clear();
            //}
            //_currentClientStack--;
            //return this._compositeComponentClients.remove(0);
        //}
        if (_currentTemplateContext > 0)
        {
            TemplateManager tm = _isolatedTemplateContext.get(_currentTemplateContext).getCompositeComponentClient();
            if (cleanClientStack)
            {
                _isolatedTemplateContext.get(_currentTemplateContext).clear();
            }
            _currentTemplateContext--;
            return tm;
        }
        return null;
    }
    

    @Override
    public void pushCompositeComponentClient(final TemplateClient client)
    {
        //this._compositeComponentClients.add(0, new CompositeComponentTemplateManager(this._facelet, client));
        //if (_currentClientStack + 1 <= _clientsStack.size())
        //{
        //    _clientsStack.add(new LinkedList<TemplateManager>());
        //}
        //_currentClientStack++;
        if (_currentTemplateContext + 1 <= _isolatedTemplateContext.size())
        {
            _isolatedTemplateContext.add(new IsolatedTemplateContextImpl());
        }
        _currentTemplateContext++;
        _isolatedTemplateContext.get(_currentTemplateContext).setCompositeComponentClient(
            new CompositeComponentTemplateManager(this._facelet, client));
    }
    
    @Override
    public void pushCompositeComponentClient(final TemplateManager client)
    {
        //this._compositeComponentClients.add(0, client);
        //if (_currentClientStack + 1 < _clientsStack.size())
        //{
        //    _clientsStack.add(new LinkedList<TemplateManager>());
        //}
        //_currentClientStack++;
        if (_currentTemplateContext + 1 < _isolatedTemplateContext.size())
        {
            _isolatedTemplateContext.add(new IsolatedTemplateContextImpl());
        }
        _currentTemplateContext++;
        _isolatedTemplateContext.get(_currentTemplateContext).setCompositeComponentClient(client);
    }*/
    
    @Override
    public void pushCompositeComponentClient(final TemplateClient client)
    {
        TemplateContext itc = new TemplateContextImpl();
        itc.setCompositeComponentClient(
                new CompositeComponentTemplateManager(this._facelet, client, getPageContext()));
        _isolatedTemplateContext.add(itc);
        _currentTemplateContext++;
        _defaultVarMapper.setTemplateContext(itc);
    }
    
    @Override
    public void popCompositeComponentClient()
    {
        if (_currentTemplateContext > 0)
        {
            _isolatedTemplateContext.remove(_currentTemplateContext);
            _currentTemplateContext--;
            _defaultVarMapper.setTemplateContext(_isolatedTemplateContext.get(_currentTemplateContext));
        }
    }
    
    @Override
    public void pushTemplateContext(TemplateContext client)
    {
        _isolatedTemplateContext.add(client);
        _currentTemplateContext++;
        _defaultVarMapper.setTemplateContext(client);
    }    

    
    @Override
    public TemplateContext popTemplateContext()
    {
        if (_currentTemplateContext > 0)
        {
            TemplateContext itc = _isolatedTemplateContext.get(_currentTemplateContext);
            _isolatedTemplateContext.remove(_currentTemplateContext);
            _currentTemplateContext--;
            _defaultVarMapper.setTemplateContext(_isolatedTemplateContext.get(_currentTemplateContext));
            return itc;
        }
        return null;
    }
    
    @Override
    public TemplateContext getTemplateContext()
    {
        return _isolatedTemplateContext.get(_currentTemplateContext);
    }

    @Override
    public boolean includeCompositeComponentDefinition(UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException
    {
        //boolean found = false;
        //TemplateManager client;

        //for (int i = 0, size = this._compositeComponentClients.size(); i < size && !found; i++)
        //{
        //    client = ((TemplateManager) this._compositeComponentClients.get(i));
        //    if (client.equals(this._facelet))
        //        continue;
        //    found = client.apply(this, parent, name);
        //}

        //return found;
        TemplateClient ccClient = _isolatedTemplateContext.get(_currentTemplateContext).getCompositeComponentClient();
        if (ccClient != null)
        {
            return ccClient.apply(this, parent, name);
        }
        return false;
    }
    
    private final static class CompositeComponentTemplateManager extends TemplateManager implements TemplateClient
    {
        private final AbstractFacelet _owner;

        protected final TemplateClient _target;

        private final Set<String> _names = new HashSet<String>();
        
        private final PageContext _pageContext;

        public CompositeComponentTemplateManager(AbstractFacelet owner, TemplateClient target, PageContext pageContext)
        {
            this._owner = owner;
            this._target = target;
            this._pageContext = pageContext;
        }

        public boolean apply(FaceletContext ctx, UIComponent parent, String name)
                throws IOException, FacesException, FaceletException,
                ELException
        {
            String testName = (name != null) ? name : "facelets._NULL_DEF_";
            if (this._names.contains(testName))
            {
                return false;
            }
            else
            {
                this._names.add(testName);
                boolean found = false;
                AbstractFaceletContext actx = new DefaultFaceletContext(
                        (DefaultFaceletContext) ctx, this._owner, false);
                ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, actx);
                try
                {
                    actx.pushPageContext(this._pageContext);
                    found = this._target
                            .apply(actx,
                                    parent, name);
                }
                finally
                {
                    actx.popPageContext();
                }
                ctx.getFacesContext().getAttributes().put(FaceletContext.FACELET_CONTEXT_KEY, ctx);
                this._names.remove(testName);
                return found;
            }
        }

        public boolean equals(Object o)
        {
            // System.out.println(this.owner.getAlias() + " == " +
            // ((DefaultFacelet) o).getAlias());
            return this._owner == o || this._target == o;
        }

        @Override
        public int hashCode()
        {
            int result = _owner != null ? _owner.hashCode() : 0;
            result = 31 * result + (_target != null ? _target.hashCode() : 0);
            return result;
        }
    }
    
    @Override
    public void pushPageContext(PageContext client)
    {
        _isolatedPageContext.add(client);
        _defaultVarMapper.setPageContext(client);
    }    

    @Override
    public PageContext popPageContext()
    {
        if (!_isolatedPageContext.isEmpty())
        {
            int currentPageContext = _isolatedPageContext.size()-1;
            PageContext itc = _isolatedPageContext.get(currentPageContext);
            _isolatedPageContext.remove(currentPageContext);
            if (!_isolatedPageContext.isEmpty())
            {
                _defaultVarMapper.setPageContext(getPageContext());
            }
            else
            {
                _defaultVarMapper.setPageContext(null);
            }
            return itc;
        }
        return null;
    }
    
    @Override
    public PageContext getPageContext()
    {
        return _isolatedPageContext.get(_isolatedPageContext.size()-1);
    }
    
    //End methods from AbstractFaceletContext
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyResolved()
    {
        return _ctx.isPropertyResolved();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPropertyResolved(boolean resolved)
    {
        _ctx.setPropertyResolved(resolved);
    }

    @Override
    public void applyCompositeComponent(UIComponent parent, Resource resource)
            throws IOException, FaceletException, FacesException, ELException
    {
        _facelet.applyCompositeComponent(this, parent, resource);
    }

    @Override
    public Iterator<AjaxHandler> getAjaxHandlers()
    {
        if (_ajaxHandlerStack != null && !_ajaxHandlerStack.isEmpty())
        {
            return _ajaxHandlerStack.iterator();
        }
        return null;
    }

    @Override
    public void popAjaxHandlerToStack()
    {
        if (_ajaxHandlerStack != null && !_ajaxHandlerStack.isEmpty())
        {
            _ajaxHandlerStack.removeFirst();
        }
    }

    @Override
    public void pushAjaxHandlerToStack(
            AjaxHandler parent)
    {
        if (_ajaxHandlerStack == null)
        {
            _ajaxHandlerStack = new LinkedList<AjaxHandler>();
        }

        _ajaxHandlerStack.addFirst(parent);
    }

    @Override
    public boolean isBuildingCompositeComponentMetadata()
    {
        return _facelet.isBuildingCompositeComponentMetadata();
    }
    
    public FaceletCompositionContext getFaceletCompositionContext()
    {
        return _mctx;
    }
    
    public boolean isAnyFaceletsVariableResolved()
    {
        //if (isAllowCacheELExpressions() && _varMapperBase != null)
        if (_varMapperBase != null)
        {
            return _varMapperBase.isAnyFaceletsVariableResolved();
        }
        return true;
    }
    
    public boolean isAllowCacheELExpressions()
    {
        return _isCacheELExpressions && getTemplateContext().isAllowCacheELExpressions() 
                && getPageContext().isAllowCacheELExpressions();
    }
    
    public void beforeConstructELExpression()
    {
        //if (isAllowCacheELExpressions() && _varMapperBase != null)
        if (_varMapperBase != null)
        {
            _varMapperBase.beforeConstructELExpression();
        }
    }
    
    public void afterConstructELExpression()
    {
        //if (isAllowCacheELExpressions() && _varMapperBase != null)
        if (_varMapperBase != null)
        {
            _varMapperBase.afterConstructELExpression();
        }
    }
    
    public ELExpressionCacheMode getELExpressionCacheMode()
    {
        return _elExpressionCacheMode;
    }
    
}
