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
package com.ibm.websphere.management.j2ee;

/**
 * The JDBCResource model identifies a JDBC resource. A JDBC resource manages
 * one or more JDBC data sources. For each JDBC resource provided on a server, there
 * must be one JDBCResource OBJECT_NAME in the servers resources list that
 * identifies it.
 */
public interface JDBCResourceMBean extends J2EEResourceMBean {

    /**
     * Identifies the JDBC data sources available on the corresponding JDBC
     * resource. For each JDBC data source available on this JDBC resource there must
     * be one JDBCDataSource OBJECT_NAME in the jdbcDataSources list.
     */
    String[] getjdbcDataSources();

}
