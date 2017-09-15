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
package javax.faces;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;

/**
 * Provide utility methods used by FactoryFinder class to lookup for SPI interface FactoryFinderProvider.
 *
 * @since 2.0.5
 */
class _FactoryFinderProviderFactory
{

    public static final String FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME = "org.apache.myfaces.spi" +
            ".FactoryFinderProviderFactory";

    public static final String FACTORY_FINDER_PROVIDER_CLASS_NAME = "org.apache.myfaces.spi.FactoryFinderProvider";

    public static final String INJECTION_PROVIDER_FACTORY_CLASS_NAME = 
        "org.apache.myfaces.spi.InjectionProviderFactory";

    // INJECTION_PROVIDER_CLASS_NAME to provide use of new Injection Provider methods which take a 
    // a class as an input. This is required for CDI 1.2, particularly in regard to constructor injection/
    public static final String INJECTION_PROVIDER_CLASS_NAME = "com.ibm.ws.jsf.spi.WASInjectionProvider";
 
    // MANAGED_OBJECT_CLASS_NAME added for CDI 1.2 support. Because CDI now creates the class, inject needs
    // to return two objects : the instance of the required class and the creational context. To do this
    // an Managed object is returned from which both of the required objects can be obtained.
    public static final String MANAGED_OBJECT_CLASS_NAME = "com.ibm.ws.managedobject.ManagedObject";

    public static final Class<?> FACTORY_FINDER_PROVIDER_FACTORY_CLASS;

    public static final Method FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD;

    public static final Method FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD;
    public static final Class<?> FACTORY_FINDER_PROVIDER_CLASS;
    public static final Method FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD;
    public static final Method FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD;
    
    public static final Class<?> INJECTION_PROVIDER_FACTORY_CLASS;
    public static final Method INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD;
    public static final Method INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD;
    public static final Class<?> INJECTION_PROVIDER_CLASS;
    public static final Method INJECTION_PROVIDER_INJECT_OBJECT_METHOD;
    public static final Method INJECTION_PROVIDER_INJECT_CLASS_METHOD;
    public static final Method INJECTION_PROVIDER_POST_CONSTRUCT_METHOD;
    public static final Method INJECTION_PROVIDER_PRE_DESTROY_METHOD;
    
    // New for CDI 1.2
    public static final Class<?> MANAGED_OBJECT_CLASS;
    public static final Method MANAGED_OBJECT_GET_OBJECT_METHOD;
    public static final Method MANAGED_OBJECT_GET_CONTEXT_DATA_METHOD;



