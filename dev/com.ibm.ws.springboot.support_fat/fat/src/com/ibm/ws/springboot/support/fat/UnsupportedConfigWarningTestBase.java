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
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public abstract class UnsupportedConfigWarningTestBase extends CommonWebServerTests {

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Test
    public void testUnsupportedSessionConfigWarning() throws Exception {
        testBasicSpringBootApplication();
        assertNotNull("No warning message was found for unsupported session config", server.waitForStringInLog("CWWKC0262W"));
    }

    @Test
    public void testUnsupportedCompressionWarning() throws Exception {
        testBasicSpringBootApplication();
        assertNotNull("No warning message was found for unsupported compression enabled", server.waitForStringInLog("CWWKC0261W"));
    }
}