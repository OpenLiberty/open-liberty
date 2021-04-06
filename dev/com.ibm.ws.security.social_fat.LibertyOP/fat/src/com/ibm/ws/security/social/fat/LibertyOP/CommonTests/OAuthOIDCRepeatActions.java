/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.LibertyOP.CommonTests;

import componenttest.rules.repeater.RepeatTestAction;

public class OAuthOIDCRepeatActions implements RepeatTestAction {

    public static final String oidc_type = "OIDC";
    public static final String oauth_type = "OAuth";

    protected String type = oidc_type;

    public OAuthOIDCRepeatActions(String inType) {

        type = inType;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {

    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        if (type != null) {
            return type;
        } else {
            return "default" ;
        }
    }
}
