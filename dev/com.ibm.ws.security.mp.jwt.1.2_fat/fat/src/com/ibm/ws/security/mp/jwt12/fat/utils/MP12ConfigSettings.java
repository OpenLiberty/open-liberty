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
package com.ibm.ws.security.mp.jwt12.fat.utils;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.MPConfigSettings;

public class MP12ConfigSettings extends MPConfigSettings {

    public static final String Header = MpJwt12FatConstants.AUTHORIZATION;
    public static final String Cookie = MpJwt12FatConstants.TOKEN_TYPE_BEARER;
    public final static String HeaderNotSet = "";
    public final static String CookieNotSet = "";
    public final static String AudiencesNotSet = "";

    private String header = Header;
    private String cookie = Cookie;
    private String audience = AudiencesNotSet;

    public MP12ConfigSettings() {
    }

    public MP12ConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType, String inHeader, String inCookie, String inAudience) {

        super(inPublicKeyLocation, inPublicKey, inIssuer, inCertType);
        header = inHeader;
        cookie = inCookie;
        audience = inAudience;
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
}