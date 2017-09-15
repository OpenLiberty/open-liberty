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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.FaceletException;

import org.apache.myfaces.util.AbstractAttributeMap;
import org.apache.myfaces.view.facelets.AbstractFacelet;
import org.apache.myfaces.view.facelets.AbstractFaceletContext;
import org.apache.myfaces.view.facelets.PageContext;
import org.apache.myfaces.view.facelets.TemplateClient;
import org.apache.myfaces.view.facelets.TemplateContext;
import org.apache.myfaces.view.facelets.TemplateManager;

/**
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1488347 $ $Date: 2013-05-31 18:30:47 +0000 (Fri, 31 May 2013) $
 * @since 2.0.1
 */
public class TemplateContextImpl extends TemplateContext
{
    /**
     * This is a dummy instance
     */
    private static final TemplateClient INITIAL_TEMPLATE_CLIENT = new InitialTemplateClient();
    
    /**
     * This is a dummy instance
     */
    private static final PageContext INITIAL_PAGE_CONTEXT = new InitialPageContext();
    
    private final List<TemplateManagerImpl> _clients;
    
    private TemplateManager _compositeComponentClient;
    
    private TemplateManagerImpl _lastClient;
    
    private boolean _isCacheELExpressions;
    
    private final TemplateClientAttributeMap _templateClientAttributeMap;
    
    private final TemplateClientKnownParameterMap _templateClientKnownParameterMap;

    public TemplateContextImpl()
    {
        super();
        _clients = new ArrayList<TemplateManagerImpl>(4);
        // Parameters registered using ui:param now are bound to template manager instances, because
        // it should follow the same rules as template clients registered here. For example, to resolve
        // params on nested ui:decorate and ui:composition the same rules applies than for ui:define and
        // ui:insert. The simplest solution is add a template manager with a dummy template client and
        // page context, so when a new context is added (like in a ui:include), all params registered go 
        // to this manager. 
        _clients.add(new TemplateManagerImpl(null, INITIAL_TEMPLATE_CLIENT, true, INITIAL_PAGE_CONTEXT));
        _lastClient = _clients.get(0); //_clients.getFirst();
        _isCacheELExpressions = true;
        _templateClientAttributeMap = new TemplateClientAttributeMap();
        _templateClientKnownParameterMap = new TemplateClientKnownParameterMap();
    }

    @Override
    public TemplateManager popClient(final AbstractFaceletContext actx)
    {
        _lastClient = null;
        return _clients.remove(0);
    }

    @Override
    public void pushClient(final AbstractFaceletContext actx, final AbstractFacelet owner, final TemplateClient client)
    {
        _clients.add(0, new TemplateManagerImpl(owner, client, true, actx.getPageContext()));
        _lastClient = _clients.get(0);
    }

    public TemplateManager popExtendedClient(final AbstractFaceletContext actx)
    {
        _lastClient = null;
        return _clients.remove(_clients.size()-1);
    }
    
    @Override
    public void extendClient(final AbstractFaceletContext actx, final AbstractFacelet owner,
                             final TemplateClient client)
    {
        _clients.add(new TemplateManagerImpl(owner, client, false, actx.getPageContext()));
        _lastClient = _clients.get(_clients.size()-1);
    }

    @Override
    public boolean includeDefinition(FaceletContext ctx, Facelet owner, UIComponent parent, String name)
            throws IOException, FaceletException, FacesException, ELException
    {
        boolean found = false;
        TemplateManager client;
        for (int i = 0; i < _clients.size() && !found; i++)
        {
            client = _clients.get(i);
            if (client.equals(owner))
            {
                continue;
            }
            found = client.apply(ctx, parent, name);
        }
        return found;
    }
    
    private final static class TemplateManagerImpl extends TemplateManager implements TemplateClient
    {
        private final AbstractFacelet _owner;

        private final TemplateClient _target;

        private final boolean _root;

        private Set<String> _names;
        
        private final PageContext _pageContext;
        
        private Map<String, ValueExpression> _parameters = null;
        
        private Set<String> _knownParameters;

