/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.actions;

import com.ibm.ws.security.fat.common.mp.jwt.MPJwt11FatConstants;

public class AuthHeaderPrefixRepeatActions {

    public static AuthHeaderPrefixTypes asBearerType() {

        return new AuthHeaderPrefixTypes(MPJwt11FatConstants.TOKEN_TYPE_BEARER);
    }

    public static AuthHeaderPrefixTypes asTokenType() {

        return new AuthHeaderPrefixTypes(MPJwt11FatConstants.TOKEN_TYPE_TOKEN);
    }

    public static AuthHeaderPrefixTypes asMiscType() {

        return new AuthHeaderPrefixTypes(MPJwt11FatConstants.TOKEN_TYPE_MISC);
    }
}
