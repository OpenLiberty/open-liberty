/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.stress.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 *
 */
public class TestManyConfigSources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        // Add 500 sources each with 3 parms overriding and overridden
        int size = 500;
        for (int i = 0; i < size; i++) {
            MySource s = new MySource();
            s.setOrdinal(1000 + i);
            s.put("p" + i, "s" + i);
            s.put("p" + (i - 1), "s-1" + (i));
            s.put("p" + (i + 1), "s+1" + (i));
            s.put("" + i + "p", "v" + i);
            b.withSources(s);
        }

        Config c = b.build();

        String msg = "ok";
        boolean passed = true;

        // most values should come from the '-1' clause above.
        for (int i = 1; i < size - 1; i++) {
            if (!c.getValue("" + i + "p", String.class).equals("v" + i) ||
                !c.getValue("p" + i, String.class).equals("s-1" + Integer.valueOf(i + 1))) {
                passed = false;
                msg = "Failed for i=" + i;
                break;
            }
        }

        if (passed) {
            return "PASSED";
        } else {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: " + msg);
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            return result.toString();
        }
    }
}
