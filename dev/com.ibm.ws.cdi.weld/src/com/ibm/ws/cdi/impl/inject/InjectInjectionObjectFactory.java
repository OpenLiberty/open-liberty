/*******************************************************************************
 * Copyright (c) 2010, 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.inject;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.TransientReference;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldManager;
import org.jboss.weld.resolution.ResolvableBuilder;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Fulfill the @inject injection for non-contextual beans
 * i.e. This class will help resolve the @Inject of a CDI bean inside a EE type
 */
public class InjectInjectionObjectFactory {

    private static final TraceComponent tc = Tr.register(InjectInjectionObjectFactory.class);

    /**
     * Comparator to sort a list of parameter injection points into the correct order.
     * <p>
     * All of the injection points in the list to be sorted must be from the same method.
     */
    private static final Comparator<InjectionPoint> PARAMETER_INJECTION_POINT_COMPARATOR = new Comparator<InjectionPoint>() {
        @Override
        public int compare(InjectionPoint o1, InjectionPoint o2) {
            AnnotatedParameter<?> p1 = (AnnotatedParameter<?>) o1.getAnnotated();
            AnnotatedParameter<?> p2 = (AnnotatedParameter<?>) o2.getAnnotated();
            return p1.getPosition() < p2.getPosition() ? -1 : p1.getPosition() == p2.getPosition() ? 0 : 1;
        }
    };

