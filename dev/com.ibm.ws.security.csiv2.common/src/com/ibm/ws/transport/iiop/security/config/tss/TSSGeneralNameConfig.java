/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import java.io.IOException;

import org.omg.CSIIOP.SCS_GeneralNames;
import org.omg.CSIIOP.ServiceConfiguration;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.config.ConfigException;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Revision: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class TSSGeneralNameConfig extends TSSServiceConfigurationConfig {

    private String name;

    public TSSGeneralNameConfig(byte[] name) throws Exception {
        this.name = Util.decodeGeneralName(name);
    }

    public TSSGeneralNameConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ServiceConfiguration generateServiceConfiguration() throws ConfigException {
        try {
            ServiceConfiguration config = new ServiceConfiguration();

            config.syntax = SCS_GeneralNames.value;
            config.name = Util.encodeGeneralName(name);

            return config;
        } catch (IOException e) {
            throw new ConfigException("Unable to encode GeneralName", e);
        }
    }

    @Override
    @Trivial
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSGeneralNameConfig: [\n");
        buf.append(moreSpaces).append("name: ").append(name).append("\n");
        buf.append(spaces).append("]\n");
    }

}
