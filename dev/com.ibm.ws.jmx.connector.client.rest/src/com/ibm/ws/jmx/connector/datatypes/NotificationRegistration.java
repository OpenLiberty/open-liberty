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
package com.ibm.ws.jmx.connector.datatypes;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

/**
 *
 */
public final class NotificationRegistration {
    public ObjectName objectName;
    public NotificationFilter filters[];
}
