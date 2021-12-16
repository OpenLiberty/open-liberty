/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import org.junit.Test;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Web fragment descriptor parsing unit tests.
 */
public class WebFragmentTest extends WebFragmentTestBase {

    // Servlet 3.0 cases ...

    // Parse 3.0.  Do not parse 3.1 or 4.0.

    @Test
    public void testEE6WebFragment30() throws Exception {
        parse(webFragment30() + webFragmentTail());
    }

    @Test
    public void testEE6WebFragment30OrderingElement() throws Exception {
        parse(webFragment30() +
              "<ordering>" +
              "</ordering>" +
              webFragmentTail());
    }

    // The prohibition against having more than one ordering element
    // was added in JavaEE7.
    @Test
    public void testEE6WebFragment30OrderingDuplicates() throws Exception {
        parse(webFragment30() +
              "<ordering>" +
              "</ordering>" +
              "<ordering>" +
              "</ordering>" +
              webFragmentTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6WebFragment31() throws Exception {
        parse(webFragment31() + webFragmentTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6WebFragment40() throws Exception {
        parse(webFragment40() + webFragmentTail());
    }

    // Servlet 3.1 cases ...

    // Parse 3.0 and 3.1.  Do not parse 4.0.

    @Test()
    public void testEE7WebFragment30() throws Exception {
        parse(webFragment30() + webFragmentTail(), WebApp.VERSION_3_1);
    }

    @Test()
    public void testEE7WebFragment31() throws Exception {
        parse(webFragment31() + webFragmentTail(), WebApp.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7WebFragment31OrderingDuplicates() throws Exception {
        parse(webFragment31() +
              "<ordering>" +
              "</ordering>" +
              "<ordering>" +
              "</ordering>" +
              webFragmentTail(), WebApp.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7WebFragment40() throws Exception {
        parse(webFragment40() + webFragmentTail(), WebApp.VERSION_3_1);
    }

    // Servlet 4.0 cases ...

    // Parse everything.

    @Test()
    public void testEE8WebFragment30() throws Exception {
        parse(webFragment30() + webFragmentTail(), WebApp.VERSION_4_0);
    }

    @Test()
    public void testEE8WebFragment31() throws Exception {
        parse(webFragment31() + webFragmentTail(), WebApp.VERSION_4_0);
    }

    @Test()
    public void testEE8WebFragment40() throws Exception {
        parse(webFragment40() + webFragmentTail(), WebApp.VERSION_4_0);
    }

    // TODO: Other specific servlet 4.0 web fragment cases ...
}
