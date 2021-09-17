/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;

/**
 * A ConfigSource which returns default values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" defaultValue="my_value" />
 *
 */
public class ServerXMLDefaultVariableConfigSource extends ServerXMLVariableConfigSource {

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
    protected Map<String, String> getServerXMLVariables() {
        ConfigVariables configVariables = getConfigVariables();
        if (configVariables != null) {//configVariables could be null if not inside an OSGi framework (e.g. unit test) or if framework is shutting down
            // We must request all Liberty variables, rather than all user defined defaults,
            // since we want to know the default values of any variables which have been overwritten.
            HashMap<String, String> result = new HashMap<>();
            for (LibertyVariable var : configVariables.getAllLibertyVariables()) {
                if (var.getDefaultValue() != null) {
                    result.put(var.getName(), var.getDefaultValue());
                }
            }
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

}
