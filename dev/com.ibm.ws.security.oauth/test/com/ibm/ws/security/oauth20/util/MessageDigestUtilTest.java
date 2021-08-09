/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 *
 */
public class MessageDigestUtilTest {

    /**
     * Test method for com.ibm.ws.security.oauth20.util.MessageDigestUtil.getDigest()
     * 
     */
    @Test
    public void getDigest() {
        String digest = MessageDigestUtil.getDigest("bob");
        //System.out.println("Message digest of bob = " + digest);
        assertTrue("Wrong digest value", digest.equals("48181acd22b3edaebc8a447868a7df7ce629920a"));
    }
}
