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
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.TestUtils;

public class ConfigSourceServiceLoaderTest {

    //Service Loader is used when default Config Sources are added
    @Test
    public void testConfigSourceServiceLoader() {
        Config configA = null;
        try {
            configA = ConfigProvider.getConfig();
            Iterable<String> keys = configA.getPropertyNames();
            TestUtils.assertContains(keys, "SLKey1");
            TestUtils.assertContains(keys, "SLKey2");
            TestUtils.assertContains(keys, "SLKey3");
            TestUtils.assertContains(keys, "SLKey4");
        } finally {
            if (configA != null) {
                ConfigProviderResolver.instance().releaseConfig(configA);
            }
        }
    }

}
