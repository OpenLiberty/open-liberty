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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.view.AttachedObjectTarget;

/**
 * Implementation of BeanInfo object used by composite components.
 * Instances of this class are found on component attribute map
 * using the key UIComponent.BEANINFO_KEY.
 * 
 * The points to take into account for implement this class are this:
 * 
 * - The following tags:
 * 
 *    composite:interface
 *    composite:attribute
 *    composite:facet
 *    composite:valueHolder
 *    composite:editableValueHolder
 *    composite:actionSource
 *    composite:extension
 *    
 *    must deal with this class, so it is expected methods that manipulate
 *    data here are called from their tag handlers.
 *    
 * - ViewDeclarationLanguage.retargetAttachedObjects and 
 *   ViewDeclarationLanguage.retargetMethodExpressions read information
 *   contained here
 * 
 * - This object goes on attribute map, so it is necessary that
 *   this instance should be Serializable. But note that BeanDescriptor
 *   is not, so the best way is implements Externalizable interface
 *   and implement its methods. The only information we need to be Serializable
 *   from this object is the related to BeanDescriptor, but note that
 *   serialize information used only in build view time ( like
 *   AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY list) is not required 
 *   and could cause serialization exceptions. 
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1406265 $ $Date: 2012-11-06 18:33:41 +0000 (Tue, 06 Nov 2012) $
 */
