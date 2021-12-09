/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.local;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public final class LocalConnectorActivator {

    /**  */
    private static final String JMX_LOCAL_ADDRESS = "com.ibm.ws.jmx.local.address";
    private volatile WsLocationAdmin locationService;
    private volatile WsResource localJMXAddressWorkareaFile;
    private volatile WsResource localJMXAddressStateFile;
    private static final TraceComponent tc = Tr.register(LocalConnectorActivator.class);

    private static final class LocalConnectorHelper {

        private static final String LOCAL_CONNECTOR_ADDRESS_PROPERTY = "com.sun.management.jmxremote.localConnectorAddress";
        private static final String LOCAL_CONNECTOR_ADDRESS = initConnectorAddress();

        private static final String RMI_SERVER_HOSTNAME_PROPERTY = "java.rmi.server.hostname";
        private static final String LOOPBACK_ADDRESS = "127.0.0.1";

        /*
         * Manually starts the JMX management agent and returns the local connector
         * address if successful.
         */
        private static String initConnectorAddress() {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    try {
                        // If the RMI hostname property has not been set yet, set it to
                        // the loopback address. See RTC defect 91188 for more information.
                        if (System.getProperty(RMI_SERVER_HOSTNAME_PROPERTY) == null) {
                            System.setProperty(RMI_SERVER_HOSTNAME_PROPERTY, LOOPBACK_ADDRESS);
                        }

                        String localConnectorAddress = null;
                        // Start the JMX agent and retrieve the local connector address.
                        if (JavaInfo.majorVersion() < 9 ||
                            (JavaInfo.majorVersion() >= 9 && !Boolean.getBoolean("jdk.attach.allowAttachSelf"))) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Using old code path for self attach using JDK internal APIs",
                                         JavaInfo.majorVersion(),
                                         Boolean.getBoolean("jdk.attach.allowAttachSelf"));
                            // Use JDK internal APIs for Java 8 and older OR if the server was launched in a way that bypassed
                            // the wlp/bin/server script and therefore did not get the -Djdk.attach.allowAttachSelf=true prop set
                            // TODO: Also go down this path if j2sec is enabled, because the proper API path has permission issues
                            //       which are being looked at under https://github.com/eclipse/openj9/issues/6119

                            // Use reflection to invoke...
                            // Agent.agentmain(null);
                            // Properties props = VMSupport.getAgentProperties()
                            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                            Class<?> Agent = JavaInfo.majorVersion() < 9 //
                                            ? Class.forName("sun.management.Agent", true, systemClassLoader) //
                                            : Class.forName("jdk.internal.agent.Agent", true, systemClassLoader);
                            Agent.getMethod("agentmain", String.class).invoke(null, (Object) null);

                            localConnectorAddress = System.getProperty(LOCAL_CONNECTOR_ADDRESS_PROPERTY);
                            if (localConnectorAddress == null) {
                                Class<?> VMSupport = JavaInfo.majorVersion() < 9 //
                                                ? Class.forName("sun.misc.VMSupport", true, systemClassLoader) //
                                                : Class.forName("jdk.internal.vm.VMSupport", true, systemClassLoader);
                                Properties props = (Properties) VMSupport.getMethod("getAgentProperties").invoke(null);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Attempting to retrieve the connector address from agent properties.");
                                }
                                localConnectorAddress = props.getProperty(LOCAL_CONNECTOR_ADDRESS_PROPERTY);
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(tc, "Using public API code path for self attach");
                            // This code path uses public Java APIs and is the preferred approach for self-attach (used for JDK 9+)

                            // Manually initialize the PlatformMBeanServer here where we have access to com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilder.
                            // If we do not manually initialize here, Hotspot will try to do so using JDK classloaders when we call
                            // VirtualMachine.startLocalManagementAgent() which will fail with a CNFE.
                            ManagementFactory.getPlatformMBeanServer();

                            // Use reflection to invoke...
                            // VirtualMachine vm = VirtualMachine.attach(String.valueOf(ProcessHandle.current().pid()));
                            // localConnectorAddress = vm.startLocalManagementAgent();
                            Class<?> ProcessHandle = Class.forName("java.lang.ProcessHandle");
                            Object processHandle = ProcessHandle.getMethod("current").invoke(null);
                            long pid = (long) ProcessHandle.getMethod("pid").invoke(processHandle);

                            Class<?> VirtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine");
                            Object vm = VirtualMachine.getMethod("attach", String.class).invoke(null, String.valueOf(pid));
                            localConnectorAddress = (String) VirtualMachine.getMethod("startLocalManagementAgent").invoke(vm);
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Returning localConnectorAddress: " + localConnectorAddress);
                        }

                        if (localConnectorAddress == null) {
                            //Call FFDC because we won't be able to make the connector address file
                            FFDCFilter.processException(new RuntimeException("Received a null connector address."), getClass().getName(), "localConnectorInitNull");
                        }

                        return localConnectorAddress;
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Received exception while retrieving connector address: ", e);
                        }
                        FFDCFilter.processException(e, getClass().getName(), "localConnectorInit");
                        return null;
                    }
                }
            });
        }

        public static String getConnectorAddress() {
            return LOCAL_CONNECTOR_ADDRESS;
        }

        private LocalConnectorHelper() {
        }
    }

    public LocalConnectorActivator() {
    }

    protected void activate(ComponentContext compContext) {
    }

    protected void deactivate(ComponentContext compContext) {
        removeJMXAddressResource(localJMXAddressWorkareaFile);
        removeJMXAddressResource(localJMXAddressStateFile);
    }

    private void removeJMXAddressResource(WsResource _localJMXAddressFile) {
        if (_localJMXAddressFile != null) {
            try {
                if (!_localJMXAddressFile.delete()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Could not delete the JMX local connector address file.");
                    }
                    return;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Successfully deleted the JMX local connector address file.");
                }

            } catch (SecurityException se) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Received a SecurityException while attemping to delete the JMX local connector address file: ", se);
                }
            }
        }
    }

    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService = locationService;
        if (localJMXAddressWorkareaFile == null) {
            localJMXAddressWorkareaFile = createJMXWorkAreaResource(locationService);
        }
        if (localJMXAddressStateFile == null) {
            localJMXAddressStateFile = createJMXStateResource(locationService);
        }
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
        if (this.locationService == locationService) {
            this.locationService = null;
        }
    }

    /**
     * @param locationService2
     * @return
     */
    private WsResource createJMXStateResource(WsLocationAdmin locationAdmin) {
        WsResource resource = locationAdmin.resolveResource(WsLocationConstants.SYMBOL_SERVER_STATE_DIR + JMX_LOCAL_ADDRESS);
        return createJmxAddressResource(resource);
    }

    private WsResource createJMXWorkAreaResource(WsLocationAdmin locationAdmin) {
        WsResource resource = locationAdmin.getServerWorkareaResource(JMX_LOCAL_ADDRESS);
        return createJmxAddressResource(resource);
    }

    /**
     * @param resource
     * @return
     */
    private WsResource createJmxAddressResource(WsResource resource) {
        final String connectorAddress = LocalConnectorHelper.getConnectorAddress();
        if (connectorAddress != null) {
            try {
                if (!resource.exists()) {
                    resource.create();
                }
                OutputStream os = resource.putStream();
                os.write(connectorAddress.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Successfully printed the JMX local connector address.");
                }

                return resource;
            } catch (IOException ioe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received IOException while writting address to file: ", ioe);
                }
                FFDCFilter.processException(ioe, getClass().getName(), "createJMXWorkAreaResourceIO");

            } catch (SecurityException se) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received SecurityException while writting address to file: ", se);
                }
                FFDCFilter.processException(se, getClass().getName(), "createJMXWorkAreaResourceSec");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Connector address was null, so we can't write the address to a file.");
        }
        return null;
    }

}
