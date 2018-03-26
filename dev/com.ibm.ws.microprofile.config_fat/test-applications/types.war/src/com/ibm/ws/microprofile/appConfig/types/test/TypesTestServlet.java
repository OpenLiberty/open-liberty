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
package com.ibm.ws.microprofile.appConfig.types.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class TypesTestServlet extends FATServlet {
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";

    /** Tests that a user can retrieve properties of type boolean */
    @Test
    public void testBooleanTypes() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        MySource s = new MySource();
        s.put("p1", "true");
        s.put("p2", "false");
        s.put("p3", "TRUE");
        s.put("p4", "FALSE");

        b.withSources(s);

        Config c = b.build();
        Boolean v1 = c.getValue("p1", Boolean.class);
        Boolean v2 = c.getValue("p2", Boolean.class);
        Boolean v3 = c.getValue("p3", Boolean.class);
        Boolean v4 = c.getValue("p4", Boolean.class);

        assertTrue(v1);
        assertFalse(v2);
        assertTrue(v3);
        assertFalse(v4);
    }

    /** Tests that a user can retrieve properties of type Integer */
    @Test
    public void testIntegerTypes() throws Exception {
        System.setProperty(DYNAMIC_REFRESH_INTERVAL_PROP_NAME, "" + 0);
        ConfigBuilder b = ConfigProviderResolver.instance().getBuilder();
        MySource s = new MySource().put("p1", "3");
        b.withSources(s);

        Config c = b.build();
        Integer v1 = c.getValue("p1", Integer.class);

        assertEquals(new Integer(3), v1);
    }
}