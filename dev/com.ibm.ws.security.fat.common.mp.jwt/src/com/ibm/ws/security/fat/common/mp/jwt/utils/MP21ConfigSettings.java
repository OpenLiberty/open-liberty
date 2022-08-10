/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt.utils;

public class MP21ConfigSettings extends MP12ConfigSettings {

//    public static final String DefaultHeader = MPJwt12FatConstants.AUTHORIZATION;
//    public static final String DefaultCookieName = MPJwt12FatConstants.TOKEN_TYPE_BEARER;
//    public final static String HeaderNotSet = "";
//    public final static String CookieNotSet = "";
//    public final static String AudiencesNotSet = "";
//    public final static String DefaultAlgorithm = MPJwt12FatConstants.SIGALG_RS256;
//    public final static String AlgorithmNotSet = "";
//    public final static String DecryptKeyLocNotSet = "";

//    private final String header = DefaultHeader;
//    private final String cookie = DefaultCookieName;
//    private final String audience = AudiencesNotSet;
//    private final String algorithm = AlgorithmNotSet;
//    private final String decryptKeyLoc = DecryptKeyLocNotSet;

    public static final int DefaultTokenAge = 360;
    public static final int DefaultClockSkew = 300;

    private int tokenAge = 0;
    private int clockSkew = 300; // (5min/300seconds)

    public MP21ConfigSettings() {
    }

    public MP21ConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType, String inHeader, String inCookie, String inAudience,
                              String inAlgorithm, String inDecryptKeyLoc, int inTokenAge, int inClockSkew) {

        super(inPublicKeyLocation, inPublicKey, inIssuer, inCertType, inHeader, inCookie, inAudience, inAlgorithm, inDecryptKeyLoc);
        tokenAge = inTokenAge;
        clockSkew = inClockSkew;
    }

    public void setTokenAge(int inTokenAge) {
        tokenAge = inTokenAge;
    }

    public int getTokenAge() {
        return tokenAge;
    }

    public void setClockSkew(int inClockSkew) {
        clockSkew = inClockSkew;
    }

    public int getClockSkew() {
        return clockSkew;
    }

    // TODO add keyManagementKey stuff
}