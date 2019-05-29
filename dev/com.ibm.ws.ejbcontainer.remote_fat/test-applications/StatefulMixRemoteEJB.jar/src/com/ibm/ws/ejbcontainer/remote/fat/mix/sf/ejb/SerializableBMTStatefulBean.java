/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import static javax.ejb.TransactionManagementType.BEAN;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Init;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.Timer;
import javax.ejb.TransactionManagement;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

@Local(StatefulPassBMTLocal.class)
@Remote(StatefulPassBMTRemote.class)
@Stateful(name = "SerializableBMTStatefulBean")
@TransactionManagement(BEAN)
public class SerializableBMTStatefulBean implements Serializable {
    private final static String CLASSNAME = SerializableBMTStatefulBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final long serialVersionUID = 1L;

    private String strVal = "";
    private Integer intVal = new Integer(100);
    private SerObj serObjVal = new SerObj();
    private Timer timerVal = null;
    private Timer nptimerVal = null;
    private SerObj2 serObj2Val = new SerObj2();

    private int passCount = 0;
    private int actCount = 0;
    private int txStatus = STATUS_NO_TRANSACTION;

    @Resource
    SessionContext sessionCtx;
    @Resource
    UserTransaction userTran;
    @EJB
    StatelessTimedLocal statelessTimedLocal;

    private Context localCtx;

    @Init
    public void create() {
        strVal = null;
        intVal = null;
        serObjVal = null;
        passCount = 0;
        actCount = 0;
    }

    @PrePassivate
    public void passivate() {
        passCount++;
    }

    @PostActivate
    public void activate() {
        actCount++;
    }

    public int getPassivateCount() {
        return passCount;
    }

    public int getActivateCount() {
        return actCount;
    }

    public Integer getIntegerValue() {
        return intVal;
    }

    public void setSerObj2Value(SerObj2 value) {
        serObj2Val = value;
    }

    public SerObj2 getSerObj2Value() {
        return serObj2Val;
    }

    public SerObj getSerObjValue() {
        return serObjVal;
    }

    public String getStringValue() {
        svLogger.info("Getting string value:" + strVal);
        return strVal;
    }

    // Required TX will passivate the bean after the call
    public void setIntegerValue(Integer value) {
        intVal = value;
    }

    public void setSerObjValue(SerObj value) {
        serObjVal = value;
    }

    public void setStringValue(String value) {
        strVal = value;
        svLogger.info("Setting string value:" + strVal);
    }

    @Remove
    public void finish() {
    }

    public void checkSessionContextStart() {
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the env-entry
        if (sessionCtx != null) {
            String str = (String) sessionCtx.lookup("java:comp/env/EntryA");
            assertEquals("Checking environment entry through sessionCtx", str, "EntryAValue");
        } else {
            fail("Injected SessionContext is null");
        }
    }

    public void checkSessionContextEnd() {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Make sure the ENC entry still exists
        if (sessionCtx != null) {
            String str = (String) sessionCtx.lookup("java:comp/env/EntryA");
            assertEquals("Checking environment entry through sessionCtx", str, "EntryAValue");
        } else {
            fail("Restored SessionContext is null");
        }
    }

    public void checkENCEntryStart() throws NamingException {
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the env-entry
        InitialContext ctx = new InitialContext();
        localCtx = (Context) ctx.lookup("java:comp/env/ent");

        String str = (String) localCtx.lookup("EntryB");
        assertEquals("Checking environment entry through localCtx", str, "EntryBValue");
    }

    public void checkENCEntryEnd() throws NamingException {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Make sure the ENC entry still exists
        if (localCtx != null) {
            String str = (String) localCtx.lookup("EntryB");
            assertEquals("Checking environment entry through localCtx", str, "EntryBValue");
        } else {
            fail("Restored Local Context is null");
        }
    }

    public void checkUserTranStart() {
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the env-entry
        if (userTran != null) {
            try {
                txStatus = userTran.getStatus();
                svLogger.info("Got transaction status: " + txStatus);
            } catch (SystemException e) {
                fail("Failed to set transaction timeout to 99: " + e.getMessage());
            }
        } else {
            fail("checkUserTranStart: Injection of UserTransaction failed");
        }
    }

    public void checkUserTranEnd() {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Make sure the ENC entry still exists
        if (userTran != null) {
            try {
                int status = userTran.getStatus();
                assertEquals("Checking status of user transaction: " + status, status, txStatus);
            } catch (SystemException e) {
                fail("Failed to get transaction status: " + e.getMessage());
            }
        } else {
            fail("checkUserTranEnd: Injection of UserTransaction failed");
        }
    }

    public void checkTimerStart() {
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the EJB ref
        if (statelessTimedLocal != null) {
            timerVal = statelessTimedLocal.createTimer(true);
            assertNotNull("Asserting timerVal is not null", timerVal);
            nptimerVal = statelessTimedLocal.createTimer(false);
            assertNotNull("Asserting timerVal is not null", nptimerVal);
        } else {
            fail("statelessTimedLocal was not injected.  ref is null");
        }
    }

    public void checkTimerEnd() {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Check the EJB ref
        assertNotNull("Asserting timerVal is not null after passivation/activation", timerVal);
        assertEquals("Checking timerVal: ", timerVal.getInfo(), "StatelessTimedBean");
        timerVal.cancel();

        assertNotNull("Asserting nptimerVal is not null after passivation/activation", nptimerVal);
        assertEquals("Checking timerVal: ", nptimerVal.getInfo(), "StatelessTimedBean");
        nptimerVal.cancel();
    }

    public void checkMySerObjStart() {
    }

    public void checkMySerObjEnd() {
    }

}
