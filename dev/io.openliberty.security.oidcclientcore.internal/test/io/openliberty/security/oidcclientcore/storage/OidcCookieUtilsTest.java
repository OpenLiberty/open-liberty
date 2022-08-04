/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.random.RandomUtils;

import io.openliberty.security.oidcclientcore.utils.Utils;
import test.common.SharedOutputManager;

public class OidcCookieUtilsTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    String strRandom = RandomUtils.getRandomAlphaNumeric(9);
    String timestamp = Utils.getTimeStamp();
    String state = timestamp + strRandom;

    @Test
    public void testCreateStateCookieValue() {
        String cookieValue = OidcCookieUtils.createStateCookieValue("secret", state);
        String timestamp = state.substring(0, Utils.TIMESTAMP_LENGTH);
        String newValue = state + "secret";
        String value = HashUtils.digest(newValue);
        String newCookieValue = timestamp + value;
        assertEquals(newCookieValue, cookieValue);
    }

    @Test
    public void testGetCookieName() {
        String cookieName = OidcCookieUtils.getCookieName("WASOidc", "client01", state);
        String newValue = state + "client01";
        String newName = Utils.getStrHashCode(newValue);
        String expectedCookieName = "WASOidc" + newName;
        assertEquals(expectedCookieName, cookieName);
    }

}
