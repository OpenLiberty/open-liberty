/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.transaction.services;

import org.osgi.framework.BundleContext;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * The TMRecoveryService class was introduced under issue #5119 to support a new Declarative Service
 * to break a potential circular reference between the TransactionManagerService and jdbc's DataSourceService
 * which are mutually dependent.
 *
 * The TMRecoveryService requires that the TransactionManagerService is available (as does the DataSourceService).
 * Once it is available, Transaction Recovery can proceed as the DataSourceService can, if necessary be activated.
 * Previously, intermittently, DS would attempt to activate the DataSourceService in the context of TransactionManagerService
 * activation. That would fail with a DS Circular Reference error.
 */
public class TMRecoveryService {

    private static final TraceComponent tc = Tr.register(TMRecoveryService.class);

    protected void activate(BundleContext ctxt) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate {0}", ctxt);

        if (!FrameworkState.isStopping()) {
            final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();

            //This needs tidying a little.
            if (cp != null) {
                if (cp instanceof JTMConfigurationProvider) {
                    JTMConfigurationProvider jtmCP = (JTMConfigurationProvider) cp;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "it's a jtmconfigurationprovider ");

                    // Set a reference to this TMRecoveryService into the JTMConfigurationProvider.
                    // If other resources are in place this method will also start recovery by calling
                    // doStart()
                    jtmCP.setTMRecoveryService(this);
                }
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Framework stopping");
        }
    }

    /**
     * Called by DS to reference the Transaction Manager Service
     *
     * @param tm
     */
    @Trivial
    public void setTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTransactionManager {0}", tm);
    }

    /**
     * Called by DS to dereference the Transaction Manager Service
     *
     * @param tm
     */
    @Trivial
    protected void unsetTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetTransactionManager {0}", tm);
    }
}
