/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
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

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Singleton;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver;

/**
 * Singleton session bean that acts as a test driver in the server process for
 * the EnvEntryTest client container test.
 * <p>
 **/
@Singleton
@Remote(EnvEntryDriver.class)
public class EnvEntryAnnDriverBean extends EnvEntryDriverCommon {
    static final String BAD1BEAN_JNDI = "java:global/EnvEntryBad1App/EnvEntryBad1EJB/Bad1AnnBean";

    @Override
    public String getBAD1BEAN_JNDI() {
        return BAD1BEAN_JNDI;
    }

    static final String BAD2BEAN_JNDI = "java:global/EnvEntryBad2App/EnvEntryBad2EJB/Bad2AnnBean";

    @Override
    public String getBAD2BEAN_JNDI() {
        return BAD2BEAN_JNDI;
    }

    static final String BAD3BEAN_JNDI = "java:global/EnvEntryBad3App/EnvEntryBad3EJB/Bad3AnnBean";

    @Override
    public String getBAD3BEAN_JNDI() {
        return BAD3BEAN_JNDI;
    }

    static final String BAD4BEAN_JNDI = "java:global/EnvEntryBad4App/EnvEntryBad4EJB/Bad4AnnBean";

    @Override
    public String getBAD4BEAN_JNDI() {
        return BAD4BEAN_JNDI;
    }

    // C1a
    @Resource(name = "EnvEntry_Class_EntryName")
    Class<?> ivEnvEntry_Class;

    @Override
    public Class<?> getIvEnvEntry_Class() {
        return ivEnvEntry_Class;
    }

    // C1a-resource-env-ref
    // This method is overridden here in EnvEntryAnnDriverBean, but not in
    // EnvEntryXmlDriverBean, since it is not applicable for XML-only
    @Resource(name = "ResourceEnvRef_Class_RefName")
    Class<?> ivResourceEnvRef_Class;

    @Override
    public String verifyC1ResourceEnvRefClass() {
        assertNotNull("ivResourceEnvRef_Class not injected (null)", ivResourceEnvRef_Class);

        // use implicit env-entry for a Class field
        assertEquals("Unexpected env-entry injection.  Expected ivResourceEnvRef_Class to be an instance of EnvEntryClass.  Instead, it is an instance of "
                     + ivResourceEnvRef_Class.getName(), ivResourceEnvRef_Class.getName(), EnvEntryClass.class.getName());

        // if ok so far, do an explicit lookup:
        Class<?> lookedUp_resourceEnvRef_Class = (Class<?>) ivContext.lookup("java:comp/env/ResourceEnvRef_Class_RefName");
        assertEquals("Unexpected env-entry lookup.  Expected ivResourceEnvRef_Class to be an instance of EnvEntryClass.  Instead, it is an instance of "
                     + lookedUp_resourceEnvRef_Class.getName(), lookedUp_resourceEnvRef_Class.getName(), EnvEntryClass.class.getName());

        return "PASS";
    }

    // E1a
    @Resource(name = "EnvEntry_Enum_EntryName")
    Enum<?> ivEnvEntry_Enum;

    @Override
    public Enum<?> getIvEnvEntry_Enum() {
        return ivEnvEntry_Enum;
    }

    // Begin 661640
    // P1a
    @Resource(name = "EnvEntry_Integer_EntryName")
    Integer ivEnvEntry_Integer;

    @Override
    public Integer getIvEnvEntry_Integer() {
        return ivEnvEntry_Integer;
    }

    // P2a
    @Resource(name = "EnvEntry_Int_EntryName")
    int ivEnvEntry_Ann_Int;

    @Override
    public int getIvEnvEntry_Int() {
        return ivEnvEntry_Ann_Int;
    }

    // P3a
    @Resource(name = "EnvEntry_EnumQual_EntryName")
    EnvEntryDriver.EnvEntryEnum ivEnvEntry_EnumQual;

    @Override
    public EnvEntryDriver.EnvEntryEnum getIvEnvEntry_EnumQual() {
        return ivEnvEntry_EnumQual;
    }

    // End 661640

