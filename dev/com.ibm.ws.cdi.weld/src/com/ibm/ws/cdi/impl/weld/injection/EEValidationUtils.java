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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.DefinitionException;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceRef;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Provides utility methods to validate injected EE objects.
 */
public class EEValidationUtils {

    private static final TraceComponent tc = Tr.register(EEValidationUtils.class);

    /**
     * Returns whether a given injection point is a producer (i.e. annotated with @Produces)
     *
     * @return true if the injection point is a producer, otherwise false
     */
    private static boolean isProducer(Annotated annotated) {
        boolean isProducer = false;

        Produces produces = annotated.getAnnotation(Produces.class);

        if (produces != null) {
            isProducer = true;
        }

        return isProducer;
    }

    private static void throwDefinitionException(Class<?> declaringClass, String memberName, Class<?> fieldType, String injectedObjectClass) throws DefinitionException {
        String producerFieldName = declaringClass.getName() + "." + memberName;
        String fieldTypeName = fieldType.getName();
        throw new DefinitionException(Tr.formatMessage(tc, "resource.producer.validation.error.CWOWB1007E", producerFieldName, fieldTypeName, injectedObjectClass));
    }

    private static void throwDefinitionException(Class<?> declaringClass, Annotated annotated, String injectedObjectClass) throws DefinitionException {
        throwDefinitionException(declaringClass, getInjectedMemberName(annotated), getInjectedClass(annotated), injectedObjectClass);
    }

    private static void throwDefinitionException(Class<?> declaringClass, Annotated annotated, Class injectedObjectClass) throws DefinitionException {
        throwDefinitionException(declaringClass, annotated, injectedObjectClass.getCanonicalName());
    }

