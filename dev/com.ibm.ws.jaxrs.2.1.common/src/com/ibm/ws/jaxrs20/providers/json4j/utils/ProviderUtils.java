/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.providers.json4j.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.ws.jaxrs20.support.JaxRsMetaDataManager;
import com.ibm.ws.jaxrs20.utils.ReflectUtil;

public class ProviderUtils {

    private final static String[] json4jClasses = new String[] { "com.ibm.json.java.JSONArray", "com.ibm.json.java.JSONObject", "com.ibm.json.java.JSONObject",
                                                                "com.ibm.json.xml.XMLToJSONTransformer" };
    private final static Map<String, Class<?>> json4jClsMaps = new HashMap<String, Class<?>>();

    private final static Map<String, Method> json4jMethodMaps = new HashMap<String, Method>();

    private static final String DEFAULT_CHARSET = "UTF-8";
    static
    {
        ClassLoader bundleCL = JaxRsMetaDataManager.class.getClassLoader();

        for (String clsName : json4jClasses) {
            Class<?> c = ReflectUtil.loadClass(bundleCL, clsName);
            if (c != null) {
                json4jClsMaps.put(clsName, c);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public static Method getMethod(String className, String methodName, Class[] paramTypes) {

        if (!json4jClsMaps.containsKey(className)) {
            return null;
        }

        Class<?> c = json4jClsMaps.get(className);

        Method m = null;

        String cachekey = className + "." + methodName;

        if (json4jMethodMaps.containsKey(cachekey)) {
            m = json4jMethodMaps.get(cachekey);
        }
        else {
            m = ReflectUtil.getMethod(c, methodName, paramTypes);
            json4jMethodMaps.put(cachekey, m);
        }

        return m;
    }

    public static Class<?> getJSON4JClass(String name) {

        if (name == null)
            return null;

        if (json4jClsMaps.containsKey(name)) {
            return json4jClsMaps.get(name);
        }

        return null;
    }

    public static String getCharset(MediaType m) {

        String name = m == null ? null : (String) m.getParameters().get(
                                                                        "charset");

        if (name != null) {

            return name;
        }

        return DEFAULT_CHARSET;
    }

    public static boolean isJAXBElement(Class<?> type, Type genericType) {
        return type == JAXBElement.class;
    }

    private static boolean isXMLRootElement(Class<?> type) {
        boolean isXmlRootElement = type.getAnnotation(XmlRootElement.class) != null;
        return isXmlRootElement;
    }

    @SuppressWarnings("rawtypes")
    public static boolean isJAXBObject(Class<?> type, Type genericType) {
        if (isJAXBObject(type))
            return true;
        if ((genericType instanceof Class)) {
            return isJAXBObject((Class) genericType);
        }
        return false;
    }

    private static boolean isXMLType(Class<?> type) {
        boolean isXmlTypeElement = type.getAnnotation(XmlType.class) != null;
        return isXmlTypeElement;
    }

    public static boolean isJAXBObject(Class<?> type) {
        return (isXMLRootElement(type)) || (isXMLType(type));
    }

}
