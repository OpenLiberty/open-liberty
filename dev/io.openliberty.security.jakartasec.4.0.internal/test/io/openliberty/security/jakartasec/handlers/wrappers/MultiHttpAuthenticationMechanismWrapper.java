/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers.wrappers;

import io.openliberty.security.jakartasec.handlers.MultiHttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

/**
 * A subclass wrapper so of MultiHttpAuthenticationMechanism so we can override internal methods
 * which return objects/components we can't easily mock.
 */

public class MultiHttpAuthenticationMechanismWrapper extends MultiHttpAuthenticationMechanism {

    private String simpleName = null;

    /**
     * @param wrappedHttpAuthenticationMechanism
     */
    public MultiHttpAuthenticationMechanismWrapper(HttpAuthenticationMechanism wrappedHttpAuthenticationMechanism, String simpleName) {
        super(wrappedHttpAuthenticationMechanism);
        this.simpleName = simpleName;
    }

    @Override
    public String getSimpleName() {
        return this.simpleName;
    }

    @Override
    public int getPriority() {
        return super.getPriority();
    }
}
