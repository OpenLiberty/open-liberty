/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.ws.jaxws.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ibm.ws.jaxws.support.JaxWsInstanceManager.InterceptException;

public final class ResourceUtils {

    private ResourceUtils() {

    }

    public static Method findPostConstructMethod(Class<?> c) {
        return findPostConstructMethod(c, null);
    }

    public static Method findPostConstructMethod(Class<?> c, String name) {
        if (Object.class == c || null == c) {
            return null;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (name != null) {
                if (m.getName().equals(name)) {
                    return m;
                }
            } else if (m.getAnnotation(PostConstruct.class) != null) {
                return m;
            }
        }
        Method m = findPostConstructMethod(c.getSuperclass(), name);
        if (m != null) {
            return m;
        }
        for (Class<?> i : c.getInterfaces()) {
            m = findPostConstructMethod(i, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    public static Method findPreDestroyMethod(Class<?> c) {
        return findPreDestroyMethod(c, null);
    }

    public static Method findPreDestroyMethod(Class<?> c, String name) {
        if (Object.class == c || null == c) {
            return null;
        }
        for (Method m : c.getDeclaredMethods()) {
            if (name != null) {
                if (m.getName().equals(name)) {
                    return m;
                }
            } else if (m.getAnnotation(PreDestroy.class) != null) {
                return m;
            }
        }
        Method m = findPreDestroyMethod(c.getSuperclass(), name);
        if (m != null) {
            return m;
        }
        for (Class<?> i : c.getInterfaces()) {
            m = findPreDestroyMethod(i, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    public static void verifySingletons(Set<Object> singletons) {
        if (singletons.isEmpty()) {
            return;
        }
        Set<String> map = new HashSet<String>();
        for (Object s : singletons) {
            if (map.contains(s.getClass().getName())) {
                throw new RuntimeException("More than one instance of the same singleton class "
                                           + s.getClass().getName() + " is available");
            } else {
                map.add(s.getClass().getName());
            }
        }
    }

    public static boolean isNotAbstractClass(Class<?> c) {
        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {
            return false;
        }
        return true;
    }

    public static void invokeLifeCycleMethod(Object instance, Method method) throws InterceptException {
        if (method != null) {

            try {
                method.invoke(instance, new Object[] {});
            } catch (InvocationTargetException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                             + " due to InvocationTargetException";
                throw new InterceptException(msg);
            } catch (IllegalAccessException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                             + " due to IllegalAccessException";
                throw new InterceptException(msg);
            }
        }
    }

}
