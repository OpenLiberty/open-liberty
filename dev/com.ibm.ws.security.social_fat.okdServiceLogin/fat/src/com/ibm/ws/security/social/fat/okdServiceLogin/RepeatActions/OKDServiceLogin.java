/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.okdServiceLogin.RepeatActions;

import com.ibm.ws.security.social.fat.okdServiceLogin.FATSuite;

import componenttest.rules.repeater.RepeatTestAction;

public class OKDServiceLogin implements RepeatTestAction {

    protected String okdService = "Stub";
    protected String id = "Stub";

    OKDServiceLogin(String okdServiceIn) {

        okdService = okdServiceIn;
        id = okdServiceIn;
    }

    OKDServiceLogin(String okdServiceIn, String idIn) {

        okdService = okdServiceIn;
        id = idIn;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {

        FATSuite.OKDService = okdService;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        if (id != null) {
            return id;
        } else {
            return "default" ; 
        }
    }
}
