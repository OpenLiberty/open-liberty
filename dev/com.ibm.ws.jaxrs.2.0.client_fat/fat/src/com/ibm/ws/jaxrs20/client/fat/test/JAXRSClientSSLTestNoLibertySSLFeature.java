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
package com.ibm.ws.jaxrs20.client.fat.test;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;

@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
@RunWith(FATRunner.class)
public class JAXRSClientSSLTestNoLibertySSLFeature extends JAXRSClientSSLTestNoLibertySSLCfg {

    @BeforeClass
    public static void setup() throws Exception {

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            serverNoSSL.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        serverNoSSL.changeFeatures(Arrays.asList("jaxrs-2.0", "ssl-1.0"));

        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
