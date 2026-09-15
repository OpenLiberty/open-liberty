/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.lang.reflect.Constructor;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
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
 * The reflective class/constructor lookup and {@link SecretKeyFactory} initialisation are
 * performed once at class-load time; {@link #getKey()} incurs no further reflection overhead.
 */
public class ICSFSecretKeyResolver implements SecretKeyResolver {

    private static final TraceComponent tc = Tr.register(ICSFSecretKeyResolver.class);

    private static final String IBMJCECCA_PROVIDER = "IBMJCECCA";
    private static final String KEY_LABEL_KEY_SPEC_CLASS = "com.ibm.crypto.hdwrCCA.provider.KeyLabelKeySpec";

    /**
     * Resolved once at class-load time. Null if IBMJCECCA or KeyLabelKeySpec is unavailable
     * on this platform; {@link #getKey()} will throw in that case using {@link #INIT_FAILURE}.
     */
    private static final Constructor<?> KEY_LABEL_KEY_SPEC_CTOR;
    private static final SecretKeyFactory SECRET_KEY_FACTORY;
    /**
     * Non-null when static initialisation failed. Each {@link #getKey()} call wraps this as a
     * cause so the caller's stack trace reflects the actual call site, not class-load time.
     */
    private static final InvalidKeySpecException INIT_FAILURE;
    /**
     * Ensures the unavailability warning is emitted at most once, deferred to the first
     * actual {@link #getKey()} call rather than at class-load time.
     */
    private static volatile boolean unavailabilityWarningLogged = false;

    static {
        Constructor<?> ctor = null;
        SecretKeyFactory factory = null;
        InvalidKeySpecException failure = null;
        try {
            ctor = Class.forName(KEY_LABEL_KEY_SPEC_CLASS).getConstructor(String.class);
            factory = SecretKeyFactory.getInstance(CryptoUtils.ENCRYPT_ALGORITHM_AES, IBMJCECCA_PROVIDER);
        } catch (Exception e) {
            failure = new InvalidKeySpecException("IBMJCECCA provider or KeyLabelKeySpec unavailable on this platform", e);
        }
        KEY_LABEL_KEY_SPEC_CTOR = ctor;
        SECRET_KEY_FACTORY = factory;
        INIT_FAILURE = failure;
    }

    private final String label;

    /**
     * @param label the ICSF CKDS key label identifying the AES key to use; must not be null or blank
     * @throws NullPointerException     if {@code label} is null
     * @throws IllegalArgumentException if {@code label} is blank
     */
    public ICSFSecretKeyResolver(String label) {
        Objects.requireNonNull(label, "label must not be null");
        if (label.trim().isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        this.label = label;
    }

    /**
     * Returns the hardware-backed AES key from the ICSF CKDS for the configured label.
     *
     * <p>Note: {@link NoSuchAlgorithmException} is declared by the {@link SecretKeyResolver}
     * interface but is never thrown by this implementation; all errors are reported as
     * {@link InvalidKeySpecException}.
     *
     * {@inheritDoc}
     */
    @Override
    public Key getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (INIT_FAILURE != null) {
            if (!unavailabilityWarningLogged) {
                unavailabilityWarningLogged = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "ICSF_PROVIDER_UNAVAILABLE", INIT_FAILURE.getCause());
                }
            }
            throw new InvalidKeySpecException(INIT_FAILURE.getMessage(), INIT_FAILURE);
        }
        try {
            KeySpec keySpec = (KeySpec) KEY_LABEL_KEY_SPEC_CTOR.newInstance(label);
            return SECRET_KEY_FACTORY.generateSecret(keySpec);
        } catch (Exception e) {
            InvalidKeySpecException ex = new InvalidKeySpecException("Failed to obtain ICSF key for label: " + label, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                Tr.error(tc, "ICSF_KEY_LOOKUP_FAILED", label, e);
            }
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "ICSFSecretKeyResolver [label=" + label + "]";
    }
}
