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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Servlet deployment descriptor parse tests.
 */
public class WebAppTest extends WebAppTestBase {

    // Servlet 3.0 cases ...

    @Test
    public void testEE6Web30() throws Exception {
        parse(webApp30() + webAppTail());
    }

    @Test
    public void testEE6Web30EnvEntryValueWhitespace() throws Exception {
        WebApp webApp = parse(webApp30() +
                              "<env-entry>" +
                              "<env-entry-name> envName </env-entry-name>" +
                              "<env-entry-value> envValue </env-entry-value>" +
                              "</env-entry>" +
                              webAppTail());

        List<EnvEntry> envEntries = webApp.getEnvEntries();
        Assert.assertNotNull(envEntries);
        Assert.assertEquals(envEntries.size(), 1);
        EnvEntry envEntry = envEntries.get(0);
        Assert.assertNotNull(envEntry);
        Assert.assertNotNull(envEntry.getName());
        Assert.assertEquals(envEntry.getName(), "envName");
        Assert.assertNotNull(envEntry.getValue());
        Assert.assertEquals(envEntry.getValue(), " envValue ");
    }

    @Test
    public void testEE6Web30AbsoluteOrderingElement() throws Exception {
        parse(webApp30() +
              "<absolute-ordering>" +
              "<name>Fragment1</name>" +
              "<name>Fragment2</name>" +
              "</absolute-ordering>" +
              webAppTail());
    }

    // The prohibition against having more than one absolute ordering element
    // was added in JavaEE7.
    @Test
    public void testEE6Web30AbsoluteOrderingDuplicateElements() throws Exception {
        parse(webApp30() +
              "<absolute-ordering>" +
              "<name>Fragment1</name>" +
              "<name>Fragment2</name>" +
              "</absolute-ordering>" +
              "<absolute-ordering>" +
              "</absolute-ordering>" +
              webAppTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web30DenyUncoveredHttpMethods() throws Exception {
        parse(webApp30() +
              "<deny-uncovered-http-methods/>" +
              webAppTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web31() throws Exception {
        parse(webApp31() + webAppTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6Web40() throws Exception {
        parse(webApp40() + webAppTail());
    }

    // Servlet 3.1 cases ...

    // Parse 3.0 and 3.1.  Do not parse 4.0.

    @Test()
    public void testEE7Web30() throws Exception {
        parse(webApp30() + webAppTail(), WebApp.VERSION_3_1);
    }

    @Test()
    public void testEE7Web31() throws Exception {
        parse(webApp31() + webAppTail(), WebApp.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web31AbsoluteOrderingDuplicates() throws Exception {
        parse(webApp31() +
              "<absolute-ordering>" +
              "<name>Fragment1</name>" +
              "<name>Fragment2</name>" +
              "</absolute-ordering>" +
              "<absolute-ordering>" +
              "</absolute-ordering>" +
              webAppTail(),
              WebApp.VERSION_3_1);
    }

    @Test
    public void testEE7Web31DenyUncoveredHttpMethods() throws Exception {
        parse(webApp31() +
              "<deny-uncovered-http-methods/>" +
              webAppTail(),
              WebApp.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web31DenyUncoveredHttpMethodsNotEmptyType() throws Exception {
        parse(webApp31() +
              "<deny-uncovered-http-methods>junk</deny-uncovered-http-methods>" +
              webAppTail(),
              WebApp.VERSION_3_1);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7Web40() throws Exception {
        parse(webApp40() + webAppTail(), WebApp.VERSION_3_1);
    }

    // Servlet 4.0 cases ...

    // Parse everything.

    @Test
    public void testEE8Web30() throws Exception {
        parse(webApp30() + webAppTail(), WebApp.VERSION_4_0);
    }

    @Test
    public void testEE8Web31() throws Exception {
        parse(webApp31() + webAppTail(), WebApp.VERSION_4_0);
    }

    @Test
    public void testEE8Web40() throws Exception {
        parse(webApp40() + webAppTail(), WebApp.VERSION_4_0);
    }
}
