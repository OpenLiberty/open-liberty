/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * This interface represents the data that is passed to InjectionMetaDataListener
 * instances upon population of the Java namespace for a given component or module.
 */
public interface InjectionMetaData
{
    /**
     * Returns the component namespace configuration passed to {@link InjectionEngine#processInjectionMetaData}.
     */
    ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration(); // F48603

    /**
     * Returns {@link ComponentNameSpaceConfiguration#getJ2EEName}.
     */
    J2EEName getJ2EEName();

    /**
     * Returns {@link ComponentNameSpaceConfiguration#getModuleMetaData}.
     */
    ModuleMetaData getModuleMetaData();

    /**
     * Gets the ReferenceContext instance that contains the <code>InjectionTargets</code>
     * discovered by the reference processing, or null if unavailable.
     *
     * The ReferenceContext must be used to obtain the <code>InjectionTargets</code>
     * for a <code>Class</code> if the reference processing framework was used to
     * drive the injection engine.
     */
    ReferenceContext getReferenceContext(); // d643203

    /**
     * Binds an object into the java:comp context.
     *
     * @param name a name relative to java:comp (e.g., "UserTransaction")
     * @param bindingObject the object to bind
     */
    void bindJavaComp(String name, Object bindingObject) // F48603
    throws InjectionException;
}
