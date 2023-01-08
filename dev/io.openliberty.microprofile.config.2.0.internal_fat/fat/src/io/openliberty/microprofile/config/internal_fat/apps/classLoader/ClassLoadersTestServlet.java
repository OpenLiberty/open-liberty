/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.classLoader;

import java.util.ServiceConfigurationError;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.config.internal_fat.apps.TestUtils;

@SuppressWarnings("serial")
@WebServlet("/")
public class ClassLoadersTestServlet extends FATServlet {

    @Test
    public void testUserLoaderErrorsConfig() throws Exception {
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.addDiscoveredConverters();

        ClassLoader cl = new CustomClassLoaderError();
        b.forClassLoader(cl);
        try {
            b.build();
            TestUtils.fail("Exception not thrown");
        } catch (ServiceConfigurationError e) {
            //expected
            TestUtils.assertEquals("org.eclipse.microprofile.config.spi.Converter: Error locating configuration files", e.getMessage());
        }
    }
}