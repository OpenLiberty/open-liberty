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
package com.ibm.ws.microprofile.appConfig.converters.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestConverterSubclass implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        b.withConverters(new MakeSubclassConverter<>());
        MySource source = new MySource("key1", "value1");
        b.withSources(source);
        Config config = b.build();
        Child child = config.getValue("key1", Child.class);
        if ("value1".equals(child.getValue())) {
            return "PASSED";
        } else {
            return "FAILED" + child.getValue();
        }
    }
}
