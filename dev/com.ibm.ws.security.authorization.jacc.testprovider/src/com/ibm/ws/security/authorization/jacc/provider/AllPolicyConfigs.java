/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.provider;

import java.util.Hashtable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 ** This class holds the mapping table of ContextId and PolicyConfiguration object.
 **/

public class AllPolicyConfigs {
    private static final TraceComponent tc = Tr.register(AllPolicyConfigs.class);
    private static AllPolicyConfigs policyConfig;
    private static boolean initialized = false;
    private final Hashtable<String, WSPolicyConfigurationImpl> policyConfigTable;

    public static AllPolicyConfigs getInstance() {
        if (!initialized) {
            policyConfig = new AllPolicyConfigs();
            initialized = true;
        }
        return policyConfig;
    }

    private AllPolicyConfigs() {
        policyConfigTable = new Hashtable<String, WSPolicyConfigurationImpl>();
    }

    public void setPolicyConfig(String contextId, WSPolicyConfigurationImpl policyConfig) {
        policyConfigTable.put(contextId, policyConfig);
    }

    public WSPolicyConfigurationImpl getPolicyConfig(String contextId) {
        return policyConfigTable.get(contextId);
    }

    public void remove(String contextId) {
        if (policyConfigTable.containsKey(contextId)) {
            policyConfigTable.remove(contextId);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Removed contextId:" + contextId + " from the policyConfigTable");
            }
        }
    }
}
