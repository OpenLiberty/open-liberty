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
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: SFRCMTa
 */
public class SFRaBean implements javax.ejb.SessionBean {
    private static final long serialVersionUID = -4273625409430782921L;
    private final static String CLASS_NAME = SFRaBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String BeanName = "SFRa";

    private static boolean svEJBRemove;
    private static byte[] svEJBRemoveTID;

    private javax.ejb.SessionContext ivSessionCtx;

    private boolean ivCtor;
    private boolean ivEJBCreate;
    private boolean ivEJBCreateArgs;
    private boolean ivEJBActivate;
    private boolean ivEJBPassivate;

    private byte[] ivCtorTID;
    private byte[] ivSetContextTID;
    private byte[] ivEJBCreateTID;
    private byte[] ivEJBActivateTID;
    private byte[] ivEJBPassivateTID;

    public boolean booleanValue;
    public byte byteValue;
    public char charValue;
    public short shortValue;
    public int intValue;
    public long longValue;
    public float floatValue;
    public double doubleValue;
    public String stringValue;
    public Integer integerValue;

    public SFRaBean() {
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
     * Gets the booleanValue
     *
     * @return Returns a boolean
     */
    public boolean getBooleanValue() {
        printMsg(BeanName, "----->getBooleanValue = " + booleanValue);
        return booleanValue;
    }

    /**
     * Sets the booleanValue.
     *
     * @param booleanValue The booleanValue to set
     */
    public void setBooleanValue(boolean booleanValue) {
        printMsg(BeanName, "----->setBooleanValue = " + booleanValue);
        this.booleanValue = booleanValue;
    }

    /**
     * Returns the byteValue.
     *
     * @return byte
     */
    public byte getByteValue() {
        printMsg(BeanName, "----->getByteValue = " + byteValue);
        return byteValue;
    }

    /**
     * Sets the byteValue.
     *
     * @param byteValue The byteValue to set
     */
    public void setByteValue(byte byteValue) {
        printMsg(BeanName, "----->setByteValue = " + byteValue);
        this.byteValue = byteValue;
    }

    /**
     * Returns the charValue.
     *
     * @return char
     */
    public char getCharValue() {
        printMsg(BeanName, "----->getCharValue = " + charValue);
        return charValue;
    }

    /**
     * Sets the charValue.
     *
     * @param charValue The charValue to set
     */
    public void setCharValue(char charValue) {
        printMsg(BeanName, "----->setCharValue = " + charValue);
        this.charValue = charValue;
    }

    /**
     * Returns the shortValue.
     *
     * @return short
     */
    public short getShortValue() {
        printMsg(BeanName, "----->getShortValue = " + shortValue);
        return shortValue;
    }

    /**
     * Sets the shortValue.
     *
     * @param shortValue The shortValue to set
     */
    public void setShortValue(short shortValue) {
        printMsg(BeanName, "----->setShortValue = " + shortValue);
        this.shortValue = shortValue;
    }

    /**
     * Returns the intValue.
     *
     * @return int
     */
    public int getIntValue() {
        printMsg(BeanName, "----->getIntValue = " + intValue);
        return intValue;
    }

    /**
     * Sets the intValue.
     *
     * @param intValue The intValue to set
     */
    public void setIntValue(int intValue) {
        printMsg(BeanName, "----->setIntValue = " + intValue);
        this.intValue = intValue;
    }

    /**
     * Returns the longValue.
     *
     * @return long
     */
    public long getLongValue() {
        printMsg(BeanName, "----->getLongValue = " + longValue);
        return longValue;
    }

    /**
     * Sets the longValue.
     *
     * @param longValue The longValue to set
     */
    public void setLongValue(long longValue) {
        printMsg(BeanName, "----->setLongValue = " + longValue);
        this.longValue = longValue;
    }

    /**
     * Returns the floatValue.
     *
     * @return float
     */
    public float getFloatValue() {
        printMsg(BeanName, "----->getFloatValue = " + floatValue);
        return floatValue;
    }

    /**
     * Sets the floatValue.
     *
     * @param floatValue The floatValue to set
     */
    public void setFloatValue(float floatValue) {
        printMsg(BeanName, "----->setFloatValue = " + floatValue);
        this.floatValue = floatValue;
    }

    /**
     * Returns the doubleValue.
     *
     * @return double
     */
    public double getDoubleValue() {
        printMsg(BeanName, "----->getDoubleValue = " + doubleValue);
        return doubleValue;
    }

    /**
     * Sets the doubleValue.
     *
     * @param doubleValue The doubleValue to set
     */
    public void setDoubleValue(double doubleValue) {
        printMsg(BeanName, "----->setDoubleValue = " + doubleValue);
        this.doubleValue = doubleValue;
    }

    /**
     * Returns the stringValue.
     *
     * @return String
     */
    public String getStringValue() {
        printMsg(BeanName, "----->getStringValue = " + stringValue);
        return stringValue;
    }

    /**
     * Sets the stringValue.
     *
     * @param stringValue The stringValue to set
     */
    public void setStringValue(String stringValue) {
        printMsg(BeanName, "----->setStringValue = " + stringValue);
        this.stringValue = stringValue;
    }

    /**
     * Returns the integerValue.
     *
     * @return Integer
     */
    public Integer getIntegerValue() {
        printMsg(BeanName, "----->getIntegerValue = " + integerValue);
        return integerValue;
    }

    /**
     * Sets the integerValue.
     *
     * @param integerValue The integerValue to set
     */
    public void setIntegerValue(Integer integerValue) {
        printMsg(BeanName, "----->setIntegerValue = " + integerValue);
        this.integerValue = integerValue;
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
        svEJBRemove = false;
        svEJBRemoveTID = null;
    }

    /**
     * ejbCreate with long values
     */
    public void ejbCreate(boolean booleanValue, byte byteValue, char charValue, short shortValue, int intValue, long longValue, float floatValue, double doubleValue,
                          String stringValue) throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbCreate with long values) booleanValue = " + booleanValue);
        printMsg(BeanName, "                             byteValue    = " + byteValue);
        printMsg(BeanName, "                             charValue    = " + charValue);
        printMsg(BeanName, "                             intValue     = " + intValue);
        printMsg(BeanName, "                             longValue    = " + longValue);
        printMsg(BeanName, "                             floatValue   = " + floatValue);
        printMsg(BeanName, "                             doubleValue  = " + doubleValue);
        printMsg(BeanName, "                             shortValue   = " + shortValue);
        printMsg(BeanName, "                             stringValue  = " + stringValue);