        public TemplateManagerImpl(AbstractFacelet owner, TemplateClient target,
                boolean root, PageContext pageContext)
        {
            this._owner = owner;
            this._target = target;
            this._root = root;
            this._pageContext = pageContext;
        }

        public boolean apply(FaceletContext ctx, UIComponent parent, String name)
                throws IOException, FacesException, FaceletException,
                ELException
        {
            String testName = (name != null) ? name : "facelets._NULL_DEF_";
            if (this._owner == null)
            {
                return false;
            }
            if (this._names != null && this._names.contains(testName))
            {
                return false;
            }
            else
            {
                if (this._names == null)
                {
                    this._names =  new HashSet<String>();
                }
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
        
        public Map<String, ValueExpression> getParametersMap()
        {
            if (_parameters == null)
            {
                _parameters = new HashMap<String, ValueExpression>();
            }
            return _parameters;
        }
        
        public boolean isParametersMapEmpty()
        {
            return _parameters == null ? true : _parameters.isEmpty();
        }

        public Set<String> getKnownParameters()
        {
            if (_knownParameters == null)
            {
                _knownParameters = new HashSet<String>(4);
            }
            return _knownParameters;
        }
        
        public boolean isKnownParametersEmpty()
        {
            return _knownParameters == null ? true : _knownParameters.isEmpty();
        }
        
        public boolean equals(Object o)
        {
            if (this._owner != null)
            {
                return this._owner == o || this._target == o;
            }
            else
            {
                return this._target == o;
            }
        }

        @Override
        public int hashCode()
        {
            int result = _owner != null ? _owner.hashCode() : 0;
            result = 31 * result + (_target != null ? _target.hashCode() : 0);
            return result;
        }

        public boolean isRoot()
        {
            return this._root;
        }
    }
    
    
    public TemplateManager getCompositeComponentClient()
    {
        return _compositeComponentClient;
    }

    public void setCompositeComponentClient(
            TemplateManager compositeComponentClient)
    {
        _compositeComponentClient = compositeComponentClient;
    }

    
    @Override
    public ValueExpression getParameter(String key)
    {
        TemplateManagerImpl client;
        for (int i = 0; i < _clients.size(); i++)
        {
            client = _clients.get(i);
            if (!client.isParametersMapEmpty() &&
                 client.getParametersMap().containsKey(key))
            {
                return client.getParametersMap().get(key);
            }
        }
        return null;
    }

    @Override
    public boolean containsParameter(String key)
    {
        TemplateManagerImpl client;
        for (int i = 0; i < _clients.size(); i++)
        {
            client = _clients.get(i);
            if (!client.isParametersMapEmpty() &&
                client.getParametersMap().containsKey(key))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setParameter(String key, ValueExpression value)
    {
        if (_lastClient != null)
        {
            _lastClient.getParametersMap().put(key, value);
            _lastClient.getKnownParameters().add(key);
        }
    }

    @Override
    public boolean isParameterEmpty()
    {
        TemplateManagerImpl client;
        for (int i = 0; i < _clients.size(); i++)
        {
            client = _clients.get(i);
            if (!client.isParametersMapEmpty())
            {
                return false;
            }
        }
        return true;
    }
    
    public Map<String, ValueExpression> getParameterMap()
    {
        return _templateClientAttributeMap;
    }
    
    public boolean isKnownParametersEmpty()
    {
        TemplateManagerImpl client;
        for (int i = 0; i < _clients.size(); i++)
        {
            client = _clients.get(i);
            if (!client.isKnownParametersEmpty())
            {
                return false;
            }
        }
        return true;
    }
    
    public Set<String> getKnownParameters()
    {
        return _templateClientKnownParameterMap.keySet();
    }
    
    @Override
    public boolean containsKnownParameter(String key)
    {
        TemplateManagerImpl client;
        for (int i = 0; i < _clients.size(); i++)
        {
            client = _clients.get(i);
            if (client.getKnownParameters().contains(key))
            {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void addKnownParameters(String key)
    {
        if (_lastClient != null)
        {        
            _lastClient.getKnownParameters().add(key);
        }
    }

    private final class TemplateClientAttributeMap extends AbstractAttributeMap<ValueExpression>
    {

        public TemplateClientAttributeMap()
        {
        }
        
        @Override
        protected ValueExpression getAttribute(String key)
        {
            TemplateManagerImpl client;
            for (int i = 0; i < _clients.size(); i++)
            {
                client = _clients.get(i);
                if (!client.isParametersMapEmpty() &&
                     client.getParametersMap().containsKey(key))
                {
                    return client.getParametersMap().get(key);
                }
            }
            return null;
        }

        @Override
        protected void setAttribute(String key, ValueExpression value)
        {
            //Use the parent methods.
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeAttribute(String key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Enumeration<String> getAttributeNames()
        {
            Set<String> attributeNames = new HashSet<String>();
            TemplateManagerImpl client;
            for (int i = 0; i < _clients.size(); i++)
            {
                client = _clients.get(i);
                if (!client.isParametersMapEmpty())
                {
                    attributeNames.addAll(client.getParametersMap().keySet());
                }
            }
            
            return new ParameterNameEnumeration(attributeNames.toArray(new String[attributeNames.size()]));
        }
    }
    
    private static class ParameterNameEnumeration implements Enumeration<String>
    {
        private final String[] _parameterNames;
        private final int _length;
        private int _index;

        public ParameterNameEnumeration(final String[] parameterNames)
        {
            _parameterNames = parameterNames;
            _length = parameterNames.length;
        }

        public boolean hasMoreElements()
        {
            return _index < _length;
        }

        public String nextElement()
        {
            if (!hasMoreElements())
            {
                throw new NoSuchElementException();
            }
            return _parameterNames[_index++];
        }
    }
    
    private final class TemplateClientKnownParameterMap extends AbstractAttributeMap<Boolean>
    {

        public TemplateClientKnownParameterMap()
        {
        }
        
        @Override
        protected Boolean getAttribute(String key)
        {
            TemplateManagerImpl client;
            for (int i = 0; i < _clients.size(); i++)
            {
                client = _clients.get(i);
                if (!client.isKnownParametersEmpty() &&
                     client.getKnownParameters().contains(key))
                {
                    return Boolean.TRUE;
                }
            }
            return null;
        }

        @Override
        protected void setAttribute(String key, Boolean value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeAttribute(String key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Enumeration<String> getAttributeNames()
        {
            Set<String> attributeNames = new HashSet<String>();
            TemplateManagerImpl client;
            for (int i = 0; i < _clients.size(); i++)
            {
                client = _clients.get(i);
                if (!client.isParametersMapEmpty())
                {
                    attributeNames.addAll(client.getParametersMap().keySet());
                }
            }
            
            return new ParameterNameEnumeration(attributeNames.toArray(new String[attributeNames.size()]));
        }
    }    
    
    /**
     * This is just a dummy template client that does nothing that is added by default
     * for each template context 
     *
     */
    public static final class InitialTemplateClient implements TemplateClient
    {
        public boolean apply(FaceletContext ctx, UIComponent parent, String name)
                throws IOException, FacesException, FaceletException, ELException
        {
            return false;
        }
    }
    
    public static final class InitialPageContext extends PageContext
    {
        private boolean _isCacheELExpressions;
        
        public InitialPageContext()
        {
            _isCacheELExpressions = true;
        }
        
        @Override
        public Map<String, ValueExpression> getAttributes()
        {
            return Collections.emptyMap();
        }

        @Override
        public int getAttributeCount()
        {
            return 0;
        }

        @Override
        public boolean isAllowCacheELExpressions()
        {
            return _isCacheELExpressions;
        }

        @Override
        public void setAllowCacheELExpressions(boolean cacheELExpressions)
        {
            _isCacheELExpressions = cacheELExpressions;
        }
    }


    @Override
    public boolean isAllowCacheELExpressions()
    {
        return _isCacheELExpressions;
    }

    @Override
    public void setAllowCacheELExpressions(boolean cacheELExpressions)
    {
        _isCacheELExpressions = cacheELExpressions;
    }
    
}
