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
package org.apache.myfaces.view.facelets.compiler;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.UniqueIdVendor;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.render.Renderer;
import javax.faces.view.Location;

import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
class UILeaf extends UIComponent implements Map<String, Object>
{
    //-------------- START TAKEN FROM UIComponentBase ----------------
    private static final String _STRING_BUILDER_KEY
            = "javax.faces.component.UIComponentBase.SHARED_STRING_BUILDER";
    
    private String _clientId = null;
    
    private String _id = null;

    public String getClientId(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (_clientId != null)
        {
            return _clientId;
        }

        //boolean idWasNull = false;
        String id = getId();
        if (id == null)
        {
            // Although this is an error prone side effect, we automatically create a new id
            // just to be compatible to the RI
            
            // The documentation of UniqueIdVendor says that this interface should be implemented by
            // components that also implements NamingContainer. The only component that does not implement
            // NamingContainer but UniqueIdVendor is UIViewRoot. Anyway we just can't be 100% sure about this
            // fact, so it is better to scan for the closest UniqueIdVendor. If it is not found use 
            // viewRoot.createUniqueId, otherwise use UniqueIdVendor.createUniqueId(context,seed).
            UniqueIdVendor parentUniqueIdVendor = _ComponentUtils.findParentUniqueIdVendor(this);
            if (parentUniqueIdVendor == null)
            {
                UIViewRoot viewRoot = context.getViewRoot();
                if (viewRoot != null)
                {
                    id = viewRoot.createUniqueId();
                }
                else
                {
                    // The RI throws a NPE
                    String location = getComponentLocation(this);
                    throw new FacesException("Cannot create clientId. No id is assigned for component"
                            + " to create an id and UIViewRoot is not defined: "
                            + getPathToComponent(this)
                            + (location != null ? " created from: " + location : ""));
                }
            }
            else
            {
                id = parentUniqueIdVendor.createUniqueId(context, null);
            }
            setId(id);
            // We remember that the id was null and log a warning down below
            // idWasNull = true;
        }

        UIComponent namingContainer = _ComponentUtils.findParentNamingContainer(this, false);
        if (namingContainer != null)
        {
            String containerClientId = namingContainer.getContainerClientId(context);
            if (containerClientId != null)
            {
                StringBuilder bld = _getSharedStringBuilder(context);
                _clientId = bld.append(containerClientId).append(
                                      context.getNamingContainerSeparatorChar()).append(id).toString();
            }
            else
            {
                _clientId = id;
            }
        }
        else
        {
            _clientId = id;
        }

        Renderer renderer = getRenderer(context);
        if (renderer != null)
        {
            _clientId = renderer.convertClientId(context, _clientId);
        }

        // -=Leonardo Uribe=- In jsf 1.1 and 1.2 this warning has sense, but in jsf 2.0 it is common to have
        // components without any explicit id (UIViewParameter components and UIOuput resource components) instances.
        // So, this warning is becoming obsolete in this new context and should be removed.
        //if (idWasNull && log.isLoggable(Level.WARNING))
        //{
        //    log.warning("WARNING: Component " + _clientId
        //            + " just got an automatic id, because there was no id assigned yet. "
        //            + "If this component was created dynamically (i.e. not by a JSP tag) you should assign it an "
        //            + "explicit static id or assign it the id you get from "
        //            + "the createUniqueId from the current UIViewRoot "
        //            + "component right after creation! Path to Component: " + getPathToComponent(this));
        //}

        return _clientId;
    }
    
    public String getId()
    {
        return _id;
    }
    
    @Override
    public void setId(String id)
    {
        // UILeaf instance are just a wrapper for html markup. It never has 
        // an user defined id. The validation check here is just useless, 
        // because facelets algorithm ensures that.
        //isIdValid(id);
        _id = id;
        _clientId = null;
    }
    /*
    private void isIdValid(String string)
    {

        // is there any component identifier ?
        if (string == null)
        {
            return;
        }

        // Component identifiers must obey the following syntax restrictions:
        // 1. Must not be a zero-length String.
        if (string.length() == 0)
        {
            throw new IllegalArgumentException("component identifier must not be a zero-length String");
        }

        // If new id is the same as old it must be valid
        if (string.equals(_id))
        {
            return;
        }

        // 2. First character must be a letter or an underscore ('_').
        if (!Character.isLetter(string.charAt(0)) && string.charAt(0) != '_')
        {
            throw new IllegalArgumentException("component identifier's first character must be a letter "
                                               + "or an underscore ('_')! But it is \""
                                               + string.charAt(0) + "\"");
        }
        for (int i = 1; i < string.length(); i++)
        {
            char c = string.charAt(i);
            // 3. Subsequent characters must be a letter, a digit, an underscore ('_'), or a dash ('-').
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_')
            {
                throw new IllegalArgumentException("Subsequent characters of component identifier must be a letter, "
                                                   + "a digit, an underscore ('_'), or a dash ('-')! "
                                                   + "But component identifier contains \""
                                                   + c + "\"");
            }
        }
    }*/
    
