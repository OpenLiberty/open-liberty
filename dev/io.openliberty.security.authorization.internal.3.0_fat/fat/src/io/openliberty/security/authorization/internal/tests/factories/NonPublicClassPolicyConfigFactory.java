/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.internal.tests.factories;

import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyContextException;

class NonPublicClassPolicyConfigFactory extends PolicyConfigurationFactory {

    @Override
    public PolicyConfiguration getPolicyConfiguration() {
        return null;
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String arg0) {
        return null;
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String arg0, boolean arg1) throws PolicyContextException {
        return null;
    }

    @Override
    public boolean inService(String arg0) throws PolicyContextException {
        return false;
    }
}
