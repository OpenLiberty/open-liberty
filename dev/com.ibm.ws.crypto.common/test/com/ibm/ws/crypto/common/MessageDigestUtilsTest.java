/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.crypto.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class MessageDigestUtilsTest {

    private final String text = "Open Liberty is an open application framework designed for the cloud. It’s small, lightweight, and designed with modern cloud-native application development in mind. It supports the full MicroProfile and Jakarta EE APIs and is composable, meaning that you can use only the features that you need, keeping everything lightweight, which is great for microservices. It also deploys to every major cloud platform, including Docker, Kubernetes, and Cloud Foundry.";

    @Test
    public void testGetMessageDigestAlgorithm() {
        assertEquals("Wrong algorithm returned", MessageDigestUtils.getMessageDigestAlgorithm(), MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA256);

    }

    @Test
    public void testGetDefaultMessageDigest() {
        try {
            MessageDigest md = MessageDigestUtils.getMessageDigest();
            assertEquals("Incorrect default MessageDigest algorithm", MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA256, md.getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            fail("Caught NoSuchAlgorithmException");
        }
    }

    @Test
    public void testGetMessageDigest() {
        try {
            MessageDigest md = MessageDigestUtils.getMessageDigest(MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA512);
            assertEquals("Failed to get SHA512 algorithm", MessageDigestUtils.MESSAGE_DIGEST_ALGORITHM_SHA512, md.getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            fail("Caught NoSuchAlgorithmException");
        }
    }

    @Test
    public void testGetUnsupportedMessageDigest() {
        try {
            MessageDigestUtils.getMessageDigest("SHA-1");
            fail("Attempt to get SHA-1 MessageDigest did not fail");
        } catch (NoSuchAlgorithmException e) {
            // This exception is expected
        }
    }

}
