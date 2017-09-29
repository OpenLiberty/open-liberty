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
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestMultiUrlResources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {
        try {
            // To test this we simply check that multiple
            // microprofile-config.properties are being
            // accessed.
            Config c = ConfigProvider.getConfig();

            boolean passed = true;

            boolean metainf = c.getValue("jar", String.class).equals("jarset");
            boolean webinf = c.getValue("web-inf.classes.meta-inf.property", String.class).equals("wiFound");

            passed = metainf && webinf;

            if (passed) {
                return "PASSED";
            } else {
                StringBuffer result = new StringBuffer();
                result.append("FAILED: ");
                Iterable<String> names = c.getPropertyNames();
                for (String name : names) {
                    result.append("\n" + name + "=" + c.getValue(name, String.class));
                }
                return result.toString();
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String s = sw.toString();
            return "FAILED" + s + t.getMessage() + t.toString();
        }
    }
}