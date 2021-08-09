/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
                configurationPid = "com.ibm.ws.security.authentication.internal.jaas.jaasLoginContextEntry",
                property = "service.vendor=IBM")
public class JAASLoginContextEntryImpl implements JAASLoginContextEntry {

    static final String KEY_SERVICE_PID = "service.pid";

    private EntryConfig config;
    private final Map<String, JAASLoginModuleConfig> loginModuleMap = new HashMap<String, JAASLoginModuleConfig>();
    private List<JAASLoginModuleConfig> loginModules = Collections.emptyList();

    public JAASLoginContextEntryImpl() {}

    @Activate
    protected void activate(EntryConfig config) {
        this.config = config;
        loginModules = new ArrayList<JAASLoginModuleConfig>(config.loginModuleRef().length);
        for (String pid : config.loginModuleRef()) {
            JAASLoginModuleConfig loginModule = loginModuleMap.get(pid);
            if (loginModule == null) {
                throw new IllegalStateException("missing login module for pid " + pid);
            }
            loginModules.add(loginModule);
        }
    }

    //SINCE THIS IS A STATIC REFERENCE DS provides enough synchronization so that we don't need to synchronize on the map.
    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    protected void setJaasLoginModuleConfig(JAASLoginModuleConfig lmc, Map<String, Object> props) {
        String pid = (String) props.get(KEY_SERVICE_PID);
        loginModuleMap.put(pid, lmc);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return config.id();
    }

    /** {@inheritDoc} */
    @Override
    public String getEntryName() {
        return config.name();
    }

    @Override
    public List<JAASLoginModuleConfig> getLoginModules() {
        return loginModules;
    }
}
