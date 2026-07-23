/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 * A ConfigSource which returns default values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" defaultValue="my_value" />
 *
 */
public class ServerXMLDefaultVariableConfigSource extends InternalConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLDefaultVariableConfigSource.class);
    private final String name;

    @Trivial
    public ServerXMLDefaultVariableConfigSource() {
        name = Tr.formatMessage(tc, "server.xml.default.variables.config.source");
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ServerXMLConstants.SERVER_XML_DEFAULT_VARIABLE_ORDINAL;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getProperties() {
        return OSGiConfigUtils.getDefaultVariablesFromServerXML();
    }

}
