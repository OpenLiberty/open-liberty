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
package com.ibm.ws.security.fat.common.jwt.utils;

import java.security.Key;

import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.utils.KeyTools;

import componenttest.topology.impl.LibertyServer;

@SuppressWarnings("restriction")
public class JwtKeyTools extends KeyTools {

    public static Class<?> thisClass = JwtKeyTools.class;

    /* key file names */
    public static final String rs256PubKey = "RS256public-key.pem";
    public static final String rs384PubKey = "RS384public-key.pem";
    public static final String rs512PubKey = "RS512public-key.pem";
    public static final String es256PubKey = "ES256public-key.pem";
    public static final String es384PubKey = "ES384public-key.pem";
    public static final String es512PubKey = "ES512public-key.pem";
    public static final String ps256PubKey = "PS256public-key.pem";
    public static final String ps384PubKey = "PS384public-key.pem";
    public static final String ps512PubKey = "PS512public-key.pem";
    public static final String rs256PrivKey = "RS256private-key.pem";
    public static final String rs384PrivKey = "RS384private-key.pem";
    public static final String rs512PrivKey = "RS512private-key.pem";
    public static final String es256PrivKey = "ES256private-key.pem";
    public static final String es384PrivKey = "ES384private-key.pem";
    public static final String es512PrivKey = "ES512private-key.pem";
    public static final String ps256PrivKey = "PS256private-key.pem";
    public static final String ps384PrivKey = "PS384private-key.pem";
    public static final String ps512PrivKey = "PS512private-key.pem";

    public static String getComplexPublicKeyForSigAlg(LibertyServer server, String sigAlg) throws Exception {

        return getComplexKey(server, getPublicKeyFileNameForAlg(sigAlg));
    }

    public static Key getPublicKeyForSigAlg(LibertyServer server, String sigAlg) throws Exception {

        return getPublicKeyFromPem(getComplexKey(server, getPublicKeyFileNameForAlg(sigAlg)));
    }

    public static String getComplexPrivateKeyForSigAlg(LibertyServer server, String sigAlg) throws Exception {

        return getComplexKey(server, getPrivateKeyFileNameForAlg(sigAlg));
    }

    public static String getPublicKeyFileNameForAlg(String sigAlg) throws Exception {

        switch (sigAlg) {
            case JwtConstants.SIGALG_RS256:
                return rs256PubKey;
            case JwtConstants.SIGALG_RS384:
                return rs384PubKey;
            case JwtConstants.SIGALG_RS512:
                return rs512PubKey;
            case JwtConstants.SIGALG_ES256:
                return es256PubKey;
            case JwtConstants.SIGALG_ES384:
                return es384PubKey;
            case JwtConstants.SIGALG_ES512:
                return es512PubKey;
            case JwtConstants.SIGALG_PS256:
                return ps256PubKey;
            case JwtConstants.SIGALG_PS384:
                return ps384PubKey;
            case JwtConstants.SIGALG_PS512:
                return ps512PubKey;
            default:
                return rs256PubKey;
        }

    }

    public static String getPrivateKeyFileNameForAlg(String sigAlg) throws Exception {

        switch (sigAlg) {
            case JwtConstants.SIGALG_RS256:
                return rs256PrivKey;
            case JwtConstants.SIGALG_RS384:
                return rs384PrivKey;
            case JwtConstants.SIGALG_RS512:
                return rs512PrivKey;
            case JwtConstants.SIGALG_ES256:
                return es256PrivKey;
            case JwtConstants.SIGALG_ES384:
                return es384PrivKey;
            case JwtConstants.SIGALG_ES512:
                return es512PrivKey;
            case JwtConstants.SIGALG_PS256:
                return ps256PrivKey;
            case JwtConstants.SIGALG_PS384:
                return ps384PrivKey;
            case JwtConstants.SIGALG_PS512:
                return ps512PrivKey;
            default:
                return rs256PrivKey;
        }

    }

}