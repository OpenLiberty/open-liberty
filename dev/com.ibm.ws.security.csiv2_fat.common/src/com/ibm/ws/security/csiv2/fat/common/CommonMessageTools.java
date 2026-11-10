/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.fat.common;

import static org.junit.Assert.assertTrue;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Adapted from OIDC for CSIv2 FAT tests.
 */
public class CommonMessageTools {

    private final Class<?> thisClass = CommonMessageTools.class;

    /**
     * 
     * Logs the result of the check and then invokes the junit assertTrue to
     * record the test "status" with junit
     * 
     * @param caller
     *            - routine that is requesting the check be performed
     * @param msg
     *            - the message that will be recorded if the test assertion
     *            fails
     * @param trueFalse
     *            - the state of the test assertion
     */
    public void assertTrueAndLog(String caller, String msg, Boolean trueFalse) {

        Log.info(thisClass, caller, "Test assertion is: " + trueFalse);
        if (!trueFalse) {
            Log.info(thisClass, caller, msg);
        }
        assertTrue(msg, trueFalse);
    }

    public void printMethodName(String strMethod) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "***************************** "
                                       + strMethod);
    }

    public void printMethodName(String strMethod, String task) {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "***************************** " + task
                                       + " " + strMethod + " *****************************");
    }

}
