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
package io.openliberty.security.oidcclientcore.authentication;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.common.random.RandomUtils;

import io.openliberty.security.oidcclientcore.utils.Utils;

public class AuthorizationRequestUtils {

    String generateStateValue(HttpServletRequest request) {
        String strRandom = RandomUtils.getRandomAlphaNumeric(9);
        String timestamp = Utils.getTimeStamp();
        String state = timestamp + strRandom;
        if (request != null && !request.getMethod().equalsIgnoreCase("GET") && request.getParameter("oidc_client") != null) {
            state = state + request.getParameter("oidc_client");
        }
        return state;
    }

    @Sensitive
    @Trivial
    public static String createStateValueForStorage(String clientSecret, String state) {
        String timestamp = state.substring(0, Utils.TIMESTAMP_LENGTH);
        String newValue = state + clientSecret; // state already has a timestamp in it
        String value = HashUtils.digest(newValue);
        return timestamp + value;
    }

}
