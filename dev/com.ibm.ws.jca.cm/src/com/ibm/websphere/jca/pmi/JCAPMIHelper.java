/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jca.pmi;

/**
 *
 */
public interface JCAPMIHelper {

    //The Below two  methods are added to support PMI data for connection pools.This can be avoid if we expose com.ibm.ejs.jca,but currently as per JIM
    //it should not be done as j2c code is partial implementation only for JDBC and JMS.In future when j2c code is fully implemented its better to
    //remove the interface JCAPMIHelper and implemented methods and update ConnectionPoolMonitor.java to use the exposed j2c code.
    public String getUniqueId();

    public String getJNDIName();

    public boolean getParkedValue();
}