        // Set my instance variables
        this.setBooleanValue(booleanValue);
        this.setByteValue(byteValue);
        this.setCharValue(charValue);
        this.setIntValue(intValue);
        this.setLongValue(longValue);
        this.setFloatValue(floatValue);
        this.setDoubleValue(doubleValue);
        this.setShortValue(shortValue);
        this.setStringValue(stringValue);

        ivEJBCreateArgs = true;
        try {
            ivEJBCreateTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
        svEJBRemove = false;
        svEJBRemoveTID = null;
    }

    /**
     * ejbPostCreate with long values
     */
    public void ejbPostCreate(boolean booleanValue, byte byteValue, char charValue, short shortValue, int intValue, long longValue, float floatValue, double doubleValue,
                              String stringValue) throws javax.ejb.CreateException {
        printMsg(BeanName, "(ejbPostCreate with long values)");
        throw new IllegalStateException("Should never be called");
    }

    /**
     * ejbActivate
     */
    @Override
    public void ejbActivate() {
        printMsg(BeanName, "(ejbActivate)");
        ivEJBActivate = true;
        try {
            ivEJBActivateTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
    }

    /**
     * ejbPassivate
     */
    @Override
    public void ejbPassivate() {
        printMsg(BeanName, "(ejbPassivate)");
        ivEJBPassivate = true;
        try {
            ivEJBPassivateTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
    }

    /**
     * ejbRemove
     */
    @Override
    public void ejbRemove() {
        printMsg(BeanName, "(ejbRemove)");
        svEJBRemove = true;
        try {
            svEJBRemoveTID = FATTransactionHelper.getTransactionId();
        } catch (IllegalStateException ex) {
            // thrown when there is no tx context
        }
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
     * Test pass-by-reference/value semenatics. Key is changed and returned.
     */
    public SFRaPassBy changePassByParm(SFRaPassBy pbr) {
        printMsg(BeanName, "----->changePassByParm");
        pbr.setKey(pbr.getKey2());
        pbr.setValue(pbr.getValue() + 1);
        return pbr;
    }

    public Serializable context_getEJBHome() {
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
        return (Serializable) rtnObj;
    }

    //    public Serializable context_getEJBLocalHome()
    //    {
    //        printMsg(BeanName, "-----> context_getEJBLocalHome starts.");
    //        Object rtnObj = null;
    //        try
    //        {
    //            rtnObj = mySessionCtx.getEJBLocalHome();
    //            printMsg( BeanName, "       return from getEJBLocalHome = " + rtnObj.getClass().getName() );
    //        } catch ( Throwable t )
    //        {
    //            printMsg( BeanName, "       Caught exception." + t );
    //            rtnObj  = t;
    //        }
    //        printMsg(BeanName,"<----- context_getEJBLocalHome ends.");
    //        return(Serializable)rtnObj;
    //    }

    public SFRa context_getEJBObject() {
        printMsg(BeanName, "----->context_getEJBObject starts.");
        SFRa rtnObj = null;

        rtnObj = (SFRa) ivSessionCtx.getEJBObject();
        printMsg(BeanName, "       return from getEJBObject = " + rtnObj.getClass().getName());

        printMsg(BeanName, "<----- context_getEJBObject ends.");
        return rtnObj;
    }

    //    public Serializable context_getEJBLocalObject()
    //    {
    //        printMsg(BeanName, "----->context_getEJBLocalObject starts.");
    //        Object rtnObj = null;
    //        try
    //        {
    //            rtnObj = mySessionCtx.getEJBLocalObject();
    //            printMsg( BeanName, "       return from getEJBLocalObject = " + rtnObj.getClass().getName() );
    //        } catch ( Throwable t )
    //        {
    //            printMsg( BeanName, "       Caught exception." + t );
    //            rtnObj  = t;
    //        }
    //        printMsg(BeanName, "<----- context_getEJBLocalObject ends.");
    //        return(Serializable)rtnObj;
    //    }

    public Serializable context_getRollbackOnly() {
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
        return (Serializable) rtnObj;
    }

    public Serializable context_getUserTransaction() {
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
        return (Serializable) rtnObj;
    }

    public Serializable context_setRollbackOnly() {
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
        return (Serializable) rtnObj;
    }

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

    @SuppressWarnings("deprecation")
    public Serializable context_getCallerIdentity() {
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
        return (Serializable) rtnObj;
    }

    public Serializable context_getCallerPrincipal() {
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
        return (Serializable) rtnObj;
    }

    @SuppressWarnings("deprecation")
    public Serializable context_isCallerInRole(java.security.Identity identity) {
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
        return (Serializable) rtnObj;
    }

    public Serializable context_isCallerInRole(String identity) {
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
        return (Serializable) rtnObj;
    }

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

    //    /**
    //     *   get Character environment variable using java:comp/env
    //     */
    //    public Character getCharacterEnvVar ( String arg1 ) throws NamingException
    //    {
    //        printMsg(BeanName,"----->getCharacterEnvVar with parameter " + arg1);
    //        Context initCtx = new InitialContext();
    //        Character value = (Character) initCtx.lookup("java:comp/env/"+arg1);
    //        printMsg(BeanName,"----->envCharacter = " + value);
    //        return value;
    //    }

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

    // d171551 Begins
    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void beanRemoveInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException {
        // Current the method is already in a global tx - TX_Required.
        SFRa ejb2 = null;
        SFRaHome fhome2 = (SFRaHome) getSessionContext().getEJBHome();
        try {
            ejb2 = fhome2.create();
        } catch (Throwable t) {
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
        printMsg(BeanName, " in beanRemoveInTransactin: created SFRa bean = " + ejb2);
        try {
            if (ejb2 != null) { // force ejb2 to enlist in the same tx as in this method call.
                ejb2.getBooleanValue();
                ejb2.remove();
                printMsg(BeanName, " in beanRemoveInTransactin: ejb2.remove complete successfully; NOT GOOD.");
            }
        } catch (RemoveException re) { // See ejb 2.0 spec 7.6 pg 79
            throw re;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
    }

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemovePKeyInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException {
        // Current the method is already in a global tx - TX_Required.
        SFRa ejb2 = null;
        SFRaHome fhome2 = (SFRaHome) getSessionContext().getEJBHome();
        try {
            ejb2 = fhome2.create();
        } catch (Throwable t) {
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
        printMsg(BeanName, " in beanRemoveInTransactin: created SFRa bean = " + ejb2);
        try {
            if (ejb2 != null) { // force ejb2 to enlist in the same tx as in this method call.
                ejb2.getBooleanValue();
                fhome2.remove(ejb2);
                printMsg(BeanName, " in beanRemoveInTransactin: ejb1.remove complete successfully; NOT GOOD.");
            }
        } catch (RemoveException re) { // See ejb 2.0 spec 6.3.2.pg 59
            throw re;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
    }

    /**
     * attempt to invoke remove on bean while in a different transaction
     */
    public void homeRemoveHandleInTransaction() throws java.rmi.RemoteException, javax.ejb.RemoveException {
        // Current the method is already in a global tx - TX_Required.
        SFRa ejb2 = null;
        SFRaHome fhome2 = (SFRaHome) getSessionContext().getEJBHome();
        try {
            ejb2 = fhome2.create();
        } catch (Throwable t) {
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
        printMsg(BeanName, " in beanRemoveInTransactin: created SFRa bean = " + ejb2);
        try {
            if (ejb2 != null) { // force ejb2 to enlist in the same tx as in this method call.
                ejb2.getBooleanValue();
                fhome2.remove(ejb2.getHandle());
                printMsg(BeanName, " in beanRemoveInTransactin: ejb1.remove complete successfully; NOT GOOD.");
            }
        } catch (RemoveException re) { // See ejb 2.0 spec 7.6 pg 79
            throw re;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new EJBException("       Caught unexpected " + t.getClass().getName());
        }
    }
    // d171551 Ends

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
    public void verify_constructor() throws RemoteException {
        if (!ivCtor) {
            throw new IllegalStateException("Ctor never called");
        }
        if (ivCtorTID == null) {
            svLogger.info("Ctor ran without a transaction context");
            // throw new IllegalStateException("Ctor ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivCtorTID)) {
            svLogger.info("Ctor TID same as method TID");
            // throw new IllegalStateException("Ctor TID same as method TID");
        }
    }

    /**
     * Verifies that setSessionContext was called on the instance with the
     * proper transaction context.
     */
    public void verify_setContext() throws RemoteException {
        if (ivSessionCtx == null) {
            throw new IllegalStateException("setSessionContext never called");
        }
        if (ivSetContextTID == null) {
            svLogger.info("setSessionContext ran without a transaction context");
            // throw new IllegalStateException("setSessionContext ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivSetContextTID)) {
            svLogger.info("setSessionContext TID same as method TID");
            // throw new IllegalStateException("setSessionContext TID same as method TID");
        }
    }

    /**
     * Verifies that ejbCreate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbCreate(boolean withArgs) throws RemoteException {
        if (withArgs) {
            if (ivEJBCreate)
                throw new IllegalStateException("Wrong ejbCreate called");
            if (!ivEJBCreateArgs)
                throw new IllegalStateException("ejbCreate(...) never called");
        } else {
            if (!ivEJBCreate)
                throw new IllegalStateException("ejbCreate never called");
            if (ivEJBCreateArgs)
                throw new IllegalStateException("Wrong ejbCreate(...) called");
        }

        if (ivEJBCreateTID == null) {
            svLogger.info("ejbCreate ran without a transaction context");
            // throw new IllegalStateException("ejbCreate ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivEJBCreateTID)) {
            svLogger.info("ejbCreate TID same as method TID");
            // throw new IllegalStateException("ejbCreate TID same as method TID");
        }
    }

    /**
     * Verifies that ejbActivate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbActivate() throws RemoteException {
        if (!ivEJBActivate) {
            throw new IllegalStateException("ejbActivate never called");
        }
        if (ivEJBActivateTID == null) {
            svLogger.info("ejbActivate ran without a transaction context");
            // throw new IllegalStateException("Ctor ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivEJBActivateTID)) {
            svLogger.info("ejbActivate TID same as method TID");
            // throw new IllegalStateException("Ctor TID same as method TID");
        }
    }

    /**
     * Verifies that ejbPassivate was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbPassivate() throws RemoteException {
        if (!ivEJBPassivate) {
            throw new IllegalStateException("ejbPassivate never called");
        }
        if (ivEJBPassivateTID == null) {
            svLogger.info("ejbPassivate ran without a transaction context");
            // throw new IllegalStateException("Ctor ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(ivEJBPassivateTID)) {
            svLogger.info("ejbPassivate TID same as method TID");
            // throw new IllegalStateException("Ctor TID same as method TID");
        }
    }

    /**
     * Verifies that ejbRemove was called on the instance with the
     * proper transaction context.
     */
    public void verify_ejbRemove() throws RemoteException {
        if (!svEJBRemove) {
            throw new IllegalStateException("ejbRemove never called");
        }
        if (svEJBRemoveTID == null) {
            svLogger.info("ejbRemove ran without a transaction context");
            // throw new IllegalStateException("Ctor ran without a transaction context");
        } else if (FATTransactionHelper.isSameTransactionId(svEJBRemoveTID)) {
            svLogger.info("ejbRemove TID same as method TID");
            // throw new IllegalStateException("Ctor TID same as method TID");
        }
    }
}
