/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.jmx.service;

import javax.management.ObjectName;

/**
 * Implemented by an MBeanServerForwarderDelegate capable of delayed MBean activation.
 */
public interface DelayedMBeanHelper {

    public boolean isDelayedMBean(ObjectName name);

    public void setMBeanServerNotificationSupport(MBeanServerNotificationSupport support);

}
