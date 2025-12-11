/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.security.config.tss;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.omg.CSIIOP.ServiceConfiguration;

import com.ibm.ws.transport.iiop.security.config.ConfigException;

/**
 *
 */
public class TSSUnknownServiceConfigurationConfig extends TSSServiceConfigurationConfig {

    private final int syntax;
    private final byte[] name;

    /**
     * @param syntax2
     * @param name
     */
    public TSSUnknownServiceConfigurationConfig(int syntax, byte[] name) {
        this.syntax = syntax;
        this.name = Arrays.copyOf(name, name.length);
    }

    /** {@inheritDoc} */
    @Override
    public ServiceConfiguration generateServiceConfiguration() throws ConfigException {
        ServiceConfiguration config = new ServiceConfiguration();

        config.syntax = syntax;
        config.name = name;

        return config;
    }

    /** {@inheritDoc} */
    @Override
    void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("TSSUnknownServiceConfigurationConfig: [\n");
        buf.append(moreSpaces).append("syntax VMCID : ").append(Integer.toHexString(syntax >> 12)).append("\n");
        buf.append(moreSpaces).append("syntax organization-scoped syntax identifier : ").append(Integer.toHexString(syntax & 0XFFF)).append("\n");
        buf.append(moreSpaces).append("name: ").append(Arrays.asList(name)).append(" (").append(new String(name, StandardCharsets.ISO_8859_1)).append(")\n");
        buf.append(spaces).append("]\n");
    }

    public int getSyntax() {
        return syntax;
    }

    public byte[] getName() {
        return Arrays.copyOf(name, name.length);
    }
}
