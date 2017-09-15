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
package javax.faces.component;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

/**
 * A custom implementation of the Map interface, where get and put calls
 * try to access getter/setter methods of an associated UIComponent before
 * falling back to accessing a real Map object.
 * <p/>
 * Some of the behaviours of this class don't really comply with the
 * definitions of the Map class; for example the key parameter to all
 * methods is required to be of type String only, and after clear(),
 * calls to get can return non-null values. However the JSF spec
 * requires that this class behave in the way implemented below. See
 * UIComponent.getAttributes for more details.
 * <p/>
 * The term "property" is used here to refer to real javabean properties
 * on the underlying UIComponent, while "attribute" refers to an entry
 * in the associated Map.
 */
class _ComponentAttributesMap implements Map<String, Object>, Serializable
{
    private static final long serialVersionUID = -9106832179394257866L;

    private static final Object[] EMPTY_ARGS = new Object[0];
    
    private final static String MARK_CREATED = "oam.vf.MARK_ID";
    
    private final static String FACET_NAME_KEY = "facelets.FACET_NAME";
    
    public final static String FACET_CREATED_UIPANEL_MARKER = "oam.vf.createdUIPanel";
    
    private final static String COMPONENT_ADDED_BY_HANDLER_MARKER = "oam.vf.addedByHandler";
    
    public static final String PROPERTY_DESCRIPTOR_MAP_KEY = "oam.cc.beanInfo.PDM";
    
    /**
     * This variable works as a check to indicate the minimun lenght we need to check
     * for the special attributes, and save some time in get(), containsKey() and 
     * put() operations.
     */
    private final static int MIN_LENGHT_CHECK = MARK_CREATED.length();

    // The component that is read/written via this map.
    private UIComponentBase _component;

    // We delegate instead of derive from HashMap, so that we can later
    // optimize Serialization
    // JSF 2.0 Changed getUnderlyingMap to point to StateHelper attributesMap
    //private Map<String, Object> _attributes = null;

    // A cached hashmap of propertyName => PropertyDescriptor object for all
    // the javabean properties of the associated component. This is built by
    // introspection on the associated UIComponent. Don't serialize this as
    // it can always be recreated when needed.
    private transient Map<String, _PropertyDescriptorHolder> _propertyDescriptorMap = null;

    // Cache for component property descriptors
    private static Map<Class<?>, Map<String, _PropertyDescriptorHolder>> propertyDescriptorCache =
        new WeakHashMap<Class<?>, Map<String, _PropertyDescriptorHolder>>();
    
    private boolean _isCompositeComponent;
    private boolean _isCompositeComponentSet;
    
    private BeanInfo _ccBeanInfo;

    /**
     * Create a map backed by the specified component.
     * <p/>
     * This method is expected to be called when a component is first created.
     */
    _ComponentAttributesMap(UIComponentBase component)
    {
        _component = component;
    }
    
    /**
     * Create a map backed by the specified component. Attributes already
     * associated with the component are provided in the specified Map
     * class. A reference to the provided map is kept; this object's contents
     * are updated during put calls on this instance.
     * <p/>
     * This method is expected to be called during the "restore view" phase.
     */
    //JSF 2.0 removed because _attributes has been replaced with StateHelper attributesMap
    //_ComponentAttributesMap(UIComponent component, Map<String, Object> attributes)
    //{
    //    _component = component;
        //_attributes = new HashMap<String, Object>(attributes);
    //}
    
    /**
     * Return the number of <i>attributes</i> in this map. Properties of the
     * underlying UIComponent are not counted.
     * <p/>
     * Note that because the get method can read properties of the
     * UIComponent and evaluate value-bindings, it is possible to have
     * size return zero while calls to the get method return non-null
     * values.
     */
    public int size()
    {
        return getUnderlyingMap().size();
    }

    /**
     * Clear all the <i>attributes</i> in this map. Properties of the
     * underlying UIComponent are not modified.
     * <p/>
     * Note that because the get method can read properties of the
     * UIComponent and evaluate value-bindings, it is possible to have
     * calls to the get method return non-null values immediately after
     * a call to clear.
     */
    public void clear()
    {
        getUnderlyingMap().clear();
    }

