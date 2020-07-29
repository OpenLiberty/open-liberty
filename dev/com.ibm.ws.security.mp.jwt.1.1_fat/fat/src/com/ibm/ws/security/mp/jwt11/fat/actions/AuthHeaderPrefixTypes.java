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

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.FATSuite;

import componenttest.rules.repeater.RepeatTestAction;

public class AuthHeaderPrefixTypes implements RepeatTestAction {

    String tokenType = MpJwtFatConstants.TOKEN_TYPE_BEARER;
//    protected String currentID = null;

    AuthHeaderPrefixTypes(String inTokenType) {

        tokenType = inTokenType;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {

        FATSuite.authHeaderPrefix = tokenType;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        if (tokenType != null) {
            return tokenType;
        } else {
            return toString();
        }
    }
}
