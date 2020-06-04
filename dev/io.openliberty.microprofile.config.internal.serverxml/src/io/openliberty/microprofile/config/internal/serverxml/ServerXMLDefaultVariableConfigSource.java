/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.serverxml;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.ConfigVariables;

/**
 * A ConfigSource which returns default values from variable elements in the server.xml file e.g.
 *
 * <variable name="my_variable" defaultValue="my_value" />
 *
 */
public class ServerXMLDefaultVariableConfigSource extends ServerXMLVariableConfigSource {

    private static final TraceComponent tc = Tr.register(ServerXMLDefaultVariableConfigSource.class);

    public ServerXMLDefaultVariableConfigSource() {
        super(ServerXMLConstants.SERVER_XML_DEFAULT_VARIABLE_ORDINAL, Tr.formatMessage(tc, "server.xml.default.variables.config.source"));
    }

    @Override
    protected Map<String, String> getServerXMLVariables() {
        Map<String, String> props = new HashMap<>();
        ConfigVariables configVariables = getConfigVariables();
        if (configVariables != null) {//configVariables could be null if not inside an OSGi framework (e.g. unit test) or if framework is shutting down
            props = OSGiConfigUtils.getDefaultVariablesFromServerXML(configVariables);
        }
        return props;
    }

}
