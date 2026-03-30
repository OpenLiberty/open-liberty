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

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyFactory;

public class NoPublicCtorPolicyFactory extends PolicyFactory {

    private NoPublicCtorPolicyFactory() {
        // private constructor to show error message comes out about not being able to create the PolicyConfigurationFactory
    }

    @Override
    public Policy getPolicy(String arg0) {
        return null;
    }

    @Override
    public void setPolicy(String arg0, Policy arg1) {
    }
}
