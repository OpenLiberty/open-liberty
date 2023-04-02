/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.fat.common.mp.jwt.utils;

public class MP21ConfigSettings extends MP12ConfigSettings {

    public static final int DefaultTokenAge = 360;
    public static final int DefaultClockSkew = 300;
    public static final String DefaultKeyMgmtKeyAlg = "";

    private int tokenAge = 0;
    private int clockSkew = 300; // (5min/300seconds)
    private String keyManagementKeyAlgorithm = "";

    public MP21ConfigSettings() {
    }

    public MP21ConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType, String inHeader, String inCookie, String inAudience,
                              String inAlgorithm, String inDecryptKeyLoc, int inTokenAge, int inClockSkew, String inKeyMgmtKeyAlias) {

        super(inPublicKeyLocation, inPublicKey, inIssuer, inCertType, inHeader, inCookie, inAudience, inAlgorithm, inDecryptKeyLoc);
        tokenAge = inTokenAge;
        clockSkew = inClockSkew;
        keyManagementKeyAlgorithm = inKeyMgmtKeyAlias;
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

    public void setKeyManagementKeyAlgorithm(String inKeyMgmtKeyAlg) {
        keyManagementKeyAlgorithm = inKeyMgmtKeyAlg;
    }

    public String getKeyManagementKeyAlgorithm() {
        return keyManagementKeyAlgorithm;
    }

}