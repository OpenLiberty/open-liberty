/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;

/**
 *
 */
public class WCDummyTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(WCDummyTest.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @SuppressWarnings("unused")
    private String failMsg(String search_msg) {
        String fail_msg = "\n FileUpload: Fail to find string: " + search_msg + "\n";
        return fail_msg;
    }

    private String failMsg(int search_msg) {
        String fail_msg = "\n FileUpload: Fail to find string: " + search_msg + "\n";
        return fail_msg;
    }

    @Test
    public void testDummy() throws Exception {
        //so there is at least one test run
        LOG.info("\n /******************************************************************************/");
        LOG.info("\n [WebContainer | WCDummyTest]: testDummy");
        LOG.info("\n /******************************************************************************/");

        assertTrue(failMsg(200), true);
    }

}
