/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.jndi.strings;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class ObserverBean {

    private String result;

    public String getResult() {
        return result;
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
        try {
            result = (String) new InitialContext().lookup("java:app/env/com.ibm.ws.cdi.jndi.test.result");
        } catch (NamingException e) {
            result = "test failed " + e.getMessage();
        }
    }
}
