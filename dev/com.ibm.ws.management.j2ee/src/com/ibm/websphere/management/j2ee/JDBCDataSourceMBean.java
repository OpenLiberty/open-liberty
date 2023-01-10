/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.websphere.management.j2ee;


/**
 * Identifies a physical JDBC data source. For each JDBC data source available to a
 * server there must be one managed object that implements the JDBCDataSource
 * model.
 */
public interface JDBCDataSourceMBean extends J2EEManagedObjectMBean {

    /**
     * The value of jdbcDriver must be an JDBCDriver OBJECT_NAME that
     * identifies the JDBC driver for the corresponding JDBC data source.
     */
    String getjdbcDriver();

}
