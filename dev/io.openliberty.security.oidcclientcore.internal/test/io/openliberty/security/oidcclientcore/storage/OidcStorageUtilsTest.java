/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.random.RandomUtils;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestUtils;
import io.openliberty.security.oidcclientcore.utils.Utils;
import test.common.SharedOutputManager;

public class OidcStorageUtilsTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    String strRandom = RandomUtils.getRandomAlphaNumeric(AuthorizationRequestUtils.STATE_LENGTH);
    String timestamp = Utils.getTimeStamp();
    String state = timestamp + strRandom;

    @Test
    public void test_createStateStorageValue() {
        String cookieValue = OidcStorageUtils.createStateStorageValue(state, "secret");
        String timestamp = state.substring(0, Utils.TIMESTAMP_LENGTH);
        String newValue = state + "secret";
        String value = HashUtils.digest(newValue);
        String newCookieValue = timestamp + value;
        assertEquals(newCookieValue, cookieValue);
    }

    @Test
    public void test_getStorageKey() {
        String storageKey = OidcStorageUtils.getStorageKey("WASOidc", "client01", state);
        String newValue = state + "client01";
        String newName = Utils.getStrHashCode(newValue);
        String expectedStorageKey = "WASOidc" + newName;
        assertEquals(expectedStorageKey, storageKey);
    }

}
