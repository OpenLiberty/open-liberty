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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package multiple.ham.custom.handlers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import multiple.ham.common.qualifiers.Admin;
import multiple.ham.common.qualifiers.Operator;
import multiple.ham.common.qualifiers.Tester;
import multiple.ham.common.qualifiers.User;

/**
 * Implementation of the HttpAuthenticationMechanismHandler interface.
 * This class defines a custom priority among the injected qualifiers
 * and prints out their injections.
 */

@Default
@ApplicationScoped
public class CustomHAMHandler extends BaseHAMHandler {
    private static final Class<?> c = CustomHAMHandler.class;

    @Inject
    @Admin
    private HttpAuthenticationMechanism adminHAM;
    @Inject
    @User
    private HttpAuthenticationMechanism userHAM;
    @Inject
    @Operator
    private HttpAuthenticationMechanism operatorHAM;
    @Inject
    @Tester
    private HttpAuthenticationMechanism testerHAM;

    @Override
    protected Class<?> getTestClass() {
        return c;
    }

    @Override
    protected HttpAuthenticationMechanism getAdminHAM() {
        return adminHAM;
    }

    @Override
    protected HttpAuthenticationMechanism getUserHAM() {
        return userHAM;
    }

    @Override
    protected HttpAuthenticationMechanism getOperatorHAM() {
        return operatorHAM;
    }

    @Override
    protected HttpAuthenticationMechanism getTesterHAM() {
        return testerHAM;
    }

    @Override
    protected HttpAuthenticationMechanism getDefaultHAM() {
        return null;
    }

    @Override
    protected HttpAuthenticationMechanism getHighestPriorityAuthMechanism() {

        HttpAuthenticationMechanism ham = null;
        if (getAdminHAM() != null) {
            ham = getAdminHAM();
        } else if (getUserHAM() != null) {
            ham = getUserHAM();
        } else if (getOperatorHAM() != null) {
            ham = getOperatorHAM();
        } else if (getTesterHAM() != null) {
            ham = getTesterHAM();
        }

        return ham;
    }
}