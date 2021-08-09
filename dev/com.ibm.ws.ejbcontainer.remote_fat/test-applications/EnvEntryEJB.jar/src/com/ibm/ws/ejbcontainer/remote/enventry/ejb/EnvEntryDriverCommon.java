/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.enventry.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.Bad;
import com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver;

public abstract class EnvEntryDriverCommon implements EnvEntryDriver {
    private static final String CLASS_NAME = EnvEntryDriverCommon.class.getName();
    protected static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    public abstract String getBAD1BEAN_JNDI();

    public abstract String getBAD2BEAN_JNDI();

    public abstract String getBAD3BEAN_JNDI();

    public abstract String getBAD4BEAN_JNDI();

    // Do an instance level injection of the SessionContext
    // This will cause the SessionContext, which contains the environment for this bean,
    // to get loaded into the variable.
    @Resource
    SessionContext ivContext;

    // C1-resource-env-ref
    // Dummy implementation here.  Expected to be overridden by
    // EnvEntryAnnDriverBean, but not EnvEntryXmlDriverBean
    @Override
    public String verifyC1ResourceEnvRefClass() {
        return "Unexpected call to verifyC1ResourceEnvRefClass";
    }

    public abstract Class<?> getIvEnvEntry_Class();

    @Override
    public void verifyC1EnvEntryClass() {
        assertNotNull("ivEnvEntry_Class not injected (null)", getIvEnvEntry_Class());

        // use implicit env-entry for a Class field
        String eeName = getIvEnvEntry_Class().getName();
        assertEquals("Unexpected env-entry injection.  Expected ivEnvEntry_Class to be an instance of EnvEntryClass.  Instead, it is an instance of " + eeName, eeName,
                     EnvEntryClass.class.getName());

        // if ok so far, do an explicit lookup:
        Class<?> lookedUp_envEntry_Class = (Class<?>) ivContext.lookup("java:comp/env/EnvEntry_Class_EntryName");
        String eeLookupName = lookedUp_envEntry_Class.getName();
        assertEquals("Unexpected env-entry lookup.  Expected ivEnvEntry_Class to be an instance of EnvEntryClass.  Instead, it is an instance of " + eeLookupName, eeLookupName,
                     EnvEntryClass.class.getName());
    }

    @SuppressWarnings("unused")
    @Override
    public void verifyC2EnvEntryNonExistingClass() throws Exception {
        try {
            Bad bad = (Bad) new InitialContext().lookup(getBAD1BEAN_JNDI());
        } catch (Exception e) {
            svLogger.info("Caught exception as expected");
            return;
        }

        fail("Should not have been able to look up Bad1 bean");
    }

    // Dummy implementation here.  Expected to be overridden by
    // EnvEntryAnnDriverBean, but not EnvEntryXmlDriverBean
    @Override
    public String verifyC3EnvEntryLookupClass() {
        return "Unexpected call to EnvEntryDriverCommon.verifyC3EnvEntryLookupClass";
    }

    // Dummy implementation here.  Expected to be overridden by
    // EnvEntryAnnDriverBean, but not EnvEntryXmlDriverBean
    @Override
    public String verifyE5EnvEntryLookupEnum() {
        return "Unexpected call to verifyE5EnvEntryLookupEnum";
    }

    // Dummy implementation here.  Expected to be overridden by
    // EnvEntryAnnDriverBean, but not EnvEntryXmlDriverBean
    @Override
    public String verifyE1ResourceEnvRefEnum() {
        return "Unexpected call to verifyE1ResourceEnvRefEnum";
    }

    public abstract Enum<?> getIvEnvEntry_Enum();

    @Override
    public void verifyE1EnvEntryEnum() {
        assertNotNull("ivEnvEntry_Enum not injected (null)", getIvEnvEntry_Enum());

        // use implicit env-entry for an Enum field
        assertEquals("Unexpected env-entry injection.  Expected ivEnvEntry_Enum to be " + EnvEntryEnum.EV2 + ".  Instead, it was " + getIvEnvEntry_Enum(), getIvEnvEntry_Enum(),
                     EnvEntryEnum.EV2);

        // if ok so far, do an explicit lookup:
        Enum<?> lookedUp_envEntry_Enum = (Enum<?>) ivContext.lookup("java:comp/env/EnvEntry_Enum_EntryName");
        assertEquals("Unexpected env-entry lookup.  Expected lookedUp_envEntry_Enum to be " + EnvEntryEnum.EV2 + ".  Instead, it was " + lookedUp_envEntry_Enum,
                     lookedUp_envEntry_Enum,
                     EnvEntryEnum.EV2);
    }

