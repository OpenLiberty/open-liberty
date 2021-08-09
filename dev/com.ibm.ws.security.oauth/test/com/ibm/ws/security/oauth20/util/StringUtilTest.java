/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 *
 */
public class StringUtilTest {

    @Test
    public void splitPair() {

        String contentString = "Content-Type=application/json";
        String[] expectedArray = { "Content-Type", "application/json" };
        String[] splitArray = StringUtil.splitPair(contentString, '=');
        assertArrayEquals(splitArray, expectedArray);

    }

    /**
     * Check with must present flag set to false
     */
    @Test
    public void splitPairCharNotPresentMustPresentNotSet() {

        String contentString = "Content-Type:application/json";
        String[] expectedArray = { contentString, "" };
        String[] splitArray = StringUtil.splitPair(contentString, '=', false);
        assertArrayEquals(splitArray, expectedArray);

    }

    /**
     * Check with must present flag set to true
     */
    @Test(expected = IllegalArgumentException.class)
    public void splitPairCharNotPresentMustPresentSet() {

        String contentString = "Content-Type:application/json";
        StringUtil.splitPair(contentString, '=', true);

    }

    @Test
    public void splitPairWithAsterisk() {

        String contentString = "*/*";
        String[] expectedArray = { "*", "*" };
        String[] splitArray = StringUtil.splitAcceptPairAllowingSingleAsterisk(contentString);
        assertArrayEquals(splitArray, expectedArray);

    }

    @Test
    public void splitPairWithAsteriskNonDefault() {

        String contentString = "*";
        String[] expectedArray = { "*", "*" };
        String[] splitArray = StringUtil.splitAcceptPairAllowingSingleAsterisk(contentString);
        assertArrayEquals(splitArray, expectedArray);

    }
}
