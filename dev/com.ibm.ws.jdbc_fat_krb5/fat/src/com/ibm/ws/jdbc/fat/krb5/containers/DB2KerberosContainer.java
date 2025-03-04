/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5.containers;

import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_KDC;
import static com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer.KRB5_REALM;

import java.time.Duration;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class DB2KerberosContainer extends Db2Container {

    private static final Class<?> c = DB2KerberosContainer.class;

    //TODO Start using ImageBuilder
//    private static final DockerImageName DB2_KRB5 = ImageBuilder
//                    .build("db2-krb5:12.1.1.0")
//                    .getDockerImageName()
//                    .asCompatibleSubstituteFor("icr.io/db2_community/db2");

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final String IMAGE = "kyleaure/db2-krb5:2.0";
    private static final DockerImageName DB2_KRB5 = DockerImageName.parse(IMAGE)
                    .asCompatibleSubstituteFor("ibmcom/db2"); //TODO update .asCompatibleSubstituteFor("icr.io/db2_community/db2")

    public DB2KerberosContainer(Network network) {
        super(DB2_KRB5);
        withNetwork(network);
    }

    @Override
    protected void configure() {
        acceptLicense();
        withExposedPorts(50000);
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC);
        withEnv("DB2_KRB5_PRINCIPAL", "db2srvc@EXAMPLE.COM");
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*SETUP SCRIPT COMPLETE.*$")
                        .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN && !FATRunner.ARM_ARCHITECTURE ? 10 : 35)));
        withLogConsumer(new SimpleLogConsumer(c, "DB2-KRB5"));
    }

    @Override
    public String getUsername() {
        return "db2inst1";
    }

    @Override
    public String getPassword() {
        return "password";
    }

    @Override
    public String getDatabaseName() {
        return "testdb";
    }

    @Override
    public Db2Container withUsername(String username) {
        throw new UnsupportedOperationException("Username is hardcoded in container");
    }

    @Override
    public Db2Container withPassword(String password) {
        throw new UnsupportedOperationException("Password is hardcoded in container");
    }

    @Override
    public Db2Container withDatabaseName(String dbName) {
        throw new UnsupportedOperationException("DB name is hardcoded in container");
    }
}
