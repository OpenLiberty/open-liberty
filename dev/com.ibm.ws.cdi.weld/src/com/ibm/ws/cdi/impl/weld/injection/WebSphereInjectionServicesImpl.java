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
package com.ibm.ws.cdi.impl.weld.injection;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.inject.Inject;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

import org.jboss.weld.injection.spi.InjectionContext;
import org.jboss.weld.injection.spi.InjectionServices;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * Responsible for Injecting Java EE component types into CDI managed beans
 * <p>
 * Instances of this class will be instantiated by the {@code ServiceLoader}. There is one instance of this class for each BDA.
 * <p>
 * Our implementation of these weld interfaces mostly delegates to the injection engine or its definitions of injection targets and bindings.
 * <p>
 * There are several strategies here:
 * <ol>
 * <li>For @Inject, we do nothing, Weld takes care of injecting CDI instances for us</li>
 * <li>For @EJB, @Resource, @WebServiceRef, @PersistenceContext and @PersistenceUnit we implement the XyzInjectionServices interfaces. These return factories which return an
 * instance which weld will inject for us.</li>
 * <li>For any other Java EE injection, we implement InjectionServices and in the aroundInject method we delegate to the injection engine to inject into the instance for us.</li>
 * <ol>
 * <p>
 * Unfortunately, we can't delegate to the injection engine for all types of injection because it doesn't handle injection into static fields (which isn't legal except in the case
 * of using a static field as a producer for a Java EE Resource).
 */

public class WebSphereInjectionServicesImpl implements WebSphereInjectionServices {

    // because we use a packinfo.java for trace options, just need this to register our group and message file
    static final TraceComponent tc = Tr.register(WebSphereInjectionServicesImpl.class);

    @SuppressWarnings("unchecked")
    /**
     * The annotations which Weld knows about and will either handle itself, or will delegate to us through one of the specific InjectionServices interfaces
     * <p>
     * Anything Weld doesn't know about, the injection engine will handle in the aroundInject method.
     */
    private static final Set<Class<?>> ANNOTATIONS_KNOWN_TO_WELD = new HashSet<Class<?>>(Arrays.asList(Inject.class,
                                                                                                       EJB.class,
                                                                                                       Resource.class,
                                                                                                       WebServiceRef.class,
                                                                                                       PersistenceContext.class,
                                                                                                       PersistenceUnit.class));

    private final InjectionEngine injectionEngine;

    private final Map<Class<?>, ReferenceContext> referenceContextMap = new HashMap<Class<?>, ReferenceContext>();
    private final Set<ReferenceContext> referenceContexts = new HashSet<ReferenceContext>();

    public WebSphereInjectionServicesImpl(CDIRuntime cdiRuntime) {
        this.injectionEngine = cdiRuntime.getInjectionEngine();
    }

    public void addReferenceContext(ReferenceContext referenceContext) {
        referenceContexts.add(referenceContext);
        Set<Class<?>> classes = referenceContext.getProcessedInjectionClasses();
        for (Class<?> clazz : classes) {
            referenceContextMap.put(clazz, referenceContext);
        }
    }

