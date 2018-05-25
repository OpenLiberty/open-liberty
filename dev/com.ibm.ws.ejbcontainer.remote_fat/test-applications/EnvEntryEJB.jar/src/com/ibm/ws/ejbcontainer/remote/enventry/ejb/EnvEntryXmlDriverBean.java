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

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.naming.InitialContext;

import com.ibm.ws.ejbcontainer.remote.enventry.shared.EnvEntryDriver;

/**
 * Singleton session bean that acts as a test driver in the server process for
 * the EnvEntryTest client container test.
 * <p>
 **/
@Singleton
@Remote(EnvEntryDriver.class)
public class EnvEntryXmlDriverBean extends EnvEntryDriverCommon {
    static final String BAD1BEAN_JNDI = "java:global/EnvEntryBad1App/EnvEntryBad1EJB/Bad1XmlBean";

    @Override
    public String getBAD1BEAN_JNDI() {
        return BAD1BEAN_JNDI;
    }

    static final String BAD2BEAN_JNDI = "java:global/EnvEntryBad2App/EnvEntryBad2EJB/Bad2XmlBean";

    @Override
    public String getBAD2BEAN_JNDI() {
        return BAD2BEAN_JNDI;
    }

    static final String BAD3BEAN_JNDI = "java:global/EnvEntryBad3App/EnvEntryBad3EJB/Bad3XmlBean";

    @Override
    public String getBAD3BEAN_JNDI() {
        return BAD3BEAN_JNDI;
    }

    static final String BAD4BEAN_JNDI = "java:global/EnvEntryBad4App/EnvEntryBad4EJB/Bad4XmlBean";

    @Override
    public String getBAD4BEAN_JNDI() {
        return BAD4BEAN_JNDI;
    }

    // C1x
    Class<?> ivEnvEntry_Class;

    @Override
    public Class<?> getIvEnvEntry_Class() {
        return ivEnvEntry_Class;
    }

    // E1x
    Enum<?> ivEnvEntry_Enum;

    @Override
    public Enum<?> getIvEnvEntry_Enum() {
        return ivEnvEntry_Enum;
    }

    // Begin 661640
    // P1x
    Integer ivEnvEntry_Integer;

    @Override
    public Integer getIvEnvEntry_Integer() {
        return ivEnvEntry_Integer;
    }

    // P2x
    int ivEnvEntry_Xml_Int;

    @Override
    public int getIvEnvEntry_Int() {
        return ivEnvEntry_Xml_Int;
    }

    // P3x
    EnvEntryDriver.EnvEntryEnum ivEnvEntry_EnumQual;

    @Override
    public EnvEntryDriver.EnvEntryEnum getIvEnvEntry_EnumQual() {
        return ivEnvEntry_EnumQual;
    }

    // End 661640

    @Override
    public String bindObject(String name, Object value) throws Exception {
        // bind an Object into the global JNDI namespace on the SERVER:

        InitialContext initCtx = new InitialContext();
        if (initCtx != null) {
            initCtx.rebind(name, value);
            svLogger.info("EnvEntryXmlDriverBean.bindObject bound " + value + " by name <" + name + ">");
        }

        return "bound";
    }
}