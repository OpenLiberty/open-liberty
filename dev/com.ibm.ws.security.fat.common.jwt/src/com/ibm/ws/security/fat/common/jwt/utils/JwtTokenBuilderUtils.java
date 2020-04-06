/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.utils;

import org.jose4j.jws.AlgorithmIdentifiers;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ServerFileUtils;

public class JwtTokenBuilderUtils {

    protected static Class<?> thisClass = JwtTokenBuilderUtils.class;
    public static ServerFileUtils serverFileUtils = new ServerFileUtils();

    protected static String defaultKeyFile = null;

    public void setDefaultKeyFile(LibertyServer server, String keyFile) throws Exception {

        defaultKeyFile = getKeyFileWithPathForServer(server, keyFile);

    }

    public String getKeyFileWithPathForServer(LibertyServer server, String keyFile) throws Exception {

        return serverFileUtils.getServerFileLoc(server) + "/" + keyFile;
    }

    /**
     * Create a new JWTTokenBuilder and initialize it with default test values
     *
     * @return - an initialized JWTTokenBuilder
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer("client01");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeMinutesIntheFuture(5);
        builder.setScope("openid profile");
        builder.setSubject("testuser");
        builder.setRealmName("BasicRealm");
        builder.setTokenType("Bearer");
        builder = builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        builder = builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecure");

        return builder;
    }

    /*
     * Wrap the call to the builder so that we can log the raw values and the generated token
     * for debug purposes and not have to duplicate 3 simple lines of code
     */
    public String buildToken(JWTTokenBuilder builder, String testName) throws Exception {
        Log.info(thisClass, "buildToken", "testing _testName: " + testName);
        Log.info(thisClass, testName, "Json claims:" + builder.getJsonClaims());
        String jwtToken = builder.build();
        Log.info(thisClass, testName, "built jwt:" + jwtToken);
        return jwtToken;
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder) throws Exception {
        updateBuilderWithRSASettings(builder, null);
    }

    public void updateBuilderWithRSASettings(JWTTokenBuilder builder, String overrideKeyFile) throws Exception {

        String keyFile = defaultKeyFile;
        // if an override wasn't given, use the default key file
        if (overrideKeyFile != null) {
            keyFile = overrideKeyFile;
        }
        builder.setAlorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        builder.setRSAKey(keyFile);
    }
}
