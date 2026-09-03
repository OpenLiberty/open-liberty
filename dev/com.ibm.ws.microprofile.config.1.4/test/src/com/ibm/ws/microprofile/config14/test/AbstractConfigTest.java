/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Before;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.microprofile.config14.impl.Config14ProviderResolverImpl;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import io.openliberty.microprofile.config.internal.serverxml.OSGiConfigUtils;
import io.openliberty.microprofile.config.internal.serverxml.TestServiceCaller;

/**
 *
 */
public abstract class AbstractConfigTest {

    @Before
    public void before() throws Exception {
        setOSGiCallers(
            new TestServiceCaller<CDIService>(CDIService.class),
            new TestServiceCaller<ConfigVariables>(ConfigVariables.class),
            new TestServiceCaller<VariableRegistry>(VariableRegistry.class));
        ConfigProviderResolver.setInstance(new Config14ProviderResolverImpl());
    }

    @After
    public void after() throws Exception {
        ((Config14ProviderResolverImpl) ConfigProviderResolver.instance()).shutdown();
        ConfigProviderResolver.setInstance(null);
        setOSGiCallers(
            new ServiceCaller<CDIService>(OSGiConfigUtils.class, CDIService.class),
            new ServiceCaller<ConfigVariables>(OSGiConfigUtils.class, ConfigVariables.class),
            new ServiceCaller<VariableRegistry>(OSGiConfigUtils.class, VariableRegistry.class));
    }

    private static void setOSGiCallers(ServiceCaller<CDIService> cdiCaller,
                                       ServiceCaller<ConfigVariables> configVarsCaller,
                                       ServiceCaller<VariableRegistry> varRegistryCaller) throws Exception {
        Field cdiField = OSGiConfigUtils.class.getDeclaredField("cdiServiceCaller");
        cdiField.setAccessible(true);
        cdiField.set(null, cdiCaller);

        Field configVarsField = OSGiConfigUtils.class.getDeclaredField("configVariablesCaller");
        configVarsField.setAccessible(true);
        configVarsField.set(null, configVarsCaller);

        Field varRegField = OSGiConfigUtils.class.getDeclaredField("variableRegistryCaller");
        varRegField.setAccessible(true);
        varRegField.set(null, varRegistryCaller);
    }

}
