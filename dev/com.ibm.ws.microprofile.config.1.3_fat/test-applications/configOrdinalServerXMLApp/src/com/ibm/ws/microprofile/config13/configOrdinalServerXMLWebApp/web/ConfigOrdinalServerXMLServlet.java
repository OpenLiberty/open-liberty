/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.configOrdinalServerXMLWebApp.web;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

import com.ibm.ws.microprofile.config13.test.ConfigOrdinalServerXMLTest;

import componenttest.app.FATServlet;

/**
 * See {@link ConfigOrdinalServerXMLTest} for test details
 */
@SuppressWarnings("serial")
@WebServlet("/ConfigOrdinalServerXML")
public class ConfigOrdinalServerXMLServlet extends FATServlet {

    @Inject
    private Config config;

    @Test
    public void testServerXmlConfigOrdinal() {
        assertEquals("config_props", config.getValue("key_config_props", String.class));
        assertEquals("serverxml_vars", config.getValue("key_serverxml_vars", String.class));
        assertEquals("serverxml_default_vars", config.getValue("key_serverxml_default_vars", String.class));
        assertEquals("env_vars", config.getValue("key_env_vars", String.class));
        assertEquals("serverxml_app_props", config.getValue("key_serverxml_app_props", String.class));
        assertEquals("system_props", config.getValue("key_system_props", String.class));
    }

}
