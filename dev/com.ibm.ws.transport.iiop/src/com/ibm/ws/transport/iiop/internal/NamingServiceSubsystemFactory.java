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
package com.ibm.ws.transport.iiop.internal;

import static org.apache.yoko.orb.spi.naming.NameServiceInitializer.NS_REMOTE_ACCESS_ARG;

import java.util.List;
import java.util.Map;

import org.apache.yoko.orb.spi.naming.NameServiceInitializer;
import org.apache.yoko.orb.spi.naming.RemoteAccess;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(service = SubsystemFactory.class,
                configurationPolicy = ConfigurationPolicy.IGNORE,
                property = { "service.vendor=IBM", "service.ranking:Integer=1" })
public class NamingServiceSubsystemFactory extends SubsystemFactory {
    private static final String INITIALIZER_CLASS_NAME = NameServiceInitializer.class.getName();

    @Override
    public String getInitializerClassName(boolean endpoint) {
        if (endpoint) {
            return INITIALIZER_CLASS_NAME;
        }
        return null;
    }

    @Override
    public void addTargetORBInitArgs(Map<String, Object> targetProperties, List<String> args) {
        args.add(NS_REMOTE_ACCESS_ARG);
        args.add(RemoteAccess.readOnly.name());
    }

    /** {@inheritDoc} */
    @Override
    public void addClientORBInitArgs(Map<String, Object> clientProps, List<String> args) {
        String nameServiceUrl = (String) clientProps.get("nameService");
        if (nameServiceUrl != null) {
            args.add("-ORBInitRef");
            args.add("NameService=" + nameServiceUrl);
        }
    }

}
