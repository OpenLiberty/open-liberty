/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import java.util.HashMap;

import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

public class PolicyContextHandlerImpl implements PolicyContextHandler {

    private static boolean initialized = false;

    private static final String[] keysArray = new String[] {
                                                             // Maintain order from EE8-. Probably doesn't matter.
                                                             "javax.security.auth.Subject.container",
                                                             "javax.xml.soap.SOAPMessage",
                                                             "javax.servlet.http.HttpServletRequest",
                                                             "javax.ejb.EnterpriseBean",
                                                             "javax.ejb.arguments",

                                                             // EE9+ unique keys below here.
                                                             "jakarta.xml.soap.SOAPMessage",
                                                             "jakarta.servlet.http.HttpServletRequest",
                                                             "jakarta.ejb.EnterpriseBean",
                                                             "jakarta.ejb.arguments"
    };

    private static PolicyContextHandlerImpl pchi;

    private PolicyContextHandlerImpl() {}

    public static PolicyContextHandlerImpl getInstance() {
        if (!initialized) {
            pchi = new PolicyContextHandlerImpl();
            initialized = true;
        }
        return pchi;
    }

    @Override
    public boolean supports(String key) throws PolicyContextException {
        for (String value : keysArray) {
            if (key.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getKeys() throws PolicyContextException {
        return keysArray.clone();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getContext(String key, Object object) throws PolicyContextException {
        if (object == null) {
            return null;
        }
        return ((HashMap<String, Object>) object).get(key);
    }

}
