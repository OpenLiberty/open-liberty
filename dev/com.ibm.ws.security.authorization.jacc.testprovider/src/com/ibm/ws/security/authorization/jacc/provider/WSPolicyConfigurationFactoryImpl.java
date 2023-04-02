/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

package com.ibm.ws.security.authorization.jacc.provider;

import java.security.SecurityPermission;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationImpl.ContextState;

public class WSPolicyConfigurationFactoryImpl extends PolicyConfigurationFactory {

    private static TraceComponent tc = Tr.register(WSPolicyConfigurationFactoryImpl.class);
    private AllPolicyConfigs allConfigs;

    public WSPolicyConfigurationFactoryImpl() {
        if (allConfigs == null) {
            allConfigs = AllPolicyConfigs.getInstance();
        }
    }

    public PolicyConfiguration getPolicyConfiguration() {
        String contextID = null;
        contextID = PolicyContext.getContextID();
        return allConfigs.getPolicyConfig(contextID);

    }

    public PolicyConfiguration getPolicyConfiguration(String contextID) {
        return allConfigs.getPolicyConfig(contextID);
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
            if (tc.isDebugEnabled())
                Tr.debug(tc, "hashMap does not contain the contextID: " + contextId);
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
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The policy configuration for the the contextID: " + contextID + " does not exist. exit value false");
            return false;
        }
        inservice = policyConfig.inService();
        return inservice;
    }
}
