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
package com.ibm.ws.microprofile.appConfig.converters.test;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestConverters implements AppConfigTestApp {
    /** {@inheritDoc} */

    @Override
    public String runTest(HttpServletRequest request) {

        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();

        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        MySource s1 = new MySource();

        s1.put("p1", "setting1=customV1,setting2=customV2");
        s1.put("p2", "value1=customV3,value2=customV4");
        s1.put("p3", "attr1=customV5,attr2=customV6");

        b.withSources(s1);
        b.addDefaultSources();

        Converter<CustomPropertyObject1> c1 = new MyConverter();
        Converter<CustomPropertyObject1> c2 = new MyConverter();
        Converter<CustomPropertyObject1> c3 = new MyConverter();
        //TODO converters added while string constructors are disabled
        Converter<CustomPropertyObject2> c4 = new MyConverter2();
        Converter<CustomPropertyObject3> c5 = new MyConverter3();

        b.withConverters(c1, c2);
        b.withConverters(c3);
        b.withConverters(c4, c5);

        Config config = b.build();

        try {
            CustomPropertyObject1 other1 = new CustomPropertyObject1("customV1", "customV2");
            TestUtils.assertContains(config, "p1", other1);
            CustomPropertyObject2 other2 = new CustomPropertyObject2("customV3", "customV4");
            TestUtils.assertContains(config, "p2", other2);
            CustomPropertyObject3 other3 = new CustomPropertyObject3("customV5", "customV6");
            TestUtils.assertContains(config, "p3", other3);

        } catch (Throwable t) {
            TestUtils.fail(t);
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
        return "PASSED";
    }
}