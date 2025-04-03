/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.transport.http_fat;

import java.io.File;
import java.util.logging.Logger;

import componenttest.common.apiservices.Bootstrap;
import componenttest.rules.repeater.EE6FeatureReplacementAction;

/**
 * Custom FeatureReplacementAction for Servlet 3.0.
 * This will repeat the tests with Servlet 3.0 if it is available
 */
public class RepeatWithServlet30 extends EE6FeatureReplacementAction {
    public static final String ID = "SERVLET30_FEATURE";
    private static final Logger LOGGER = Logger.getLogger(RepeatWithServlet30.class.getName());

    @Override
    public boolean isEnabled() {
        LOGGER.info("Checking if Servlet 3.0 feature is available...");

        try {
            Bootstrap b = Bootstrap.getInstance();
            String installRoot = b.getValue("libertyInstallPath");
            LOGGER.info(installRoot);
            File servlet30Feature = new File(installRoot + "/lib/features/com.ibm.websphere.appserver.servlet-3.0.mf");

            boolean exists = servlet30Feature.exists();
            LOGGER.info("Servlet 3.0 feature found: " + exists);
            return exists;
        } catch (Exception e) {
            LOGGER.severe("Error checking for Servlet 3.0: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        return "Set Servlet feature to 3.0 version";
    }

    @Override
    public String getID() {
        return ID;
    }

}
