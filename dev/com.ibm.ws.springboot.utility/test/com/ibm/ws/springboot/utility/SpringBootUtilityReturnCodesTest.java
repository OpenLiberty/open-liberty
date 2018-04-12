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
package com.ibm.ws.springboot.utility;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * These tests exist to make sure we do not inadventently change the return code values
 */
public class SpringBootUtilityReturnCodesTest {

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_OK() {
        assertEquals("FAIL: The return code value for 'OK' was changed",
                     0, SpringBootUtilityReturnCodes.OK.getReturnCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_ERR_GENERIC() {
        assertEquals("FAIL: The return code value for 'ERR_GENERIC' was changed",
                     1, SpringBootUtilityReturnCodes.ERR_GENERIC.getReturnCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_ERR_APP_NOT_FOUND() {
        assertEquals("FAIL: The return code value for 'ERR_APP_NOT_FOUND' was changed",
                     2, SpringBootUtilityReturnCodes.ERR_APP_NOT_FOUND.getReturnCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_ERR_APP_DEST_IS_DIR() {
        assertEquals("FAIL: The return code value for 'ERR_APP_DEST_IS_DIR' was changed",
                     3, SpringBootUtilityReturnCodes.ERR_APP_DEST_IS_DIR.getReturnCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_ERR_LIB_DEST_IS_FILE() {
        assertEquals("FAIL: The return code value for 'ERR_LIB_DEST_IS_FILE' was changed",
                     4, SpringBootUtilityReturnCodes.ERR_LIB_DEST_IS_FILE.getReturnCode());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.SpringBootUtilityReturnCodes#getReturnCode()}.
     */
    @Test
    public void getReturnCode_ERR_MAKE_DIR() {
        assertEquals("FAIL: The return code value for 'ERR_MAKE_DIR' was changed",
                     5, SpringBootUtilityReturnCodes.ERR_MAKE_DIR.getReturnCode());
    }
}
