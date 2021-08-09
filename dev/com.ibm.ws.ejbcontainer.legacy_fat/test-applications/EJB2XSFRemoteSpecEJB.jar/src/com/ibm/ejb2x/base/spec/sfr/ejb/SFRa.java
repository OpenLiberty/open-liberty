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
package com.ibm.ejb2x.base.spec.sfr.ejb;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.ejb.EJBObject;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

/**
 * Remote interface for Enterprise Bean: SFRa.
 */
public interface SFRa extends EJBObject {
    /**
     * Get accessor for persistent attribute: booleanValue
     */
    public boolean getBooleanValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: booleanValue
     */
    public void setBooleanValue(boolean newBooleanValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: byteValue
     */
    public byte getByteValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: byteValue
     */
    public void setByteValue(byte newByteValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: charValue
     */
    public char getCharValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: charValue
     */
    public void setCharValue(char newCharValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: shortValue
     */
    public short getShortValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: shortValue
     */
    public void setShortValue(short newShortValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: intValue
     */
    public int getIntValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: intValue
     */
    public void setIntValue(int newIntValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: longValue
     */
    public long getLongValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: longValue
     */
    public void setLongValue(long newLongValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: floatValue
     */
    public float getFloatValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: floatValue
     */
    public void setFloatValue(float newFloatValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: doubleValue
     */
    public double getDoubleValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: doubleValue
     */
    public void setDoubleValue(double newDoubleValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: stringValue
     */
    public java.lang.String getStringValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: stringValue
     */
    public void setStringValue(java.lang.String newStringValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: integerValue
     */
    public java.lang.Integer getIntegerValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: integerValue
     */
    public void setIntegerValue(java.lang.Integer newIntegerValue) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 1:34:28 PM)
     *
     * @return java.lang.String
     * @param arg1 java.lang.String
     */
    public String method1(String arg1) throws RemoteException;

    /**
     * Test pass-by-reference/value semenatics. Key is changed and returned.
     */
    public SFRaPassBy changePassByParm(SFRaPassBy key) throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:07:00 PM)
     */
    public Serializable context_getEJBHome() throws RemoteException;

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:08:09 PM)
     */
    public SFRa context_getEJBObject() throws RemoteException;

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
     * attempt to invoke remove on bean while in a different transaction
     */
    public void beanRemoveInTransaction() throws RemoteException, RemoveException; // d171551

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemovePKeyInTransaction() throws RemoteException, RemoveException; // d171551

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemoveHandleInTransaction() throws RemoteException, RemoveException; // d171551

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
