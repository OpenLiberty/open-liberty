/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.pitt.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * A simple container managed entity bean to test container and
 * deployment tools. <p>
 * 
 * @author Chriss Stephens
 * @version $Id: CMEntity.java,v 1.15 2001/02/09 19:27:41 amber Exp $
 */
public interface CMEntity extends EJBObject {
    public static final int BOTCH_BEFORE_UPDATE = 1;
    public static final int BOTCH_AFTER_UPDATE = 2;
    public static final int BOTCHED_BOTCH = 3;

    /**
     * Increment the persistent counter associated with this entity bean.
     * 
     * @return an <code>int</code> containing the value of the persistent counter
     *         after the increment has completed.
     */
    public int increment() throws RemoteException;

    /**
     * Increment the persistent counter associated with this entity bean. <p>
     * 
     * Must be deployed with TX_MANDATORY transaction attribute. <p>
     * 
     * @return an <code>int</code> containing the value of the persistent counter
     *         after the increment has completed.
     */
    public int txMandatoryIncrement() throws RemoteException;

    /**
     * Increment the persistent counter associated with this entity bean or throw
     * a "bean-developer defined" exception. <p>
     * 
     * @param botch an <code>int</code> that controls whether this method
     *            completes successfully or throws a pre-defined exception
     * @exception RemoteException thrown if system level failure occurs when
     *                executing this method
     * @exception BeanException1 thrown if botch parameter equals 1
     * @exception BeanException2 thrown if botch parameter equals 2
     */
    public void incrementOrBotch(int botch) throws BeanException1, BeanException2, RemoteException;

    /**
     * Get the current value of this entity bean.
     */
    public int getValue() throws RemoteException;

    /**
     * Test get/setRollbackOnly calls.
     */
    public void testRollbackOnly() throws RemoteException;

    /**
     * Set the value associated with this entity bean. <p>
     * 
     * @param v an <code>int</code> to set the current value of this entity bean to
     */
    public void setValue(int v) throws RemoteException;

    /**
     * Get the key associated with this entity bean. <p>
     */
    public CMKey getKey() throws RemoteException;

    // for RT97202; test comp env access
    public void testCompEnvAccess() throws RemoteException;

    public void setTestId(String testId) throws RemoteException;

    public void clearTestId() throws RemoteException;

    public String getValue_NotSupported() throws RemoteException;

    public void setValue_NotSupported(String newValue) throws RemoteException;
}