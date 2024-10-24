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
package com.ibm.ws.jmx.connector.datatypes;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

public final class ServerNotificationRegistration {
    public enum Operation {
        Add, RemoveAll, RemoveSpecific
    }

    public Operation operation;
    public ObjectName objectName, listener;
    public NotificationFilter filter;
    public Object handback;
    public int filterID, handbackID;
}
