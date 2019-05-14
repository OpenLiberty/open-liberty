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

import static javax.ejb.TransactionAttributeType.REQUIRED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Init;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Local(StatefulPassLocal.class)
@LocalHome(StatefulPassEJBLocalHome.class)
@Remote(StatefulPassRemote.class)
@RemoteHome(StatefulPassEJBRemoteHome.class)
@Stateful(name = "NonSerializableStatefulBean")
@ExcludeDefaultInterceptors
public class NonSerializableStatefulBean {

    private String strVal = "";
    private Integer intVal = new Integer(100);
    private SerObj serObjVal = new SerObj();
    private SerObj2 serObj2Val = new SerObj2();
    private Timer timerVal = null;
    private Timer nptimerVal = null;
    private MySerObj mySerObjVal = new MySerObj();

    private int passCount = 0;
    private int actCount = 0;

    @Resource
    SessionContext sessionCtx;
    @EJB
    StatelessTimedLocal statelessTimedLocal;

    private Context localCtx;

    @Init
    @PostConstruct
    public void create() {
        strVal = null;
        intVal = null;
        serObjVal = null;
        passCount = 0;
        actCount = 0;
    }

    @Init
    public void create(String str, Integer i, SerObj serObj, SerObj2 serObj2) {
        strVal = str;
        intVal = i;
        serObjVal = serObj.clone(); // Clone serObj instead of just storing the ref
        serObj2Val = serObj2.clone(); // Clone serObj2 instead of just storing the ref
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

    @TransactionAttribute(REQUIRED)
    public int getPassivateCount() {
        return passCount;
    }

    @TransactionAttribute(REQUIRED)
    public int getActivateCount() {
        return actCount;
    }

    @TransactionAttribute(REQUIRED)
    public Integer getIntegerValue() {
        return intVal;
    }

    @TransactionAttribute(REQUIRED)
    public SerObj getSerObjValue() {
        return serObjVal;
    }

    @TransactionAttribute(REQUIRED)
    public String getStringValue() {
        return strVal;
    }

    // Required TX will passivate the bean after the call
    @TransactionAttribute(REQUIRED)
    public void setIntegerValue(Integer value) {
        intVal = value;
    }

    @TransactionAttribute(REQUIRED)
    public void setSerObjValue(SerObj value) {
        serObjVal = value;
    }

    @TransactionAttribute(REQUIRED)
    public void setStringValue(String value) {
        strVal = value;
    }

    @TransactionAttribute(REQUIRED)
    public SerObj2 getSerObj2Value() {
        return serObj2Val;
    }

    @TransactionAttribute(REQUIRED)
    public void setSerObj2Value(SerObj2 value) {
        serObj2Val = value;
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

    public void checkTimerStart() {
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the EJB ref
        if (statelessTimedLocal != null) {
            timerVal = statelessTimedLocal.createTimer(true);
            assertNotNull("Asserting timerVal is not null", timerVal);
            nptimerVal = statelessTimedLocal.createTimer(false);
            assertNotNull("Asserting nptimerVal is not null", nptimerVal);
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
        // Make sure the bean was activated and passivated once at this point
        assertEquals("Checking passivate count: " + passCount, 1, passCount);
        assertEquals("Checking activate count: " + actCount, 1, actCount);

        // Check the EJB ref
        mySerObjVal = new MySerObj("TestA", "TestTransient");
        assertEquals("Asserting mySerObj was created properly", mySerObjVal.getStrVal(), "TestA");
    }

    public void checkMySerObjEnd() {
        // Make sure the bean was activated and passivated twice at this point
        assertEquals("Checking passivate count: " + passCount, 2, passCount);
        assertEquals("Checking activate count: " + actCount, 2, actCount);

        // Check the EJB ref
        assertNotNull("Asserting mySerObjVal is not null after passivation/activation", mySerObjVal);
        mySerObjVal = new MySerObj("TestA", "TestTransient");
        assertEquals("Asserting mySerObj was created properly", mySerObjVal.getStrVal(), "TestA");
    }

    @Remove
    public void finish() {
    }

}
