/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Provider;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 *
 */
@Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.jwt.Claim;org.eclipse.microprofile.jwt.Claims;org.eclipse.microprofile.jwt.ClaimValue;javax.json.JsonValue;javax.json.JsonNumber;javax.json.JsonString;javax.json.JsonStructure;javax.json.JsonArray;javax.json.JsonObject" }, immediate = true)
public class JwtCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JwtCDIExtension.class);

    private final Map<Claim, Set<Type>> injectionTypes = new HashMap<Claim, Set<Type>>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<ClaimProducer> producer = bm.createAnnotatedType(ClaimProducer.class);
        bbd.addAnnotatedType(producer, CDIServiceUtils.getAnnotatedTypeIdentifier(producer, this.getClass()));
    }

    public void processInjectionTarget(@Observes ProcessInjectionTarget<?> pit) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processInjectionTarget", pit);
        }

        Class<?> targetClass = pit.getAnnotatedType().getJavaClass();
        ClassLoader classLoader = targetClass.getClassLoader();

        for (InjectionPoint injectionPoint : pit.getInjectionTarget().getInjectionPoints()) {
            Claim claim = getClaimAnnotation(injectionPoint);
            if (claim != null) {

                if ((Claims.UNKNOWN.equals(claim.standard()) == false && claim.value().trim().isEmpty() == false) && (claim.value() != claim.standard().name())) {
                    String translatedMsg = Tr.formatMessage(tc, "MPJWT_CDI_CONFLICTING_CLAIM_NAMES", injectionPoint, claim.value(), claim.standard());
                    pit.addDefinitionError(new DeploymentException(translatedMsg));
                    continue;
                }

                Type type = injectionPoint.getType();
                Throwable configException = null;
                AnnotatedType<?> annotatedType = pit.getAnnotatedType();
                Class<?> annotatedClass = annotatedType.getJavaClass();

                if (type instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) type;
                    configException = processParameterizedType(pit, annotatedClass, injectionPoint, pType, classLoader, claim);
                } else {
                    if (ClaimValue.class.isAssignableFrom((Class<?>) type) == false && (annotatedClass.getAnnotationsByType(ApplicationScoped.class).length != 0 ||
                                                                                        annotatedClass.getAnnotationsByType(SessionScoped.class).length != 0)) {
                        String translatedMsg = Tr.formatMessage(tc, "MPJWT_CDI_INVALID_SCOPE_FOR_RAW_TYPE", injectionPoint);
                        pit.addDefinitionError(new DeploymentException(translatedMsg));
                    } else {
                        configException = validateInjectionPoint(injectionPoint, type, type, classLoader, false, claim);
                    }
                }
                if (configException != null) {
                    Tr.error(tc, "MPJWT_CDI_CANNOT_RESOLVE_INJECTION_POINT", injectionPoint, configException);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processInjectionTarget");
        }
    }

    private Claim getClaimAnnotation(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getClaimAnnotation", injectionPoint);
        }
        Claim claim = null;

        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        if (qualifiers != null) {
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(Claim.class)) {
                    claim = (Claim) qualifier;
                    break;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getClaimAnnotation", claim);
        }
        return claim;
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "afterBeanDiscovery", abd, beanManager);
        }

        // TODO: Uncomment if Claim's name and standard are ever changed to binding to register beans per type.
