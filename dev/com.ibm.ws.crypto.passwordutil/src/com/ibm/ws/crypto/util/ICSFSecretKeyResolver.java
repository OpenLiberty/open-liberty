/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.crypto.util;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.ibm.wsspi.security.crypto.SecretKeyResolver;

/**
 * {@link SecretKeyResolver} that retrieves a hardware-backed AES key from the
 * IBM ICSF Cryptographic Key Data Set (CKDS) via the IBMJCECCA security provider.
 *
 * <p>The key is located by its label in the CKDS. Key material never leaves hardware;
 * callers must not invoke {@code getEncoded()} on the returned {@link Key}.
 *
 * <p>This class uses reflection to load {@code com.ibm.crypto.hdwrCCA.provider.KeyLabelKeySpec}
 * so that it compiles on non-z/OS platforms where IBMJCECCA is unavailable.
 */
public class ICSFSecretKeyResolver implements SecretKeyResolver {

    private static final String IBMJCECCA_PROVIDER = "IBMJCECCA";
    private static final String KEY_LABEL_KEY_SPEC_CLASS = "com.ibm.crypto.hdwrCCA.provider.KeyLabelKeySpec";

    private final String label;

    /**
     * @param label the ICSF CKDS key label identifying the AES key to use
     */
    public ICSFSecretKeyResolver(String label) {
        this.label = label;
    }

    /** {@inheritDoc} */
    @Override
    public Key getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            Class<?> keyLabelKeySpecClass = Class.forName(KEY_LABEL_KEY_SPEC_CLASS);
            java.lang.reflect.Constructor<?> ctor = keyLabelKeySpecClass.getConstructor(String.class);
            java.security.spec.KeySpec keySpec = (java.security.spec.KeySpec) ctor.newInstance(label);
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("AES", IBMJCECCA_PROVIDER);
            return factory.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidKeySpecException("Failed to obtain ICSF key for label: " + label, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "ICSFSecretKeyResolver[label=" + label + "]";
    }
}
