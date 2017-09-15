/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.internal.callback;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 * Helper class used to assert that the callbacks contain the expected values.
 */
public class CallbacksAssertionHelper {

    public static void assertCallbacksValues(Callback[] callbacks, Object[] expectedValues) throws Exception {
        for (int i = 0; i < callbacks.length; i++) {
            Callback callback = callbacks[i];
            String callbackClassName = callback.getClass().getName();
            Object expectedValue = expectedValues[i];
            Object currentValue = getValueFromCallback(callback);
            String assertionMessage = "The " + callbackClassName + " callback must be set with the expected value by the callback handler.";
            if (expectedValue instanceof Object[]) {
                assertArrayEquals(assertionMessage, (Object[]) expectedValue, (Object[]) currentValue);
            } else {
                assertEquals(assertionMessage, expectedValue, currentValue);
            }
        }
    }

    private static Object getValueFromCallback(Callback callback) {
        Object value = retrieveValue(callback);
        value = stringifyValueIfNeeded(value);
        return value;
    }

    private static Object retrieveValue(Callback callback) {
        Object value = null;
        if (callback instanceof NameCallback) {
            value = ((NameCallback) callback).getName();
        } else if (callback instanceof PasswordCallback) {
            value = ((PasswordCallback) callback).getPassword();
        } else if (callback instanceof WSRealmNameCallbackImpl) {
            value = ((WSRealmNameCallbackImpl) callback).getRealmName();
        } else if (callback instanceof WSAppContextCallback) {
            value = ((WSAppContextCallback) callback).getContext();
        } else if (callback instanceof WSCredTokenCallbackImpl) {
            value = ((WSCredTokenCallbackImpl) callback).getCredToken();
        } else if (callback instanceof WSServletRequestCallback) {
            value = ((WSServletRequestCallback) callback).getHttpServletRequest();
        } else if (callback instanceof WSServletResponseCallback) {
            value = ((WSServletResponseCallback) callback).getHttpServletResponse();
        } else if (callback instanceof WSX509CertificateChainCallback) {
            value = ((WSX509CertificateChainCallback) callback).getX509CertificateChain();
        } else if (callback instanceof WSAuthMechOidCallbackImpl) {
            value = ((WSAuthMechOidCallbackImpl) callback).getAuthMechOid();
        }
        return value;
    }

    private static Object stringifyValueIfNeeded(Object value) {
        Object newValue = value;
        if (value instanceof byte[]) {
            newValue = new String((byte[]) value);
        } else if (value instanceof char[]) {
            newValue = new String((char[]) value);
        }
        return newValue;
    }

}
