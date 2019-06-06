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
import java.lang.reflect.Method;
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

                        // Start the JMX agent and retrieve the local connector address.
                        // TODO: Find a proper way to get the JMX agent's local connector address
                        //       for now we need to depend on JDK internal APIs
                        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
                        Class<?> clazz1 = JavaInfo.majorVersion() < 9 
							? Class.forName("sun.management.Agent", true, systemClassLoader) 
							: Class.forName("jdk.internal.agent.Agent", true, systemClassLoader);
                        Method m1 = clazz1.getMethod("agentmain", String.class);
                        m1.invoke(null, (Object) null);
                        String localConnectorAddress = System.getProperty(LOCAL_CONNECTOR_ADDRESS_PROPERTY);
                        if (localConnectorAddress == null) {
                            Class<?> clazz2 = JavaInfo.majorVersion() < 9 
								? Class.forName("sun.misc.VMSupport", true, systemClassLoader) 
								: Class.forName("jdk.internal.vm.VMSupport", true, systemClassLoader);
                            Method m2 = clazz2.getMethod("getAgentProperties");
                            Properties props = (Properties) m2.invoke(null);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Attempting to retrieve the connector address from agent properties.");
                            }
                            localConnectorAddress = props.getProperty(LOCAL_CONNECTOR_ADDRESS_PROPERTY);
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

        private LocalConnectorHelper() {}
    }

    public LocalConnectorActivator() {}

    protected void activate(ComponentContext compContext) {}

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
                os.write(connectorAddress.getBytes("UTF-8"));
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
