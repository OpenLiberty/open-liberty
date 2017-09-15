/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;

/**
 * Information about all the EJBs in a module.
 *
 * How to obtain an instance depends on the runtime environment:
 *
 * On Liberty, an instance can be obtained
 * via {@link com.ibm.wsspi.adaptable.module.Container#adapt} obtained
 * via {@link com.ibm.ws.container.service.app.deploy.ModuleInfo#getContainer},
 * and it should not be obtained after
 * the {@link com.ibm.ws.container.service.state.ModuleStateListener#moduleStarting} event.
 *
 * On traditional WAS, an instance can be obtained
 * via {@link com.ibm.ws.runtime.service.EJBContainer#getEJBEndpoints}.
 */
public interface EJBEndpoints {
    /**
     * Returns the module version.
     */
    int getModuleVersion();

    /**
     * Return the list of interceptor classes for the EJBs in the module as a read-only set.
     */
    Set<String> getEJBInterceptorClassNames();

    /**
     * Return the list of EJBs in the module as a read-only list.
     */
    List<EJBEndpoint> getEJBEndpoints();

    /**
     * Find the EJBEndpoint in the module that is identified by the specified
     * annotation (@EJB) and injection type. <p>
     *
     * A ClassCastException is thrown if the annotation identifies an endpoint in
     * this module, but the injection type is not compatible. <p>
     *
     * Null is returned if the annotation does not match an endpoint in the module.
     *
     * @param annotation EJB annotation identifying an EJB to inject.
     * @param injectionType target type of the injection.
     * @param application name of the application containing the annotation
     * @param module name of the module containing the annotation
     *
     * @return the matching EJBEndpoint, null if not found, or ClassCastException if
     *         found but not compatible.
     *
     * @throws ClassNotFoundException if the annotation specifies an EJB in this
     *             module but the EJB could not be found.
     * @throws ClassCastException if the EJB is located in this module but does not
     *             support the injection type.
     */
    EJBEndpoint findEJBEndpoint(EJB annotation, Class<?> injectionType, String application, String module) throws ClassNotFoundException, ClassCastException;
}