    private String getComponentLocation(UIComponent component)
    {
        Location location = (Location) component.getAttributes()
                .get(UIComponent.VIEW_LOCATION_KEY);
        if (location != null)
        {
            return location.toString();
        }
        return null;
    }

    private String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }

    private void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot) component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0, intBuf.toString());

        getPathToComponent(component.getParent(), buf);
    }
    
    static StringBuilder _getSharedStringBuilder(FacesContext facesContext)
    {
        Map<Object, Object> attributes = facesContext.getAttributes();

        StringBuilder sb = (StringBuilder) attributes.get(_STRING_BUILDER_KEY);

        if (sb == null)
        {
            sb = new StringBuilder();
            attributes.put(_STRING_BUILDER_KEY, sb);
        }
        else
        {

            // clear out the stringBuilder by setting the length to 0
            sb.setLength(0);
        }

        return sb;
    }

    //-------------- END TAKEN FROM UICOMPONENTBASE ------------------
    

    private static Map<String, UIComponent> facets = new HashMap<String, UIComponent>()
    {

        @Override
        public void putAll(Map<? extends String, ? extends UIComponent> map)
        {
            // do nothing
        }

        @Override
        public UIComponent put(String name, UIComponent value)
        {
            return null;
        }
    };

    private UIComponent parent;
    
    //private _ComponentAttributesMap attributesMap;

    @Override
    public Map<String, Object> getAttributes()
    {
        // Since all components extending UILeaf are only transient references
        // to text or instructions, we can do the following simplifications:  
        // 1. Don't use reflection to retrieve properties, because it will never
        // be done
        // 2. Since the only key that will be saved here is MARK_ID, we can create
        // a small map of size 2. In practice, this will prevent create a lot of
        // maps, like the ones used on state helper
        return this;
    }

    @Override
    public void clearInitialState()
    {
       //this component is transient, so it can be marked, because it does not have state!
    }

    @Override
    public boolean initialStateMarked()
    {
        //this component is transient, so it can be marked, because it does not have state!
        return false;
    }

    @Override
    public void markInitialState()
    {
        //this component is transient, so no need to do anything
    }

    
    @Override
    public void processEvent(ComponentSystemEvent event)
            throws AbortProcessingException
    {
        //Do nothing, because UILeaf will not need to handle "binding" property
    }

    @Override
    @SuppressWarnings("deprecation")
    public ValueBinding getValueBinding(String binding)
    {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setValueBinding(String name, ValueBinding binding)
    {
        // do nothing
    }

    @Override
    public ValueExpression getValueExpression(String name)
    {
        return null;
    }

    @Override
    public void setValueExpression(String name, ValueExpression arg1)
    {
        // do nothing
    }

    public String getFamily()
    {
        return "facelets.LiteralText";
    }

    @Override
    public UIComponent getParent()
    {
        return this.parent;
    }

    @Override
    public void setParent(UIComponent parent)
    {
        this.parent = parent;
    }

    @Override
    public boolean isRendered()
    {
        return true;
    }

    @Override
    public void setRendered(boolean rendered)
    {
        // do nothing
    }

    @Override
    public String getRendererType()
    {
        return null;
    }

    @Override
    public void setRendererType(String rendererType)
    {
        // do nothing
    }

    @Override
    public boolean getRendersChildren()
    {
        return true;
    }

    @Override
    public List<UIComponent> getChildren()
    {
        List<UIComponent> children = Collections.emptyList();
        return children;
    }

    @Override
    public int getChildCount()
    {
        return 0;
    }

    @Override
    public UIComponent findComponent(String id)
    {
        return null;
    }

    @Override
    public Map<String, UIComponent> getFacets()
    {
        return facets;
    }

    @Override
    public int getFacetCount()
    {
        return 0;
    }

    @Override
    public UIComponent getFacet(String name)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<UIComponent> getFacetsAndChildren()
    {
        // Performance: Collections.emptyList() is Singleton,
        // but .iterator() creates new instance of AbstractList$Itr every invocation, because
        // emptyList() extends AbstractList.  Therefore we cannot use Collections.emptyList() here. 
        return EmptyIterator.INSTANCE;
    }

    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        // do nothing
    }

    @Override
    public void decode(FacesContext faces)
    {
        // do nothing
    }

    @Override
    public void encodeBegin(FacesContext faces) throws IOException
    {
        // do nothing
    }

    @Override
    public void encodeChildren(FacesContext faces) throws IOException
    {
        // do nothing
    }

    @Override
    public void encodeEnd(FacesContext faces) throws IOException
    {
        // do nothing
    }

    @Override
    public void encodeAll(FacesContext faces) throws IOException
    {
        this.encodeBegin(faces);
    }

    @Override
    protected void addFacesListener(FacesListener faces)
    {
        // do nothing
    }

    @Override
    protected FacesListener[] getFacesListeners(Class faces)
    {
        return null;
    }

    @Override
    protected void removeFacesListener(FacesListener faces)
    {
        // do nothing
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        // do nothing
    }

    @Override
    public void processRestoreState(FacesContext faces, Object state)
    {
        // do nothing
    }

    @Override
    public void processDecodes(FacesContext faces)
    {
        // do nothing
    }

    @Override
    public void processValidators(FacesContext faces)
    {
        // do nothing
    }

    @Override
    public void processUpdates(FacesContext faces)
    {
        // do nothing
    }

    @Override
    public Object processSaveState(FacesContext faces)
    {
        return null;
    }

    @Override
    protected FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    @Override
    protected Renderer getRenderer(FacesContext faces)
    {
        return null;
    }

    public Object saveState(FacesContext faces)
    {
        return null;
    }

    public void restoreState(FacesContext faces, Object state)
    {
        // do nothing
    }

    public boolean isTransient()
    {
        return true;
    }

    public void setTransient(boolean tranzient)
    {
        // do nothing
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
            throws FacesException
    {
        //this component will never be a target for a callback, so always return false.
        return false;
    }

    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback)
    {
        // the visiting is complete and it shouldn't affect the visiting of the other
        // children of the parent component, therefore return false
        return false;
    }

    //-------------- START ATTRIBUTE MAP IMPLEMENTATION ----------------

    private Map<String, Object> _attributes = null;
    private String _markCreated = null;

    public void setMarkCreated(String markCreated)
    {
        _markCreated = markCreated;
    }

    public int size()
    {
        return _attributes == null ? 0 : _attributes.size();
    }
         
    public void clear()
    {
        if (_attributes != null)
        {
         _attributes.clear();
        _markCreated = null;
        }
    }
         
    public boolean isEmpty()
    {
        if (_markCreated == null)
        {
            return _attributes == null ? false : _attributes.isEmpty();
        }
        else
        {
            return false;
        }
    }
         
    public boolean containsKey(Object key)
    {
        checkKey(key);

        if (ComponentSupport.MARK_CREATED.equals(key))
        {
            return _markCreated != null;
        }
        else
        {
            return (_attributes == null ? false :_attributes.containsKey(key));
        }
    }
         
    public boolean containsValue(Object value)
    {
        if (_markCreated != null && _markCreated.equals(value))
        {
            return true;
        }
        return (_attributes == null) ? false : _attributes.containsValue(value);
    }

    public Collection<Object> values()
    {
        return getUnderlyingMap().values();
    }

    public void putAll(Map<? extends String, ? extends Object> t)
    {
        for (Map.Entry<? extends String, ? extends Object> entry : t.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public Set<Entry<String, Object>> entrySet()
    {
        return getUnderlyingMap().entrySet();
    }

    public Set<String> keySet()
    {
        return getUnderlyingMap().keySet();
    }

    public Object get(Object key)
    {
        checkKey(key);

        if ("rendered".equals(key))
        {
            return true;
        }
        if ("transient".equals(key))
        {
            return true;
        }
        if (ComponentSupport.MARK_CREATED.equals(key))
        {
            return _markCreated;
        }
        return (_attributes == null) ? null : _attributes.get(key);
    }

    public Object remove(Object key)
    {
        checkKey(key);

        if (ComponentSupport.MARK_CREATED.equals(key))
        {
            _markCreated = null;
        }
         return (_attributes == null) ? null : _attributes.remove(key);
    }
         
    public Object put(String key, Object value)
    {
        checkKey(key);

        if (ComponentSupport.MARK_CREATED.equals(key))
        {
            String old = _markCreated;
            _markCreated = (String) value;
            return old;
        }
        return getUnderlyingMap().put(key, value);
    }

    private void checkKey(Object key)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        if (!(key instanceof String))
        {
            throw new ClassCastException("key is not a String");
        }
    }

    Map<String, Object> getUnderlyingMap()
    {
        if (_attributes == null)
        {
            _attributes = new HashMap<String, Object>(2,1);
        }
        return _attributes;
    }

    @Override
    public Map<String, Object> getPassThroughAttributes(boolean create)
    {
        if (create)
        {
            // Just return an empty map, because UILeaf cannot contain
            // passthrough attributes.
            return Collections.emptyMap();
        }
        else
        {
            return null;
        }
    }

}
