/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 * A ConfigSource which returns values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" value="my_value" />
 *
 */
public class ServerXMLVariableConfigSource extends InternalConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLVariableConfigSource.class);
    private final String name;

    @Trivial
    public ServerXMLVariableConfigSource() {
        name = Tr.formatMessage(tc, "server.xml.variables.config.source");
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ServerXMLConstants.SERVER_XML_VARIABLE_ORDINAL;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        return OSGiConfigUtils.runPrivilegedIfNeeded(OSGiConfigUtils::getVariablesFromServerXML);
    }

}
