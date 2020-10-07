/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.classLoaders.test;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class ClassLoadersTestServlet extends FATServlet {

    /**
     *
     * @throws Exception
     */
    @Test
    public void testMultiUrlResources() throws Exception {
        // To test this we simply check that multiple
        // microprofile-config.properties are being
        // accessed.
        Config c = ConfigProvider.getConfig();

        boolean passed = true;

        boolean metainf = c.getValue("jar", String.class).equals("jarset");
        boolean webinf = c.getValue("web-inf.classes.meta-inf.property", String.class).equals("wiFound");

        passed = metainf && webinf;

        if (!passed) {
            StringBuffer result = new StringBuffer();
            result.append("FAILED: ");
            Iterable<String> names = c.getPropertyNames();
            for (String name : names) {
                result.append("\n" + name + "=" + c.getValue(name, String.class));
            }
            fail(result.toString());
        }
    }
}