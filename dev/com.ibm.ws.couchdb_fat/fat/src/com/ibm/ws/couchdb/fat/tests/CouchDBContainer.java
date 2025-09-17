/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.couchdb.fat.tests;

import static com.ibm.ws.couchdb.fat.FATSuite.couchdb;

import java.util.Base64;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.HttpsRequest;

public class CouchDBContainer extends GenericContainer<CouchDBContainer> {

    public static final int PORT = 5984;
    public static final int PORT_SECURE = 6984;
    private String user = "dbuser";
    private String pass = "dbpass";

    public CouchDBContainer(String image) {
        super(image);
    }

    public CouchDBContainer(ImageFromDockerfile image) {
        super(image);
    }

    public CouchDBContainer(DockerImageName image) {
        super(image);
    }

    public CouchDBContainer withUser(String user) {
        this.user = user;
        return this;
    }

    public String getUser() {
        return user;
    }

    public CouchDBContainer withPassword(String pass) {
        this.pass = pass;
        return this;
    }

    public String getPassword() {
        return pass;
    }

    @Override
    protected void configure() {
        withEnv("COUCHDB_USER", user);
        withEnv("COUCHDB_PASSWORD", pass);
        withExposedPorts(5984, 6984);
        waitingFor(Wait.forHttp("/").forPort(PORT));
    }

    public String getPort(boolean secure) {
        return secure ? couchdb.getMappedPort(PORT_SECURE).toString() : couchdb.getMappedPort(PORT).toString();
    }

    public String getURL(boolean secure) {
        return secure ? "https://" + couchdb.getHost() + ':' + couchdb.getMappedPort(PORT_SECURE) : //
                        "http://" + couchdb.getHost() + ':' + couchdb.getMappedPort(PORT);
    }

    public String createDb(String dbName) throws Exception {
        Log.info(getClass(), "createDb", "Creating DB " + dbName);
        String auth = "Basic " + Base64.getEncoder().encodeToString((user + ':' + pass).getBytes());

        String response = new HttpsRequest(getURL(false) + "/" + dbName)
                        .method("PUT")
                        .allowInsecure()
                        .requestProp("Authorization", auth)
                        .requestProp("Accept", "application/json")
                        .requestProp("Content-type", "application/json")
                        .requestProp("User-Agent", "java-couchdb/unknown")
                        .expectCode(201) // HTTP 201/202 mean create successfully
                        .expectCode(202)
                        .run(String.class);
        Log.info(getClass(), "createDb", "Create DB response: " + response);
        return response;
    }
}
