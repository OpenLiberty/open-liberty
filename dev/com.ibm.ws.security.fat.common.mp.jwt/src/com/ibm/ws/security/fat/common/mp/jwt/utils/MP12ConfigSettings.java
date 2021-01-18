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
package com.ibm.ws.security.fat.common.mp.jwt.utils;

import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;

public class MP12ConfigSettings extends MP11ConfigSettings {

    public static final String DefaultHeader = MPJwt12FatConstants.AUTHORIZATION;
    public static final String DefaultCookieName = MPJwt12FatConstants.TOKEN_TYPE_BEARER;
    public final static String HeaderNotSet = "";
    public final static String CookieNotSet = "";
    public final static String AudiencesNotSet = "";
    public final static String DefaultAlgorithm = MPJwt12FatConstants.SIGALG_RS256;
    public final static String AlgorithmNotSet = "";
    public final static String DecryptKeyLocNotSet = "";

    private String header = DefaultHeader;
    private String cookie = DefaultCookieName;
    private String audience = AudiencesNotSet;
    private String algorithm = AlgorithmNotSet;
    private String decryptKeyLoc = DecryptKeyLocNotSet;

    public MP12ConfigSettings() {
    }

    public MP12ConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType, String inHeader, String inCookie, String inAudience,
                              String inAlgorithm, String inDecryptKeyLoc) {

        super(inPublicKeyLocation, inPublicKey, inIssuer, inCertType);
        header = inHeader;
        cookie = inCookie;
        audience = inAudience;
        algorithm = inAlgorithm;
        decryptKeyLoc = inDecryptKeyLoc;
    }

    public void setHeader(String inHeader) {
        header = inHeader;
    }

    public String getHeader() {
        return header;
    }

    public void setCookie(String inCookie) {
        cookie = inCookie;
    }

    public String getCookie() {
        return cookie;
    }

    public void setAudience(String inAudience) {
        audience = inAudience;
    }

    public String getAudience() {
        return audience;
    }

    public void setAlgorithm(String inAlgorithm) {
        algorithm = inAlgorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setDecryptKeyLoc(String inDecryptKeyLoc) {
        decryptKeyLoc = inDecryptKeyLoc;
    }

    public String getDecryptKeyLoc() {
        return decryptKeyLoc;
    }
}