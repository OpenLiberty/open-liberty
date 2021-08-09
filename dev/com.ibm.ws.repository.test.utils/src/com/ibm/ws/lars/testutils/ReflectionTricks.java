/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.lars.testutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.transport.model.Asset;

public class ReflectionTricks {

    /**
     * Invoke a method reflectively that does not use primitive arguments
     *
     * @param targetObject
     * @param methodName
     * @param varargs
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object reflectiveCallNoPrimitives(Object targetObject, String methodName, Object... varargs)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException,
            IllegalArgumentException, InvocationTargetException {

        // Usage example of this method
        // int i = (Integer)reflectiveCallAnyTypes(targetObject,"methodName",1)

        // create a class array from the vararg object array
        @SuppressWarnings("rawtypes")
        Class[] classes;
        if (varargs != null) {
            classes = new Class[varargs.length];
            for (int i = 0; i < varargs.length; i++) {
                classes[i] = varargs[i].getClass();
            }
        } else {
            classes = new Class[0];
        }

        return reflectiveCallAnyTypes(targetObject, methodName, classes, varargs);
    }

    /**
     * Invoke a method reflectively if that does not use primitive arguments
     *
     * @param targetObject
     * @param methodName
     * @param classes
     * @param values
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static Object reflectiveCallAnyTypes(Object targetObject, String methodName, @SuppressWarnings("rawtypes") Class[] classes, Object[] values)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException,
            IllegalArgumentException, InvocationTargetException {

        // Usage example of this method
        // int i = (Integer)reflectiveCallAnyTypes(targetObject,"methodName",
        //       new Class[] {int.class},
        //       new Object[] {9});

        // Get the class of the targetObject
        Class<?> c = null;
        if (targetObject instanceof Class) {
            c = (Class<?>) targetObject;
        } else {
            c = targetObject.getClass();
        }

        Method method = null;
        boolean finished = false;
        while (!finished) {

            try {
                // get method
                method = c.getDeclaredMethod(methodName, classes);
                finished = true;
                method.setAccessible(true);
            } catch (NoSuchMethodException nsme) {
                if (c.getName().equals("java.lang.Object")) {
                    throw nsme;
                } else {
                    c = c.getSuperclass();
                }
            }
        }

        // Invoke MassiveResource.getAsset()
        return method.invoke(targetObject, values);
    }

    public static Asset getAsset(RepositoryResource resource) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getAssetMethod = RepositoryResourceImpl.class.getDeclaredMethod("getAsset");
        getAssetMethod.setAccessible(true);
        return (Asset) getAssetMethod.invoke(resource);
    }

}
