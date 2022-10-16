/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

import componenttest.topology.impl.LibertyServer;

public class ResponseValues {

    static private final Class<?> thisClass = ResponseValues.class;

    LibertyServer rpServer = null;
    String subject = "testuser";
    String clientId = "client_1";
    String realm = "BasicRealm";
    String issuer = "https://localhost:8920/oidc/endpoint/OP1"; // users should always override this
    String tokenType = "Bearer";
    String originalRequest = null;

    public void setRPServer(LibertyServer inRPServer) {

        rpServer = inRPServer;
    };

    public LibertyServer getRPServer() {

        return rpServer;
    };

    public void setSubject(String inSubject) {

        subject = inSubject;
    };

    public String getSubject() {

        return subject;
    };

    public void setClientId(String inClientId) {

        clientId = inClientId;
    };

    public String getClientId() {

        return clientId;
    };

    public void setRealm(String inRealm) {

        realm = inRealm;
    };

    public String getRealm() {

        return realm;
    };

    public void setIssuer(String inIssuer) {

        issuer = inIssuer;
    };

    public String getIssuer() {

        return issuer;
    };

    public void setTokenType(String inTokenType) {

        tokenType = inTokenType;
    };

    public String getTokenType() {

        return tokenType;
    };

    public void setOriginalRequest(String inOriginalRequest) {

        originalRequest = inOriginalRequest;
    };

    public String getOriginalRequest() {

        return originalRequest;
    };

}
