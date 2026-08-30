/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.registry.saf;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Factory for obtaining the SAFPasswordUtility.
 *
 * @author IBM Corporation
 * @version 1.0
 * @ibm-api
 */
public class SAFPasswordUtilityFactory {

    /**
     * Tracker for standard runtime handling of the SAFPasswordUtility service.
     */
    private static ServiceTracker<SAFPasswordUtility, SAFPasswordUtility> safPUTracker = null;

    /**
     * Returns an instance of the SAFPasswordUtility.
     *
     * @return An instance of SAFPasswordUtility.
     */
    public static SAFPasswordUtility getInstance() {
        if (safPUTracker == null) {
            Bundle bundle = FrameworkUtil.getBundle(SAFPasswordUtility.class);
            if (bundle == null) {
                return null;
            }
            BundleContext bc = bundle.getBundleContext();
            ServiceTracker<SAFPasswordUtility, SAFPasswordUtility> tmp = new ServiceTracker<SAFPasswordUtility, SAFPasswordUtility>(bc, SAFPasswordUtility.class.getName(), null);
            tmp.open();
            safPUTracker = tmp;
        }
        return safPUTracker.getService();
    }
}
