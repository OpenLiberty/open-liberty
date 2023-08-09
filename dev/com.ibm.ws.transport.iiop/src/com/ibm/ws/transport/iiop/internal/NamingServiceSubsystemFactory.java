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
package com.ibm.ws.transport.iiop.internal;

import static org.apache.yoko.orb.spi.naming.NameServiceInitializer.NS_REMOTE_ACCESS_ARG;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.List;
import java.util.Map;

import org.apache.yoko.orb.spi.naming.NameServiceInitializer;
import org.apache.yoko.orb.spi.naming.RemoteAccess;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(configurationPolicy = IGNORE, property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class NamingServiceSubsystemFactory implements SubsystemFactory {
    private static final String INITIALIZER_CLASS_NAME = NameServiceInitializer.class.getName();

    @Override
    public String getInitializerClassName(boolean endpoint) {
        return endpoint ? INITIALIZER_CLASS_NAME : null;
    }

    @Override
    public void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {
        args.add(NS_REMOTE_ACCESS_ARG);
        args.add(RemoteAccess.readOnly.name());
    }

    @Override
    public void addClientORBInitArgs(Map<String, Object> clientProps, List<String> args) {
        String nameServiceUrl = (String) clientProps.get("nameService");
        if (nameServiceUrl != null) {
            args.add("-ORBInitRef");
            args.add("NameService=" + nameServiceUrl);
        }
    }
}
