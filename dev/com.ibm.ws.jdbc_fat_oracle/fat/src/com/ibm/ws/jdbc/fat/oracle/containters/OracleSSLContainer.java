/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle.containters;

import java.time.Duration;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.ibm.ws.jdbc.fat.oracle.FATSuite;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Custom Oracle SSL Container class
 */
public class OracleSSLContainer extends OracleContainer {

    private static final int TCPS_PORT = 1522;
    private static final String WALLET_PASS = "WalletPasswd123";

    //TODO update this image to be built on top of gvenzl/oracle-xe
    private static final String IMAGE_NAME_STRING = "kyleaure/oracle-ssl-18.4.0-xe-prebuilt:2.0";
    private static final DockerImageName IMAGE_NAME = DockerImageName.parse(IMAGE_NAME_STRING).asCompatibleSubstituteFor("gvenzl/oracle-xe:18.4.0-slim");

    public OracleSSLContainer() {
        super(IMAGE_NAME);
        super.addExposedPort(TCPS_PORT);
        super.withPassword("oracle"); //Tell superclass the hardcoded password
        super.usingSid(); //Maintain current behavior of connecting with SID instead of pluggable database
        super.waitingFor(Wait.forLogMessage(".*DONE: Executing user defined scripts.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 25)));
        super.withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Oracle-SSL"));
    }

    //Do not allow developer to use a custom password
    @Override
    public OracleContainer withPassword(String password) {
        throw new UnsupportedOperationException("Oracle SSL container does not support use of a customer password.");
    }

    //Override default wait strategy since we want to wait for custom script to be executed first
    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    public int getOracleSSLPort() {
        return this.getMappedPort(TCPS_PORT);
    }

    public String getJdbcSSLUrl() {
        return "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=" + getHost() + ")(PORT=" + getOracleSSLPort() + "))(CONNECT_DATA=(SERVICE_NAME=" + getSid() + ")))";
    }

    public String getWalletPassword() {
        return WALLET_PASS;
    }

    @Override
    protected void configure() {
        //DO NOTHING
    }
}
