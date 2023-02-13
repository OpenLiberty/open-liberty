/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.ws.messaging.lifecycle.Singleton;

public interface JsAdminService extends Singleton {
    Pattern INVALID_CHARS = Pattern.compile("[:*\"?,=]");
    /**
     * Does the specified string contain characters valid in a JMX key property?
     *
     * @param s the string to be checked
     * @return boolean if true, indicates the string is valid, otherwise false
     */
    static boolean isValidJmxPropertyValue(String s) {
        return ! INVALID_CHARS.matcher(s).find();
    }

    /**
     * Check whether a specified string contains any characters which are invalid in a
     * JMX key property name, and if so, quote the name.
     *
     * @param s the string to be checked
     * @return String an output string, converted if necessary
     */
    String quoteJmxPropertyValue(String s);

    /**
     * Check whether a specified string contains any characters which are invalid in a
     * JMX key property name, and if so, unquote the name. Reverses the action performed
     * by the quoteJmxPropertyValue method.
     *
     * @param s the string to be checked
     * @return String an output string, converted if necessary
     */
    String unquoteJmxPropertyValue(String s);

    boolean isInitialized();

    /**
     * Return the instance of the JsBus corresponding to the named Bus. The instance will only
     * exist if a WCCM SIBMessagingEngine object connected to that bus is configured on the
     * server process in which this method is invoked.
     *
     * @param name
     * @return
     */
    JsBus getBus(String name) throws SIBExceptionBusNotFound;

    /**
     * Returns an instance of the JsBus corresponding to the named bus. Unlike
     * getBus it will return the JsBus for buses that do not have a bus member
     * on this server process. This method is intended to be used by those
     * services of a bus that are not necessarily tied to a bus member, such as
     * TRM bootstrap.
     *
     * @param name the name of the bus.
     * @return the JsBus.
     * @throws SIBExceptionBusNotFound if the bus does not exist.
     */
    JsBus getDefinedBus(String name) throws SIBExceptionBusNotFound;

    List<String> listDefinedBuses();

    JsProcessComponent getProcessComponent(String className);

    /**
     * List all instances of Messaging Engines in this Server JVM. If invoked on zOS, this
     * will return instances associated with the JVM in that zOS region only.
     *
     * @return Enumeration
     */
    Enumeration listMessagingEngines();

    /**
     * List all instances of Messaging Engines on a specified SIBus in this Server JVM.
     * If invoked on zOS, this will return instances associated with the JVM in that
     * zOS region only.
     *
     * @param busName
     * @return Enumeration
     */
    Enumeration listMessagingEngines(String busName);

    /**
     * Get all the UUIDs for all the Messaging Engines configured on a specified
     * Bus.
     *
     * @param busName the name of the bus
     * @return a set containing the UUIDs in string format
     */
    Set getMessagingEngineSet(String busName);

    /**
     * Get an instance of a Messaging Engine in this Server JVM. If invoked on zOS,
     * this will return instances associated with the JVM in that zOS region only.
     *
     * @param busName
     * @param engineName
     * @return JsMessagingEngine
     */
    JsMessagingEngine getMessagingEngine(String busName, String engineName);

    /**
     * Activate the JMSResource MBean in this server.
     */
    void activateJMSResource();

    /**
     * Deactivate the JMSResource MBean in this server.
     */
    void deactivateJMSResource();

    /**
     * Return the named WebSphere runtime service.
     */
    Object getService(Class<?> c);

    /**
     * Is this process running as a WAS Standalone Server?
     *
     * @return
     */
    boolean isStandaloneServer();
}
