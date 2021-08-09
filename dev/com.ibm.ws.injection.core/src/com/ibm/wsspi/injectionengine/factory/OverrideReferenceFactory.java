/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine.factory;

import java.lang.annotation.Annotation;
import javax.naming.Reference;

/**
 * This interface provides a mechanism to override the algorithm used to
 * identify the target of a reference defined for the application
 * component's environment (java:comp/env). <p>
 *
 * Instances of this interface are used to create naming Reference objects
 * with lookup information for component references, which the caller then
 * binds to a JNDI name space. When the object is looked up or injected,
 * the associated ObjectFactory is used to obtain an instance for the
 * component reference. <p>
 *
 * Implementations of this interface must be thread safe. <p>
 *
 * An implementation of this interface may be registered using {@link com.ibm.wsspi.injectionengine.InjectionEngine#registerOverrideReferenceFactory}.
 **/
public interface OverrideReferenceFactory<A extends Annotation>
{
    /**
     * This method creates a naming Reference for the specified Java EE
     * reference that has been declared in either the deployment descriptor
     * (XML) or in the class as an annotation. Null should be returned if the
     * OverrideReferenceFactory implementation does not wish to override the
     * specified reference. <p>
     *
     * The annotation form of the reference is provided, as well as information
     * about the containing component, the name of the reference (as it will be
     * bound into the java:comp/env name space), and the associated data
     * type of the reference. Note that the provided data type will be the most
     * specific type required to satisfy any injection of the reference. <p>
     *
     * @param application name of the application containing the ref.
     * @param module name of the module containing the ref.
     * @param component name of the component containing the ref; may be null.
     * @param refName name of the ref in the java:comp/env name space.
     * @param refType data type of the ref.
     * @param bindingName the global binding name specified, if any.
     * @param annotation the annotation representation of the ref.
     *
     * @return the created naming Reference.
     **/
    public Reference createReference(String application,
                                     String module,
                                     String component,
                                     String refName,
                                     Class<?> refType,
                                     String bindingName,
                                     A annotation);

    /**
     * Returns 'true' if the override reference factory instance would like
     * to be called for references defined in the specified application and
     * module. If 'false' is returned, then the createReference method on
     * this factory will not be called for any of the references defined
     * in the specified application and module. <p>
     *
     * This method allows the injection service to optimize performance by
     * avoiding unnecessary calls to factories that are not applicable to
     * specific applications or modules. <p>
     *
     * The injection service will call this method for every component
     * environment (java:comp name space) that is constructed. For .war
     * modules, there is only one component environment, so this method
     * will only be called once. However, for EJB .jar modules, there
     * is a component environment for every EJB, so this method will be
     * called once per EJB. <p>
     *
     * @param applicationName name of the application.
     * @param moduleName name of the module within the application.
     *
     * @return 'true' if the override reference factory instance would like
     *         to be called for references defined in the specified application
     *         and module; otherwise 'false'.
     **/
    public boolean hasModuleOverride(String applicationName,
                                     String moduleName);

}
