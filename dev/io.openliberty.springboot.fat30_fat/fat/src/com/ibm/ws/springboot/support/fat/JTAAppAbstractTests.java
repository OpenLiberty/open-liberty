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

import static org.junit.Assert.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.WebApplication;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;

import componenttest.custom.junit.runner.FATRunner;
import ejbapp1.EJBEvent;
import ejbapp1.LocalEJBServlet;
import ejbapp1.LocalInterface;
import ejbapp1.TestObserver;

@RunWith(FATRunner.class)
public abstract class JTAAppAbstractTests extends AbstractSpringTests {
    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        appConfig.setStartAfter("ejbapp1");
    }

    @Override
    public void modifyAppConfiguration(WebApplication appConfig) {
        appConfig.setStartAfter("ejbapp1");
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        WebArchive ejbMisc = ShrinkWrap.create(WebArchive.class, "ejbapp1.war")
                        .addClass(LocalEJBServlet.class)
                        .addClass(TestObserver.class)
                        .addClass(LocalInterface.class)
                        .addClass(EJBEvent.class)
                        .addPackages(true, LocalEJBServlet.class.getPackage())
                        .add(new FileAsset(new java.io.File("test-applications/ejbapp1/resources/META-INF/permissions.xml")),
                             "/META-INF/permissions.xml")
                        .add(new FileAsset(new java.io.File("test-applications/ejbapp1/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        try {
            ShrinkHelper.exportAppToServer(server, ejbMisc, DeployOptions.SERVER_ONLY);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        WebApplication ejbapp1War = new WebApplication();
        ejbapp1War.setId("ejbapp1");
        ejbapp1War.setLocation("ejbapp1.war");
        ejbapp1War.setContextRoot("ejbapp1");
        config.getWebApplications().add(ejbapp1War);

        ConfigElementList<Library> libraries = config.getLibraries();
        libraries.clear();
        ConfigElementList<DataSource> dataSources = config.getDataSources();

        File derbyJar = new File();
        derbyJar.setName("${shared.resource.dir}/derby/derby.jar");
        Library derbyLib = new Library();
        derbyLib.setId("DerbyLib");
        derbyLib.getFiles().add(derbyJar);
        libraries.add(derbyLib);

        JdbcDriver derbyDriver = new JdbcDriver();
        derbyDriver.setLibraryRef("DerbyLib");

        Properties_derby_embedded derbyDS1Props = new Properties_derby_embedded();
        derbyDS1Props.setCreateDatabase("create");
        derbyDS1Props.setDatabaseName("memory:myDB1");

        DataSource derbyDS1 = new DataSource();
        derbyDS1.setId("DerbyDS1");
        derbyDS1.setJndiName("jdbc/DerbyDS1");
        derbyDS1.setType("javax.sql.XADataSource");
        derbyDS1.getJdbcDrivers().add(derbyDriver);
        derbyDS1.getProperties_derby_embedded().add(derbyDS1Props);
        dataSources.add(derbyDS1);

        Properties_derby_embedded derbyDS2Props = new Properties_derby_embedded();
        derbyDS2Props.setCreateDatabase("create");
        derbyDS2Props.setDatabaseName("memory:myDB2");

        DataSource derbyDS2 = new DataSource();
        derbyDS2.setId("DerbyDS2");
        derbyDS2.setJndiName("jdbc/DerbyDS2");
        derbyDS2.setType("javax.sql.XADataSource");
        derbyDS2.getJdbcDrivers().add(derbyDriver);
        derbyDS2.getProperties_derby_embedded().add(derbyDS2Props);
        dataSources.add(derbyDS2);
    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_TRANSACTIONS;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }
}
