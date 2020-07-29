/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.config;

/**
 *
 */
public interface MpConstants {

    public final static String ISSUER = "mp.jwt.verify.issuer";
    public final static String PUBLIC_KEY = "mp.jwt.verify.publickey";
    public final static String KEY_LOCATION = "mp.jwt.verify.publickey.location";

    // Properties added by 1.2 specification
    public final static String PUBLIC_KEY_ALG = "mp.jwt.verify.publickey.algorithm";
    public final static String DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";
    public final static String VERIFY_AUDIENCES = "mp.jwt.verify.audiences";
    public final static String TOKEN_HEADER = "mp.jwt.token.header";
    public final static String TOKEN_COOKIE = "mp.jwt.token.cookie";

}
