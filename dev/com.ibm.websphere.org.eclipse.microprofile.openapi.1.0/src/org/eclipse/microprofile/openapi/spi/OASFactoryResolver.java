/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.spi;

import org.eclipse.microprofile.openapi.models.Constructible;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ServiceLoader;

/**
 * This class is not intended to be used by end-users. It should
 * be used by vendors to set their implementation of OASFactoryResolver.
 *
 * <br><br>Service provider for OASFactoryResolver. The implementation registers
 * itself via the {@link java.util.ServiceLoader} mechanism or by manually
 * setting their implementation using the setInstance method.
 *
 */
public abstract class OASFactoryResolver {

    private static volatile OASFactoryResolver instance = null;

    /**
     * Create a new instance of a constructible element from the OpenAPI model tree.
     * 
     * @param <T> describes the type parameter
     * @param clazz represents a model which extends the org.eclipse.microprofile.openapi.models.Constructible interface

     * @return a new instance of the requested model class
     * 
     * @throws NullPointerException if the specified class is null
     * @throws IllegalArgumentException if an instance could not be created, most likely, due to an illegal or inappropriate class
     */
    public abstract <T extends Constructible> T createObject(Class<T> clazz);

    /**
     * Creates an OASFactoryResolver object.
     * Only used internally from within {@link org.eclipse.microprofile.openapi.OASFactory}
     * 
     * @return an instance of OASFactoryResolver
     */
    public static OASFactoryResolver instance() {
        if (instance == null) {
            synchronized (OASFactoryResolver.class) {
                if (instance != null) {
                    return instance;
                }

                ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
                if (cl == null) {
                    cl = OASFactoryResolver.class.getClassLoader();
                }

                OASFactoryResolver newInstance = loadSpi(cl);

                if (newInstance == null) {
                    throw new IllegalStateException("No OASFactoryResolver implementation found!");
                }

                instance = newInstance;
            }
        }

        return instance;
    }

    private static OASFactoryResolver loadSpi(ClassLoader cl) {
        if (cl == null) {
            return null;
        }

        OASFactoryResolver instance = loadSpi(cl.getParent());

        if (instance == null) {
            ServiceLoader<OASFactoryResolver> sl = ServiceLoader.load(OASFactoryResolver.class, cl);
            for (OASFactoryResolver spi : sl) {
                if (instance != null) {
                    throw new IllegalStateException("Multiple OASFactoryResolver implementations found: " + spi.getClass().getName() + " and "
                            + instance.getClass().getName());
                }
                else {
                    instance = spi;
                }
            }
        }
        return instance;
    }

    /**
     * Set the instance. It is used by OSGi environment while service loader pattern is not supported.
     *
     * @param factory set the instance.
     */
    public static void setInstance(OASFactoryResolver factory) {
        instance = factory;
    }
}