    @SuppressWarnings("rawtypes")
    public static Object getObjectInstance(InjectInjectionBinding iBinding, Object targetObject, final CreationalContext<Object> cc,
            final CreationalContext<Object> methodInvocactionContext,
            CDIRuntime cdiRuntime) throws Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getObjectInstance", new Object[] { iBinding, Util.identity(targetObject), Util.identity(cc), Util.identity(methodInvocactionContext) });
        }

        Member targetMember = iBinding.getInjectionTarget().getMember();

        Class<?> targetClass;
        if (Modifier.isStatic(targetMember.getModifiers())) {
            // Static injection, no instance so use the member's declaring class
            targetClass = targetMember.getDeclaringClass();
        } else if (CDIUtils.isWeldProxy(targetObject)) {
            // Instance is a weld proxy, find the real type
            targetClass = targetObject.getClass().getSuperclass();
        } else {
            // Normal instance, use its type
            targetClass = targetObject.getClass();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Target class: " + targetClass.getName());
        }

        BeanManager beanManager = cdiRuntime.getClassBeanManager(targetClass);

        if (beanManager == null) {
            beanManager = cdiRuntime.getCurrentModuleBeanManager();
        }

        allBeanDebug(beanManager, iBinding);
        if (null == beanManager) {
            if (!CDIUtils.isInjectionFailureIgnored()) {
                Tr.error(tc, "no.injection.target.CWOWB1008E", targetMember, targetClass.getName());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getObjectInstance", null);
            }
            return null;
        }

        // Note: we only handle field and method injection here,
        // constructor injection is handled separately in the
        // managed object factory.

        // Find the right member from the AnnotatedType and create
        // injection points from the field or method parameters

        //NOTE: although this is a list, the current implementations mean that it's probably not ordered
        List<InjectionPoint> injectionPoints;
        // first see if this is a managed bean
        Set<Bean<?>> beans = beanManager.getBeans(targetClass);
        Bean<?> bean = beanManager.resolve(beans);

        // 2nd to see whether this is a session bean
        if ((bean == null)) {
            WebSphereBeanDeploymentArchive bda = cdiRuntime.getClassBeanDeploymentArchive(targetClass);
            if (bda != null) {
                Set<EjbDescriptor<?>> ejbDescriptors = bda.getEjbDescriptor(targetClass);

                if ((ejbDescriptors != null) && (!ejbDescriptors.isEmpty())) {
                    WeldManager weldManager = (WeldManager) beanManager;
                    //We are just getting one ejb descriptor as we are only interested in the injection points defined on it
                    EjbDescriptor<?> ejbDescriptor = ejbDescriptors.iterator().next();
                    String ejb_name = ejbDescriptor.getEjbName();
                    EjbDescriptor<?> realEjbDescriptor = weldManager.getEjbDescriptor(ejb_name);
                    bean = weldManager.getBean(realEjbDescriptor);
                }

            }
        }
        // check if the target member is static, if static we need to create our own injection point
        if (bean != null && !Modifier.isStatic(targetMember.getModifiers())) {
            // managed bean; just check it for the target member
            injectionPoints = findManagedInjectionPoints(bean, targetMember);
        } else {
            // unmanaged; need to check the target class directly for the target member
            WebSphereBeanDeploymentArchive bda = cdiRuntime.getClassBeanDeploymentArchive(targetClass);
            injectionPoints = findUnmanagedInjectionPoints(beanManager, bda, targetClass, targetMember);
        }

        debugInjectionPoints(injectionPoints);
        List<Object> references = new ArrayList<Object>();
        final BeanManager localBeanManager = beanManager;
        for (final InjectionPoint injectionPoint : injectionPoints) {

            Object injectionObject = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.entry(tc, "run");
                    }
                    Object ref = null;
                    if ((injectionPoint.getAnnotated() instanceof AnnotatedParameter<?>)
                            && injectionPoint.getAnnotated().getBaseType().equals(InjectionPoint.class)){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Creating an InjectionPoint for {0}", injectionPoint);
                        }
                        ref = localBeanManager.createInjectionPoint((AnnotatedParameter<?>) injectionPoint.getAnnotated());
                    }
                    else if ((injectionPoint.getAnnotated() instanceof AnnotatedParameter<?>)
                            && (((AnnotatedParameter<?>) injectionPoint.getAnnotated()).isAnnotationPresent(TransientReference.class))) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Getting an injectable ref from the bean manager with {0}, {1}", injectionPoint, Util.identity(methodInvocactionContext));
                        }
                        ref = localBeanManager.getInjectableReference(injectionPoint, methodInvocactionContext);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Getting an injectable ref from the bean manager with {0}, {1}", injectionPoint, Util.identity(cc));
                        }
                        ref = localBeanManager.getInjectableReference(injectionPoint, cc);
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "run", Util.identity(ref));
                    }
                    return ref;
                }
            });

            if (injectionObject != null) {
                references.add(injectionObject);
            } else {
                BeanManagerImpl beanManagerImpl = (BeanManagerImpl) localBeanManager;
                ResolvableBuilder resolvableBuilder = new ResolvableBuilder(injectionPoint, beanManagerImpl);
                Bean maybeProducerBean = beanManagerImpl.getBean(resolvableBuilder.create());
                if (maybeProducerBean instanceof org.jboss.weld.bean.ProducerMethod
                        && javax.enterprise.context.Dependent.class.isAssignableFrom(
                                maybeProducerBean.getScope())) {
                    references.add(new InjectableNull());
                }

            }
        }

        debugInjectionObjects(references.toArray());
        if (references.size() == 1) {
                Object reference = references.get(0);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getObjectInstance", Util.identity(reference));
            }
            return reference;
        } else {
            Object[] referencesArray = references.toArray(new Object[references.size()]);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                StringBuilder exitStringBuilder = new StringBuilder("[");
                boolean first = true;
                for (Object object : referencesArray){ 
                    if(!first){
                        exitStringBuilder.append(", ");
                    }
                    else first = false;
                    exitStringBuilder.append(Util.identity(object));
                }
                exitStringBuilder.append("]");
                Tr.exit(tc, "getObjectInstance", exitStringBuilder.toString());
            }
            return referencesArray;
        }
    }

    private static List<InjectionPoint> findUnmanagedInjectionPoints(BeanManager beanManager, WebSphereBeanDeploymentArchive bda, Class<?> targetClass,
            Member targetMember) throws CDIException {
        List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
        List<InjectionPoint> allInjectionPoints = null;
        if (bda != null) {
            allInjectionPoints = bda.getJEEComponentInjectionPoints(targetClass);
        }

        if (allInjectionPoints == null) {
            injectionPoints = createUnmanagedInjectionPoints(beanManager, targetClass, targetMember);
        } else {
            for (InjectionPoint injectionPoint : allInjectionPoints) {
                Member injectionPointMember = injectionPoint.getMember();
                if (targetMember.equals(injectionPointMember)) {
                    injectionPoints.add(injectionPoint);
                    if (targetMember instanceof Field) {
                        break;
                    }
                }
            }

            if (targetMember instanceof Method) {
                Collections.sort(injectionPoints, PARAMETER_INJECTION_POINT_COMPARATOR);
            }
        }
        return injectionPoints;
    }

    private static List<InjectionPoint> createUnmanagedInjectionPoints(BeanManager beanManager, Class<?> targetClass, Member targetMember) {
        List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
        AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(targetClass);
        if (targetMember instanceof Field) {
            for (AnnotatedField<?> field : annotatedType.getFields()) {
                if (field.getJavaMember().equals(targetMember)) {
                    injectionPoints.add(beanManager.createInjectionPoint(field));
                    break;
                }
            }
        } else if (targetMember instanceof Method) {
            for (AnnotatedMethod<?> method : annotatedType.getMethods()) {
                if (method.getJavaMember().equals(targetMember)) {
                    for (AnnotatedParameter<?> parameter : method.getParameters()) {
                        injectionPoints.add(beanManager.createInjectionPoint(parameter));
                    }
                    break;
                }
            }
        }
        return injectionPoints;
    }

    private static List<InjectionPoint> findManagedInjectionPoints(Bean<?> bean, Member member) {
        List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
        for (InjectionPoint injectionPoint : bean.getInjectionPoints()) {
            if (member.equals(injectionPoint.getMember())) {
                injectionPoints.add(injectionPoint);
            }
        }

        if (member instanceof Method) {
            Collections.sort(injectionPoints, PARAMETER_INJECTION_POINT_COMPARATOR);
        }

        return injectionPoints;
    }

    @Trivial
    private static final void debugInjectionPoints(List<InjectionPoint> injectionPoints) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "InjectionPoints: SIZE: " + injectionPoints.size());
            for (InjectionPoint injectionPoint : injectionPoints) {
                Tr.debug(tc, "\t InjectionPoint: " + injectionPoint);
            }
        }
    }

    /* support method ... Purely for debug purposes */
    @Trivial
    private static final void allBeanDebug(BeanManager beanManager, InjectInjectionBinding iBinding) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (beanManager == null) {
                Tr.debug(tc, "CDI runtime cannot resolve " + iBinding.getDisplayName() + " because of a null BeanManager");
            } else {
                Set<Bean<?>> allBeans = beanManager.getBeans(Object.class);
                Tr.debug(tc, "\t \t ***** ALL Beans: ***** ");
                for (Bean<?> bean : allBeans) {
                    Tr.debug(tc, "Bean: " + bean);
                }
                Tr.debug(tc, "\t \t ***** ALL Beans: ***** ");
            }
        }
    }

    /* support method ... Purely for debug purposes */
    @Trivial
    private static final void debugInjectionObjects(Object[] injectionObjects) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (injectionObjects.length == 0) {
                Tr.debug(tc, " []");
                return;
            }

            StringBuilder buffer = new StringBuilder(injectionObjects.length * 16);
            buffer.append('[');
            for (int i = 0; i < injectionObjects.length; i++) {
                if (injectionObjects[i] != injectionObjects) {
                    buffer.append(Util.identity(injectionObjects[i]));
                } else {
                    buffer.append("(this Collection)");
                }

                if (i < injectionObjects.length - 1) {
                    buffer.append(',');
                }
            }
            buffer.append(']');
            Tr.debug(tc, "SUCCESS " + buffer.toString());
        }
    }
}

