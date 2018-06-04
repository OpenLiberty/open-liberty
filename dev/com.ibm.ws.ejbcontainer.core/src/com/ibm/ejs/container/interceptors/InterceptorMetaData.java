/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.interceptors;

import java.lang.reflect.Method;
import java.util.Map;

import com.ibm.ejs.container.LifecycleInterceptorWrapper;
import com.ibm.ejs.container.ManagedBeanOBase;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ConstructionCallback;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionTarget;

/**
 * This class is used to hold the interceptor meta data required for
 * invoking lifecycle callback interceptor methods for a given EJB.
 * The intent is the BeanMetaData for the EJB contains the reference to a
 * InterceptorMetaData instance if the EJB has at least 1 interceptor
 * class and/or the EJB class itself has at least 1 lifecycle callback
 * interceptor method. The BeanMetaData reference to InterceptorMetaData
 * must be null if the EJB does not have any interceptor classes or
 * interceptor methods.
 * <p>
 * Note, the interceptor meta data for invoking AroundInvoke interceptor methods
 * is found in the EJBMethodInfoImpl object, not in the BeanMetaData object.
 * <p>
 * A single InterceptorMetaData object created for a EJB contains the
 * following arrays of InterceptorProxy object (which are used to invoke an
 * interceptor method at runtime):
 * <ul>
 * <li>Array of PostConstruct interceptor methods.
 * <li>Array of PostActivate interceptor methods.
 * <li>Array of PrePassivate interceptor methods.
 * <li>Array of PreDestroy interceptor methods.
 * </ul>
 * <p>
 * The life cycle callback arrays are ordered so that iterating over the array
 * from index 0 to the last element results in the interceptor methods being invoked
 * in the order defined by section 12.4.1 Multiple Callback Interceptor Methods for
 * Life Cycle Callback Events if the EJB 3 core specification.
 */
public class InterceptorMetaData {
    private static final String CLASS_NAME = InterceptorMetaData.class.getName();
    private static final TraceComponent tc = Tr.register(InterceptorMetaData.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Interceptor classes for this bean or null if the bean does
     * not have any interceptor classes.
     */
    final public Class<?>[] ivInterceptorClasses;

    /**
     * Interceptor factories for this bean or null if the bean does
     * not have any interceptors.
     */
    // F87720
    final public ManagedObjectFactory<?>[] ivInterceptorFactories;

    /**
     * An ordered array of AroundConstruct interceptor methods. The methods in this
     * array must be ordered as defined in "12.4.1 Multiple Callback Interceptor
     * Methods for a Life Cycle Callback Event" of the EJB 3 specification.
     */
    final public InterceptorProxy[] ivAroundConstructInterceptors;

    /**
     * An ordered array of PrePassivate interceptor methods. The methods in this
     * array must be ordered as defined in "12.4.1 Multiple Callback Interceptor
     * Methods for a Life Cycle Callback Event" of the EJB 3 specification.
     */
    final public InterceptorProxy[] ivPrePassivateInterceptors;

    /**
     * An ordered array of PreDestroy interceptor methods. The methods in this
     * array must be ordered as defined in "12.4.1 Multiple Callback Interceptor
     * Methods for a Life Cycle Callback Event" of the EJB 3 specification.
     */
    final public InterceptorProxy[] ivPreDestroyInterceptors;

    /**
     * An ordered array of PostConstruct interceptor methods. The methods in this
     * array must be ordered as defined in "12.4.1 Multiple Callback Interceptor
     * Methods for a Life Cycle Callback Event" of the EJB 3 specification.
     */
    final public InterceptorProxy[] ivPostConstructInterceptors;

    /**
     * An ordered array of PostActivate interceptor methods. The methods in this
     * array must be ordered as defined in "12.4.1 Multiple Callback Interceptor
     * Methods for a Life Cycle Callback Event" of the EJB 3 specification.
     */
    final public InterceptorProxy[] ivPostActivateInterceptors;

    // F743-1751
    /**
     * Lifecycle methods declared on the bean class. Indexed by the MID
     * constants in {@link com.ibm.ejs.container.LifecycleInterceptorWrapper}.
     * This field will only be non-null if lifecycle interceptors need to use
     * preInvokeForLifecycleInterceptors. An array element will only be non-null
     * if the bean actually declared that lifecycle interceptor method.
     */
    final public Method[] ivBeanLifecycleMethods;

    // F743-21481
    /**
     * Two dimensional array mapping an interceptor class to the list of
     * <code>InjectionTargets</code> that are visible to it. The first
     * index represents an interceptor class, and the second array index
     * represents the <code>InjectionTargets</code> seen by that class.
     *
     * The first index of this array (ie, the interceptor classes) MUST
     * be kept in the exact same order as the <code>ivInterceptorClasses</code>
     * list. These two lists are used in conjunction when instantiating
     * the interceptor classes, and if they get out-of-sync with each
     * other, then the interceptor classes will be associated with the
     * wrong injection targets.
     *
     * This mapping is null until the reference processing has completed.
     *
     */
    public InjectionTarget[][] ivInterceptorInjectionTargets;

    /**
     * Create InterceptorMetaData instance to hold interceptor meta data needed
     * for invoking interceptor methods.
     *
     * @param classes is the array of class objects to use when creating interceptor instances.
     * @param aroundConstruct is the array of InterceptorProxy objects for invoking around-construct
     *            interceptor methods. Null if there are no interceptor methods of this type.
     * @param postConstruct is the array of InterceptorProxy objects for invoking post-construct
     *            interceptor methods. Null if there are no interceptor methods of this type.
     * @param postActivate is the array of InterceptorProxy objects for invoking post-activate
     *            interceptor methods. Null if there are no interceptor methods of this type.
     * @param prePassivate is the array of InterceptorProxy objects for invoking pre-passivate
     *            interceptor methods. Null if there are no interceptor methods of this type.
     * @param preDestroy is the array of InterceptorProxy objects for invoking pre-destroy
     *            interceptor methods. Null if there are no interceptor methods of this type.
     * @param beanLifecycleMethods is an array of java reflection Method objects for the
     *            lifecycle interceptor methods declared directly on the bean class.
     */
    public InterceptorMetaData(Class<?>[] classes, ManagedObjectFactory<?>[] factories, InterceptorProxy[] aroundConstruct, InterceptorProxy[] postConstruct,
                               InterceptorProxy[] postActivate, InterceptorProxy[] prePassivate, InterceptorProxy[] preDestroy, Method[] beanLifecycleMethods // F743-1751
    ) {
        ivAroundConstructInterceptors = aroundConstruct;
        ivInterceptorClasses = classes;
        ivInterceptorFactories = factories;
        ivPostConstructInterceptors = postConstruct;
        ivPostActivateInterceptors = postActivate;
        ivPrePassivateInterceptors = prePassivate;
        ivPreDestroyInterceptors = preDestroy;
        ivBeanLifecycleMethods = beanLifecycleMethods; // F743-1751
    }

    /**
     * Create the interceptor instances associated with this
     * enterprise bean instance. The lifetime of interceptor instances
     * is identical to lifetime of the EJB instance.
     *
     * @param injectionEngine the injection engine
     * @param interceptors the array of interceptor instances to populate
     * @param managedObjectContext the managed object context for the bean associated with these interceptors
     * @param targetContext the injection target context
     */
    public void createInterceptorInstances(InjectionEngine injectionEngine,
                                           Object[] interceptors,
                                           ManagedObjectContext managedObjectContext,
                                           ManagedBeanOBase targetContext) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createInterceptorInstances");
        }

