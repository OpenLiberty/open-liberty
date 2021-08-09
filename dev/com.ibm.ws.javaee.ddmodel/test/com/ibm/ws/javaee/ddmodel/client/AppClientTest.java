/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import org.junit.Test;

import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Application deployment descriptor parse tests.
 */
public class AppClientTest extends AppClientTestBase {

    // JavaEE6 cases ...

    // Parse everything except 7.0 and 8.0.

    @Test
    public void testEE6AppClient12() throws Exception {
        parse(appClient12() + appClientTail());
    }

    @Test
    public void testEE6AppClient13() throws Exception {
        parse(appClient13() + appClientTail());
    }

    @Test
    public void testEE6AppClient14() throws Exception {
        parse(appClient14() + appClientTail());
    }

    @Test
    public void testEE6AppClient50() throws Exception {
        parse(appClient50() + appClientTail());
    }

    @Test
    public void testEE6AppClient60() throws Exception {
        parse(appClient60() + appClientTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6AppClient70() throws Exception {
        parse(appClient70() + appClientTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6AppClient80() throws Exception {
        parse(appClient80() + appClientTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6AppClient90() throws Exception {
        parse(appClient90() + appClientTail());
    }

    // JavaEE7 cases ...

    // Parse everything except 8.0.

    @Test
    public void testEE7AppClient12() throws Exception {
        parse(appClient12() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test
    public void testEE7AppClient13() throws Exception {
        parse(appClient13() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test
    public void testEE7AppClient14() throws Exception {
        parse(appClient14() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test
    public void testEE7AppClient50() throws Exception {
        parse(appClient50() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test
    public void testEE7AppClient60() throws Exception {
        parse(appClient60() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test
    public void testEE7AppClient70() throws Exception {
        parse(appClient70() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7AppClient80() throws Exception {
        parse(appClient80() + appClientTail(), ApplicationClient.VERSION_7);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7AppClient90() throws Exception {
        parse(appClient90() + appClientTail(), ApplicationClient.VERSION_7);
    }

    // JavaEE8 cases ...

    // Parse everything.

    @Test
    public void testEE8AppClient12() throws Exception {
        parse(appClient12() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient13() throws Exception {
        parse(appClient13() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient14() throws Exception {
        parse(appClient14() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient50() throws Exception {
        parse(appClient50() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient60() throws Exception {
        parse(appClient60() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient70() throws Exception {
        parse(appClient70() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test
    public void testEE8AppClient80() throws Exception {
        parse(appClient80() + appClientTail(), ApplicationClient.VERSION_8);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE8AppClient90() throws Exception {
        parse(appClient90() + appClientTail(), ApplicationClient.VERSION_8);
    }
}
