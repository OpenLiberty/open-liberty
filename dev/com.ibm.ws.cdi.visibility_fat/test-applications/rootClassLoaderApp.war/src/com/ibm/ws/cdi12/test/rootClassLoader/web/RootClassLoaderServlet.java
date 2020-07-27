/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.rootClassLoader.web;

import static org.junit.Assert.assertNotNull;

import java.util.Random;
import java.util.Timer;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi12.test.rootClassLoader.extension.OSName;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class RootClassLoaderServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    Random random;

    @Inject
    Timer timer;

    @Inject
    @OSName
    String osName;

    @Test
    @Mode(TestMode.LITE)
    public void testInjectionFromRootClassloader() {
        assertNotNull("random was null", random);
        assertNotNull("timer was null", timer);
        assertNotNull("osName was null", osName);
    }

}
