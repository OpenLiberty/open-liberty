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
package com.ibm.ws.microprofile.appConfig.defaultSources.tests.libertyTests;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.test.utils.TestUtils;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/libertyDefaultSourcesTestServlet")
public class LibertyDefaultSourcesTestServlet extends FATServlet {

    /**
     *
     * @throws Exception
     */
    @Test
    public void defaultsGetConfigPathProcEnv() throws Exception {
        Map<String, String> env = new HashMap<>(System.getenv());
        Properties props = System.getProperties();

        //Properties override environment variables.
        for (Map.Entry<?, ?> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            env.put(key, value);
        }

        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDefaultSources();
        Config config = builder.build();

        TestUtils.assertContains(config, env);
    }
}