//        for (Entry<Claim, Set<Type>> entrySet : injectionTypes.entrySet()) {
//            Claim claim = entrySet.getKey();
//            for (Type type : entrySet.getValue()) {
//                try {
//                    if (type instanceof TypeVariable) {
//                        TypeVariable<?> typeVar = (TypeVariable<?>) type;
//                        Type[] bounds = typeVar.getBounds();
//                        for (Type bound : bounds) {
//                            addClaimBean(abd, beanManager, bound, claim);
//                        }
//                    } else {
//                        addClaimBean(abd, beanManager, type, claim);
//                    }
//                } catch (ClaimTypeException e) {
//                    abd.addDefinitionError(e);
//                }
//            }
//        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "afterBeanDiscovery");
        }
    }

    private Throwable processParameterizedType(ProcessInjectionTarget<?> pit, Class<?> annotatedClass, InjectionPoint injectionPoint, ParameterizedType injectionType,
                                               ClassLoader classLoader, Claim claim) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processParameterizedType", pit, annotatedClass, injectionPoint, injectionType, classLoader, claim);
        }

        Throwable configException = null;
        Type rawType = injectionType.getRawType();
        Type returnType = injectionType.getActualTypeArguments()[0];

        if (Provider.class.isAssignableFrom((Class<?>) rawType)) {
            configException = validateInjectionPoint(injectionPoint, returnType, returnType, classLoader, false, claim);
        } else if (Optional.class.isAssignableFrom((Class<?>) rawType)) {
            Type[] aTypes = injectionType.getActualTypeArguments();
            returnType = aTypes[0];

            Type type = returnType;
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getRawType();
            }

            if (ClaimValue.class.isAssignableFrom((Class<?>) type) == false && (annotatedClass.getAnnotationsByType(ApplicationScoped.class).length != 0 ||
                                                                                annotatedClass.getAnnotationsByType(SessionScoped.class).length != 0)) {
                String translatedMsg = Tr.formatMessage(tc, "MPJWT_CDI_INVALID_SCOPE_FOR_RAW_TYPE", injectionPoint);
                pit.addDefinitionError(new DeploymentException(translatedMsg));
            } else {
                configException = validateInjectionPoint(injectionPoint, returnType, injectionType, classLoader, true, claim);
            }
        } else {
            if (returnType instanceof ParameterizedType) {
                returnType = ((ParameterizedType) returnType).getRawType();
            }

            if (ClaimValue.class.isAssignableFrom((Class<?>) rawType) == false && (annotatedClass.getAnnotationsByType(ApplicationScoped.class).length != 0 ||
                                                                                   annotatedClass.getAnnotationsByType(SessionScoped.class).length != 0)) {
                String translatedMsg = Tr.formatMessage(tc, "MPJWT_CDI_INVALID_SCOPE_FOR_RAW_TYPE", injectionPoint);
                pit.addDefinitionError(new DeploymentException(translatedMsg));
            } else {
                configException = validateInjectionPoint(injectionPoint, returnType, injectionType, classLoader, false, claim);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processParameterizedType", configException);
        }
        return configException;
    }

    private Throwable validateInjectionPoint(InjectionPoint injectionPoint, Type conversionType, Type injectionType, ClassLoader classLoader, boolean optional, Claim claim) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateInjectionPoint", injectionPoint, conversionType, injectionType, classLoader, optional, claim);
        }

        Throwable configException = null;
        Type rawInjectionType = injectionType;

        if (injectionType instanceof ParameterizedType) {
            rawInjectionType = ((ParameterizedType) injectionType).getRawType();
        }

        try {
            Set<Type> injectionTypesForQualifier = injectionTypes.get(claim);
            if (injectionTypesForQualifier == null) {
                injectionTypesForQualifier = new HashSet<Type>();
                injectionTypes.put(claim, injectionTypesForQualifier);
            }
            injectionTypesForQualifier.add(injectionType);
        } catch (Throwable e) {
            configException = e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateInjectionPoint", configException);
        }
        return configException;
    }

    private void addClaimBean(AfterBeanDiscovery abd, BeanManager beanManager, Type type, Claim claim) throws ClaimTypeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addClaimBean", abd, beanManager, type, claim);
        }

        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (!clazz.isPrimitive()) {
                addClaimBean(abd, beanManager, type, clazz, claim);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            addClaimBean(abd, beanManager, pType, claim);
        } else {
            throw new ClaimTypeException(Tr.formatMessage(tc, "MPJWT_CDI_INVALID_INJECTION_TYPE", type));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addClaimBean");
        }
    }

    private void addClaimBean(AfterBeanDiscovery abd, BeanManager beanManager, ParameterizedType type, Claim claim) throws ClaimTypeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addClaimBean", abd, beanManager, type, claim);
        }

        Type rawInjectionType = type.getRawType();
        if (rawInjectionType instanceof Class) {
            Class<?> clazz = (Class<?>) rawInjectionType;
            addClaimBean(abd, beanManager, type, clazz, claim);
        } else {
            throw new ClaimTypeException(Tr.formatMessage(tc, "MPJWT_CDI_INVALID_INJECTION_TYPE", type));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addClaimBean");
        }
    }

    private <T> void addClaimBean(AfterBeanDiscovery abd, BeanManager beanManager, Type beanType, Class<T> clazz, Claim claim) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addClaimBean", abd, beanManager, beanType, clazz, claim);
        }

        ClaimBean<T> converterBean = new ClaimBean<T>(beanManager, beanType, clazz, claim);
        abd.addBean(converterBean);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addClaimBean");
        }
    }

    public void processInjectionPoint(@Observes ProcessInjectionPoint<?, JsonWebToken> pip, BeanManager beanManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processInjectionPoint", pip, beanManager);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processInjectionPoint");
        }
    }

}
