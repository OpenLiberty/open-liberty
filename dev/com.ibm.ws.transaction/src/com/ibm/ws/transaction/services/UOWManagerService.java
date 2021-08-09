/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.services;

import javax.transaction.Synchronization;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.uow.embeddable.UOWManagerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWActionException;
import com.ibm.wsspi.uow.UOWException;
import com.ibm.wsspi.uow.UOWManager;

@Component
public class UOWManagerService implements UOWManager {

    private UOWManager uowm;

    @Activate
    protected void activate(BundleContext context) {
        if (WsLocationConstants.LOC_PROCESS_TYPE_CLIENT.equals(context.getProperty(WsLocationConstants.LOC_PROCESS_TYPE))) {
            uowm = UOWManagerFactory.getClientUOWManager();
        } else {
            uowm = UOWManagerFactory.getUOWManager();
        }
    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        uowm = null;
    }

    /** {@inheritDoc} */
    @Override
    public int getUOWTimeout() throws IllegalStateException {
        return uowm.getUOWTimeout();
    }

    /** {@inheritDoc} */
    @Override
    public void runUnderUOW(int uowType, boolean join, UOWAction uowAction) throws UOWActionException, UOWException {
        uowm.runUnderUOW(uowType, join, uowAction);
    }

    /** {@inheritDoc} */
    @Override
    public void setUOWTimeout(int uowType, int timeout) {
        uowm.setUOWTimeout(uowType, timeout);
    }

    /** {@inheritDoc} */
    @Override
    public long getLocalUOWId() {
        return uowm.getLocalUOWId();
    }

    /** {@inheritDoc} */
    @Override
    public Object getResource(Object key) throws NullPointerException {
        return uowm.getResource(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getRollbackOnly() {
        return uowm.getRollbackOnly();
    }

    /** {@inheritDoc} */
    @Override
    public String getUOWName() {
        return uowm.getUOWName();
    }

    /** {@inheritDoc} */
    @Override
    public int getUOWStatus() {
        return uowm.getUOWStatus();
    }

    /** {@inheritDoc} */
    @Override
    public int getUOWType() {
        return uowm.getUOWType();
    }

    /** {@inheritDoc} */
    @Override
    public void putResource(Object key, Object value) {
        uowm.putResource(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        uowm.registerInterposedSynchronization(sync);
    }

    /** {@inheritDoc} */
    @Override
    public void setRollbackOnly() {
        uowm.setRollbackOnly();
    }

    /** {@inheritDoc} */
    @Override
    public long getUOWExpiration() throws IllegalStateException {
        return uowm.getUOWExpiration();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.uow.UOWManager#runUnderUOW(int, boolean, com.ibm.wsspi.uow.ExtendedUOWAction, java.lang.Class[], java.lang.Class[])
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Object runUnderUOW(int uowType, boolean join, ExtendedUOWAction uowAction, Class[] rollbackOn, Class[] dontRollbackOn) throws Exception {
        return uowm.runUnderUOW(uowType, join, uowAction, rollbackOn, dontRollbackOn);
    }
}