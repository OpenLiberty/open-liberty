/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

package com.ibm.ws.security.authorization.jacc.web.impl;

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

    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId, boolean flag) throws PolicyContextException {
        return pc;
    }

    @Override
    public boolean inService(String contextID) throws PolicyContextException {
        return true;
    }
}
