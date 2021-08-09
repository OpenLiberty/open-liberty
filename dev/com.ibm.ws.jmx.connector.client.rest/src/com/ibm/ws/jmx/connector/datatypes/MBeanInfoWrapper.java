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

import java.util.Map;

import javax.management.MBeanInfo;

/**
 *
 */
public final class MBeanInfoWrapper {
    public MBeanInfo mbeanInfo;
    public String attributesURL;
    public Map<String, String> attributeURLs, operationURLs;
}
