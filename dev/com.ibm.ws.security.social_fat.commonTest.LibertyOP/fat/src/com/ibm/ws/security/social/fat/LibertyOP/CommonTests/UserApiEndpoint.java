/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.LibertyOP.CommonTests;

import com.ibm.ws.security.social.fat.LibertyOP.FATSuite;

import componenttest.rules.repeater.RepeatTestAction;

public class UserApiEndpoint implements RepeatTestAction {

    protected String endpoint = "userinfo";

    UserApiEndpoint(String endpointIn) {

        endpoint = endpointIn;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {

        FATSuite.UserApiEndpoint = endpoint;
    }

    @Override
    public String getID() {
        if (endpoint != null) {
            return endpoint;
        } else {
            return "default" ;
        }
    }
}
