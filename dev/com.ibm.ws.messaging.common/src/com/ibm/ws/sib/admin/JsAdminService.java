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

package com.ibm.ws.sib.admin;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMain;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.JsProcessComponent;
import com.ibm.ws.sib.admin.SIBExceptionBusNotFound;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class JsAdminService {

    private static final TraceComponent tc = SibTr.register(JsAdminService.class, JsConstants.MSG_GROUP, JsConstants.MSG_BUNDLE);


    /**
     * Does the specified string contain characters valid in a JMX key property?
     * 
     * @param s the string to be checked
     * @return boolean if true, indicates the string is valid, otherwise false
     */
    public static boolean isValidJmxPropertyValue(String s) {
        if ((s.indexOf(":") >= 0)
            || (s.indexOf("*") >= 0)
            || (s.indexOf('"') >= 0)
            || (s.indexOf("?") >= 0)
            || (s.indexOf(",") >= 0)
            || (s.indexOf("=") >= 0)) {
            return false;
        } else
            return true;
    }

    /**
     * Check whether a specified string contains any characters which are invalid in a
     * JMX key property name, and if so, quote the name.
     * 
     * @param s the string to be checked
     * @return String an output string, converted if necessary
     */
    public abstract String quoteJmxPropertyValue(String s);

    /**
     * Check whether a specified string contains any characters which are invalid in a
     * JMX key property name, and if so, unquote the name. Reverses the action performed
     * by the quoteJmxPropertyValue method.
     * 
     * @param s the string to be checked
     * @return String an output string, converted if necessary
     */
    public abstract String unquoteJmxPropertyValue(String s);

    /**
     * Method setAdminMain.
     */
    public abstract void setAdminMain(JsMain e);

    /**
     * Method getAdminMain.
     * 
     * @return JsMain
     */
    public abstract JsMain getAdminMain() throws Exception;

    /**
     * Method isInitialized.
     * 
     * @return boolean
     */
    public abstract boolean isInitialized();

    /**
     * Return the instance of the JsBus corresponding to the named Bus. The instance will only
     * exist if a WCCM SIBMessagingEngine object connected to that bus is configured on the
     * server process in which this method is invoked.
     * 
     * @param name
     * @return
     */
    public abstract JsBus getBus(String name) throws SIBExceptionBusNotFound;

    /* ------------------------------------------------------------------------ */
    /*
     * getDefinedBus method
     * /* ------------------------------------------------------------------------
     */
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
    public abstract JsBus getDefinedBus(String name) throws SIBExceptionBusNotFound;

    /**
     * List defined buses in the cell.
     * 
     * @return List list of defined buses.
     */
    public abstract List<String> listDefinedBuses();

    /**
     * Get an instance of a JsProcessComponent.
     * 
     * @return JsProcessComponent
     */
    public abstract JsProcessComponent getProcessComponent(String className);

    /**
     * List all instances of Messaging Engines in this Server JVM. If invoked on zOS, this
     * will return instances associated with the JVM in that zOS region only.
     * 
     * @return Enumeration
     */
    public abstract Enumeration listMessagingEngines();

    /**
     * List all instances of Messaging Engines on a specified SIBus in this Server JVM.
     * If invoked on zOS, this will return instances associated with the JVM in that
     * zOS region only.
     * 
     * @param busName
     * @return Enumeration
     */
    public abstract Enumeration listMessagingEngines(String busName);

    /**
     * Get all the UUIDs for all the Messsaging Engines configured on a specified
     * Bus.
     * 
     * @param busName the name of the bus
     * @return a set containing the UUIDs in string format
     */
    public abstract Set getMessagingEngineSet(String busName);

    /**
     * Get an instance of a Messaging Engine in this Server JVM. If invoked on zOS,
     * this will return instances associated with the JVM in that zOS region only.
     * 
     * @param busName
     * @param engineName
     * @return JsMessagingEngine
     */
    public abstract JsMessagingEngine getMessagingEngine(String busName, String engineName);

    /**
     * Activate the JMSResource MBean in this server.
     */
    public abstract void activateJMSResource();

    /**
     * Deactivate the JMSResource MBean in this server.
     */
    public abstract void deactivateJMSResource();

    /**
     * Return the named WebSphere runtime service.
     * 
     * @param c
     * @return
     */
    public abstract Object getService(Class c);

    /**
     * Is this process running as a WAS Standalone Server?
     * 
     * @return
     */
    public abstract boolean isStandaloneServer();
}
