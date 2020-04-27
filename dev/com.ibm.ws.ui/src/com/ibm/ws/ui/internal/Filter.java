/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Helper class
 */
public class Filter
{

    /**
     * Apply a filter to the given objects so that the response send back user can me limited.
     * 
     * @param filter The filter string, e.g. "tool.name,tool.id,tool.icon".
     * @param list The given object. The object must implements com.ibm.ws.ui.util.IConverter interface.
     * 
     * @return The filtered objects in a LinkedHashMap.
     */
    public Object applyFieldFilter(String filter, Object object) throws IntrospectionException,
                    InvocationTargetException, IllegalAccessException
    {
        if (object == null)
            return null;
        if (filter == null)
        {
            return object;
        }
        Vector<String> vsa = getFilterVector(filter, ",");
        if (vsa.isEmpty() == true)
            return object;

        Map<Object, Object> kvPair = convertBeanObjectToMap(object);
        return filterMap(vsa, "", kvPair);

    }

    private Vector<String> getFilterVector(String in, String separator)
    {
        String[] sa = in.split(separator);
        Vector<String> vsa = new Vector<String>();
        for (int i = 0; i < sa.length; i++)
        {
            String thisFilter = sa[i].trim();
            if (thisFilter.isEmpty() == false)
            {
                vsa.add(thisFilter);
            }
        }
        return vsa;

    }

    /**
     * Convert a Bean compliant Object into a Map.
     * 
     * @param object The Bean compliant Object to convert
     * @return A map
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> convertBeanObjectToMap(Object object) throws IntrospectionException,
                    InvocationTargetException, IllegalAccessException {
        if (object instanceof Map<?, ?>) {
            return (Map<Object, Object>) object;
        }
        Map<Object, Object> lhm = new LinkedHashMap<Object, Object>();
        BeanInfo testBeanInfo = Introspector.getBeanInfo(object.getClass(), Object.class);
        for (PropertyDescriptor propertyDescriptor : testBeanInfo.getPropertyDescriptors()) {
            Method getter = propertyDescriptor.getReadMethod();
            // If there is no getter, then we ignore the setter
            if (getter != null) {
                Object val = getter.invoke(object);
                lhm.put(propertyDescriptor.getName(), val);
            }
        }
        return lhm;
    }

    private Map<Object, Object> filterMap(Vector<String> vsa, String name, Map<Object, Object> sourceMap)
                    throws IntrospectionException,
                    InvocationTargetException, IllegalAccessException
    {
        Map<Object, Object> lhm = new LinkedHashMap<Object, Object>();

        for (final Iterator<Entry<Object, Object>> iterator = sourceMap.entrySet().iterator(); iterator.hasNext();)
        {
            Entry<Object, Object> entry = iterator.next();
            String key = entry.getKey().toString();
            // check if we should keep the key value pair
            if (checkField(vsa, name, key))
            {
                Object value = entry.getValue();
                if (value != null && value instanceof List)
                {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    List<Object> arrayList = new ArrayList<Object>();
                    for (Object obj : list)
                    {
                        Map<Object, Object> kvPair = convertBeanObjectToMap(obj);
                        String newKey = (name.isEmpty() == true) ? key : name + "." + key;
                        arrayList.add(filterMap(vsa, newKey, kvPair));

                    }
                    lhm.put(key, arrayList);
                }
                else if (value != null && value instanceof Map<?, ?>)
                {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> map = (Map<Object, Object>) value;
                    String newKey = (name.isEmpty() == true) ? key : name + "." + key;
                    lhm.put(key, filterMap(vsa, newKey, map));

                }

                else if (value != null)
                {
                    lhm.put(key, value);
                }
            }
        }
        return lhm;
    }

    public boolean checkField(Vector<String> vsa, String name, String key)
    {
        String keyToCheck = (name.isEmpty() == true) ? key : name + "." + key;
        String parentKeyToCheck = (name.isEmpty() == true) ? "" : name;

        boolean ret = false;
        Iterator<String> iterator = vsa.iterator();
        while (iterator.hasNext() && ret == false)
        {
            String me = iterator.next();

            if (me.equalsIgnoreCase(keyToCheck) || me.equalsIgnoreCase(parentKeyToCheck))
            {
                ret = true;
            }
            else
            {
                Vector<String> v = new Vector<String>();
                StringTokenizer st = new StringTokenizer(me, ".");
                while (st.hasMoreTokens() && ret == false)
                {
                    String s = st.nextToken();
                    String ps = (v.isEmpty() == true) ? s : v.lastElement() + "." + s;
                    v.add(ps);
                    if (ps.equalsIgnoreCase(keyToCheck))
                    {
                        ret = true;
                    }
                }
            }
        }

        return ret;
    }
}
