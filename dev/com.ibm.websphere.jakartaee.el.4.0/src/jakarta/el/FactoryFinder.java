/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

import static java.io.File.separator;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

class FactoryFinder {

    /**
     * Creates an instance of the specified class using the specified <code>ClassLoader</code> object.
     *
     * @exception ELException if the given class could not be found or could not be instantiated
     */
    private static Object newInstance(String className, ClassLoader classLoader, Properties properties) {
        try {
            Class<?> spiClass;
            if (classLoader == null) {
                spiClass = Class.forName(className);
            } else {
                spiClass = classLoader.loadClass(className);
            }

            if (properties != null) {
                Constructor<?> constr = null;
                try {
                    constr = spiClass.getConstructor(Properties.class);
                } catch (Exception ex) {
                }

                if (constr != null) {
                    return constr.newInstance(properties);
                }
            }
            return spiClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException x) {
            throw new ELException("Provider " + className + " not found", x);
        } catch (Exception x) {
            throw new ELException("Provider " + className + " could not be instantiated: " + x, x);
        }
    }

    /**
     * Finds the implementation <code>Class</code> object for the given factory. The following search order is used:
     * <ol>
     * <li>{@link ServiceLoader} lookup using <code>serviceClass</code></li>
     * <li>Property file located as <code>$java.home/lib/el.properties</code></li>
     * <li>System property lookup using <code>factoryId</code></li>
     * <li>Create an instance of <code>fallbackClassName</code></li>
     * </ol>
     * This method is package private so that this code can be shared.
     *
     * @return the <code>Class</code> object of the specified message factory; may not be <code>null</code>
     *
     * @param serviceClass The class to use when searching for the factory using the ServiceLoader mechanism
     * @param factoryId the name of the factory to find, which is a system property
     * @param fallbackClassName the implementation class name, which is to be used only if nothing else is found;
     * <code>null</code> to indicate that there is no fallback class name
     * @exception ELException if there is an error
     */
    static Object find(Class<?> serviceClass, String factoryId, String fallbackClassName, Properties properties) {
        ClassLoader classLoader;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Exception x) {
            throw new ELException(x.toString(), x);
        }

        // try to find services in CLASSPATH
        try {
            ServiceLoader<?> serviceLoader = ServiceLoader.load(serviceClass, classLoader);
            Iterator<?> iter = serviceLoader.iterator();
            while (iter.hasNext()) {
                Object service = iter.next();
                if (service != null) {
                    return service;
                }
            }
        } catch (Exception ex) {
        }

        // Try to read from $java.home/lib/el.properties
        try {
            String javah = System.getProperty("java.home");
            String configFileName = javah + separator + "lib" + separator + "el.properties";

            File configFile = new File(configFileName);
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(configFile));
                String factoryClassName = props.getProperty(factoryId);

                return newInstance(factoryClassName, classLoader, properties);
            }
        } catch (Exception ex) {
        }

        // Use the system property
        try {
            String systemProp = System.getProperty(factoryId);
            if (systemProp != null) {
                return newInstance(systemProp, classLoader, properties);
            }
        } catch (SecurityException se) {
        }

        if (fallbackClassName == null) {
            throw new ELException("Provider for " + factoryId + " cannot be found", null);
        }

        return newInstance(fallbackClassName, classLoader, properties);
    }
}
