/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.internal;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

public class DummyPolicyConfigurationFactory extends PolicyConfigurationFactory {
    private PolicyConfiguration pc = null;

    public DummyPolicyConfigurationFactory(PolicyConfiguration pc) {
        this.pc = pc;
    }

    public DummyPolicyConfigurationFactory() {
    }

    public PolicyConfiguration getPolicyConfiguration() {
        return pc;
    }

    public PolicyConfiguration getPolicyConfiguration(String contextID) {
        return pc;
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId, boolean flag) throws PolicyContextException {
        return pc;
    }

    @Override
    public boolean inService(String contextID) throws PolicyContextException {
        return true;
    }
}
