/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.spi;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import javax.resource.ResourceException;

/**
 *
 */
public class DSConfigHelper {

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    public static final String EOLN = System.getProperty("line.separator");

    static void setDataSourceProperties(Object ds, Properties props) throws ResourceException {

        final Class dsClass = ds.getClass();

        PropertyDescriptor[] descriptors = null;

        try {
            descriptors = (PropertyDescriptor[]) AccessController
                            .doPrivileged(new PrivilegedExceptionAction() {
                                @Override
                                public Object run() throws Exception {
                                    return Introspector.getBeanInfo(dsClass)
                                                    .getPropertyDescriptors();
                                }
                            });
        } catch (PrivilegedActionException privX) {
            Exception ex = privX.getException();
            throw new ResourceException(ex);
        }
        String name, value;

        value = props.getProperty(USER);
        boolean isUserSpecified = value != null && value.trim().length() > 0;
        boolean isPassword;

        for (int i = 0; i < descriptors.length; i++)
            if ((value = (String) props.remove(name = descriptors[i].getName())) != null)
                try {
                    value = value.trim();

                    isPassword = name.equals(PASSWORD);

                    if (value.length() > 0 || (isPassword && isUserSpecified))
                        setProperty(ds, descriptors[i], value, !isPassword);
                } catch (Exception ex) { // couldn't set the property
                    throw new ResourceException(ex);
                }
        if (!props.isEmpty())
            System.out.println("Properties not completely set " + props);

    }

    /**
     * Handles the setting of any property for which a public single-parameter
     * setter exists on the DataSource and for which the property data type is
     * either a primitive or has a single-parameter String constructor.
     *
     * @param obj
     *            the Object to set the property on.
     * @param pd
     *            the PropertyDescriptor describing the property to set.
     * @param value
     *            a String representing the new value.
     *
     * @throws Exception
     *             if an error occurs.
     */
    public static void setProperty(Object obj, PropertyDescriptor pd,
                                   String value, boolean doTraceValue) throws Exception {
        Object param = null;
        String propName = pd.getName();
        java.lang.reflect.Method setter = pd.getWriteMethod();

        if (setter == null)
            throw new NoSuchMethodException("set" + propName);

        Class<?> paramType = setter.getParameterTypes()[0];

        if (!paramType.isPrimitive()) {
            if (paramType.equals(String.class)) // the most common case: String
                param = value;

            else if (paramType.equals(Properties.class)) // special case:
                                                         // Properties
                param = toProperties(value);

            else if (paramType.equals(Character.class)) // special case:
                                                        // Character
                param = Character.valueOf(value.charAt(0)); // LIDB4602-2

            else
                // the generic case: any object with a single parameter String
                // constructor
                param = paramType.getConstructor(String.class).newInstance(
                                                                           value);
        }
        // begin LIDB4602-2
        else if (paramType.equals(int.class))
            param = Integer.valueOf(value);
        else if (paramType.equals(long.class))
            param = Long.valueOf(value);
        else if (paramType.equals(boolean.class))
            param = Boolean.valueOf(value);
        else if (paramType.equals(double.class))
            param = Double.valueOf(value);
        else if (paramType.equals(float.class))
            param = Float.valueOf(value);
        else if (paramType.equals(short.class))
            param = Short.valueOf(value);
        else if (paramType.equals(byte.class))
            param = Byte.valueOf(value);
        else if (paramType.equals(char.class))
            param = Character.valueOf(value.charAt(0));
        // end LIDB4602-2

        setter.invoke(obj, new Object[] { param });
    }

    public static Properties toProperties(String properString) throws java.io.IOException {
        Properties p = new Properties();

        p.load(new java.io.ByteArrayInputStream(properString.replaceAll(";",
                                                                        EOLN)
                        .getBytes()));

        return p;
    }

    public static Object createDataSource(Object dsClassName, ClassLoader cl) throws ResourceException // LIDB4500-2
    {
        if (cl == null) {
            cl = DSConfigHelper.class.getClassLoader();
        }
        try {
            Class dsClass;
            if (dsClassName instanceof Class)
                dsClass = (Class) dsClassName;
            else
                dsClass = cl.loadClass((String) dsClassName);
            Object ds = dsClass.newInstance();
            return ds;
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

}
