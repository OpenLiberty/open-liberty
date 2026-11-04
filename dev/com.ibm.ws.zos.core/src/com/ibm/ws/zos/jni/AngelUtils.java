/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni;

import java.util.Set;

/**
 * This service provides Angel related utilities. Such as verifying Service registration for native associated
 * functions.
 *
 */
public interface AngelUtils {

    /**
     * Check that each service name in the input list is an Server service.
     *
     * @param services Set of Server services.
     * @return true if all supplied services are Server services. Otherwise, false.
     */
    public boolean areServicesAvailable(Set<String> services);

    /**
     * Return a Set of all native registered services.
     *
     * @return Set of all available services
     */
    public Set<String> getAvailableServices();
}
