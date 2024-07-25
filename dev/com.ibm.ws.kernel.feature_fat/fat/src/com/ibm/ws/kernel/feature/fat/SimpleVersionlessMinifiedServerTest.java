/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.feature.fat;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class SimpleVersionlessMinifiedServerTest {

    static MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();

    @BeforeClass
    public static void setup() throws Exception {
        minifyUtils.setupAndStartMinifiedServer(SimpleVersionlessMinifiedServerTest.class.getName(),
                                                "com.ibm.ws.kernel.feature.fat.versionless.minify",
                                                LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.versionless.minify"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        minifyUtils.tearDown();
    }

    @Test
    public void testStaticContentForDefaultServer() throws Exception {
        //a simple test, gets the static file, looks for the eyecatcher & pass lines.
        minifyUtils.testViaHttpGet(new URL(minifyUtils.staticUrlPrefix + "/test.txt"));
    }
}
