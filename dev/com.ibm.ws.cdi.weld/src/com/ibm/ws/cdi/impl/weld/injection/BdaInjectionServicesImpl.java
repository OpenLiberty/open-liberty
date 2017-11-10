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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceRef;

import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JaxwsInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.EjbEndpointService;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;
import com.ibm.ws.cdi.internal.interfaces.WebSphereInjectionServices;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Implements EjbInjectionServices, JaxwsInjectionServices, JpaInjectionServices and ResourceInjectionServices.
 *
 * All of these handle creating instances in the same way, by delegating to an injection binding from the injection engine.
 *
 * Some methods are not implemented because they are deprecated or just never called anywhere in the Weld code.
 */
public class BdaInjectionServicesImpl implements EjbInjectionServices, JaxwsInjectionServices, JpaInjectionServices, ResourceInjectionServices {

    private static final TraceComponent tc = Tr.register(BdaInjectionServicesImpl.class);

    private final WebSphereInjectionServices webSphereInjectionServices;
    private final EjbEndpointService ejbEndpointService;
    private final CDIArchive archive;

    private final CDIRuntime cdiRuntime;

    public BdaInjectionServicesImpl(WebSphereInjectionServices webSphereInjectionServices,
                                    CDIRuntime cdiRuntime,
                                    WebSphereCDIDeployment cdiDeployment,
                                    CDIArchive archive) {
        this.webSphereInjectionServices = webSphereInjectionServices;
        this.ejbEndpointService = cdiRuntime.getEjbEndpointService();
        this.archive = archive;
        this.cdiRuntime = cdiRuntime;
    }

    /**
     * Returns whether a given injection point is a producer (i.e. annotated with @Produces)
     *
     * @return true if the injection point is a producer, otherwise false
     */
    private static boolean isProducer(InjectionPoint injectionPoint) {
        boolean isProducer = false;

        Produces produces = injectionPoint.getAnnotated().getAnnotation(Produces.class);

        if (produces != null) {
            isProducer = true;
        }

        return isProducer;
    }

    /**
     * Throw a definition exception for a resource producer field where the field type does not match the resource type
     *
     * @param injectionPoint the injection point for the resource producer field
     * @param fieldType the type of the field
     * @throws DefinitionException always
     */
    private static void throwDefinitionException(InjectionPoint injectionPoint, Class<?> fieldType) throws DefinitionException {
        String producerFieldName = injectionPoint.getMember().getDeclaringClass().getName() + "." + injectionPoint.getMember().getName();
        String fieldTypeName = fieldType.getName();
        throw new DefinitionException(Tr.formatMessage(tc, "resource.producer.validation.error.CWOWB1007E", producerFieldName, fieldTypeName));
    }

    @FFDCIgnore(ClassCastException.class)
    private void validateEjb(EJB ejb, InjectionPoint injectionPoint) {
        if (ejbEndpointService != null && injectionPoint.getType() instanceof Class<?>) {
            Class<?> ipClass = (Class<?>) injectionPoint.getType();

            try {
                ejbEndpointService.validateEjbInjection(ejb, archive, ipClass);
            } catch (CDIException e) {
                // We were unable to get the EJB endpoints, quietly skip validation
            } catch (ClassCastException e) {
                // Validation failed, the EJB does not match the field type
                BdaInjectionServicesImpl.throwDefinitionException(injectionPoint, ipClass);
            }
        }

    }

    /**
     * Validates that the object referenced by a JNDI name has the given type
     * <p>
     * Validation is lenient, if we can't find the object for any reason, we won't fail validation.
     *
     * @throws DefinitionException if the object is found and does not have the correct type
     */
    private void validateJndiLookup(String lookupString, InjectionPoint injectionPoint) {

        // We need to set a current component before doing anything to do with JNDI
        try {
            cdiRuntime.beginContext(archive);
            InitialContext c = new InitialContext();

            validateJndiLookup(c, lookupString, injectionPoint);

        } catch (NamingException ex) {
            // Failed to look up the object, just return without failing validation
        } catch (CDIException e) {
            throw new IllegalStateException(e);
        } finally {
            cdiRuntime.endContext();
        }
    }

