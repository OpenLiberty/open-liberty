/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.derbyra.resourceadapter;

import java.io.Serializable;
import java.util.Timer;

import javax.resource.ResourceException;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionSynchronizationRegistry;

/**
 * Administered object that exposes BootstrapContext to the application.
 * We would never recommend this to a resource adapter vendor. But it's very convenient for testing.
 */
public class DerbyBootstrapContext implements BootstrapContext, ResourceAdapterAssociation, Serializable {
    private static final long serialVersionUID = 1557430607598372401L;

    private transient DerbyResourceAdapter adapter;

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (DerbyResourceAdapter) adapter;
    }

    @Override
    public Timer createTimer() throws UnavailableException {
        return adapter.bootstrapContext.createTimer();
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    @Override
    public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return adapter.bootstrapContext.getTransactionSynchronizationRegistry();
    }

    @Override
    public WorkManager getWorkManager() {
        return adapter.bootstrapContext.getWorkManager();
    }

    @Override
    public XATerminator getXATerminator() {
        return adapter.bootstrapContext.getXATerminator();
    }

    @Override
    public boolean isContextSupported(Class<? extends WorkContext> workContext) {
        return adapter.bootstrapContext.isContextSupported(workContext);
    }
}