    /**
     * Return true if there are no <i>attributes</i> in this map. Properties
     * of the underlying UIComponent are not counted.
     * <p/>
     * Note that because the get method can read properties of the
     * UIComponent and evaluate value-bindings, it is possible to have
     * isEmpty return true, while calls to the get method return non-null
     * values.
     */
    public boolean isEmpty()
    {
        return getUnderlyingMap().isEmpty();
    }

    /**
     * Return true if there is an <i>attribute</i> with the specified name,
     * but false if there is a javabean <i>property</i> of that name on the
     * associated UIComponent.
     * <p/>
     * Note that it should be impossible for the attributes map to contain
     * an entry with the same name as a javabean property on the associated
     * UIComponent.
     *
     * @param key <i>must</i> be a String. Anything else will cause a
     *            ClassCastException to be thrown.
     */
    public boolean containsKey(Object key)
    {
        checkKey(key);

        int keyLength = ((String)key).length();
        if (keyLength >= MIN_LENGHT_CHECK)
        {
            if (MARK_CREATED.length() == keyLength &&
                MARK_CREATED.equals(key))
            {
                return ((UIComponentBase)_component).getOamVfMarkCreated() != null;
            }
            else if (FACET_NAME_KEY.length() == keyLength &&
                FACET_NAME_KEY.equals(key))
            {
                return _component.getOamVfFacetName() != null;
            }
            else if (COMPONENT_ADDED_BY_HANDLER_MARKER.length() == keyLength &&
                COMPONENT_ADDED_BY_HANDLER_MARKER.equals(key))
            {
                return _component.isOamVfAddedByHandler();
            }
            else if (FACET_CREATED_UIPANEL_MARKER.length() == keyLength &&
                FACET_CREATED_UIPANEL_MARKER.equals(key))
            {
                return _component.isOamVfFacetCreatedUIPanel();
            }
            // The most common call to this method comes from UIComponent.isCompositeComponent()
            // to reduce the impact. This is better than two lookups, once over property descriptor map
            // and the other one from the underlying map.
            if (Resource.COMPONENT_RESOURCE_KEY.length() == keyLength &&
                Resource.COMPONENT_RESOURCE_KEY.equals(key))
            {
                if (!_isCompositeComponentSet)
                {
                    // Note we are not setting _isCompositeComponentSet, because when the component tree is built
                    // using JSF 1.2 state saving, PostAddToViewEvent is propagated and the component is check 
                    // if is a composite component, but the state is not restored, so the check return always
                    // false. A check for processing events was added to prevent that scenario, but anyway that 
                    // makes invalid set _isCompositeComponentSet to true on this location.
                    _isCompositeComponent = getUnderlyingMap().containsKey(Resource.COMPONENT_RESOURCE_KEY);
                }
                return _isCompositeComponent;
            }
        }
        return getPropertyDescriptor((String) key) == null ? getUnderlyingMap().containsKey(key) : false;
    }

    /**
     * Returns true if there is an <i>attribute</i> with the specified
     * value. Properties of the underlying UIComponent aren't examined,
     * nor value-bindings.
     *
     * @param value null is allowed
     */
    public boolean containsValue(Object value)
    {
        return getUnderlyingMap().containsValue(value);
    }

    /**
     * Return a collection of the values of all <i>attributes</i>. Property
     * values are not included, nor value-bindings.
     */
    public Collection<Object> values()
    {
        return getUnderlyingMap().values();
    }

