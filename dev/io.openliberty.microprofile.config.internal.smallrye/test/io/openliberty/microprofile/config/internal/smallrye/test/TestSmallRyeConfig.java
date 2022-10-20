/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.smallrye.test;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import io.openliberty.microprofile.config.internal.smallrye.OLSmallRyeConfigProviderResolver;

/**
 *
 */
public class TestSmallRyeConfig {

    //a really simple test that just creates a config and gets everything from it
    @Test
    public void testConfig() {
        ConfigProviderResolver.setInstance(new OLSmallRyeConfigProviderResolver());
        Config config = ConfigProvider.getConfig();
        Iterable<String> names = config.getPropertyNames();
        for (String name : names) {
            System.out.println(name + "=" + config.getOptionalValue(name, String.class).orElse("NULL"));
        }
    }

}
