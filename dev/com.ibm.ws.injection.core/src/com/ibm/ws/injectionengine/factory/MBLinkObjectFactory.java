/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.factory;

import static com.ibm.ws.injectionengine.factory.MBLinkInfoRefAddr.ADDR_TYPE;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.MethodMap;
import com.ibm.wsspi.injectionengine.MethodMap.MethodInfo;

/**
 * MB-Link Resolver Factory for auto-link resolution. <p>
 *
 * This class is used as an object factory that returns a ManagedBean
 * instance. <p>
 *
 * This factory is used when injection occurs or when a component performs
 * a lookup in the java:comp name space for managed bean references with no
 * binding override.
 */
public class MBLinkObjectFactory implements ObjectFactory
{
    private static final String CLASS_NAME = MBLinkObjectFactory.class.getName();

    private static final TraceComponent tc = Tr.register(MBLinkObjectFactory.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private final static ThreadContextAccessor svThreadContextAccessor =
                    AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    private static final String PC_NAME = PostConstruct.class.getSimpleName();

    /**
     * Default constructor for an MBLinkObjectFactory.
     */
    public MBLinkObjectFactory()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    /**
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context, java.util.Hashtable)
     */
    @Override
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        Class<?> mbClass = null;
        Object retObj = null;

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference))
        {
            Exception inex = new InjectionException
                            ("Binding object is not a Reference : " + obj);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + inex);
            throw inex;
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!ref.getFactoryClassName().equals(CLASS_NAME))
        {
            Exception inex = new InjectionException
                            ("Incorrect factory for Reference : " + obj);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + inex);
            throw inex;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(ADDR_TYPE);
        if (addr == null)
        {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + nex);
            throw nex;
        }

        // Reference has the right factory and non empty address,
        // so it is OK to generate the object now
        MBLinkInfo info = (MBLinkInfo) addr.getContent();

        ClassLoader classLoader;
        try
        {
            classLoader = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
            mbClass = classLoader.loadClass(info.ivBeanType);
            retObj = mbClass.newInstance();
        } catch (Throwable ex)
        {
            // All failures that may occur while creating the bean instance
            // must cause the injection to fail.
            FFDCFilter.processException(ex, CLASS_NAME + ".getObjectInstance",
                                        "125", this);
            String msg = "The " + info.ivRefName + " managed bean reference in the " +
                         info.ivModule + " module of the " + info.ivApplication + " application" +
                         " could not be resolved. A failure occurred creating an instance of the " +
                         info.ivBeanType + " managed bean.";

            InjectionException inex = new InjectionException(msg, ex);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.exit(tc, "getObjectInstance: " + inex);

            throw inex;
        }

        // Perform dependency injection for the bean instance.             d702400
        inject(classLoader, mbClass, retObj, info);

        // Perform PostConstruct processing, if present.                   d702400
        callLifecycleInterceptors(mbClass, retObj);

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + retObj.getClass().getName());

        return retObj;
    }

    /**
     * Processes the injection metadata for the ManagedBean class, locating the
     * injection targets, then uses the injection targets to obtain the objects
     * to inject, and finally injects them into the ManagedBean instance.
     *
     * @param classLoader the class loader
     * @param mbClass ManagedBean class.
     * @param mbInstance an instance of the ManagedBean class to be injected.
     * @param info ManagedBean information including application and module names/
     *
     * @throws InjectionException if any failure occurs attempting to determine
     *             the injection targets or perform the actual injection.
     */
    // d702400
    private void inject(ClassLoader classLoader,
                        Class<?> mbClass,
                        Object mbInstance,
                        MBLinkInfo info)
                    throws InjectionException
    {
        String displayName = mbClass.getName();
        // Caution: Do NOT port this change to traditional WAS.
        if (mbClass != String.class)
        {
            throw new UnsupportedOperationException();
        }

        J2EEName j2eeName = null;

        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(displayName, j2eeName);
        compNSConfig.setOwningFlow(ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN); // F50309.8
        compNSConfig.setClassLoader(classLoader);
        compNSConfig.setInjectionClasses(Collections.<Class<?>> singletonList(mbClass));

        InjectionEngine injectionEngine = InjectionEngineAccessor.getInstance();
        HashMap<Class<?>, InjectionTarget[]> injectionTargets = new HashMap<Class<?>, InjectionTarget[]>();
        injectionEngine.processInjectionMetaData(injectionTargets, compNSConfig);

        for (InjectionTarget injectionTarget : injectionTargets.get(mbClass)) // d719917
        {
            injectionEngine.inject(mbInstance, injectionTarget, null);
        }
    }

    /**
     * Invoke any lifecycle interceptors associated with this bean instance.
     *
     * Called when creating a new managed bean instance
     *
     * @param mbClass ManagedBean class.
     * @param mbInstance an instance of the ManagedBean class
     *
     * @throws InjectionException if any failure occurs attempting to locate
     *             or call a lifecycle method, including configuration problems with
     *             the lifecycle method.
     */
    // d702400
    private void callLifecycleInterceptors(Class<?> mbClass, Object mbInstance)
                    throws InjectionException
    {
        Collection<MethodInfo> methods = MethodMap.getAllDeclaredMethods(mbClass);

        for (MethodInfo methodInfo : methods)
        {
            Method method = methodInfo.getMethod();
            PostConstruct postConstruct = method.getAnnotation(PostConstruct.class);
            if (postConstruct != null)
            {
                method.setAccessible(true);
                validateLifeCycleSignature(PC_NAME, method, true);

                try
                {
                    method.invoke(mbInstance, (Object[]) null);
                } catch (InvocationTargetException ex)
                {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    cause.printStackTrace();
                    throw new InjectionException(PC_NAME + " interceptor \"" + method
                                                 + "\" failed with the following error : "
                                                 + cause);
                } catch (Throwable ex)
                {
                    ex.printStackTrace();
                    throw new InjectionException(PC_NAME + " interceptor \"" + method
                                                 + "\" failed with the following error : "
                                                 + ex);
                }
            }
        }
    }

    /**
     * Verify that a specified life cycle event interceptor method has correct
     * method modifiers, parameter types, return type, and exception types for
     * the throws clause.
     *
     * @param lifeCycle is a string that identifies the type of life cycle event callback.
     *
     * @param m is the java reflection Method object for the life cycle interceptor method.
     *
     * @param beanClass must be boolean true if the m is a method of the bean class,
     *            including super classes. If m is a method of an interceptor class,
     *            then boolean false must be specified.
     *
     * @throws InjectionConfigurationException is thrown if any configuration error is detected.
     */
    // d702400
    public static void validateLifeCycleSignature(String lifeCycle, Method m, boolean beanClass)
                    throws InjectionConfigurationException
    {
        // Get the modifiers for the interceptor method and verify that it
        // is neither final nor static as required by EJB specification.
        int mod = m.getModifiers();
        if (Modifier.isFinal(mod) || Modifier.isStatic(mod))
        {
            // CNTR0229E: The {0} interceptor method must not be declared as final or static.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_INTERCEPTOR_METHOD_MODIFIER_CNTR0229E", method);
            throw new InjectionConfigurationException(lifeCycle + " interceptor \"" + method
                                                      + "\" must not be declared as final or static.");
        }

        // Verify return type is void.
        Class<?> returnType = m.getReturnType();
        if (returnType == java.lang.Void.TYPE)
        {
            // Return type is void as required.
        }
        else
        {
            // CNTR0231E: The {0} method signature is not valid as
            // a {1} method of an enterprise bean class.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0231E", method, lifeCycle);
            throw new InjectionConfigurationException(lifeCycle + " interceptor \"" + method
                                                      + "\" must have void as return type.");
        }

        // Verify method does not have a throws clause since lifecycle methods not allowed
        // to throw checked exceptions.
        Class<?>[] exceptionTypes = m.getExceptionTypes();

        // d629675 - The spec requires that lifecycle interceptor methods do not
        // have a throws clause, but we keep this checking for compatibility with
        // previous releases.
        if (exceptionTypes.length != 0)
        {
            // CNTR0231E: The {1} method specifies the {0} method signature of the
            // lifecycle interceptor, which is not correct for an enterprise bean.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0231E", lifeCycle, method);
            throw new InjectionConfigurationException(lifeCycle + " interceptor \"" + method
                                                      + "\" must not throw application exceptions.");
        }

        // Now verify method parameter types.
        Class<?>[] parmTypes = m.getParameterTypes();
        if (beanClass)
        {
            // This is bean class, so interceptor should have no parameters.
            if (parmTypes.length != 0)
            {
                // CNTR0231E: "{0}" interceptor method "{1}" signature is incorrect.
                String method = m.toGenericString();
                Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0231E", lifeCycle, method);
                throw new InjectionConfigurationException(lifeCycle + " interceptor \"" + method
                                                          + "\" must have zero parameters.");
            }
        }
        else
        {
            // This is an interceptor class, so InvocationContext is a required parameter
            // for the interceptor method.
            if ((parmTypes.length == 1))//&& ( parmTypes[0].equals(InvocationContext.class) ) )
            {
                // Has correct signature of 1 parameter of type InvocationContext
            }
            else
            {
                // CNTR0232E: The {0} method does not have the required
                // method signature for a {1} method of a interceptor class.
                String method = m.toGenericString();
                Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0232E", method, lifeCycle);
                throw new InjectionConfigurationException("CNTR0232E: The \"" + method
                                                          + "\" method does not have the required method signature for a \""
                                                          + lifeCycle + "\" method of a interceptor class.");
            }
        }
    }

}
