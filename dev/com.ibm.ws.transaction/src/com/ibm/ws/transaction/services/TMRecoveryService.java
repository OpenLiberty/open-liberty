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
package com.ibm.ws.transaction.services;

import org.osgi.framework.BundleContext;

import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

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
            Tr.debug(tc, "activate  context " + ctxt);
        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();

        //This needs tidying a little.
        if (cp != null) {
            if (cp instanceof JTMConfigurationProvider) {
                JTMConfigurationProvider jtmCP = (JTMConfigurationProvider) cp;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "its a jtmconfigurationprovider ");

                // Set a reference to this TMRecoveryService into the JTMConfigurationProvider.
                // If other resources are in place this method will also start recovery by calling
                // doStart()
                jtmCP.setTMRecoveryService(this);

            }
        }
    }

    /**
     * Called by DS to reference the Transaction Manager Service
     *
     * @param tm
     */
    public void setTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setTransactionManagerService " + tm);
    }

    /**
     * Called by DS to dereference the Transaction Manager Service
     *
     * @param tm
     */
    protected void unsetTransactionManager(EmbeddableWebSphereTransactionManager tm) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetTransactionManagerService, tms " + tm);
    }
}
