/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld.injection;

import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
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
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionTargetListener;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
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

    /**
     * The annotations which Weld knows about and will either handle itself, or will delegate to us through one of the specific InjectionServices interfaces
     * <p>
     * Anything Weld doesn't know about, the injection engine will handle in the aroundInject method.
     */
    private static final Set<Class<?>> ANNOTATIONS_KNOWN_TO_WELD = new HashSet<Class<?>>(Arrays.asList(Inject.class));

    private final Map<Class<?>, ReferenceContext> referenceContextMap = new HashMap<Class<?>, ReferenceContext>();
    private final Set<ReferenceContext> referenceContexts = new HashSet<ReferenceContext>();

    private final Map<Object, WebSphereInjectionTargetListener<?>> injectionTargetListeners = new ConcurrentHashMap<>();

    private final WebSphereCDIDeployment deployment;

    /**
     * @param webSphereCDIDeployment
     */
    public WebSphereInjectionServicesImpl(WebSphereCDIDeployment webSphereCDIDeployment) {
        this.deployment = webSphereCDIDeployment;
    }

    public void addReferenceContext(ReferenceContext referenceContext) {
        referenceContexts.add(referenceContext);
        Set<Class<?>> classes = referenceContext.getProcessedInjectionClasses();
        for (Class<?> clazz : classes) {
            referenceContextMap.put(clazz, referenceContext);
        }
    }

    private void injectJavaEEResources(final InjectionContext<?> injectionContext) {

        if (injectionContext != null) {
            try {
                Boolean hasTarget = callInject(injectionContext);
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

    private Boolean callInject(final InjectionContext<?> injectionContext) throws PrivilegedActionException {
        Boolean hasTargets = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                //This is EE injection without @Produces
                Boolean hasTargets = inject(injectionContext);
                return hasTargets;
            }
        });
        return hasTargets;
    }

    private Boolean inject(final InjectionContext<?> injectionContext) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "inject", new Object[] { Util.identity(injectionContext) });
        }

        Boolean hasTargets = Boolean.FALSE;

        Object toInject = injectionContext.getTarget();
        Class<?> toInjectClass = injectionContext.getAnnotatedType().getJavaClass();
        hasTargets = inject(toInject, toInjectClass, injectionContext);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "inject", hasTargets);
        }
        return hasTargets;
    }

    private Boolean inject(Object toInject, Class<?> toInjectClass, final InjectionContext<?> injectionContext) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "inject", new Object[] { Util.identity(toInject), Util.identity(injectionContext) });
        }

        //Class<?> clazz = toInject.getClass();
        Boolean hasTargets = Boolean.FALSE;

        InjectionTarget[] targets = getInjectionTargets(toInjectClass, toInject);
        if (null != targets && targets.length > 0) {
            hasTargets = Boolean.TRUE;

            WebSphereInjectionTargetListener<?> listener = null;
            if (toInject != null) {
                listener = injectionTargetListeners.get(toInject);
            }
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

                        InjectionTargetContext ctx;
                        if (listener != null) {
                            ctx = listener.getCurrentInjectionTargetContext();
                        } else {
                            ctx = new InjectionTargetContext() {
                                @Override
                                public <T> T getInjectionTargetContextData(Class<T> arg0) {
                                    return null;
                                }
                            };
                        }

                        target.inject(toInject, ctx);
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

                if (listener != null) {
                    listener.injectionTargetProcessed(target);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "inject");

        return hasTargets;
    }

    @Override
    public InjectionTarget[] getInjectionTargets(Class<?> clazz) throws InjectionException {
        return getInjectionTargets(clazz, null);
    }

    private InjectionTarget[] getInjectionTargets(Class<?> clazz, Object toInject) throws InjectionException {
        // clazz is the class that may receive the injection.
        // mod   is the app stuff that may give injections.

        Class<?> injectionClass = null;

        if (toInject != null) {
            injectionClass = toInject.getClass();
            if (CDIUtils.isWeldProxy(toInject)) {
                injectionClass = injectionClass.getSuperclass();
            }
        } else {
            injectionClass = clazz;
        }

        ReferenceContext referenceContext = referenceContextMap.get(injectionClass);

        if (referenceContext == null) {
            referenceContext = findReferenceContext(injectionClass);
        }

        InjectionTarget[] targets = null;

        if (referenceContext != null) {
            targets = referenceContext.getInjectionTargets(injectionClass);
            if (targets != null && targets.length > 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getInjectionTargets", injectionClass + " injection targets found " + Arrays.asList(targets));
                }

                for (InjectionTarget target : targets) {
                    Class<?> declaringClass = target.getMember().getDeclaringClass();
                    if (declaringClass != injectionClass && !referenceContextMap.containsKey(declaringClass)) {
                        referenceContextMap.put(declaringClass, referenceContext);
                    }
                }

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getInjectionTargets", injectionClass + " no injection targets found");
                }
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
        injectionTargetListeners.clear();
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
        injectJavaEEResources(injectionContext);

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
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "registerInjectionTarget", new Object[] { injectionTarget, Util.identity(annotatedType) });
        }
        Class<?> declaringClass = annotatedType.getJavaClass();
        WebSphereBeanDeploymentArchive wbda = deployment.getBeanDeploymentArchiveFromClass(declaringClass);
        //If it's not an application class we don't need to validate it
        if (wbda != null) {
            CDIArchive cdiArchive = wbda.getArchive();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Injection Target Annotations: " + annotatedType.getAnnotations());
            }
            //We don't need to worry about constructors because constructors cannot be a producer.
            for (Annotated annotated : annotatedType.getFields()) {
                validateAnnotatedMember(annotated, annotatedType.getJavaClass(), cdiArchive);
            }
            for (AnnotatedMethod<?> annotatedMethod : annotatedType.getMethods()) {
                validateAnnotatedMethod(annotatedMethod, declaringClass, cdiArchive);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "registerInjectionTarget");
        }
    }

    private <T> void validateAnnotatedMethod(AnnotatedMethod<T> annotatedMethod, Class<?> declaringClass, CDIArchive cdiArchive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateAnnotatedMethod", new Object[] { Util.identity(annotatedMethod), declaringClass, cdiArchive });
        }
        List<AnnotatedParameter<T>> parameters = annotatedMethod.getParameters();
        for (AnnotatedParameter<?> injectedParameter : parameters) {
            validateAnnotatedMember(injectedParameter, declaringClass, cdiArchive);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateAnnotatedMethod");
        }
    }

    private void validateAnnotatedMember(Annotated annotated, Class<?> declaringClass, CDIArchive cdiArchive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateAnnotatedMember", new Object[] { Util.identity(annotated), declaringClass, cdiArchive });
        }
        for (Annotation annotation : annotated.getAnnotations()) {
            if (annotation instanceof EJB) {
                EEValidationUtils.validateEjb(((EJB) annotation), declaringClass, annotated, cdiArchive);
            } else if (annotation instanceof Resource) {
                EEValidationUtils.validateResource(((Resource) annotation), declaringClass, annotated, cdiArchive);
            } else if (annotation instanceof WebServiceRef) {
                EEValidationUtils.validateWebServiceRef(((WebServiceRef) annotation), declaringClass, annotated);
            } else if (annotation instanceof PersistenceContext) {
                EEValidationUtils.validatePersistenceContext(((PersistenceContext) annotation), declaringClass, annotated);
            } else if (annotation instanceof PersistenceUnit) {
                EEValidationUtils.validatePersistenceUnit(((PersistenceUnit) annotation), declaringClass, annotated);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateAnnotatedMember");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws InjectionException
     */
    @Override
    public void registerInjectionTargetListener(WebSphereInjectionTargetListener<?> listener) {
        Object instance = listener.getObject();
        injectionTargetListeners.put(instance, listener);
    }

    @Override
    public void deregisterInjectionTargetListener(WebSphereInjectionTargetListener<?> listener) {
        Object instance = listener.getObject();
        injectionTargetListeners.remove(instance);
    }
}
