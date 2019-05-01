/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threading;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public interface WorkContext extends Map<String, Serializable> {
    static final String WORK_TYPE_IIOP_NAMING = "IIOPNaming";
    static final String WORK_TYPE_IIOP_EJB = "IIOPEJB";
    static final String WORK_TYPE_EJB_TIMER = "EJBTimer";
    static final String WORK_TYPE_EJB_ASYNC = "EJBAsync";
    static final String WORK_TYPE_HTTP = "HTTP";
    static final String WORK_TYPE_JCA_MDB = "JCAMDB";
    static final String WORK_TYPE_UNKNOWN = "Unknown";

    public String getWorkType();

    // use Map.get() to retrieve property values using a defined set of property keys
}