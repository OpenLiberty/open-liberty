/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
        // System.out.println("Message digest of bob = " + digest);
        assertTrue("Wrong digest value: " + digest, digest.equals("81b637d8fcd2c6da6359e6963113a1170de795e4b725b84d1e0b4cfd9ec58ce9"));
    }
}