    private void injectJavaEEResources(Object managedBeanInstance) {

        // This is the manage bean that may need to receive injections
        final Object mbInstance = managedBeanInstance;

        if (mbInstance != null) {
            try {
                Boolean hasTarget = callInject(mbInstance);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "inject", "hasTarget [" + hasTarget + "]");

            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, "cdi.resource.injection.error.CWOWB1000E", e.getLocalizedMessage());
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "inject - null mbInstance");
            }
        }
    }

    private Boolean callInject(final Object mbInstance) throws PrivilegedActionException {
        Boolean hasTargets = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                //This is EE injection without @Produces
                Boolean hasTargets = inject(mbInstance.getClass(), mbInstance);
                return hasTargets;
            }
        });
        return hasTargets;
    }

    private Boolean inject(Class<?> clazz, Object toInject) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "inject", new Object[] { Util.identity(toInject) });

        Boolean hasTargets = Boolean.FALSE;

        InjectionTarget[] targets = getInjectionTargets(clazz, toInject);
        if (null != targets && targets.length > 0) {
            hasTargets = Boolean.TRUE;
            for (InjectionTarget target : targets) {
                // for each possible giveable injection target for this manage bean class, see if the target has a binding. If
                // it does then inject it into our manage bean object.

                if (ANNOTATIONS_KNOWN_TO_WELD.contains(target.getInjectionBinding().getAnnotationType())) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "inject", "skipping --> [" + target + "]");
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "inject", "about to inject resource --> [" + target + "]");

                    try {

                        injectionEngine.inject(toInject, target);
                    } catch (Exception e) {
                        if (tc.isErrorEnabled()) {
                            Tr.error(tc, "cdi.resource.injection.error.CWOWB1000E", e.getMessage());
                        }

                        com.ibm.ws.ffdc.FFDCFilter.processException(e, getClass().getName() + ".inject", "248", this);
                        throw e;
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "inject", "injected resource --> [" + target + "]");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "inject");

        return hasTargets;
    }

    @Override
    public InjectionTarget[] getInjectionTargets(Class<?> clazz) throws CDIException {
        return getInjectionTargets(clazz, null);
    }

    InjectionTarget[] getInjectionTargets(Class<?> clazz, Object toInject) throws CDIException {
        // clazz is the class that may receive the injection.
        // mod   is the app stuff that may give injections.

        Class<?> injectionClass = clazz;

        if (toInject != null && CDIUtils.isWeldProxy(toInject)) {
            injectionClass = clazz.getSuperclass();
        }
        ReferenceContext referenceContext = referenceContextMap.get(injectionClass);

        if (referenceContext == null) {
            referenceContext = findReferenceContext(injectionClass);
        }

        InjectionTarget[] targets = null;

        if (referenceContext != null) {
            try {
                targets = referenceContext.getInjectionTargets(injectionClass);

                if (targets != null && targets.length > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getInjectionTargets", injectionClass + " injection targets found " + Arrays.asList(targets));
                    }

                    for (InjectionTarget target : targets) {
                        Class<?> declaringClass = target.getMember().getDeclaringClass();
                        if (declaringClass != clazz && !referenceContextMap.containsKey(declaringClass)) {
                            referenceContextMap.put(declaringClass, referenceContext);
                        }
                    }

                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getInjectionTargets", injectionClass + " no injection targets found");
                    }
                }

            } catch (InjectionException e) {
                throw new CDIException(e);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getInjectionTargets", injectionClass + " ReferenceContext not found");
            }
        }

        return targets;
    }

    private ReferenceContext findReferenceContext(Class<?> injectionClass) {
        ReferenceContext referenceContext = null;
        for (ReferenceContext ctx : referenceContexts) {
            Set<Class<?>> clazzes = ctx.getProcessedInjectionClasses();
            if (clazzes.contains(injectionClass)) {
                referenceContext = ctx;
                addReferenceContext(referenceContext);
            }
        }

        return referenceContext;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.weld.bootstrap.api.Service#cleanup()
     */
    @Override
    public void cleanup() {
        //no-op
    }

    /**
     * Perform Injection.
     * For EE
     */
    @Override
    public <T> void aroundInject(final InjectionContext<T> injectionContext) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Annotations: " + injectionContext.getAnnotatedType());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Perform EE injection.");
        }
        injectJavaEEResources(injectionContext.getTarget());

        // perform Weld injection
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                injectionContext.proceed();
                return null;
            }

        });

    }

    /**
     * This method validates the injection point and then returns the object
     * {@link InjectionServices#registerInjectionTarget(javax.enterprise.inject.spi.InjectionTarget, AnnotatedType)}
     */
    @Override
    public <T> void registerInjectionTarget(javax.enterprise.inject.spi.InjectionTarget<T> injectionTarget, AnnotatedType<T> annotatedType) {
        //no op
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Injection Target Annotations: " + annotatedType.getAnnotations());
        }

    }
}
