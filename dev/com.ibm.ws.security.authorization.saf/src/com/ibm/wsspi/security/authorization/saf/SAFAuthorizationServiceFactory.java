/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.security.authorization.saf;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Factory for obtaining the SAFAuthorizationService.
 *
 * @author IBM Corporation
 * @version 1.0
 * @ibm-spi
 */
public class SAFAuthorizationServiceFactory {

    /**
     * Tracker for standard runtime handling of the SAFAuthorizationService service.
     */
    private static ServiceTracker<SAFAuthorizationService, SAFAuthorizationService> safASTracker = null;

    /**
     * Returns an instance of the SAFAuthorizationService.
     *
     * @return An instance of SAFAuthorizationService.
     */
    public static SAFAuthorizationService getInstance() {
        if (safASTracker == null) {
            Bundle bundle = FrameworkUtil.getBundle(SAFAuthorizationService.class);
            if (bundle == null) {
                return null;
            }
            BundleContext bc = bundle.getBundleContext();
            ServiceTracker<SAFAuthorizationService, SAFAuthorizationService> tmp = new ServiceTracker<SAFAuthorizationService, SAFAuthorizationService>(bc, SAFAuthorizationService.class.getName(), null);
            tmp.open();
            safASTracker = tmp;
        }
        return safASTracker.getService();
    }
}
