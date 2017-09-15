/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.kernel.server;

/**
 * The ServerInfoMBean represents information about the server.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 *
 * @ibm-api
 */
public interface ServerInfoMBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=kernel,name=ServerInfo";

    /**
     * Answers with the server's default hostname.
     * <p>
     * The default hostname is specified by the variable ${defaultHostName}.
     * The value of the hostname is always returned in lower case.
     * </p>
     * <p>
     * For example, if defaultHostName is defined in the server.xml:
     * &lt;variable name="defaultHostName" value="myHost"/&gt;
     * The return value would be 'myhost'.
     * </p>
     *
     * @return The server's default hostname, in lower case.
     */
    String getDefaultHostname();

    /**
     * Answers with the server's user directory.
     *
     * @return The server's user directory.
     */
    String getUserDirectory();

    /**
     * Answers with the server's wlp install directory.
     *
     * @return The server's wlp install directory.
     */
    String getInstallDirectory();

    /**
     * Answers with the server's name.
     *
     * @return The server's name.
     */
    String getName();

    /**
     * Answers with the product runtime version as provided by the properties file in
     * the <liberty_home>/lib/versions directory, eg 16.0.0.2.
     *
     * @return The runtime version.
     */
    String getLibertyVersion();

    /**
     * Answers with the Java specification version as provided by the Java Runtime Environment (JRE), eg 1.8.
     *
     * @return The Java specification version.
     */
    String getJavaSpecVersion();

    /**
     * Answers with the version (service level) of the Java Runtime Environment (JRE), eg 1.8.0_91-b14.
     *
     * @return The service level of the JRE.
     */
    String getJavaRuntimeVersion();

}
