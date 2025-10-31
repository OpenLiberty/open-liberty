/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import org.junit.Before;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public abstract class JPAAppAbstractTests extends AbstractSpringTests {

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {

        ConfigElementList<Library> libraries = config.getLibraries();
        libraries.clear();
        ConfigElementList<DataSource> dataSources = config.getDataSources();

        File derbyJar = new File();
        derbyJar.setName("${shared.resource.dir}/derby16/derby.jar");
        File derbyToolsJar = new File();
        derbyToolsJar.setName("${shared.resource.dir}/derby16/derbytools.jar");
        File derbySharedJar = new File();
        derbySharedJar.setName("${shared.resource.dir}/derby16/derbyshared.jar");

        Library derbyLib = new Library();
        derbyLib.setId("DerbyLib");
        derbyLib.getFiles().add(derbyJar);
        derbyLib.getFiles().add(derbyToolsJar);
        derbyLib.getFiles().add(derbySharedJar);
        libraries.add(derbyLib);

        JdbcDriver derbyDriver = new JdbcDriver();
        derbyDriver.setLibraryRef("DerbyLib");

        Properties_derby_embedded customerDSProps = new Properties_derby_embedded();
        customerDSProps.setCreateDatabase("create");
        customerDSProps.setDatabaseName("memory:customerDB");

        DataSource customerDS = new DataSource();
        customerDS.setId("customerDataSource");
        customerDS.setJndiName("jdbc/CUSTOMER_UNIT");
        customerDS.setType("javax.sql.XADataSource");
        customerDS.getJdbcDrivers().add(derbyDriver);
        customerDS.getProperties_derby_embedded().add(customerDSProps);
        dataSources.add(customerDS);

        Properties_derby_embedded employeeDSProps = new Properties_derby_embedded();
        employeeDSProps.setCreateDatabase("create");
        employeeDSProps.setDatabaseName("memory:employeeDB");

        DataSource employeeDS = new DataSource();
        employeeDS.setId("employeeDataSource");
        employeeDS.setJndiName("jdbc/EMPLOYEE_UNIT");
        employeeDS.setType("javax.sql.XADataSource");
        employeeDS.getJdbcDrivers().add(derbyDriver);
        employeeDS.getProperties_derby_embedded().add(employeeDSProps);
        dataSources.add(employeeDS);
    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_DATA;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }
}
