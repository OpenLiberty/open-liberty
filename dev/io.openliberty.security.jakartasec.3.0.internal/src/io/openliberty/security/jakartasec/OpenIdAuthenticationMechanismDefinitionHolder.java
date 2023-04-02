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
package io.openliberty.security.jakartasec;

import com.ibm.websphere.ras.annotation.Sensitive;

import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;

/**
 * Temporarily holds the OpenIdAuthenticationMechanismDefinition instance to prevent tracing its secrets.
 */
public class OpenIdAuthenticationMechanismDefinitionHolder {

    @Sensitive
    private final OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition;

    @Sensitive
    public OpenIdAuthenticationMechanismDefinitionHolder(OpenIdAuthenticationMechanismDefinition oidcMechanismDefinition) {
        this.oidcMechanismDefinition = oidcMechanismDefinition;
    }

    @Sensitive
    public OpenIdAuthenticationMechanismDefinition getOpenIdAuthenticationMechanismDefinition() {
        return oidcMechanismDefinition;
    }

}
