/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.tx.jta.embeddable.TransactionSettingsProvider;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.GlobalTranConfigDataImpl;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.LocalTranConfigDataImpl;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Provide transaction settings on a per EJB basis.
 */
@Component(service = TransactionSettingsProvider.class)
public class EJBTransactionSettingsProvider implements TransactionSettingsProvider {

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Override
    public boolean isActive() {
        return getBeanMetaData() != null;
    }

    @Override
    public GlobalTransactionSettings getGlobalTransactionSettings() {
        BeanMetaData bmd = getBeanMetaData();

        return bmd != null ? (GlobalTranConfigDataImpl) bmd._globalTran : null;
    }

    @Override
    public LocalTransactionSettings getLocalTransactionSettings() {
        BeanMetaData bmd = getBeanMetaData();

        return bmd != null ? (LocalTranConfigDataImpl) bmd._localTran : null;
    }

    private BeanMetaData getBeanMetaData() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        return cmd instanceof BeanMetaData ? (BeanMetaData) cmd : null;
    }

}
