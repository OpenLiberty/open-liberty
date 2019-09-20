/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.sl.provider;

import java.security.SecurityPermission;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import com.ibm.ws.security.authorization.jacc.sl.provider.WSPolicyConfigurationImpl.ContextState;

public class WSPolicyConfigurationFactoryImpl extends PolicyConfigurationFactory {

    private AllPolicyConfigs allConfigs;

    public WSPolicyConfigurationFactoryImpl() {
        if (allConfigs == null) {
            allConfigs = AllPolicyConfigs.getInstance();
        }
    }

    @Override
    public PolicyConfiguration getPolicyConfiguration(String contextId, boolean flag) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }
        if (flag && allConfigs.getPolicyConfig(contextId) != null) {
            allConfigs.remove(contextId);
        }
        WSPolicyConfigurationImpl policyConfig = allConfigs.getPolicyConfig(contextId);
        if (policyConfig == null) {
            policyConfig = new WSPolicyConfigurationImpl(contextId);
            allConfigs.setPolicyConfig(contextId, policyConfig);
        }
        policyConfig.setState(ContextState.STATE_OPEN);

        return policyConfig;
    }

    @Override
    public boolean inService(String contextID) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        boolean inservice = false;
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }
        WSPolicyConfigurationImpl policyConfig = allConfigs.getPolicyConfig(contextID);
        if (policyConfig == null) {
            return false;
        }
        inservice = policyConfig.inService();
        return inservice;
    }
}
