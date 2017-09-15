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
package com.ibm.ws.eba.wab.integrator;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;

/**
 * This is just a marker service used to inform others that the subsystem
 * has been installed as an application and is known by the EbaProvider.
 */
public interface OSGiAppInfo {
    /**
     * A service property that indicates the subsystem service EBA info is for.
     * The value of this property is of type {@link ServiceReference}
     */
    public static final String SERVICE_PROP_FOR_SUBSYSTEM = "com.ibm.ws.http.whiteboard.context.for.subsystem";

    /**
     * The application info for this EBA
     * 
     * @return application info
     */
    public ApplicationInfo getApplicationInfo();
}