    // E1a-resource-env-ref
    // This method is overridden here in EnvEntryAnnDriverBean, but not in
    // EnvEntryXmlDriverBean, since it is not applicable for XML-only
    @Resource(name = "ResourceEnvRef_Enum_RefName")
    Enum<?> ivResourceEnvRef_Enum;

    @Override
    public String verifyE1ResourceEnvRefEnum() {
        assertNotNull("ivResourceEnvRef_Enum not injected (null)", ivResourceEnvRef_Enum);

        // use implicit env-entry for an Enum field
        assertEquals("Unexpected env-entry injection.  Expected ivResourceEnvRef_Enum to be " + EnvEntryEnum.EV3 + ".  Instead, it was " + ivResourceEnvRef_Enum,
                     ivResourceEnvRef_Enum,
                     EnvEntryEnum.EV3);

        // if ok so far, do an explicit lookup:
        Enum<?> lookedUp_resourceEnvRef_Enum = (Enum<?>) ivContext.lookup("java:comp/env/ResourceEnvRef_Enum_RefName");
        assertEquals("Unexpected env-entry lookup.  Expected lookedUp_resourceEnvRef_Enum to be " + EnvEntryEnum.EV3 + ".  Instead, it was " + lookedUp_resourceEnvRef_Enum,
                     lookedUp_resourceEnvRef_Enum, EnvEntryEnum.EV3);

        return "PASS";
    }

    // C3a
    // This method is overridden here in EnvEntryAnnDriverBean, but not in
    // EnvEntryXmlDriverBean, since it is not applicable for XML-only
    @Resource(name = "EnvEntry_Class_Using_Lookup", lookup = "envEntryClassBound")
    Class<?> ivEnvEntry_Lookup_Class;

    @Override
    public String verifyC3EnvEntryLookupClass() {
        assertNotNull("ivEnvEntry_Lookup_Class not injected (null)", ivEnvEntry_Lookup_Class);

        // use implicit env-entry for a Class field
        assertEquals("Unexpected env-entry injection.  Expected ivEnvEntry_Lookup_Class to be an instance of EnvEntryClass.  Instead, it is an instance of "
                     + ivEnvEntry_Lookup_Class.getName(), ivEnvEntry_Lookup_Class.getName(), EnvEntryClass.class.getName());

        // if ok so far, do an explicit lookup:
        Class<?> lookedUp_envEntry_Class = (Class<?>) ivContext.lookup("java:comp/env/EnvEntry_Class_Using_Lookup");
        assertEquals("Unexpected env-entry lookup.  Expected lookedUp_envEntry_Class to be an instance of EnvEntryClass.  Instead, it is an instance of "
                     + lookedUp_envEntry_Class.getName(), lookedUp_envEntry_Class.getName(), EnvEntryClass.class.getName());

        return "PASS";
    }

    // E5a
    // This method is overridden here in EnvEntryAnnDriverBean, but not in
    // EnvEntryXmlDriverBean, since it is not applicable for XML-only
    @Resource(name = "EnvEntry_Enum_Using_Lookup", lookup = "envEntryEnumBound")
    Enum<?> ivEnvEntry_Lookup_Enum;

    @Override
    public String verifyE5EnvEntryLookupEnum() {
        assertNotNull("ivEnvEntry_Lookup_Enum not injected (null)", ivEnvEntry_Lookup_Enum);

        // use implicit env-entry for an Enum field
        assertEquals("Unexpected env-entry injection.  Expected ivEnvEntry_Lookup_Enum to be " + EnvEntryEnum.EV3 + ".  Instead, it was " + ivEnvEntry_Lookup_Enum,
                     ivEnvEntry_Lookup_Enum, EnvEntryEnum.EV3);

        // if ok so far, do an explicit lookup:
        Enum<?> lookedUp_envEntry_Enum = (Enum<?>) ivContext.lookup("java:comp/env/EnvEntry_Enum_Using_Lookup");
        assertEquals("Unexpected env-entry lookup.  Expected lookedUp_envEntry_Enum to be " + EnvEntryEnum.EV3 + ".  Instead, it was " + lookedUp_envEntry_Enum,
                     lookedUp_envEntry_Enum, EnvEntryEnum.EV3);

        return "PASS";
    }
}