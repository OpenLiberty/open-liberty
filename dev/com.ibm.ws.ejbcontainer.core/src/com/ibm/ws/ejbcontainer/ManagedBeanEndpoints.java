/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
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

/**
 * Information about all the managed beans in a module.
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
 * via {@link com.ibm.ws.runtime.service.EJBContainer#getManagedBeanEndpoints}.
 */
public interface ManagedBeanEndpoints {
    /**
     * Returns the module version.
     */
    int getModuleVersion();

    /**
     * Return the list of interceptor classes for the managed beans in the module.
     */
    Set<String> getManagedBeanInterceptorClassNames();

    /**
     * Return the list of managed beans in the module.
     */
    List<ManagedBeanEndpoint> getManagedBeanEndpoints();
}