public class CompositeComponentBeanInfo extends SimpleBeanInfo 
    implements Externalizable
{
    
    public static final String PROPERTY_DESCRIPTOR_MAP_KEY = "oam.cc.beanInfo.PDM";

    /**
     * Most of the information here are filled on composite:interface tag.
     * It is also expected that AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY
     * key is used by other handlers inside the inner map for this BeanDescriptor.
     */
    private BeanDescriptor _descriptor;
    
    /**
     * ViewDeclarationLanguage.retargetMethodExpressions algorithm
     * suggest that every attribute should have a PropertyDescriptor
     * object defined for it. So, this list is expected to be filled
     * on composite:attribute tag. This list is used only when
     * retargetAttachedObjects and retargetAttachedMethodExpressions,
     * and this methods are called only on buildView time, so
     * we don't need to Serialize this list. 
     */
    private List<PropertyDescriptor> _propertyDescriptors;
    
    private static final PropertyDescriptor[] EMPTY_PROPERTY_DESCRIPTOR_ARRAY = new PropertyDescriptor[0];
    
    private PropertyDescriptor[] _propertyDescriptorsArray;
    
    private Map<String, PropertyDescriptor> _propertyDescriptorsMap;
    
    /**
     * Used for Serialization
     */
    public CompositeComponentBeanInfo()
    {
        super();
    }
    
    public CompositeComponentBeanInfo(BeanDescriptor descriptor)
    {
        super();
        _descriptor = descriptor;
        getBeanDescriptor().setValue(PROPERTY_DESCRIPTOR_MAP_KEY, new PropertyDescriptorMap());
    }
    
    @Override
    public BeanDescriptor getBeanDescriptor()
    {
        return _descriptor;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        if (_propertyDescriptors == null)
        {
            return EMPTY_PROPERTY_DESCRIPTOR_ARRAY;
        }
        else
        {
            if (_propertyDescriptorsArray == null)
            {
                _propertyDescriptorsArray = _propertyDescriptors.toArray(
                        new PropertyDescriptor[_propertyDescriptors.size()]); 
            }
            else if (_propertyDescriptorsArray.length != _propertyDescriptors.size())
            {
                _propertyDescriptorsArray = _propertyDescriptors.toArray(
                        new PropertyDescriptor[_propertyDescriptors.size()]);
            }
            return _propertyDescriptorsArray; 
        }
    }

    public List<PropertyDescriptor> getPropertyDescriptorsList()
    {
        if (_propertyDescriptors == null)
        {
            _propertyDescriptors = new ArrayList<PropertyDescriptor>();
        }
        return _propertyDescriptors;
    }

    public void setPropertyDescriptorsList(List<PropertyDescriptor> descriptors)
    {
        _propertyDescriptors = descriptors;
    }

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException
    {
        Class beanClass = (Class) in.readObject();
        Class customizerClass = (Class) in.readObject();
        if (customizerClass == null)
        {
            _descriptor = new BeanDescriptor(beanClass);
        }
        else
        {
            _descriptor = new BeanDescriptor(beanClass, customizerClass);
        }
        _descriptor.setDisplayName((String) in.readObject());
        _descriptor.setExpert(in.readBoolean());
        _descriptor.setName((String) in.readObject());
        _descriptor.setPreferred(in.readBoolean());
        _descriptor.setShortDescription((String) in.readObject());
        _descriptor.setValue(PROPERTY_DESCRIPTOR_MAP_KEY, new PropertyDescriptorMap());
        
        Map<String,Object> map = (Map) in.readObject();
        
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            _descriptor.setValue(entry.getKey(), entry.getValue());
        }
        _propertyDescriptors = (List<PropertyDescriptor>) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(_descriptor.getBeanClass());
        out.writeObject(_descriptor.getCustomizerClass());
        out.writeObject(_descriptor.getDisplayName());
        out.writeBoolean(_descriptor.isExpert());
        out.writeObject(_descriptor.getName());
        out.writeBoolean(_descriptor.isPreferred());
        out.writeObject(_descriptor.getShortDescription());
        
        Map<String,Object> map = new HashMap<String, Object>(4,1);
        
        for (Enumeration<String> e = _descriptor.attributeNames(); e.hasMoreElements();)
        {
            String name = e.nextElement();
            
            // It is not necessary to serialize AttachedObjectTarget list because
            // we only use it when VDL.retargetAttachedObjects() is called and this only
            // happen when the view is built. Also, try to serialize this instances could
            // cause unwanted exceptions.
            if (!AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY.equals(name) &&
                !PROPERTY_DESCRIPTOR_MAP_KEY.equals(name))
            {
                map.put(name, _descriptor.getValue(name));
            }
        }
        out.writeObject(map);
        out.writeObject(_propertyDescriptors);
        
    }
    
    public Map<String, PropertyDescriptor> getPropertyDescriptorsMap()
    {
        if (_propertyDescriptors == null)
        {
            return Collections.emptyMap();
        }
        else
        {
            if (_propertyDescriptors.isEmpty())
            {
                return Collections.emptyMap();
            }
            else if (_propertyDescriptorsMap == null)
            {
                int initCapacity = (_propertyDescriptors.size() * 4 + 3) / 3;
                _propertyDescriptorsMap = new HashMap<String, PropertyDescriptor>(initCapacity);
                for (PropertyDescriptor p : _propertyDescriptors)
                {
                    if (!_propertyDescriptorsMap.containsKey(p.getName()))
                    {
                        _propertyDescriptorsMap.put(p.getName(), p);
                    }
                }
            }
            else if (_propertyDescriptorsMap.size() != _propertyDescriptors.size())
            {
                for (PropertyDescriptor p : _propertyDescriptors)
                {
                    if (!_propertyDescriptorsMap.containsKey(p.getName()))
                    {
                        _propertyDescriptorsMap.put(p.getName(), p);
                    }
                }
                if (_propertyDescriptorsMap.size() != _propertyDescriptors.size())
                {
                    // PropertyDescriptor was removed
                    _propertyDescriptorsMap.clear();
                    for (PropertyDescriptor p : _propertyDescriptors)
                    {
                        if (!_propertyDescriptorsMap.containsKey(p.getName()))
                        {
                            _propertyDescriptorsMap.put(p.getName(), p);
                        }
                    }
                }
            }
            return _propertyDescriptorsMap;
        }
    }
    
    /**
     * Read only map for fast access. It works as an indirection over the real list.
     */
    public class PropertyDescriptorMap implements Map<String, PropertyDescriptor>
    {
        
        public int size()
        {
            return getPropertyDescriptorsMap().size();
        }

        public boolean isEmpty()
        {
            return getPropertyDescriptorsMap().isEmpty();
        }

       
        public boolean containsKey(Object key)
        {
            return getPropertyDescriptorsMap().containsKey(key);
        }

        public boolean containsValue(Object value)
        {
            return getPropertyDescriptorsMap().containsValue(value);
        }

        public PropertyDescriptor get(Object key)
        {
            return getPropertyDescriptorsMap().get(key);
        }

        public PropertyDescriptor put(String key, PropertyDescriptor value)
        {
            throw new UnsupportedOperationException();
        }

        public PropertyDescriptor remove(Object key)
        {
            throw new UnsupportedOperationException();
        }

        public void putAll(Map<? extends String, ? extends PropertyDescriptor> m)
        {
            throw new UnsupportedOperationException();
        }

        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        public Set<String> keySet()
        {
            return getPropertyDescriptorsMap().keySet();
        }

        public Collection<PropertyDescriptor> values()
        {
            return getPropertyDescriptorsMap().values();
        }

        public Set<Entry<String, PropertyDescriptor>> entrySet()
        {
            return getPropertyDescriptorsMap().entrySet();
        }
    }
}
