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

package com.ibm.ejb2x.base.spec.sll.ejb;

import javax.naming.NamingException;

/**
 * Remote interface for Enterprise Bean: SLRa.
 */
public interface SLLa extends javax.ejb.EJBLocalObject {
    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 1:34:28 PM)
     *
     * @return java.lang.String
     * @param arg1 java.lang.String
     */
    public String method1(String arg1);

    /**
     * Test pass-by-reference/value semantics. Key is changed and returned.
     */
    public SLLaPassBy changePassByParm(SLLaPassBy key);

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:07:00 PM)
     */
    public Object context_getEJBHome();

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:08:09 PM)
     */
    public Object context_getEJBObject();

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:09:42 PM)
     */
    public Object context_getRollbackOnly();

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:10:33 PM)
     */
    public Object context_getUserTransaction();

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Object context_getCallerIdentity();

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Object context_getCallerPrincipal();

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    @SuppressWarnings("deprecation")
    public Object context_isCallerInRole(java.security.Identity identity);

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Object context_isCallerInRole(String identity);

    /**
     * Insert the method's description here.
     * Creation date: (08/23/2002)
     */
    public int context_getUserTransactionJ();

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:11:58 PM)
     */
    public Object context_setRollbackOnly();

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public String context_getEnvironment(String envVal);

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Object context_getEJBLocalHome();

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Object context_getEJBLocalObject();

    /**
     * get Boolean environment variable using java:comp/env
     */
    public Boolean getBooleanEnvVar(String arg1) throws NamingException;

    /**
     * get Byte environment variable using java:comp/env
     */
    public Byte getByteEnvVar(String arg1) throws NamingException;

    /**
     * get Character environment variable using java:comp/env
     */
    public Character getCharacterEnvVar(String arg1) throws NamingException;

    /**
     * get Short environment variable using java:comp/env
     */
    public Short getShortEnvVar(String arg1) throws NamingException;

    /**
     * get Integer environment variable using java:comp/env
     */
    public Integer getIntegerEnvVar(String arg1) throws NamingException;

    /**
     * get Long environment variable using java:comp/env
     */
    public Long getLongEnvVar(String arg1) throws NamingException;

    /**
     * get Float environment variable using java:comp/env
     */
    public Float getFloatEnvVar(String arg1) throws NamingException;

    /**
     * get Double environment variable using java:comp/env
     */
    public Double getDoubleEnvVar(String arg1) throws NamingException;

    /**
     * get String environment variable using java:comp/env
     */
    public String getStringEnvVar(String arg1) throws NamingException;

    /**
     * bind EnvVar using java:comp/env
     */
    public void bindEnvVar(String arg1, Object newValue) throws NamingException;

    /**
     * rebind EnvVar using java:comp/env
     */
    public void rebindEnvVar(String arg1, Object newValue) throws NamingException;

    /**
    */
    @Override
    public String toString();

    /**
     * Method that throws a RuntimeException.
     */
    public void throwRuntimeException();

    /**
     * Verifies that the instance constructor was called with the
     * proper transaction context.
     */
    public void verify_constructor();

    /**
     * Verifies that setSessionContext was called on the instance with the
     * proper transaction context.
     */
    public void verify_setContext();

    /**
     * Verifies that ejbCreate was called on the instance constructor with the
     * proper transaction context.
     */
    public void verify_ejbCreate();
}
