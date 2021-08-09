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

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: SLLCMTa
 */
@SuppressWarnings("serial")
public class SLLaBean implements javax.ejb.SessionBean {
    private final static String CLASS_NAME = SLLaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String BeanName = "SLLa";

    private javax.ejb.SessionContext ivSessionCtx;

    private boolean ivCtor = false;
    private boolean ivEJBCreate = false;

    private byte[] ivCtorTID;
    private byte[] ivSetContextTID;
    private byte[] ivEJBCreateTID;

    public SLLaBean() {
        ivCtor = true;
        try {
            ivCtorTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
    }

    /**
     * getSessionContext
     */
    public javax.ejb.SessionContext getSessionContext() {
        printMsg(BeanName, "(getSessionContext)");
        return ivSessionCtx;
    }

    /**
     * setSessionContext
     */
    @Override
    public void setSessionContext(javax.ejb.SessionContext ctx) {
        printMsg(BeanName, "(setSessionContext)");
        ivSessionCtx = ctx;
        try {
            ivSetContextTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
    }

    /**
     * ejbCreate
     */
    public void ejbCreate() throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbCreate)");
        ivEJBCreate = true;
        try {
            ivEJBCreateTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg(BeanName, "(ejbActivate)");
        throw new IllegalStateException("Should never be called");
    }

    /**
     * ejbPassivate
     */
    @Override
    public void ejbPassivate() {
        printMsg(BeanName, "(ejbPassivate)");
        throw new IllegalStateException("Should never be called");
    }

    /**
     * ejbRemove
     */
    @Override
    public void ejbRemove() {
        printMsg(BeanName, "(ejbRemove)");
    }

    /**
     * Insert the method's description here.
     */
    public void printMsg(String beanName, String msg) {
        svLogger.info("       " + beanName + " : " + msg);
    }

    /**
     * method1.
     */
    public String method1(String arg1) {
        printMsg(BeanName, "----->method1 arg = " + arg1);
        return arg1;
    }

    /**
     * Test pass-by-reference/value semantics. Key is changed and returned.
     */
    public SLLaPassBy changePassByParm(SLLaPassBy pbr) {
        printMsg(BeanName, "----->changePassByParm");
        pbr.setKey(pbr.getKey2());
        pbr.setValue(pbr.getValue() + 1);
        return pbr;
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:07:00 PM)
     */
    public Object context_getEJBHome() {
        printMsg(BeanName, "-----> context_getEJBHome starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getEJBHome();
            printMsg(BeanName, "       return from getEJBHome = " + rtnObj.getClass().getName());
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getEJBHome ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Object context_getEJBLocalHome() {
        printMsg(BeanName, "-----> context_getEJBLocalHome starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getEJBLocalHome();
            printMsg(BeanName, "       return from getEJBLocalHome = " + rtnObj.getClass().getName());
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getEJBLocalHome ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:08:09 PM)
     */
    public Object context_getEJBObject() {
        printMsg(BeanName, "----->context_getEJBObject starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getEJBObject();
            printMsg(BeanName, "       return from getEJBObject = " + rtnObj.getClass().getName());
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getEJBObject ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (08/07/2002)
     */
    public Object context_getEJBLocalObject() {
        printMsg(BeanName, "----->context_getEJBLocalObject starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getEJBLocalObject();
            printMsg(BeanName, "       return from getEJBLocalObject = " + rtnObj.getClass().getName());
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getEJBLocalObject ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:09:42 PM)
     */
    public Object context_getRollbackOnly() {
        printMsg(BeanName, "-----> context_getRollbackOnly starts.");
        Object rtnObj = null;
        boolean b;
        try {
            b = ivSessionCtx.getRollbackOnly();
            printMsg(BeanName, "       return from getRollbackOnly = " + b);
            rtnObj = new Boolean(b);
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getRollbackOnly ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:10:33 PM)
     */
    public Object context_getUserTransaction() {
        printMsg(BeanName, "-----> context_getUserTransaction starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getUserTransaction().getClass().getName();
            printMsg(BeanName, "       return from getUserTransaction = " + rtnObj);
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getUserTransaction ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (09/21/2000 4:11:58 PM)
     */
    public Object context_setRollbackOnly() {
        printMsg(BeanName, "-----> context_setRollbackOnly starts.");
        Object rtnObj = null;
        try {
            ivSessionCtx.setRollbackOnly();
            printMsg(BeanName, "       return from setRollbackOnly.");
            rtnObj = Boolean.TRUE;
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_setRollbackOnly ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    @SuppressWarnings("deprecation")
    public String context_getEnvironment(String envVal) {
        java.util.Properties prop;
        String tempStr;
        printMsg(BeanName, "-----> context_getEnvironment starts.");
        try {
            prop = ivSessionCtx.getEnvironment();
            tempStr = (String) prop.get(envVal);
        } catch (Throwable t) {
            t.printStackTrace();
            printMsg(BeanName, "***** Error context_getEnvironment ends abnormally. *****");
            return null;
        }

        printMsg(BeanName, "   return value = " + tempStr);
        printMsg(BeanName, "<----- context_getEnvironment ends.");
        return tempStr;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    @SuppressWarnings("deprecation")
    public Object context_getCallerIdentity() {
        printMsg(BeanName, "-----> context_getCallerIdentity starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getCallerIdentity();
            printMsg(BeanName, "       return from getCallerIdentity.");
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getCallerIdentity ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Object context_getCallerPrincipal() {
        printMsg(BeanName, "-----> context_getCallerPrincipal starts.");
        Object rtnObj = null;
        try {
            rtnObj = ivSessionCtx.getCallerPrincipal().getName();
            printMsg(BeanName, "       return from getCallerPrincipal : " + rtnObj);
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_getCallerPrincipal ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    @SuppressWarnings("deprecation")
    public Object context_isCallerInRole(java.security.Identity identity) {
        printMsg(BeanName, "-----> context_isCallerInRole(Identity) starts.");
        Object rtnObj = null;
        boolean b;
        try {
            b = ivSessionCtx.isCallerInRole(identity);
            printMsg(BeanName, "       return from isCallerInRole =" + b);
            rtnObj = new Boolean(b);
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_isCallerInRole(Identity) ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (10/11/2000 10:51:36 AM)
     */
    public Object context_isCallerInRole(String identity) {
        printMsg(BeanName, "-----> context_isCallerInRole(String) starts.");
        Object rtnObj = null;
        boolean b;
        try {
            b = ivSessionCtx.isCallerInRole(identity);
            printMsg(BeanName, "       return from isCallerInRole =" + b);
            rtnObj = new Boolean(b);
        } catch (Throwable t) {
            printMsg(BeanName, "       Caught exception." + t);
            rtnObj = t;
        }
        printMsg(BeanName, "<----- context_isCallerInRole(String) ends.");
        return rtnObj;
    }

    /**
     * Insert the method's description here.
     * Creation date: (08/23/2002)
     */
    //     0=normal for bean managed transaction
    //    -1=unexpected exception(error)
    //    -2=IllegalStateException
    //    -3=NameNotFoundException (Expected in the test case.)
    public int context_getUserTransactionJ() {
        printMsg(BeanName, "----->context_getUserTransactionJ starts.");
        try {
            Context initCtx = new InitialContext();

            javax.transaction.UserTransaction utx = (javax.transaction.UserTransaction) initCtx.lookup(
                                                                                                       "java:comp/UserTransaction");
            utx.begin();
            printMsg(BeanName, "     Inside utx.begin()");
            utx.commit();
        } catch (javax.naming.NameNotFoundException e1) {
            e1.printStackTrace();
            printMsg(BeanName, "***** Error context_getUserTransactionJ ends NameNotFoundException as expected. *****");
            return -3;
        } catch (Exception e2) {
            e2.printStackTrace();
            printMsg(BeanName, "***** Error context_getUserTransactionJ ends abnormally. *****");
            return -1;
        }

        printMsg(BeanName, "<-----context_getUserTransactionJ ends.");
        return 0;
    }

    /**
     * get Boolean environment variable using java:comp/env
     */
    public Boolean getBooleanEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getBooleanEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Boolean value = (Boolean) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envBoolean = " + value);
        return value;
    }

    /**
     * get Byte environment variable using java:comp/env
     */
    public Byte getByteEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getByteEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Byte value = (Byte) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envByte = " + value);
        return value;
    }

    /**
     * get Character environment variable using java:comp/env
     */
    public Character getCharacterEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getCharacterEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Character value = (Character) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envCharacter = " + value);
        return value;
    }

    /**
     * get Short environment variable using java:comp/env
     */
    public Short getShortEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getShortEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Short value = (Short) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envShort = " + value);
        return value;
    }

    /**
     * get Integer environment variable using java:comp/env
     */
    public Integer getIntegerEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getIntegerEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Integer value = (Integer) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envInteger = " + value);
        return value;
    }

    /**
     * get Long environment variable using java:comp/env
     */
    public Long getLongEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getLongEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Long value = (Long) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envLong = " + value);
        return value;
    }

    /**
     * get Float environment variable using java:comp/env
     */
    public Float getFloatEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getFloatEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Float value = (Float) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envFloat = " + value);
        return value;
    }

    /**
     * get Double environment variable using java:comp/env
     */
    public Double getDoubleEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getDoubleEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        Double value = (Double) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envDouble = " + value);
        return value;
    }

    /**
     * get String environment variable using java:comp/env
     */
    public String getStringEnvVar(String arg1) throws NamingException {
        printMsg(BeanName, "----->getStringEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        String value = (String) initCtx.lookup("java:comp/env/" + arg1);
        printMsg(BeanName, "----->envString = " + value);
        return value;
    }

    /**
     * bind EnvVar using java:comp/env
     */
    public void bindEnvVar(String arg1, Object newValue) throws NamingException {
        printMsg(BeanName, "----->bindEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        initCtx.bind("java:comp/env/" + arg1, newValue);
    }

    /**
     * rebind EnvVar using java:comp/env
     */
    public void rebindEnvVar(String arg1, Object newValue) throws NamingException {
        printMsg(BeanName, "----->rebindEnvVar with parameter " + arg1);
        Context initCtx = new InitialContext();
        initCtx.rebind("java:comp/env/" + arg1, newValue);
    }

    /**
    */
    @Override
    public String toString() {
        return "SLLaBean.toString()";
    }

    /**
     * Method that throws a RuntimeException.
     */
    public void throwRuntimeException() {
        throw new RuntimeException("Expected test exception");
    }

    /**
     * Verifies that the instance constructor was called with the
     * proper transaction context.
     */
    public void verify_constructor() {
        if (!ivCtor) {
            throw new IllegalStateException("Ctor never called");
        }
        if (ivCtorTID == null) {
            svLogger.info("Ctor ran without a transaction context");
            throw new IllegalStateException("Ctor ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivCtorTID)) {
            svLogger.info("Ctor TID same as method TID");
            //Pre EJB3.0 Session Beans don't start a new LTC for lifecycle methods - see 176162
            //throw new IllegalStateException("Ctor TID same as method TID");
        }
    }

    /**
     * Verifies that setSessionContext was called on the instance with the
     * proper transaction context.
     */
    public void verify_setContext() {
        if (ivSessionCtx == null) {
            throw new IllegalStateException("setSessionContext never called");
        }
        if (ivSetContextTID == null) {
            svLogger.info("setSessionContext ran without a transaction context");
            throw new IllegalStateException("setSessionContext ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivSetContextTID)) {
            svLogger.info("setSessionContext TID same as method TID");
            //Pre EJB3.0 Session Beans don't start a new LTC for lifecycle methods - see 176162
            //throw new IllegalStateException("setSessionContext TID same as method TID");
        }
    }

    /**
     * Verifies that ejbCreate was called on the instance constructor with the
     * proper transaction context.
     */
    public void verify_ejbCreate() {
        if (!ivEJBCreate) {
            throw new IllegalStateException("ejbCreate never called");
        }
        if (ivEJBCreateTID == null) {
            svLogger.info("ejbCreate ran without a transaction context");
            throw new IllegalStateException("ejbCreate ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivEJBCreateTID)) {
            svLogger.info("ejbCreate TID same as method TID");
            //Pre EJB3.0 Session Beans don't start a new LTC for lifecycle methods - see 176162
            //throw new IllegalStateException("ejbCreate TID same as method TID");
        }
    }
}
