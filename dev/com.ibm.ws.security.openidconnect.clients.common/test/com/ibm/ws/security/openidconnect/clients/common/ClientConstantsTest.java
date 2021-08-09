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
package com.ibm.ws.security.openidconnect.clients.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;

import test.common.SharedOutputManager;

public class ClientConstantsTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            ClientConstants cc = new ClientConstants();
            assertNotNull("There must be a ClientConstants", cc);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testClientConstants() {
        final String methodName = "testClientConstants";
        try {
            List<String> primaryKeys = Arrays.asList(new String[] { ClientConstants.JTI,
                    ClientConstants.SUB,
                    ClientConstants.SCOPE,
                    ClientConstants.CLIENT_ID,
                    ClientConstants.CID,
                    ClientConstants.GRANT_TYPE,
                    ClientConstants.USER_ID,
                    ClientConstants.USER_NAME,
                    ClientConstants.EMAIL,
                    ClientConstants.IAT,
                    ClientConstants.EXP,
                    ClientConstants.ISS,
                    ClientConstants.AUD });
            boolean[] abFound = new boolean[primaryKeys.size()];
            for (boolean bfound : abFound) {
                assertFalse(bfound);
            }

            for (String key : ClientConstants.primaryKeys) {
                assertTrue("key " + key + " is not in primaryKeys", primaryKeys.contains(key));
                int index = primaryKeys.indexOf(key);
                abFound[index] = true;
            }
            for (boolean bfound : abFound) {
                assertTrue("some key is not found in primaryKeys", bfound);
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

};
