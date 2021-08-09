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
package com.ibm.ws.jaxws.web;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;

/**
 *
 */
public class VirtualHostOSGIService {
    private VirtualHostOSGIService() {

    }

    private static VirtualHostOSGIService instance = null;

    public static VirtualHostOSGIService getInstance() {
        if (instance == null) {
            instance = new VirtualHostOSGIService();
        }
        return instance;
    }

    private DynamicVirtualHostManager _vhostManager;

    public DynamicVirtualHostManager getDynamicVirtualHostManagerService() {
        if (_vhostManager == null) {
            BundleContext context = FrameworkUtil.getBundle(DynamicVirtualHostManager.class)
                            .getBundleContext();
            ServiceReference<DynamicVirtualHostManager> serviceRef = context
                            .getServiceReference(DynamicVirtualHostManager.class);
            _vhostManager = context.getService(serviceRef);
        }
        return _vhostManager;

    }
}
