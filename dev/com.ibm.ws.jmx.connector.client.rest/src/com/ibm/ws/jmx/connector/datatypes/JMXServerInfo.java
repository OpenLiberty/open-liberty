/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.datatypes;

/**
 *
 */
public final class JMXServerInfo {
    public int version;
    public String mbeansURL, createMBeanURL, mbeanCountURL, defaultDomainURL, domainsURL, notificationsURL, instanceOfURL, fileTransferURL, apiURL, graphURL;
}
