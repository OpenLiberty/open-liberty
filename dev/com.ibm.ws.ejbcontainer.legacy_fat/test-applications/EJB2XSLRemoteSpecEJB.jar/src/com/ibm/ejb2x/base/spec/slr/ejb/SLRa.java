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
package com.ibm.ejb2x.base.spec.slr.ejb;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.ejb.EJBObject;
import javax.naming.NamingException;

/**
 * Remote interface for Enterprise Bean: SLRa.
 */
public interface SLRa extends EJBObject {
    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 1:34:28 PM)
     * 
     * @return java.lang.String
     * @param arg1 java.lang.String
     */
    public String method1(String arg1) throws RemoteException;

    /**
     * Test pass-by-reference/value semantics. Key is changed and returned.
     */
    public SLRaPassBy changePassByParm(SLRaPassBy key) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:07:00 PM)
     */
    public Serializable context_getEJBHome() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:08:09 PM)
     */
    public SLRa context_getEJBObject() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:09:42 PM)
     */
    public Serializable context_getRollbackOnly() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:10:33 PM)
     */
    public Serializable context_getUserTransaction() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Serializable context_getCallerIdentity() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Serializable context_getCallerPrincipal() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    @SuppressWarnings("deprecation")
    public Serializable context_isCallerInRole(java.security.Identity identity) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Serializable context_isCallerInRole(String identity) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (08/23/2002)
     */
    public int context_getUserTransactionJ() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:11:58 PM)
     */
    public Serializable context_setRollbackOnly() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public String context_getEnvironment(String envVal) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Serializable context_getEJBLocalHome() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Serializable context_getEJBLocalObject() throws RemoteException;

    /**
     * get Boolean environment variable using java:comp/env
     */
    public Boolean getBooleanEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Byte environment variable using java:comp/env
     */
    public Byte getByteEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Character environment variable using java:comp/env
     */
    public Character getCharacterEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Short environment variable using java:comp/env
     */
    public Short getShortEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Integer environment variable using java:comp/env
     */
    public Integer getIntegerEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Long environment variable using java:comp/env
     */
    public Long getLongEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Float environment variable using java:comp/env
     */
    public Float getFloatEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get Double environment variable using java:comp/env
     */
    public Double getDoubleEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * get String environment variable using java:comp/env
     */
    public String getStringEnvVar(String arg1) throws RemoteException, NamingException;

    /**
     * bind EnvVar using java:comp/env
     */
    public void bindEnvVar(String arg1, Object newValue) throws RemoteException, NamingException;

    /**
     * rebind EnvVar using java:comp/env
     */
    public void rebindEnvVar(String arg1, Object newValue) throws RemoteException, NamingException;

    /**
     * Method that throws a RuntimeException.
     */
    public void throwRuntimeException() throws RemoteException;

    /**
     * Verifies that the instance constructor was called with the
     * proper transaction context.
     */
    public void verify_constructor() throws RemoteException;

    /**
     * Verifies that setSessionContext was called on the instance with the
     * proper transaction context.
     */
    public void verify_setContext() throws RemoteException;

    /**
     * Verifies that ejbCreate was called on the instance constructor with the
     * proper transaction context.
     */
    public void verify_ejbCreate() throws RemoteException;
}