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
package com.ibm.ws.monitor.internal;

import com.ibm.websphere.monitor.annotation.Monitor;

/**
 *
 */
public class MonitorObject {

    Long bundleId;
    Monitor groups;
    Object monitor;
    Class<?> clazz;
    Boolean registered = false;

    /**
     * @param bundleId
     * @param groups
     * @param monitor
     * @param clazz
     */
    public MonitorObject(Long bundleId, Monitor groups, Object monitor, Class<?> clazz) {
        this.bundleId = bundleId;
        this.groups = groups;
        this.monitor = monitor;
        this.clazz = clazz;
    }

    /**
     * @return the bundleId
     */
    public Long getBundleId() {
        return bundleId;
    }

    /**
     * @return the groups
     */
    public Monitor getGroups() {
        return groups;
    }

    /**
     * @return the monitor
     */
    public Object getMonitor() {
        return monitor;
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @param monitor the monitor to set
     */
    public void setMonitor(Object monitor) {
        this.monitor = monitor;
    }

    /**
     * @return the registered
     */
    public Boolean getRegistered() {
        return registered;
    }

    /**
     * @param registered the registered to set
     */
    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

}