        if (ivInterceptorClasses != null) {

            int numberOfInterceptors = ivInterceptorClasses.length;
            for (int i = 0; i < numberOfInterceptors; i++) {
                if (ivInterceptorFactories == null || ivInterceptorFactories[i] == null) {
                    interceptors[i] = createInterceptorInstanceUsingConstructor(injectionEngine, ivInterceptorClasses[i], ivInterceptorInjectionTargets[i], targetContext);
                } else {
                    interceptors[i] = createInterceptorInstancesUsingMOF(ivInterceptorInjectionTargets[i], managedObjectContext, targetContext, ivInterceptorFactories[i]);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createInterceptorInstances");
        }
    }

    private Object createInterceptorInstanceUsingConstructor(InjectionEngine injectionEngine,
                                                             Class<?> interceptorClass,
                                                             InjectionTarget[] targetsForClass,
                                                             ManagedBeanOBase targetContext) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createInterceptorInstanceUsingConstructor");
        }

        Object interceptor = interceptorClass.newInstance();

        // F743-21481
        // Inject into the class, if needed.
        // This injection used to be performed in each of the BeanO instances,
        // but now its been moved into this common location.
        try {
            if (targetsForClass.length > 0) {
                for (InjectionTarget oneTarget : targetsForClass) {
                    injectionEngine.inject(interceptor, oneTarget, targetContext);
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + "createInterceptorInstanceUsingConstructor", "248", this);
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Injection failure", t);
            }
            throw ExceptionUtil.EJBException("Injection failure", t);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createInterceptorInstanceUsingConstructor", interceptor);
        }
        return interceptor;
    }

    public Object createInterceptorInstancesUsingMOF(InjectionTarget[] targetsForClass,
                                                     ManagedObjectContext managedObjectContext,
                                                     ManagedBeanOBase targetContext,
                                                     ManagedObjectFactory<?> interceptorFactory) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createInterceptorInstancesUsingMOF");
        }

        @SuppressWarnings("rawtypes")
        ManagedObjectInvocationContext invocationContext = new ManagedObjectInvocationContextImpl(managedObjectContext);
        @SuppressWarnings("unchecked")
        ManagedObject<?> managedObject = interceptorFactory.createManagedObject(invocationContext);
        Object interceptor = managedObject.getObject();

        // Inject into the class, if needed.
        // This injection used to be performed in each of the BeanO instances,
        // but now its been moved into this common location.
        try {
            managedObject.inject(targetsForClass, targetContext);
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + "createInterceptorInstancesUsingMOF", "284", this);
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Injection failure", t);
            }
            throw ExceptionUtil.EJBException("Injection failure", t);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createInterceptorInstancesUsingMOF", interceptor);
        }

        return interceptor;
    }

    // F743-17630
    /**
     * Dumps contents of metadata.
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("Interceptor MetaData:\n");

        if (ivInterceptorClasses != null) {
            buffer.append("     Interceptor classes:\n");
            for (Class<?> oneClass : ivInterceptorClasses) {
                buffer.append("          " + oneClass.getName() + "\n");
            }
        } else {
            buffer.append("     Interceptor classes: NONE\n");
        }

        if (ivPostConstructInterceptors != null) {
            buffer.append("     PostConstruct interceptor methods:\n");
            for (InterceptorProxy oneProxy : ivPostConstructInterceptors) {
                buffer.append("          " + oneProxy.toString() + "\n");
            }
        } else {
            buffer.append("     PostConstruct interceptor methods: NONE\n");
        }

        if (ivPostActivateInterceptors != null) {
            buffer.append("     PostActivate interceptor methods:\n");
            for (InterceptorProxy oneProxy : ivPostActivateInterceptors) {
                buffer.append("          " + oneProxy.toString() + "\n");
            }
        } else {
            buffer.append("     PostActivate interceptor methods: NONE\n");
        }

        if (ivPrePassivateInterceptors != null) {
            buffer.append("     PrePassivate interceptor methods:\n");
            for (InterceptorProxy oneProxy : ivPrePassivateInterceptors) {
                buffer.append("          " + oneProxy.toString() + "\n");
            }
        } else {
            buffer.append("     PrePassivate interceptor methods: NONE\n");
        }

        if (ivPreDestroyInterceptors != null) {
            buffer.append("     PreDestroy interceptor methods:\n");
            for (InterceptorProxy oneProxy : ivPreDestroyInterceptors) {
                buffer.append("          " + oneProxy.toString() + "\n");
            }
        } else {
            buffer.append("     PreDestroy interceptor methods: NONE\n");
        }

        if (ivAroundConstructInterceptors != null) {
            buffer.append("     AroundConstruct interceptor methods:\n");
            for (InterceptorProxy oneProxy : ivAroundConstructInterceptors) {
                buffer.append("          " + oneProxy.toString() + "\n");
            }
        } else {
            buffer.append("     AroundConstruct interceptor methods: NONE\n");
        }

        if (ivBeanLifecycleMethods != null) {
            buffer.append("     BeanLifecycle methods:\n");
            for (int i = 0; i < ivBeanLifecycleMethods.length; i++) {
                buffer.append("          ").append(LifecycleInterceptorWrapper.TRACE_NAMES[i]).append(": ").append(ivBeanLifecycleMethods[i]).append("\n");
            }
        } else {
            buffer.append("     BeanLifeCycle methods: NONE\n");
        }

        // F743-21481
        if (ivInterceptorInjectionTargets != null) {
            buffer.append("     InjectionTargets for interceptor classes:\n");
            for (int a = 0; a < ivInterceptorInjectionTargets.length; a++) {
                buffer.append("          Interceptor class: " + a + "\n");
                for (int b = 0; b < ivInterceptorInjectionTargets[a].length; b++) {
                    buffer.append("               " + ivInterceptorInjectionTargets[a][b] + "\n");
                }
            }
        } else {
            buffer.append("     InjectionTargets for interceptor classes: NONE\n");
        }

        return buffer.toString();
    }

    /**
     * Simple implementation of {@link ManagedObjectInvocationContext} that just makes
     * the EJB ManagedObjectContext available to the interceptor ManagedObjectFactory.
     */
    @SuppressWarnings("rawtypes")
    private static final class ManagedObjectInvocationContextImpl implements ManagedObjectInvocationContext {
        private final ManagedObjectContext managedObjectContext;

        private ManagedObjectInvocationContextImpl(ManagedObjectContext managedObjectContext) {
            this.managedObjectContext = managedObjectContext;
        }

        @Override
        public Object aroundConstruct(ConstructionCallback constructionCallback, Object[] parameters, Map data) throws Exception {
            //just create the instance ... only used when CDI is not creating the instance
            return constructionCallback.getConstructor().newInstance(parameters);
        }

        @Override
        public ManagedObjectContext getManagedObjectContext() {
            return managedObjectContext;
        }

        public void prePostConstruct(ManagedObject mo) {
            // Nothing to do for EJB
        }

    }

}
