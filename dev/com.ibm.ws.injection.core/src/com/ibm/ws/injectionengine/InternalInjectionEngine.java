/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

/**
 * Extends the base InjectionEngine interface with methods that are intended
 * for use internally to the reference processing / injection framework. <p>
 *
 * This interface enables server type specific implementations of internal
 * InjectionEngine methods. <p>
 */
public interface InternalInjectionEngine extends InjectionEngine
{
    /**
     * Returns true if this injection engine is running in the embeddable EJB
     * container.
     */
    boolean isEmbeddable();

    /**
     * Checks whether validation messages should be logged or not. <p>
     *
     * @param appCustomPropertySetting value of the current application custom property
     */
    boolean isValidationLoggable(boolean appCustomPropertySetting);

    /**
     * Checks whether more significant validation messages should
     * result in a failure or not. <p>
     *
     * @param appCustomPropertySetting value of the current application custom property
     */
    boolean isValidationFailable(boolean appCustomPropertySetting);

    /**
     * Returns the list of override factories for the specified class.
     *
     * @param klass the class
     */
    <A extends Annotation> OverrideReferenceFactory<A>[] getOverrideReferenceFactories(Class<A> klass);

    /**
     * Invokes all registered {@link InjectionMetaDataListener}s.
     */
    void notifyInjectionMetaDataListeners(ReferenceContext referenceContext,
                                          ComponentNameSpaceConfiguration compNSConfig)
                    throws InjectionException;

    /**
     * Creates a new ResourceRefConfigList.
     */
    ResourceRefConfigList createResourceRefConfigList();

    /**
     * Returns a ResourceRefConfig with default values. The object should be
     * considered read-only.
     */
    ResourceRefConfig getDefaultResourceRefConfig();

    /**
     * Creates a reference to a resource definition. If a binding name is
     * specified, an indirect reference should be created. If a resource is
     * created, the Reference should be added to the InjectionScopeData.
     *
     * @param nameSpaceConfig is the name space config
     * @param scope the namespace scope, or null for java:comp/env
     * @param jndiName the JNDI name
     * @param bindingName the binding name
     * @param type the definition type
     * @param properties the properties
     * @return the Reference
     */
    Reference createDefinitionReference(ComponentNameSpaceConfiguration nameSpaceConfig,
                                        InjectionScope scope,
                                        String refName,
                                        String bindingName,
                                        String type,
                                        Map<String, Object> properties) throws Exception;

    /**
     * Destroys a reference created by {@link #createDefinitionReference}.
     *
     * @param reference the data source reference
     */
    void destroyDefinitionReference(Reference reference)
                    throws Exception;

    /**
     * Rebinds the object with the specified name to the name space such that
     * all intermediate subcontexts in the name are created if they do not already
     * exist. If any non-leaf component of the name identifies a bound object which
     * is not a context, a NotContextException is thrown. If no object is already
     * bound with the specified name, the object is bound and no exception is thrown.
     *
     * @param compNSConfig The component namespace configuration.
     * @param scope The java namespace scope, or null for java:comp/env.
     * @param name Name of the object to rebind.
     * @param binding The InjectionBinding for this name, or null if there is no
     *            corresponding InjectionBinding.
     * @param obj The object to rebind.
     * @throws InjectionException An exception was encountered.
     */
    void bindJavaNameSpaceObject(ComponentNameSpaceConfiguration compNSConfig,
                                 InjectionScope scope,
                                 String name,
                                 InjectionBinding<?> binding,
                                 Object obj)
                    throws InjectionException;

    /**
     * Obtains an injectable object from an InjectionBinding. <p>
     *
     * Intended use is to support java:global/:app/:module bindings; which need
     * to perform a full lookup since the attributes provided by the current
     * component might not necessarily be fully merged. The result cannot be
     * cached since it might change from injection to injection. <p>
     *
     * @param binding the originating binding
     * @param targetObject the object being injected into
     * @param targetContext the context for the target object
     */
    Object getInjectableObject(InjectionBinding<?> binding,
                               Object targetObject,
                               InjectionTargetContext targetContext)
                    throws InjectionException;

    /**
     * Creates a Java name space instance for an ISOLATED, FEDERATED,
     * or SERVER_DEPLOYED client appropriate for the server type
     * (i.e traditional, embeddable, or composable). <p>
     *
     * The result of this operation should be used as input to {@link #createComponentNameSpaceContext(Object)}. <p>
     *
     * @param logicalAppName the logical application name (or logical module
     *            name for stand alone modules.
     * @param moduleName the module name (include extension)
     * @param logicalModuleName the logical module name
     * @param componentName the component name (for improved trace)
     */
    // F46994.3
    Object createJavaNameSpace(String logicalAppName,
                               String moduleName,
                               String logicalModuleName,
                               String componentName) throws NamingException;

    /**
     * Creates and initializes the component name space Context instance. <p>
     *
     * @param componentNameSpace the result of {@link #createJavaNameSpace}.
     */
    // F46994.3
    Context createComponentNameSpaceContext(Object componentNameSpace) throws NamingException;

    /**
     * Creates an InjectionObjectFactory for the specified reference.
     *
     * @param objectFactoryClassName the object factory class name
     * @param objectFactoryClass the object factory class, or null if the
     *            objectFactoryClassName has been specified
     * @return the object factory
     */
    // F54050
    ObjectFactory getObjectFactory(String objectFactoryClassName,
                                   Class<? extends ObjectFactory> objectFactoryClass)
                    throws InjectionException;
}
