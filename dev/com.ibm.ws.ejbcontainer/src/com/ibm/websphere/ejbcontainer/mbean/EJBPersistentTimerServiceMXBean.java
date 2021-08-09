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
package com.ibm.websphere.ejbcontainer.mbean;

/**
 * This MBean provides an abstraction over the persistent storage of EJB timers.
 *
 * @ibm-api
 */
public interface EJBPersistentTimerServiceMXBean {
    /**
     * Returns all persistent timers for all EJBs in all modules in an
     * application.
     *
     * @param appName the application name
     * @return the persistent timers for the application, or an empty array if
     *         there are no persistent timers for the application
     */
    EJBPersistentTimerInfo[] getTimers(String appName);

    /**
     * Returns all persistent timers for all EJBs in a module.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     * @return the persistent timers for the application, or an empty array if
     *         there are no persistent timers for the module
     */
    EJBPersistentTimerInfo[] getTimers(String appName, String moduleURI);

    /**
     * Returns all persistent timers for an EJB.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     * @param ejbName the EJB name
     * @return the persistent timers for the application, or an empty array if
     *         there are no persistent timers for the EJB
     */
    EJBPersistentTimerInfo[] getTimers(String appName, String moduleURI, String ejbName);

    /**
     * Cancels a persistent EJB timer.
     *
     * <p>Canceling an automatic timer will not cause it to be recreated when
     * the application is deployed unless the persistent indicator is also
     * removed using one of the {@link #removeAutomaticTimers} methods.
     *
     * @param id the ID as returned by {@link EJBPersistentTimerInfo#getId}
     * @return true if the timer was canceled, or false if the timer was not found
     */
    boolean cancelTimer(String id);

    /**
     * Cancels all persistent timers for all EJBs in all modules in an
     * application.
     *
     * <p>Canceling an automatic timer will not cause it to be recreated when
     * the application is deployed unless the persistent indicator is also
     * removed using one of the {@link #removeAutomaticTimers} methods.
     *
     * @param appName the application name
     * @return true if any timers were canceled
     */
    boolean cancelTimers(String appName);

    /**
     * Cancels all persistent timers for all EJBs in a modules.
     *
     * <p>Canceling an automatic timer will not cause it to be recreated when
     * the application is deployed unless the persistent indicator is also
     * removed using one of the {@link #removeAutomaticTimers} methods.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     * @return true if any timers were canceled
     */
    boolean cancelTimers(String appName, String moduleURI);

    /**
     * Cancels all persistent timers for an EJB.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     * @param ejbName the EJB name
     * @return true if any timers were canceled
     */
    boolean cancelTimers(String appName, String moduleURI, String ejbName);

    /**
     * Returns true if timers have been automatically created for any module in
     * an application.
     *
     * @param appName the application name
     */
    boolean containsAutomaticTimers(String appName);

    /**
     * Returns true if timers have been automatically created for a module.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     */
    boolean containsAutomaticTimers(String appName, String moduleURI);

    /**
     * Cancels all persistent timers for all EJBs in all modules in an
     * application, and removes the persistent indicator that timers were
     * automatically created.
     *
     * @param appName the application name
     */
    boolean removeAutomaticTimers(String appName);

    /**
     * Cancels all persistent timers for all EJBs in a module, and removes the
     * persistent indicator that timers were automatically created.
     *
     * @param appName the application name
     * @param moduleURI the module URI
     */
    boolean removeAutomaticTimers(String appName, String moduleURI);
}