    private void validateJndiLookup(InitialContext c, String lookupString, InjectionPoint injectionPoint) throws NamingException, CDIException {
        Name lookupName = c.getNameParser("").parse(lookupString);

        // Split the name into the suffix and the parent context
        String name = lookupName.get(lookupName.size() - 1);
        Name prefix = lookupName.getPrefix(lookupName.size() - 1);

        if (!(injectionPoint.getType() instanceof Class)) {
            // Don't validate if we don't know the class we're expecting
            return;
        }

        Class<?> clazz = (Class<?>) injectionPoint.getType();
        if (clazz.isPrimitive() || clazz.isArray()) {
            // Don't validate if we're expecting an array or primitive
            return;
        }

        // Loop through the contents of the parent context, looking for the suffix
        // so that we can validate the class type without actually getting the object back
        // since this sometimes causes problems.
        NamingEnumeration<NameClassPair> contents = c.list(prefix);
        while (contents.hasMore()) {
            NameClassPair pair = contents.next();
            if (name.equals(pair.getName())) {
                try {
                    String className = pair.getClassName();
                    Class<?> jndiClass = archive.getClassLoader().loadClass(className);
                    if ("javax.resource.cci.ConnectionFactory".equals(className)) {
                        try {
                            Object o = c.lookup(lookupName);
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
                            jndiClass = archive.getClassLoader().loadClass(className);
                        } catch (ClassNotFoundException ex) {
                            // Couldn't load the jndiClass name, can't validate
                        }
                    }
                    if (!clazz.isAssignableFrom(jndiClass)) {
                        BdaInjectionServicesImpl.throwDefinitionException(injectionPoint, clazz);
                    } else {
                        // We found the class and it matched the type, all is well
                        return;
                    }
                } catch (ClassNotFoundException ex) {
                    // Couldn't load the jndiClass name, can't validate
                }
            }
        }
        // We didn't find the class, we can't validate it
    }

    /**
     * Does additional validation on the WebServiceRef annotation. Note that some validation has already been done at this point by WebServiceRefProcessor.
     * <p>
     * If we are injecting into a Service type, we check that the value() attribute is compatible with the type.
     * <p>
     * If we are injecting into a non-service type, we find the port types of the service type specified by the value() attribute and check that one of them is compatible.
     *
     * @param wsRef the WebServiceRef annotation
     * @param ipClass the type of the injection point field
     * @param the injection point
     */
    private void validateWebServiceRef(WebServiceRef wsRef, Class<?> ipClass, InjectionPoint injectionPoint) {
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
            /*
             * Original code:
             *
             * // We're injecting a service object, field should match service class
             * if (!effectiveClass.isAssignableFrom(serviceClass)) {
             * throwDefinitionException(injectionPoint, ipClass);
             * }
             *
             * New code, currently under test in Liberty build:
             */
            if (effectiveClass.getName().equals(Service.class.getName())) {
                if (!Service.class.isAssignableFrom(serviceClass) || serviceClass.getName().equals(Service.class.getName())) {
                    throwDefinitionException(injectionPoint, ipClass);
                }
            } else {
                if (!serviceClass.getName().equals(effectiveClass.getName()) && !serviceClass.getName().equals(Service.class.getName())) {

                    // We're injecting a service object, field should match service class
                    //if (!effectiveClass.isAssignableFrom(serviceClass)) {
                    throwDefinitionException(injectionPoint, ipClass);
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
                throwDefinitionException(injectionPoint, ipClass);
            }

        }
    }

