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

package org.apache.cxf.common.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class ProxyHelper {
    static final ProxyHelper HELPER = new ProxyHelper(); //Liberty change

    private static final Logger LOG = LogUtils.getL7dLogger(ProxyHelper.class);
    
    protected ProxyClassLoaderCache proxyClassLoaderCache = 
        new ProxyClassLoaderCache();
      
    
    protected ProxyHelper() {
    }

    protected Object getProxyInternal(ClassLoader loader, Class<?>[] interfaces, InvocationHandler handler) {
        ClassLoader combinedLoader = getClassLoaderForInterfaces(loader, interfaces);
        return Proxy.newProxyInstance(combinedLoader, interfaces, handler);
    }

    /**
     * Return a classloader that can see all the given interfaces If the given loader can see all interfaces
     * then it is used. If not then a combined classloader of all interface classloaders is returned.
     *
     * @param loader use supplied class loader
     * @param interfaces
     * @return classloader that sees all interfaces
     */
    private ClassLoader getClassLoaderForInterfaces(final ClassLoader loader, final Class<?>[] interfaces) {
        if (canSeeAllInterfaces(loader, interfaces)) {
            LOG.log(Level.FINE, "current classloader " + loader + " can see all interface");
            return loader;
        }
        String sortedNameFromInterfaceArray = getSortedNameFromInterfaceArray(interfaces);
        ClassLoader cachedLoader = proxyClassLoaderCache.getProxyClassLoader(loader, interfaces);
        if (canSeeAllInterfaces(cachedLoader, interfaces)) {
            LOG.log(Level.FINE, "find required loader from ProxyClassLoader cache with key" 
                 + sortedNameFromInterfaceArray);
            return cachedLoader;
        } else {
            LOG.log(Level.FINE, "find a loader from ProxyClassLoader cache with interfaces " 
                + sortedNameFromInterfaceArray
                + " but can't see all interfaces");
            for (Class<?> currentInterface : interfaces) {
                String ifName = currentInterface.getName();
                
                if (!ifName.startsWith("org.apache.cxf") && !ifName.startsWith("java")) {
                    // remove the stale ProxyClassLoader and recreate one
                    proxyClassLoaderCache.removeStaleProxyClassLoader(currentInterface);
                    cachedLoader = proxyClassLoaderCache.getProxyClassLoader(loader, interfaces);
                    
                }
            }
        }
               
        return cachedLoader;
    }
    
    private String getSortedNameFromInterfaceArray(Class<?>[] interfaces) {
        SortedArraySet<String> arraySet = new SortedArraySet<String>();
        for (Class<?> currentInterface : interfaces) {
            arraySet.add(currentInterface.getName() + ClassLoaderUtils.getClassLoaderName(currentInterface)); //Liberty change
        }
        return arraySet.toString();
    }


    @FFDCIgnore({ClassNotFoundException.class})
    private boolean canSeeAllInterfaces(ClassLoader loader, Class<?>[] interfaces) {
        for (Class<?> currentInterface : interfaces) {
            String ifName = currentInterface.getName();
            try {
                Class<?> ifClass = Class.forName(ifName, true, loader);
                if (ifClass != currentInterface) {
                    return false;
                }
                //we need to check all the params/returns as well as the Proxy creation
                //will try to create methods for all of this even if they aren't used
                //by the client and not available in the clients classloader
                for (Method m : ifClass.getMethods()) {
                    Class<?> returnType = m.getReturnType();
                    if (!returnType.isPrimitive()) {
                        Class.forName(returnType.getName(), true, loader);
                    }
                    for (Class<?> p : m.getParameterTypes()) {
                        if (!p.isPrimitive()) {
                            Class.forName(p.getName(), true, loader);
                        }
                    }
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public static Object getProxy(ClassLoader loader, Class<?>[] interfaces, InvocationHandler handler) {
        return HELPER.getProxyInternal(loader, interfaces, handler);
    }
    
}
