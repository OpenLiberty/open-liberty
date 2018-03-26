/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import org.junit.Test;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.version.JavaEEVersion;

/**
 * Application deployment descriptor parse tests.
 */
public class AppTest extends AppTestBase {

    // JavaEE6 cases ...

    // Parse everything except 7.0 and 8.0.

    @Test
    public void testEE6App12() throws Exception {
        parse(app12() + appTail());
    }

    @Test
    public void testEE6App13() throws Exception {
        parse(app13() + appTail());
    }

    @Test
    public void testEE6App14() throws Exception {
        parse(app14() + appTail());
    }

    @Test
    public void testEE6App50() throws Exception {
        parse(app50() + appTail());
    }

    @Test
    public void testEE6App60() throws Exception {
        parse(app60() + appTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6App70() throws Exception {
        parse(app70() + appTail());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE6App80() throws Exception {
        parse(app80() + appTail());
    }

    // JavaEE7 cases ...

    // Parse everything except 8.0.
    
    @Test
    public void testEE7App12() throws Exception {
        parse(app12() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App13() throws Exception {
        parse(app13() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App14() throws Exception {
        parse(app14() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App50() throws Exception {
        parse(app50() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App60() throws Exception {
        parse(app60() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test
    public void testEE7App70() throws Exception {
        parse(app70() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    @Test(expected = DDParser.ParseException.class)
    public void testEE7App80() throws Exception {
        parse(app80() + appTail(), JavaEEVersion.VERSION_7_0);
    }

    // JavaEE8 cases ...

    // Parse everything.
    
    @Test
    public void testEE8App12() throws Exception {
        parse(app12() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App13() throws Exception {
        parse(app13() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App14() throws Exception {
        parse(app14() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App50() throws Exception {
        parse(app50() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App60() throws Exception {
        parse(app60() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App70() throws Exception {
        parse(app70() + appTail(), JavaEEVersion.VERSION_8_0);
    }

    @Test
    public void testEE8App80() throws Exception {
        parse(app80() + appTail(), JavaEEVersion.VERSION_8_0);
    }
}