    // Begin 661640
    public abstract Integer getIvEnvEntry_Integer();

    @Override
    public void verifyP1EnvEntryInteger() {
        assertNotNull("ivEnvEntry_Integer not injected (null)", getIvEnvEntry_Integer());

        // use implicit env-entry for an Integer field
        assertTrue("Unexpected env-entry injection.  Expected ivEnvEntry_Integer to be 451. Instead, it was " + getIvEnvEntry_Integer(), getIvEnvEntry_Integer() == 451);

        // if ok so far, do an explicit lookup:
        Integer lookedUp_envEntry_Integer = (Integer) ivContext.lookup("java:comp/env/EnvEntry_Integer_EntryName");
        assertTrue("Unexpected env-entry lookup.  Expected lookedUp_envEntry_Integer to be 451.  Instead, it was " + lookedUp_envEntry_Integer, lookedUp_envEntry_Integer == 451);
    }

    public abstract int getIvEnvEntry_Int();

    @Override
    public void verifyP2EnvEntryInt() {
        assertTrue("ivEnvEntry_Int not injected", getIvEnvEntry_Int() != 0);

        // use implicit env-entry for an int field
        assertTrue("Unexpected env-entry injection.  Expected ivEnvEntry_Int to be 452. Instead, it was " + getIvEnvEntry_Int(), getIvEnvEntry_Int() == 452);

        // if ok so far, do an explicit lookup:
        int lookedUp_envEntry_Int = (Integer) ivContext.lookup("java:comp/env/EnvEntry_Int_EntryName");
        assertTrue("Unexpected env-entry lookup.  Expected lookedUp_envEntry_Int to be 452.  Instead, it was " + lookedUp_envEntry_Int, lookedUp_envEntry_Int == 452);
    }

    public abstract EnvEntryDriver.EnvEntryEnum getIvEnvEntry_EnumQual();

    @Override
    public void verifyP3EnvEntryEnumQual() {
        assertNotNull("ivEnvEntry_EnumQual not injected (null)", getIvEnvEntry_EnumQual());

        // use implicit env-entry for a fully-qualified Enum field
        assertEquals("Unexpected env-entry injection.  Expected ivEnvEntry_EnumQual to be " + EnvEntryEnum.EV1 + ".  Instead, it was " + getIvEnvEntry_EnumQual(),
                     getIvEnvEntry_EnumQual(), EnvEntryEnum.EV1);

        // if ok so far, do an explicit lookup:
        EnvEntryDriver.EnvEntryEnum lookedUp_envEntry_EnumQual = (EnvEntryDriver.EnvEntryEnum) ivContext.lookup("java:comp/env/EnvEntry_EnumQual_EntryName");
        assertEquals("Unexpected env-entry lookup.  Expected lookedUp_envEntry_EnumQual to be " + EnvEntryEnum.EV1 + ".  Instead, it was " + lookedUp_envEntry_EnumQual,
                     lookedUp_envEntry_EnumQual, EnvEntryEnum.EV1);
    }

    // End 661640

    @SuppressWarnings("unused")
    @Override
    public void verifyE2EnvEntryNonExistingEnumType() throws Exception {
        try {
            Bad bad = (Bad) new InitialContext().lookup(getBAD2BEAN_JNDI());
        } catch (Exception e) {
            svLogger.info("Caught exception as expected");
            return;
        }

        fail("Should not have been able to look up Bad2 bean");
    }

    @SuppressWarnings("unused")
    @Override
    public void verifyE3EnvEntryNonExistingEnumValue() throws Exception {
        try {
            Bad bad = (Bad) new InitialContext().lookup(getBAD3BEAN_JNDI());
        } catch (Exception e) {
            svLogger.info("Caught exception as expected");
            return;
        }

        fail("Should not have been able to look up Bad3 bean");
    }

    @SuppressWarnings("unused")
    @Override
    public void verifyE4EnvEntryExistingNonEnumNonClass() throws Exception {
        try {
            Bad bad = (Bad) new InitialContext().lookup(getBAD4BEAN_JNDI());
        } catch (Exception e) {
            svLogger.info("Caught exception as expected");
            return;
        }

        fail("Should not have been able to look up Bad4 bean");
    }

    // Dummy implementation here.  Expected to be overridden by
    // EnvEntryXmlDriverBean, but not EnvEntryAnnDriverBean
    @Override
    public String bindObject(String name, Object value) throws Exception {
        return "Unexpected call to bindObject";
    }
}