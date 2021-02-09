/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.inout.adapter;

import javax.resource.ResourceException;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.ConnectionDefinition;

import com.ibm.adapter.spi.ManagedConnectionFactoryImpl;

@ConnectionDefinition(connectionFactory = javax.sql.DataSource.class, connectionFactoryImpl = com.ibm.adapter.jdbc.JdbcDataSource.class, connection = java.sql.Connection.class,
                      connectionImpl = com.ibm.adapter.jdbc.JdbcConnection.class)
public class InoutManagedConnectionFactoryImpl extends ManagedConnectionFactoryImpl implements javax.resource.spi.ManagedConnectionFactory {

    @Override
    //@ConfigProperty(type = java.lang.String.class, description = "Derby specific property. Set to create if you want the database created")
    @ConfigProperty(type = java.lang.String.class)
    public void setCreateDatabase(String createDatabase) throws ResourceException {
        super.setCreateDatabase(createDatabase);
    }

    @Override
    //@ConfigProperty(defaultValue = "jtest1", type = java.lang.String.class, description = "The name of the database")
    @ConfigProperty(defaultValue = "jtest1", type = java.lang.String.class)
    public void setDatabaseName(String databaseName) throws ResourceException {
        super.setDatabaseName(databaseName);
    }

    @Override
    //@ConfigProperty(defaultValue = "com.ibm.db2.jcc.DB2XADataSource", type = java.lang.String.class, description = "The datasource implementation class")
    @ConfigProperty(defaultValue = "com.ibm.db2.jcc.DB2XADataSource", type = java.lang.String.class)
    public void setDataSourceClass(String dataSourceClass) throws ResourceException {
        super.setDataSourceClass(dataSourceClass);
    }

    @Override
    //@ConfigProperty(type = java.lang.String.class, description = "The driver type for DB2.This can be set to Type 2 or Type 4, but Type 2 will not work on z/OS")
    @ConfigProperty(type = java.lang.String.class)
    public void setDriverType(String newDriverType) throws ResourceException {
        super.setDriverType(newDriverType);
    }

    @Override
    //@ConfigProperty(defaultValue = "true", description = "Specify whether connection handles support implicit reactivation. (Smart Handle support). Value may be \"true\" or \"false\".")
    @ConfigProperty(defaultValue = "true")
    public void setInactiveConnectionSupport(Boolean inactiveConnectionSupport) {
        super.setInactiveConnectionSupport(inactiveConnectionSupport);
    }

    /**
     * Returns the inactiveConnectionSupport.
     *
     * @return boolean
     */
    public Boolean getInactiveConnectionSupport() {
        return super.isInactiveConnectionSupport();
    }

    @Override
    //@ConfigProperty(defaultValue = "dynamic", type = java.lang.String.class, description = "Type of transaction resource registration (enlistment).  Valid values are either 'static' (immediate) or 'dynamic' (deferred).")
    @ConfigProperty(defaultValue = "dynamic", type = java.lang.String.class)
    public void setTransactionResourceRegistration(
                                                   String transactionResourceRegistration) {
        super.setTransactionResourceRegistration(transactionResourceRegistration);
    }

}
