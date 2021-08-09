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
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import com.ibm.ws.security.fat.common.utils.MySkipRule;

public class RSSkipIfJWTToken extends MySkipRule {

    public static Boolean useJWT = false;

    public Boolean callSpecificCheck() {
        return useJWT;
    }
}