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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.osgi.framework.Constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.microprofile.config.internal.common.ConfigException;

/**
 * Assorted osgi based utility methods
 */
public class OSGiConfigUtils {

    private static final TraceComponent tc = Tr.register(OSGiConfigUtils.class);
    /** Specifies the Factory PID attribute name in the Configuration. This will be used when searching the config. */
    private static final String CFG_SERVICE_FACTORY_PID = "service.factoryPid";
    /** Specifies the AppProperties value for a Factory PID sought in the Configuration */
    private static final String CFG_APP_PROPERTIES = "com.ibm.ws.appconfig.appProperties";
    /** Specifies the Parent PID attribute name in the Configuration. This will be used when searching the config. */
    private static final String CFG_CONFIG_PARENT_PID = "config.parentPID";
    /** Specifies the nested AppProperties Property value for a Factory PID sought in the Configuration */
    private static final String CFG_APP_PROPERTIES_PROPERTY = "com.ibm.ws.appconfig.appProperties.property";

    //Not final as these are replaced by mock classes in unittests
    private static ServiceCaller<CDIService> cdiServiceCaller = new ServiceCaller<CDIService>(OSGiConfigUtils.class, CDIService.class);
    private static ServiceCaller<ConfigVariables> configVariablesCaller = new ServiceCaller<ConfigVariables>(OSGiConfigUtils.class, ConfigVariables.class);
    private static ServiceCaller<VariableRegistry> variableRegistryCaller = new ServiceCaller<VariableRegistry>(OSGiConfigUtils.class, VariableRegistry.class);

    /**
     * Get the j2ee name of the application. If the ComponentMetaData is available on the thread then that can be used, otherwise fallback
     * to asking the CDIService for the name ... the CDI context ID is the same as the j2ee name.
     *
     * If CDI is not enabled then the CDIService will not be available and this method may return null
     *
     * @return the application name or null
     */
    static String getApplicationName() {
        String applicationName = null;
        if (FrameworkState.isValid()) {
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

            if (cmd == null) {
                //if the component metadata is null then we're probably running in the CDI startup sequence so try asking CDI for the application
                //During CDI startup, the CDIService knows which application it is currently working with so we can ask it!
                //CDI may not be enabled, in which case the service will not be found and this method will just return an empty optional, which will become null to match the existing expectations.

                applicationName = cdiServiceCaller.run(CDIService::getCurrentApplicationContextID).orElse(null);
            } else {
                applicationName = cmd.getJ2EEName().getApplication();
                if (applicationName == null) {
                    throw new ConfigException(Tr.formatMessage(tc, "no.application.name.CWMCG0201E"));
                }
            }
        }
        return applicationName;
    }

    static <R> R runPrivilegedIfNeeded(Supplier<R> supplier) {
        if (System.getSecurityManager() == null) {
            return supplier.get();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<R>) () -> supplier.get());
        }
    }

    /**
     * @return a Map that represents the name/value pairs of <variable name="x" value="y"> elements in the server.xml
     */
    static Map<String, String> getVariablesFromServerXML() {
        Map<String, String> preprocessedVariables = configVariablesCaller.run(ConfigVariables::getUserDefinedVariables).orElse(Collections.emptyMap());

        /*
         * In the past we had special treatment for variables in the format ${env.X}, they would be evaluated to an environment variable of just {X}.
         * This special case was not included in the microprofile config specification, so it was removed from liberty docs but code remains for
         * backwards compatibility.
         *
         * However this backwards compatibility intentionally does not extend to our microprofile config implementation. I am documenting this here because
         * I previously had a customer ask about this, and implemented code here to support it before learning that Liberty's mpConfig Impl
         * actually shouldn't treat ${env.X} as a special case.
         */
        return preprocessedVariables;
    }

    /**
     * @return a Map that represents the name/value pairs of <variable name="x" defaultValue="y"> elements in the server.xml
     */
    static Map<String, String> getDefaultVariablesFromServerXML() {
        return configVariablesCaller.run(ConfigVariables::getUserDefinedVariableDefaults).orElse(Collections.emptyMap());
    }

    /**
     * Get the internal OSGi identifier for the Application with the given name
     *
     * @param applicationName The application name to look for
     * @return The application pid
     */
    static String getApplicationPID(String applicationName) {
        String filter = FilterUtils.createPropertyFilter("name", applicationName);

        return ServiceCaller.currentProperty(OSGiConfigUtils.class, Application.class, Constants.SERVICE_PID, filter)
                        .map(String.class::cast)
                        .orElse(null);
    }

}
