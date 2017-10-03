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

/**
 * server.env and microprofile-config.properties are also about 1K entries.
 */
public class TestMultipleSameTypeConverters implements AppConfigTestApp {
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
        Converter<CustomPropertyObject1> c2 = new MyConverter1_2();
        //TODO converter added while string constructors are disabled
        Converter<CustomPropertyObject2> c3 = new MyConverter2();

        b.withConverters(c1, c2, c3);

        Config c = b.build();

        CustomPropertyObject1 other1 = new CustomPropertyObject1("Optional", "orElse");
        CustomPropertyObject1 cpo1 = c.getOptionalValue("p1", CustomPropertyObject1.class).orElse(other1);

        CustomPropertyObject2 other2 = new CustomPropertyObject2("Optional", "orElse");
        CustomPropertyObject2 cpo2 = c.getOptionalValue("p2", CustomPropertyObject2.class).orElse(other2);

        boolean passed = cpo1.setting1.equals("customV1") && cpo1.setting2.equals("customV2")
                         && cpo2.value1.equals("customV3") && cpo2.value2.equals("customV4");

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
    }
}