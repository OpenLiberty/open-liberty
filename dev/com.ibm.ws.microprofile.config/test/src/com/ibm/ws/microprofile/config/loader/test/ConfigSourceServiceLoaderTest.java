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
package com.ibm.ws.microprofile.config.loader.test;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;
import com.ibm.ws.microprofile.test.AbstractConfigTest;

public class ConfigSourceServiceLoaderTest extends AbstractConfigTest {

    //Service Loader is used when default Config Sources are added
    @Test
    public void testConfigSourceServiceLoader() {
        Config configA = null;
        try {
            configA = ConfigProviderResolver.instance().getConfig();
            TestUtils.assertContainsKey(configA, "SLKey1");
            TestUtils.assertContainsKey(configA, "SLKey2");
            TestUtils.assertContainsKey(configA, "SLKey3");
            TestUtils.assertContainsKey(configA, "SLKey4");
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
        }
    }

}
