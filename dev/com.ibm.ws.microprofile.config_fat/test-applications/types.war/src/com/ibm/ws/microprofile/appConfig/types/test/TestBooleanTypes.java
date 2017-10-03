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
package com.ibm.ws.microprofile.appConfig.types.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 *
 */
public class TestBooleanTypes implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        MySource s = new MySource();
        s.put("p1", "true");
        s.put("p2", "false");
        s.put("p3", "TRUE");
        s.put("p4", "FALSE");

        b.withSources(s);

        Boolean missing = new Boolean("true");
        Config c = b.build();
        Boolean v1 = c.getValue("p1", Boolean.class);
        Boolean v2 = c.getValue("p2", Boolean.class);
        Boolean v3 = c.getValue("p3", Boolean.class);
        Boolean v4 = c.getValue("p4", Boolean.class);

        if (v1 && !v2 && v3 && !v4) {
            return "PASSED";
        } else {
            return "FAILED";
        }
    }

}