    /**
     * Call put(key, value) for each entry in the provided map.
     */
    public void putAll(Map<? extends String, ?> t)
    {
        for (Map.Entry<? extends String, ?> entry : t.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Return a set of all <i>attributes</i>. Properties of the underlying
     * UIComponent are not included, nor value-bindings.
     */
    public Set<Map.Entry<String, Object>> entrySet()
    {
        return getUnderlyingMap().entrySet();
    }

    /**
     * Return a set of the keys for all <i>attributes</i>. Properties of the
     * underlying UIComponent are not included, nor value-bindings.
     */
    public Set<String> keySet()
    {
        return getUnderlyingMap().keySet();
    }

    /**
     * In order: get the value of a <i>property</i> of the underlying
     * UIComponent, read an <i>attribute</i> from this map, or evaluate
     * the component's value-binding of the specified name.
     *
     * @param key must be a String. Any other type will cause ClassCastException.
     */
    public Object get(Object key)
    {
        checkKey(key);
        
        Object value;

        int keyLength = ((String)key).length();
        if (keyLength >= MIN_LENGHT_CHECK)
        {
            if (MARK_CREATED.length() == keyLength &&
                MARK_CREATED.equals(key))
            {
                return _component.getOamVfMarkCreated();
            }
            else if (FACET_NAME_KEY.length() == keyLength &&
                FACET_NAME_KEY.equals(key))
            {
                return _component.getOamVfFacetName();
            }
            else if (COMPONENT_ADDED_BY_HANDLER_MARKER.length() == keyLength &&
                COMPONENT_ADDED_BY_HANDLER_MARKER.equals(key))
            {
                return _component.isOamVfAddedByHandler();
            }
            else if (FACET_CREATED_UIPANEL_MARKER.length() == keyLength &&
                FACET_CREATED_UIPANEL_MARKER.equals(key))
            {
                return _component.isOamVfFacetCreatedUIPanel();
            }
        }
        // is there a javabean property to read?
        _PropertyDescriptorHolder propertyDescriptor = getPropertyDescriptor((String) key);
        if (propertyDescriptor != null)
        {
            value = getComponentProperty(propertyDescriptor);
        }
        else
        {
            // is there a literal value to read?
            value = getUnderlyingMap().get(key);
            if (value == null)
            {
                // is there a value-binding to read?
                ValueExpression ve = _component.getValueExpression((String) key);
                if (ve != null)
                {
                    value = ve.getValue(_component.getFacesContext().getELContext());
                }
                else
                {
                    if (!_isCompositeComponentSet)
                    {
                        _isCompositeComponent = getUnderlyingMap().containsKey(Resource.COMPONENT_RESOURCE_KEY);
                        _isCompositeComponentSet = true;
                    }
                    if (_isCompositeComponent)
                    {
                        BeanInfo ccBeanInfo = _ccBeanInfo != null ? _ccBeanInfo :
                            (BeanInfo) getUnderlyingMap().get(UIComponent.BEANINFO_KEY);
                        if (ccBeanInfo != null)
                        {
                            //Fast shortcut to allow fast lookup.
                            Map<String, PropertyDescriptor> attributeMap = (Map<String, PropertyDescriptor>) 
                                ccBeanInfo.getBeanDescriptor().getValue(
                                    PROPERTY_DESCRIPTOR_MAP_KEY);
                            if (attributeMap != null)
                            {
                                PropertyDescriptor attribute = attributeMap.get(key);
                                if (attribute != null)
                                {
                                    String attributeName = attribute.getName();
                                    boolean isKnownMethod = "action".equals(attributeName)
                                            || "actionListener".equals(attributeName)
                                            || "validator".equals(attributeName)
                                            || "valueChangeListener".equals(attributeName);

                                    // <composite:attribute> method-signature attribute is 
                                    // ValueExpression that must evaluate to String
                                    ValueExpression methodSignatureExpression
                                            = (ValueExpression) attribute.getValue("method-signature");
                                    String methodSignature = null;
                                    if (methodSignatureExpression != null)
                                    {
                                        // Check if the value expression holds a method signature
                                        // Note that it could be null, so in that case we don't have to 
                                        // do anything
                                        methodSignature = (String) methodSignatureExpression.getValue(
                                                                    _component.getFacesContext().getELContext());
                                    }

                                    // either the attributeName has to be a knownMethod
                                    // or there has to be a method-signature
                                    if (isKnownMethod || methodSignature != null)
                                    {
                                        //In this case it is expecting a ValueExpression
                                        return attribute.getValue("default");
                                    }
                                    else
                                    {
                                        value = attribute.getValue("default");
                                    }
                                }
                            }
                            else
                            {
                                // Failsafe if another implementation for composite components is set
                                for (PropertyDescriptor attribute : ccBeanInfo.getPropertyDescriptors())
                                {
                                    if (attribute.getName().equals(key))
                                    {
                                        String attributeName = attribute.getName();
                                        boolean isKnownMethod = "action".equals(attributeName)
                                                || "actionListener".equals(attributeName)
                                                || "validator".equals(attributeName)
                                                || "valueChangeListener".equals(attributeName);

                                        // <composite:attribute> method-signature attribute is 
                                        // ValueExpression that must evaluate to String
                                        ValueExpression methodSignatureExpression
                                                = (ValueExpression) attribute.getValue("method-signature");
                                        String methodSignature = null;
                                        if (methodSignatureExpression != null)
                                        {
                                            // Check if the value expression holds a method signature
                                            // Note that it could be null, so in that case we don't have to 
                                            // do anything
                                            methodSignature = (String) methodSignatureExpression.getValue(
                                                                        _component.getFacesContext().getELContext());
                                        }

                                        // either the attributeName has to be a knownMethod
                                        // or there has to be a method-signature
                                        if (isKnownMethod || methodSignature != null)
                                        {
                                            //In this case it is expecting a ValueExpression
                                            return attribute.getValue("default");
                                        }
                                        else
                                        {
                                            value = attribute.getValue("default");
                                            break;
                                        }
                                    }
                                }
                            }
                            // We have to check for a ValueExpression and also evaluate it
                            // here, because in the PropertyDescriptor the default values are
                            // always stored as (Tag-)ValueExpressions.
                            if (value != null && value instanceof ValueExpression)
                            {
                                return ((ValueExpression) value).getValue(_component.getFacesContext().getELContext());
                            }
                        }
                    }
                    // no value found
                    //return null;
                }
            }
        }
        
        // Otherwise, return the actual value from the get() method. 
        return value;
    }

    /**
     * Remove the attribute with the specified name. An attempt to
     * remove an entry whose name is that of a <i>property</i> on
     * the underlying UIComponent will cause an IllegalArgumentException.
     * Value-bindings for the underlying component are ignored.
     *
     * @param key must be a String. Any other type will cause ClassCastException.
     */
    public Object remove(Object key)
    {
        checkKey(key);
        int keyLength = ((String)key).length();
        if (keyLength >= MIN_LENGHT_CHECK)
        {
            if (MARK_CREATED.length() == keyLength &&
                MARK_CREATED.equals(key))
            {
                Object oldValue = _component.getOamVfMarkCreated();
                _component.setOamVfMarkCreated(null);
                return oldValue;
            }
            else if (FACET_NAME_KEY.length() == keyLength &&
                FACET_NAME_KEY.equals(key))
            {
                Object oldValue = _component.getOamVfFacetName();
                _component.setOamVfFacetName(null);
                return oldValue;
            }
            else if (COMPONENT_ADDED_BY_HANDLER_MARKER.length() == keyLength &&
                COMPONENT_ADDED_BY_HANDLER_MARKER.equals(key))
            {
                Object oldValue = _component.isOamVfAddedByHandler();
                _component.setOamVfAddedByHandler(false);
                return oldValue;
            }
            else if (FACET_CREATED_UIPANEL_MARKER.length() == keyLength &&
                FACET_CREATED_UIPANEL_MARKER.equals(key))
            {
                Object oldValue = _component.isOamVfFacetCreatedUIPanel();
                _component.setOamVfFacetCreatedUIPanel(false);
                return oldValue;
            }
            else if (UIComponent.BEANINFO_KEY.length() == keyLength 
                && UIComponent.BEANINFO_KEY.equals(key))
            {
                _ccBeanInfo = null;
            }
        }
        _PropertyDescriptorHolder propertyDescriptor = getPropertyDescriptor((String) key);
        if (propertyDescriptor != null)
        {
            throw new IllegalArgumentException("Cannot remove component property attribute");
        }
        return _component.getStateHelper().remove(
                UIComponentBase.PropertyKeys.attributesMap, key);
    }

    /**
     * Store the provided value as a <i>property</i> on the underlying
     * UIComponent, or as an <i>attribute</i> in a Map if no such property
     * exists. Value-bindings associated with the component are ignored; to
     * write to a value-binding, the value-binding must be explicitly
     * retrieved from the component and evaluated.
     * <p/>
     * Note that this method is different from the get method, which
     * does read from a value-binding if one exists. When a value-binding
     * exists for a non-property, putting a value here essentially "masks"
     * the value-binding until that attribute is removed.
     * <p/>
     * The put method is expected to return the previous value of the
     * property/attribute (if any). Because UIComponent property getter
     * methods typically try to evaluate any value-binding expression of
     * the same name this can cause an EL expression to be evaluated,
     * thus invoking a getter method on the user's model. This is fine
     * when the returned value will be used; Unfortunately this is quite
     * pointless when initialising a freshly created component with whatever
     * attributes were specified in the view definition (eg JSP tag
     * attributes). Because the UIComponent.getAttributes method
     * only returns a Map class and this class must be package-private,
     * there is no way of exposing a "putNoReturn" type method.
     *
     * @param key   String, null is not allowed
     * @param value null is allowed
     */
    public Object put(String key, Object value)
    {
        if (key == null)
        {
            throw new NullPointerException("key");
        }
        int keyLength = ((String)key).length();
        if (keyLength >= MIN_LENGHT_CHECK)
        {
            if (MARK_CREATED.length() == keyLength &&
                MARK_CREATED.equals(key))
            {
                String oldValue = _component.getOamVfMarkCreated();
                _component.setOamVfMarkCreated((String)value);
                return oldValue;
            }
            else if (FACET_NAME_KEY.length() == keyLength &&
                FACET_NAME_KEY.equals(key))
            {
                Object oldValue = _component.getOamVfFacetName();
                _component.setOamVfFacetName((String)value);
                return oldValue;
            }
            else if (COMPONENT_ADDED_BY_HANDLER_MARKER.length() == keyLength &&
                COMPONENT_ADDED_BY_HANDLER_MARKER.equals(key))
            {
                Object oldValue = _component.isOamVfAddedByHandler();
                _component.setOamVfAddedByHandler((Boolean)value);
                return oldValue;
            }
            else if (FACET_CREATED_UIPANEL_MARKER.length() == keyLength &&
                FACET_CREATED_UIPANEL_MARKER.equals(key))
            {
                Object oldValue = _component.isOamVfFacetCreatedUIPanel();
                _component.setOamVfFacetCreatedUIPanel((Boolean)value);
                return oldValue;
            }
        }
        _PropertyDescriptorHolder propertyDescriptor = getPropertyDescriptor(key);
        if (propertyDescriptor == null)
        {
            if (value == null)
            {
                throw new NullPointerException("value is null for a not available property: " + key);
            }
        }
        else
        {
            if (propertyDescriptor.getReadMethod() != null)
            {
                Object oldValue = getComponentProperty(propertyDescriptor);
                setComponentProperty(propertyDescriptor, value);
                return oldValue;
            }
            setComponentProperty(propertyDescriptor, value);
            return null;
        }
        // To keep this code in good shape, The fastest way to compare is look if the length first here
        // because we avoid an unnecessary cast later on equals().
        if ( Resource.COMPONENT_RESOURCE_KEY.length() == keyLength 
             && Resource.COMPONENT_RESOURCE_KEY.equals(key))
        {
            _isCompositeComponent = true;
            _isCompositeComponentSet = true;
        }
        if (UIComponent.BEANINFO_KEY.length() == keyLength 
            && UIComponent.BEANINFO_KEY.equals(key))
        {
            _ccBeanInfo = (BeanInfo) value;
        }
        return _component.getStateHelper().put(UIComponentBase.PropertyKeys.attributesMap, key, value);
    }

    /**
     * Retrieve info about getter/setter methods for the javabean property
     * of the specified name on the underlying UIComponent object.
     * <p/>
     * This method optimises access to javabean properties of the underlying
     * UIComponent by maintaining a cache of ProperyDescriptor objects for
     * that class.
     * <p/>
     * TODO: Consider making the cache shared between component instances;
     * currently 100 UIInputText components means performing introspection
     * on the UIInputText component 100 times.
     */
    private _PropertyDescriptorHolder getPropertyDescriptor(String key)
    {
        if (_propertyDescriptorMap == null)
        {
            // Try to get descriptor map from cache
            _propertyDescriptorMap = propertyDescriptorCache.get(_component.getClass());
            // Cache miss: create descriptor map and put it in cache
            if (_propertyDescriptorMap == null)
            {
                // Create descriptor map...
                BeanInfo beanInfo;
                try
                {
                    beanInfo = Introspector.getBeanInfo(_component.getClass());
                }
                catch (IntrospectionException e)
                {
                    throw new FacesException(e);
                }
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                _propertyDescriptorMap = new HashMap<String, _PropertyDescriptorHolder>();
                for (int i = 0; i < propertyDescriptors.length; i++)
                {
                    PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
                    Method readMethod = propertyDescriptor.getReadMethod();
                    if (readMethod != null)
                    {
                        _propertyDescriptorMap.put(propertyDescriptor.getName(),
                                new _PropertyDescriptorHolder(propertyDescriptor, readMethod));
                    }
                }
                // ... and put it in cache
                synchronized(propertyDescriptorCache)
                {
                    // Use a synchronized block to ensure proper operation on concurrent use cases.
                    // This is a racy single check, because initialization over the same class could happen
                    // multiple times, but the same result is always calculated. The synchronized block 
                    // just ensure thread-safety, because only one thread will modify the cache map
                    // at the same time.
                    propertyDescriptorCache.put(_component.getClass(), _propertyDescriptorMap);
                }
            }
        }
        return _propertyDescriptorMap.get(key);
    }


    /**
     * Execute the getter method of the specified property on the underlying
     * component.
     *
     * @param propertyDescriptor specifies which property to read.
     * @return the value returned by the getter method.
     * @throws IllegalArgumentException if the property is not readable.
     * @throws FacesException           if any other problem occurs while invoking
     *                                  the getter method.
     */
    private Object getComponentProperty(_PropertyDescriptorHolder propertyDescriptor)
    {
        Method readMethod = propertyDescriptor.getReadMethod();
        if (readMethod == null)
        {
            throw new IllegalArgumentException("Component property " + propertyDescriptor.getName()
                                               + " is not readable");
        }
        try
        {
            return readMethod.invoke(_component, EMPTY_ARGS);
        }
        catch (Exception e)
        {
            FacesContext facesContext = _component.getFacesContext();
            throw new FacesException("Could not get property " + propertyDescriptor.getName() + " of component "
                                     + _component.getClientId(facesContext), e);
        }
    }

    /**
     * Execute the setter method of the specified property on the underlying
     * component.
     *
     * @param propertyDescriptor specifies which property to write.
     * @throws IllegalArgumentException if the property is not writable.
     * @throws FacesException           if any other problem occurs while invoking
     *                                  the getter method.
     */
    private void setComponentProperty(_PropertyDescriptorHolder propertyDescriptor, Object value)
    {
        Method writeMethod = propertyDescriptor.getWriteMethod();
        if (writeMethod == null)
        {
            throw new IllegalArgumentException("Component property " + propertyDescriptor.getName()
                                               + " is not writable");
        }
        try
        {
            writeMethod.invoke(_component, new Object[]{value});
        }
        catch (Exception e)
        {
            FacesContext facesContext = _component.getFacesContext();
            throw new FacesException("Could not set property " + propertyDescriptor.getName() +
                    " of component " + _component.getClientId(facesContext) + " to value : " + value + " with type : " +
                    (value == null ? "null" : value.getClass().getName()), e);
        }
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

    /**
     * Return the map containing the attributes.
     * <p/>
     * This method is package-scope so that the UIComponentBase class can access it
     * directly when serializing the component.
     */
    Map<String, Object> getUnderlyingMap()
    {
        StateHelper stateHelper = _component.getStateHelper(false);
        Map<String, Object> attributes = null;
        if (stateHelper != null)
        {
            attributes = (Map<String, Object>) stateHelper.get(UIComponentBase.PropertyKeys.attributesMap);
        }
        return attributes == null ? Collections.EMPTY_MAP : attributes;
    }
    
    /**
     * TODO: Document why this method is necessary, and why it doesn't try to
     * compare the _component field.
     */
    @Override
    public boolean equals(Object obj)
    {
        return getUnderlyingMap().equals(obj);
    }

    @Override
    public int hashCode()
    {
        return getUnderlyingMap().hashCode();
    }
}