    private static Class<?> getInjectedClass(Annotated annotated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getInjectedClass", new Object[] { Util.identity(annotated) });
        }
        if (annotated instanceof AnnotatedField) {
            return ((AnnotatedField<?>) annotated).getJavaMember().getType();
        } else if (annotated instanceof AnnotatedParameter) {
            AnnotatedCallable<?> callable = ((AnnotatedParameter<?>) annotated).getDeclaringCallable(); //In Java 8 we can replace this, AnnotatedParameter lets us get the underlying type directly
            Class<?>[] paramaterTypes = null;
            if (callable instanceof AnnotatedMethod) {
                Method method = ((AnnotatedMethod<?>) callable).getJavaMember();
                paramaterTypes = method.getParameterTypes();
            } else if (callable instanceof AnnotatedConstructor) {
                Constructor<?> constructor = ((AnnotatedConstructor<?>) callable).getJavaMember();
                paramaterTypes = constructor.getParameterTypes();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectedClass");
            }
            return paramaterTypes[((AnnotatedParameter<?>) annotated).getPosition()];
        } else {
            throw new UnsupportedOperationException("Only AnnotatedFields or AnnotatedMethods should be here");
        }
    }

    private static String getInjectedMemberName(Annotated annotated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getInjectedMemberName", new Object[] { Util.identity(annotated) });
        }
        if (annotated instanceof AnnotatedField) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectedClass");
            }
            return ((AnnotatedField<?>) annotated).getJavaMember().getName();
        } else if (annotated instanceof AnnotatedParameter) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getInjectedClass");
            }
            return ((AnnotatedParameter<?>) annotated).getDeclaringCallable().getJavaMember().getName();
        } else {
            throw new UnsupportedOperationException("Only AnnotatedFields or AnnotatedMethods should be here");
        }
    }

    @FFDCIgnore(ClassCastException.class)
    public static void validateEjb(EJB ejb, Class<?> declaringClass, Annotated annotated, CDIArchive cdiArchive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateEjb", new Object[] { Util.identity(annotated), cdiArchive });
        }
        CDIRuntime cdiRuntime = cdiArchive.getCDIRuntime();
        EjbEndpointService ejbEndpointService = cdiRuntime.getEjbEndpointService();

        if (ejbEndpointService != null && isProducer(annotated)) {
            try {
                ejbEndpointService.validateEjbInjection(ejb, cdiArchive, getInjectedClass(annotated));
            } catch (CDIException e) {
                // We were unable to get the EJB endpoints, quietly skip validation
            } catch (ClassCastException e) {
                // Validation failed, the EJB does not match the field type
                
                /*
                 * The className is not exposed via any interfaces but com.ibm.ws.ejbcontainer.osgi.internal.ModuleInitDataImpl
                 * puts it in the error message: 

                 * throw new ClassCastException("The " + beanNames[0] +
                 *                       " EJB does not implement the " +
                 *                       injectionType.getName() + " injection type.");
                 */
                String className = "UNKOWN";
                try {
                    e.getMessage().substring(4);
                    className = className.substring(0, className.indexOf("EJB interface is not"));
                } catch (IndexOutOfBoundsException ex) {
                    //Do nothing
                }
                EEValidationUtils.throwDefinitionException(declaringClass, annotated, className);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateEjb");
        }
    }

    /**
     * Validates that the object referenced by a JNDI name has the given type
     * <p>
     * Validation is lenient, if we can't find the object for any reason, we won't fail validation.
     *
     * @throws DefinitionException if the object is found and does not have the correct type
     */
    private static void validateJndiLookup(String lookupString, Annotated annotated, Class<?> declaringClass, CDIArchive cdiArchive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateJndiLookup", new Object[] { Util.identity(annotated), declaringClass, cdiArchive });
        }
        // We need to set a current component before doing anything to do with JNDI
        CDIRuntime cdiRuntime = cdiArchive.getCDIRuntime();
        try {
            cdiRuntime.beginContext(cdiArchive);
            InitialContext initialContext = new InitialContext();

            validateJndiLookup(initialContext, lookupString, annotated, declaringClass, cdiArchive);

        } catch (NamingException ex) {
            // Failed to look up the object, just return without failing validation
        } catch (CDIException e) {
            throw new IllegalStateException(e);
        } finally {
            cdiRuntime.endContext();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateJndiLookup", new Object[] { Util.identity(annotated) });
        }
    }

    private static void validateJndiLookup(InitialContext initialContext, String lookupString, Annotated annotated, Class<?> declaringClass,
                                           CDIArchive cdiArchive) throws NamingException, CDIException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateJndiLookup", new Object[] { initialContext, lookupString, Util.identity(annotated), declaringClass });
        }
        Name lookupName = initialContext.getNameParser("").parse(lookupString);

        // Split the name into the suffix and the parent context
        String name = lookupName.get(lookupName.size() - 1);
        Name prefix = lookupName.getPrefix(lookupName.size() - 1);

        Class<?> injectedClass = getInjectedClass(annotated);

        if (!(injectedClass instanceof Class)) {
            // Don't validate if we don't know the class we're expecting
            return;
        }

        if (injectedClass.isPrimitive() || injectedClass.isArray()) {
            // Don't validate if we're expecting an array or primitive
            return;
        }

        // Loop through the contents of the parent context, looking for the suffix
        // so that we can validate the class type without actually getting the object back
        // since this sometimes causes problems.
        NamingEnumeration<NameClassPair> contents = initialContext.list(prefix);
        while (contents.hasMore()) {
            NameClassPair pair = contents.next();
            if (name.equals(pair.getName())) {
                String className = pair.getClassName();
                Class<?> jndiClass = null;
                if ("javax.resource.cci.ConnectionFactory".equals(className)) {
                    try {
                        Object o = initialContext.lookup(lookupName);
                        if (o != null) {
                            jndiClass = o.getClass();
                        }
                    } catch (RuntimeException e) {
                        // An error occurred while getting the object from JNDI. This may happen
                        // at this early point in the initialisation process, but if so we just
                        // skip validation.
                    }
                } else {
                    try {
                        jndiClass = cdiArchive.getClassLoader().loadClass(className);
                    } catch (ClassNotFoundException ex) {
                        // Couldn't load the jndiClass name, can't validate
                    }
                }
                if (jndiClass != null && !injectedClass.isAssignableFrom(jndiClass)) {
                    EEValidationUtils.throwDefinitionException(declaringClass, annotated, jndiClass);
                } else {
                    // We found the class and it matched the type, all is well
                    return;
                }
            }
        }
        // We didn't find the class, we can't validate it
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateJndiLookup", new Object[] { Util.identity(annotated) });
        }
    }

    /**
     * Does additional validation on the WebServiceRef annotation. Note that some validation has already been done at this point by WebServiceRefProcessor.
     * <p>
     * If we are injecting into a Service type, we check that the value() attribute is compatible with the type.
     * <p>
     * If we are injecting into a non-service type, we find the port types of the service type specified by the value() attribute and check that one of them is compatible.
     *
     * @param wsRef the WebServiceRef annotation
     * @param declaringClass class containing this WebServiceRef
     * @param Annotated the member annotated with this WebServiceRef
     * @param the injection point
     */
    public static void validateWebServiceRef(WebServiceRef wsRef, Class<?> declaringClass, Annotated annotated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateWebServiceRef", new Object[] { wsRef, declaringClass, Util.identity(annotated) });
        }
        Class<?> ipClass = getInjectedClass(annotated);
        /*
         * note: Thorough WebService validation is performed later, by
         * com.ibm.ws.jaxws.client.injection.WebServiceRefProcessor.validateAndSetMemberLevelWebServiceRef()
         * This method performs a limited subset of that validation, in order to get the CDI CTS test to pass.
         * It would perhaps be better for us to delegate to WebServiceRefProcessor.validateAndSetMemberLevelWebServiceRef().
         */
        if (!wsRef.lookup().isEmpty()) {
            // If there's a lookup specified, don't validate
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Validating WebServiceRef injection point", wsRef);
        }

        Class<?> serviceClass = wsRef.value();

        // The injected type is determined by the field type and the attribute type parameter
        // It has already been validated that they are compatible, we want whichever is the subclass of the other
        Class<?> effectiveClass = ipClass;
        if (ipClass.isAssignableFrom(wsRef.type())) {
            effectiveClass = wsRef.type();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Found service type and effective injection types", serviceClass, effectiveClass);
        }

        if (Service.class.isAssignableFrom(effectiveClass)) {
            if (effectiveClass.getName().equals(Service.class.getName())) {
                if (!Service.class.isAssignableFrom(serviceClass) || serviceClass.getName().equals(Service.class.getName())) {
                    throwDefinitionException(declaringClass, annotated, serviceClass);
                }
            } else {
                if (!serviceClass.getName().equals(effectiveClass.getName()) && !serviceClass.getName().equals(Service.class.getName())) {

                    // We're injecting a service object, field should match service class
                    //if (!effectiveClass.isAssignableFrom(serviceClass)) {
                    throwDefinitionException(declaringClass, annotated, serviceClass);
                }
            }
        } else {
            // We're injecting a port type
            // Enumerate the port types
            Set<Class<?>> portTypes = new HashSet<Class<?>>();
            for (Method method : serviceClass.getMethods()) {
                if (method.getAnnotation(WebEndpoint.class) != null) {
                    portTypes.add(method.getReturnType());
                }
            }

            // Check that the effective class matches one of the port types
            if (!portTypes.isEmpty()) {
                for (Class<?> endpointType : portTypes) {
                    if (effectiveClass.isAssignableFrom(endpointType)) {
                        // There is an endpoint type matching the injection point type
                        return;
                    }
                }
                // There were endpoint types but none of them matched the injection point type
                throwDefinitionException(declaringClass, annotated, serviceClass);
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateWebServiceRef");
        }
    }

    public static void validateResource(Resource res, Class<?> declaringClass, Annotated annotated, CDIArchive cdiArchive) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateResource", new Object[] { res, declaringClass, Util.identity(annotated), cdiArchive });
        }
        // If the injection point is a resource producer, we need to validate it
        if (EEValidationUtils.isProducer(annotated)) {
            if (!res.lookup().isEmpty()) {
                validateJndiLookup(res.lookup(), annotated, declaringClass, cdiArchive);
            } else if (!res.name().isEmpty()) {
                validateJndiLookup("java:comp/env/" + res.name(), annotated, declaringClass, cdiArchive);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateResource");
        }
    }

    public static void validatePersistenceContext(PersistenceContext persistenceContext, Class<?> declaringClass, Annotated annotated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validatePersistenceContext", new Object[] { persistenceContext, declaringClass, Util.identity(annotated) });
        }
        if (EEValidationUtils.isProducer(annotated)) {
            if (getInjectedClass(annotated) instanceof Class<?>) {
                Class<?> ipClass = getInjectedClass(annotated);
                if (!ipClass.isAssignableFrom(EntityManager.class)) {
                    EEValidationUtils.throwDefinitionException(declaringClass, annotated, EntityManager.class);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validatePersistenceContext");
        }
    }

    public static void validatePersistenceUnit(PersistenceUnit PersistenceUnit, Class<?> declaringClass, Annotated annotated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "validatePersistenceUnit", new Object[] { PersistenceUnit, declaringClass, Util.identity(annotated) });
        }
        if (EEValidationUtils.isProducer(annotated)) {
            if (getInjectedClass(annotated) instanceof Class<?>) {
                Class<?> ipClass = getInjectedClass(annotated);
                if (!ipClass.isAssignableFrom(EntityManagerFactory.class)) {
                    EEValidationUtils.throwDefinitionException(declaringClass, annotated, EntityManagerFactory.class);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "validatePersistenceUnit");
        }
    }

}
