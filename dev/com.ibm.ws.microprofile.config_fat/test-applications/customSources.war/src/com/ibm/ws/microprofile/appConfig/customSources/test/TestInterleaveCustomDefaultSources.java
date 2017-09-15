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
package com.ibm.ws.microprofile.appConfig.customSources.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 *
 */
public class TestInterleaveCustomDefaultSources implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

        try {
            System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
            ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

            MySource s1 = new MySource();
            MySource s2 = new MySource();

// server.env has:
//          server_env_property=server.env.value
//          server_env_property_override=server.env.o.value
//          server_env_property_override2=server.env.o.value2

            s1.setOrdinal(1000);
            s1.put("test1", "test1");
            s1.put("server_env_property_override", "server.env.value.overridden");

            s2.setOrdinal(1);
            s2.put("server_env_property_override2", "server.env.value.overriddenNOT");

            b.addDefaultSources();
            b.withSources(s1, s2);

            Config c = b.build();

            if (c.getValue("test1", String.class).equals("test1") &&
                c.getValue("server_env_property", String.class).equals("server.env.value") &&
                c.getValue("server_env_property_override", String.class).equals("server.env.value.overridden") &&
                c.getValue("server_env_property_override2", String.class).equals("server.env.o.value2")) {

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
            return t.getStackTrace().toString();
        }
    }
}
