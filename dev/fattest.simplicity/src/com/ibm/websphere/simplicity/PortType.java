/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

/**
 * Represents the default WebSphere server port names. A port is not synonymous with
 * a connector, however, which is the mechanism by which a client connects to a running
 * instance of WebSphere Application Server. For those values, see {@link ConnectorType}.
 */
public enum PortType {
    /**
     * Telnet access to the OSGi console.
     */
    OSGi("OSGi"),
    /**
     * Used by RMI and JSR160RMI.
     */
//    BOOTSTRAP_ADDRESS("BOOTSTRAP_ADDRESS"),
    /**
     * Used by CORBA clients.
     */
//    ORB_LISTENER_ADDRESS("ORB_LISTENER_ADDRESS"),
//    ORB_SSL_LISTENER_ADDRESS("ORB_SSL_LISTENER_ADDRESS"),
//    SAS_SSL_SERVERAUTH_LISTENER_ADDRESS("SAS_SSL_SERVERAUTH_LISTENER_ADDRESS"),
    /**
     * Admin console port when security is disabled.
     */
//    WC_adminhost("WC_adminhost"),
    /**
     * Admin console port when security is enabled.
     */
//    WC_adminhost_secure("WC_adminhost_secure"),
    /**
     * The standard HTTP (unsecured) port.
     */
    WC_defaulthost("WC_defaulthost"),
    /**
     * The standard HTTPS (secured) port.
     */
    WC_defaulthost_secure("WC_defaulthost_secure"),
    /**
     * Admin Agent subsystems act as the connection host for
     * registered nodes (application servers whose node has been
     * registered to an Admin Agent process). Use this port for
     * RMI connections to a registered node.
     */
//    RMI_CONNECTOR_ADDRESS("RMI_CONNECTOR_ADDRESS"),
//    HTTP("HTTP"),
//    HTTP_SECURE("HTTP_SECURE"),
    IIOP("HTTP_SECURE"),
    JMX_REST("HTTP_SECURE");

    public static PortType fromName(String name) {
        for (PortType pt : PortType.values())
            if (pt.getPortName().equalsIgnoreCase(name))
                return pt;
        return null;
    }

    private final String portName;

    private PortType(String portName) {
        this.portName = portName;
    }

    /**
     * Returns the port name used in the property file for this Port
     * 
     * @return The port name used in the property file for this Port
     */
    public String getPortName() {
        return this.portName;
    }

    @Override
    public String toString() {
        return this.portName;
    }

}