    static
    {
        Class factoryFinderFactoryClass = null;
        Method factoryFinderproviderFactoryGetMethod = null;
        Method factoryFinderproviderFactoryGetFactoryFinderMethod = null;
        Class<?> factoryFinderProviderClass = null;

        Method factoryFinderProviderGetFactoryMethod = null;
        Method factoryFinderProviderSetFactoryMethod = null;
        Method factoryFinderProviderReleaseFactoriesMethod = null;
        
        Class injectionProviderFactoryClass = null;
        Method injectionProviderFactoryGetInstanceMethod = null;
        Method injectionProviderFactoryGetInjectionProviderMethod = null;
        Class injectionProviderClass = null;
        Method injectionProviderInjectObjectMethod = null;
        Method injectionProviderInjectClassMethod = null;
        Method injectionProviderPostConstructMethod = null;
        Method injectionProviderPreDestroyMethod = null;
        
        Class managedObjectClass = null;
        Method managedObjectGetObjectMethod = null;
        Method managedObjectGetContextDataMethod = null;

        try
        {
            factoryFinderFactoryClass = classForName(FACTORY_FINDER_PROVIDER_FACTORY_CLASS_NAME);
            if (factoryFinderFactoryClass != null)
            {
                factoryFinderproviderFactoryGetMethod = factoryFinderFactoryClass.getMethod
                        ("getInstance", null);
                factoryFinderproviderFactoryGetFactoryFinderMethod = factoryFinderFactoryClass
                        .getMethod("getFactoryFinderProvider", null);
            }

            factoryFinderProviderClass = classForName(FACTORY_FINDER_PROVIDER_CLASS_NAME);
            if (factoryFinderProviderClass != null)
            {
                factoryFinderProviderGetFactoryMethod = factoryFinderProviderClass.getMethod("getFactory",
                        new Class[]{String.class});
                factoryFinderProviderSetFactoryMethod = factoryFinderProviderClass.getMethod("setFactory",
                        new Class[]{String.class, String.class});
                factoryFinderProviderReleaseFactoriesMethod = factoryFinderProviderClass.getMethod
                        ("releaseFactories", null);
            }
            
            injectionProviderFactoryClass = classForName(INJECTION_PROVIDER_FACTORY_CLASS_NAME);
            
            if (injectionProviderFactoryClass != null)
            {
                injectionProviderFactoryGetInstanceMethod = injectionProviderFactoryClass.
                    getMethod("getInjectionProviderFactory", null);
                injectionProviderFactoryGetInjectionProviderMethod = injectionProviderFactoryClass.
                    getMethod("getInjectionProvider", ExternalContext.class);
            }
            
            injectionProviderClass = classForName(INJECTION_PROVIDER_CLASS_NAME);
            
            if (injectionProviderClass != null)
            {
                injectionProviderInjectObjectMethod = injectionProviderClass.
                    getMethod("inject", Object.class);
                injectionProviderInjectClassMethod = injectionProviderClass.
                                getMethod("inject", Class.class);
                injectionProviderPostConstructMethod = injectionProviderClass.
                    getMethod("postConstruct", Object.class, Object.class);
                injectionProviderPreDestroyMethod = injectionProviderClass.
                    getMethod("preDestroy", Object.class, Object.class);
            }
            
            
            // get managed object and getObject and getContextData methods for CDI 1.2 support.
            // getObject() is used to the created object instance
            // getContextData(Class) is used to get the creational context
            managedObjectClass = classForName(MANAGED_OBJECT_CLASS_NAME);
            if (managedObjectClass != null)
            {
                managedObjectGetObjectMethod = managedObjectClass.getMethod("getObject", null);
                managedObjectGetContextDataMethod = managedObjectClass.getMethod("getContextData", Class.class);
            }

        }
        catch (Exception e)
        {
            // no op
        }

        FACTORY_FINDER_PROVIDER_FACTORY_CLASS = factoryFinderFactoryClass;
        FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD = factoryFinderproviderFactoryGetMethod;
        FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD = factoryFinderproviderFactoryGetFactoryFinderMethod;
        FACTORY_FINDER_PROVIDER_CLASS = factoryFinderProviderClass;

        FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD = factoryFinderProviderGetFactoryMethod;
        FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD = factoryFinderProviderSetFactoryMethod;
        FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD = factoryFinderProviderReleaseFactoriesMethod;
        
        INJECTION_PROVIDER_FACTORY_CLASS = injectionProviderFactoryClass;
        INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD = injectionProviderFactoryGetInstanceMethod;
        INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD = injectionProviderFactoryGetInjectionProviderMethod;
        INJECTION_PROVIDER_CLASS = injectionProviderClass;
        INJECTION_PROVIDER_INJECT_OBJECT_METHOD = injectionProviderInjectObjectMethod;
        INJECTION_PROVIDER_INJECT_CLASS_METHOD = injectionProviderInjectClassMethod;
        INJECTION_PROVIDER_POST_CONSTRUCT_METHOD = injectionProviderPostConstructMethod;
        INJECTION_PROVIDER_PRE_DESTROY_METHOD = injectionProviderPreDestroyMethod;
        
        MANAGED_OBJECT_CLASS = managedObjectClass;
        MANAGED_OBJECT_GET_OBJECT_METHOD = managedObjectGetObjectMethod;
        MANAGED_OBJECT_GET_CONTEXT_DATA_METHOD = managedObjectGetContextDataMethod;

    }

    public static Object getInstance()
    {
        if (FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD != null)
        {
            try
            {
                return FACTORY_FINDER_PROVIDER_GET_INSTANCE_METHOD.invoke(FACTORY_FINDER_PROVIDER_FACTORY_CLASS, null);
            }
            catch (Exception e)
            {
                //No op
                Logger log = Logger.getLogger(_FactoryFinderProviderFactory.class.getName());
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "Cannot retrieve current FactoryFinder instance from " +
                            "FactoryFinderProviderFactory." +
                            " Default strategy using thread context class loader will be used.", e);
                }
            }
        }
        return null;
    }

    // ~ Methods Copied from _ClassUtils
    // ------------------------------------------------------------------------------------

    /**
     * Tries a Class.loadClass with the context class loader of the current thread first and automatically falls back
     * to
     * the ClassUtils class loader (i.e. the loader of the myfaces.jar lib) if necessary.
     *
     * @param type fully qualified name of a non-primitive non-array class
     * @return the corresponding Class
     * @throws NullPointerException   if type is null
     * @throws ClassNotFoundException
     */
    public static Class<?> classForName(String type) throws ClassNotFoundException
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }
        try
        {
            // Try WebApp ClassLoader first
            return Class.forName(type, false, // do not initialize for faster startup
                    getContextClassLoader());
        }
        catch (ClassNotFoundException ignore)
        {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar lib)
            return Class.forName(type, false, // do not initialize for faster startup
                    _FactoryFinderProviderFactory.class.getClassLoader());
        }
    }

    /**
     * Gets the ClassLoader associated with the current thread. Returns the class loader associated with the specified
     * default object if no context loader is associated with the current thread.
     *
     * @return ClassLoader
     */
    protected static ClassLoader getContextClassLoader()
    {
        if (System.getSecurityManager() != null)
        {
            try
            {
                Object cl = AccessController.doPrivileged(new PrivilegedExceptionAction()
                {
                    public Object run() throws PrivilegedActionException
                    {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
                return (ClassLoader) cl;
            }
            catch (PrivilegedActionException pae)
            {
                throw new FacesException(pae);
            }
        }
        else
        {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