    /**
     * Create a ResourceReferenceFactoryImpl for the given injection point.
     * <p>
     * Note that we may occasionally return null if weld asks us for factories for non-bean classes. This should be safe as weld shouldn't actually try to use the factory since
     * it
     * shouldn't be instantiating these classes.
     *
     * @param injectionPoint the injection point
     * @return the ResourceReferenceFactory, or null if we could not find anything to inject for the given injection point
     */
    private <T> ResourceReferenceFactory<T> getResourceReferenceFactory(InjectionPoint injectionPoint) {
        ResourceReferenceFactory<T> resourceReferenceFactory = new ResourceReferenceFactoryImpl<T>(this.webSphereInjectionServices, injectionPoint);

        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReferenceFactory<Object> registerEjbInjectionPoint(InjectionPoint injectionPoint) {
        // If the injection point is a resource producer, we need to validate it
        if (BdaInjectionServicesImpl.isProducer(injectionPoint)) {
            EJB ejb = injectionPoint.getAnnotated().getAnnotation(EJB.class);
            validateEjb(ejb, injectionPoint);
        }

        // Get resource factory for @EJB injection
        ResourceReferenceFactory<Object> resourceReferenceFactory = getResourceReferenceFactory(injectionPoint);
        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Object resolveEjb(InjectionPoint injectionPoint) {
        // Deprecated, never called by Weld
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(InjectionPoint injectionPoint) {
        // If the injection point is a resource producer, we need to validate it
        if (BdaInjectionServicesImpl.isProducer(injectionPoint)) {
            Resource res = injectionPoint.getAnnotated().getAnnotation(Resource.class);
            if (!res.lookup().isEmpty()) {
                validateJndiLookup(res.lookup(), injectionPoint);
            } else if (!res.name().isEmpty()) {
                validateJndiLookup("java:comp/env/" + res.name(), injectionPoint);
            }
        }
        // Get resource factory for @Resource injection
        ResourceReferenceFactory<Object> resourceReferenceFactory = getResourceReferenceFactory(injectionPoint);
        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(String jndiName, String mappedName) {
        // I'm not sure how to implement this, we can't easily match the names to an InjectionBinding.
        // Fortunately, Weld never calls this method so we just won't implement it.
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Object resolveResource(InjectionPoint injectionPoint) {
        // Deprecated, never called by Weld
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Object resolveResource(String jndiName, String mappedName) {
        // Deprecated, never called by Weld
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <T> ResourceReferenceFactory<T> registerWebServiceRefInjectionPoint(InjectionPoint injectionPoint) {
        // If this is a producer injection point, validate that the field type matches the annotation type
        if (BdaInjectionServicesImpl.isProducer(injectionPoint)) {
            if (injectionPoint.getType() instanceof Class<?>) {
                Class<?> ipClass = (Class<?>) injectionPoint.getType();
                WebServiceRef wsRef = injectionPoint.getAnnotated().getAnnotation(WebServiceRef.class);
                validateWebServiceRef(wsRef, ipClass, injectionPoint);
            }
        }
        // Get resource factory for @WebServiceRef injection
        ResourceReferenceFactory<T> resourceReferenceFactory = getResourceReferenceFactory(injectionPoint);
        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(InjectionPoint injectionPoint) {
        if (BdaInjectionServicesImpl.isProducer(injectionPoint)) {
            if (injectionPoint.getType() instanceof Class<?>) {
                Class<?> ipClass = (Class<?>) injectionPoint.getType();
                if (!ipClass.isAssignableFrom(EntityManager.class)) {
                    BdaInjectionServicesImpl.throwDefinitionException(injectionPoint, ipClass);
                }
            }
        }
        // Get resource factory for @PersistenceContext injection
        ResourceReferenceFactory<EntityManager> resourceReferenceFactory = getResourceReferenceFactory(injectionPoint);
        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(InjectionPoint injectionPoint) {
        if (BdaInjectionServicesImpl.isProducer(injectionPoint)) {
            if (injectionPoint.getType() instanceof Class<?>) {
                Class<?> ipClass = (Class<?>) injectionPoint.getType();
                if (!ipClass.isAssignableFrom(EntityManagerFactory.class)) {
                    BdaInjectionServicesImpl.throwDefinitionException(injectionPoint, ipClass);
                }
            }
        }
        ResourceReferenceFactory<EntityManagerFactory> resourceReferenceFactory = getResourceReferenceFactory(injectionPoint);
        return resourceReferenceFactory;
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
        // Deprecated, never called by Weld
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
        // Deprecated, never called by Weld
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void cleanup() {
        // no-op
    }

}