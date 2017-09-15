/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2;

/**
 * CSIv2 Minor Codes. Uses a subset of the minor codes from tWAS to maintain compatibility.
 * When adding new minor codes, please ensure compatibility with minor codes in tWAS.
 */
public class SecurityMinorCodes {

    public static final int SECURITY_FAMILY_BASE = 0x49424300;
    public static final int AUTHENTICATION_FAILED = SECURITY_FAMILY_BASE + 0x0;
    public static final int INVALID_IDENTITY_TOKEN = SECURITY_FAMILY_BASE + 0xC;
    public static final int IDENTITY_SERVER_NOT_TRUSTED = SECURITY_FAMILY_BASE + 0xD;

    public static final int SECURITY_DISTRIBUTED_BASE = 0x49421090;
    public static final int CREDENTIAL_NOT_AVAILABLE = SECURITY_DISTRIBUTED_BASE + 0x2;
    public static final int SECURITY_MECHANISM_NOT_SUPPORTED = SECURITY_DISTRIBUTED_BASE + 0x3;
    public static final int GSS_FORMAT_ERROR = SECURITY_DISTRIBUTED_BASE + 0x35;

}
