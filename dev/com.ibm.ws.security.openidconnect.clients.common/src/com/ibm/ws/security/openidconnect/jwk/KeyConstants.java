/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.jwk;

public class KeyConstants {
    public static final String kid = "kid";
    public static final String x5t = "x5t"; //Thumprint
    public static final String alg = "alg";
    public static final String use = "use"; //optional
    public static final String kty = "kty"; //key type, required
    public static final String sig = "sig";
    public static final String RSA = "RSA";
    public static final String RS256 = "RS256";
    public static final String HS256 = "HS256";
    public static final String n = "n";
    public static final String e = "e";
    public static final String x = "x"; // for EC
    public static final String y = "y"; // for EC
    public static final String crv = "crv"; // for EC

}
