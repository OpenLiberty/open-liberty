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
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestConverterExceptions implements AppConfigTestApp {
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

        Converter<CustomPropertyObject1> c1 = new MyBadConverter();

        b.withConverters(c1);

        Config c = b.build();

        try {
            @SuppressWarnings("unused")
            CustomPropertyObject1 p1 = c.getValue("p1", CustomPropertyObject1.class);
            return "FAILED: IllegalArgumentException not thrown";
        } catch (IllegalArgumentException e) {
            TestUtils.assertEquals("Converter throwing intentional exception", e.getMessage());
        }

        return "PASSED";
    }
}