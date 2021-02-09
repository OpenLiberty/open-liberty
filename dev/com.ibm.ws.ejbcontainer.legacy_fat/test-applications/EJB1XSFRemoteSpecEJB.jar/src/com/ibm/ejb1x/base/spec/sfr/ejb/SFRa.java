/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.ejb;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.naming.NamingException;

/**
 * Remote interface for Enterprise Bean: SFRa.
 */
public interface SFRa extends javax.ejb.EJBObject {
    /**
     * Get accessor for persistent attribute: booleanValue
     */
    public boolean getBooleanValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: booleanValue
     */
    public void setBooleanValue(boolean newBooleanValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: byteValue
     */
    public byte getByteValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: byteValue
     */
    public void setByteValue(byte newByteValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: charValue
     */
    public char getCharValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: charValue
     */
    public void setCharValue(char newCharValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: shortValue
     */
    public short getShortValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: shortValue
     */
    public void setShortValue(short newShortValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: intValue
     */
    public int getIntValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: intValue
     */
    public void setIntValue(int newIntValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: longValue
     */
    public long getLongValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: longValue
     */
    public void setLongValue(long newLongValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: floatValue
     */
    public float getFloatValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: floatValue
     */
    public void setFloatValue(float newFloatValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: doubleValue
     */
    public double getDoubleValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: doubleValue
     */
    public void setDoubleValue(double newDoubleValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: stringValue
     */
    public java.lang.String getStringValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: stringValue
     */
    public void setStringValue(java.lang.String newStringValue) throws java.rmi.RemoteException;

    /**
     * Get accessor for persistent attribute: integerValue
     */
    public java.lang.Integer getIntegerValue() throws java.rmi.RemoteException;

    /**
     * Set accessor for persistent attribute: integerValue
     */
    public void setIntegerValue(java.lang.Integer newIntegerValue) throws java.rmi.RemoteException;

    /**
     *
     * @return java.lang.String
     * @param arg1 java.lang.String
     */
    public String method1(String arg1) throws java.rmi.RemoteException;

    /**
     * Test pass-by-reference/value semenatics. Key is changed and returned.
     */
    public SFRaPassBy changePassByParm(SFRaPassBy key) throws java.rmi.RemoteException;

    public Serializable context_getEJBHome() throws java.rmi.RemoteException;

    public SFRa context_getEJBObject() throws java.rmi.RemoteException;

    public Serializable context_getRollbackOnly() throws java.rmi.RemoteException;

    public Serializable context_getUserTransaction() throws java.rmi.RemoteException;

    public Serializable context_getCallerIdentity() throws java.rmi.RemoteException;

    public Serializable context_getCallerPrincipal() throws java.rmi.RemoteException;

    @SuppressWarnings("deprecation")
    public Serializable context_isCallerInRole(java.security.Identity identity) throws java.rmi.RemoteException;

    public Serializable context_isCallerInRole(String identity) throws java.rmi.RemoteException;

    public int context_getUserTransactionJ() throws java.rmi.RemoteException;

    public Serializable context_setRollbackOnly() throws java.rmi.RemoteException;

    public String context_getEnvironment(String envVal) throws java.rmi.RemoteException;

    //    public Serializable context_getEJBLocalHome() throws java.rmi.RemoteException;

    //    public Serializable context_getEJBLocalObject() throws java.rmi.RemoteException;

    /**
     * get Boolean environment variable using java:comp/env
     */
    public Boolean getBooleanEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Byte environment variable using java:comp/env
     */
    public Byte getByteEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    //    /**
    //     *   get Character environment variable using java:comp/env
    //     */
    //    public Character getCharacterEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Short environment variable using java:comp/env
     */
    public Short getShortEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Integer environment variable using java:comp/env
     */
    public Integer getIntegerEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Long environment variable using java:comp/env
     */
    public Long getLongEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Float environment variable using java:comp/env
     */
    public Float getFloatEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get Double environment variable using java:comp/env
     */
    public Double getDoubleEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * get String environment variable using java:comp/env
     */
    public String getStringEnvVar(String arg1) throws java.rmi.RemoteException, NamingException;

    /**
     * bind EnvVar using java:comp/env
     */
    public void bindEnvVar(String arg1, Object newValue) throws java.rmi.RemoteException, NamingException;

    /**
     * rebind EnvVar using java:comp/env
     */
    public void rebindEnvVar(String arg1, Object newValue) throws java.rmi.RemoteException, NamingException;

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void beanRemoveInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException; // d171551

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemovePKeyInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException; // d171551

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemoveHandleInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException; // d171551

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
     * Verifies that ejbCreate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbCreate(boolean withArgs) throws RemoteException;

    /**
     * Verifies that ejbActivate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbActivate() throws RemoteException;

    /**
     * Verifies that ejbPassivate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbPassivate() throws RemoteException;

    /**
     * Verifies that ejbRemove was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbRemove() throws RemoteException;
}
