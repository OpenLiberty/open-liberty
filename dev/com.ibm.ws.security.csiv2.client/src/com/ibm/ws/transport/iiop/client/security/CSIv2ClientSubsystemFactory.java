/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.client.security;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.security.csiv2.client.config.css.ClientContainerConfigHelper;
import com.ibm.ws.transport.iiop.security.AbstractCsiv2SubsystemFactory;
import com.ibm.ws.transport.iiop.security.ClientPolicy;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.ssl.yoko.SocketFactory;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(configurationPolicy = IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=3" })
public class CSIv2ClientSubsystemFactory extends AbstractCsiv2SubsystemFactory implements SubsystemFactory {
    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        // TODO: Determine if system.RMI_OUTBOUND should be created and used for outbound.
        CSSConfig cssConfig = new ClientContainerConfigHelper(defaultAlias).getCSSConfig(properties);
        ClientPolicy clientPolicy = new ClientPolicy(cssConfig);
        return clientPolicy;
    }

    @Override
    public void addClientORBInitArgs(Map<String, Object> clientProperties, List<String> args) {
        args.add("-IIOPconnectionHelper");
        args.add(SocketFactory.class.getName());
    }

    @Override
    protected Set<String> extractSslRefs(Map<String, Object> properties, List<IIOPEndpoint> endpoints) {
        return new ClientContainerConfigHelper(defaultAlias).extractSslRefs(properties);
    }
}
