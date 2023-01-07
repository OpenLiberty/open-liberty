/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
public class JNDIStrings {

    @PostConstruct
    public void bind() {
        try {
            InitialContext ctx = new InitialContext();
            ctx.bind("myApp/test2", "Value from Bind");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public String getFromConfig() {
        String fromConfig = null;
        try {
            fromConfig = (String) new InitialContext().lookup("myApp/test1");
        } catch (NamingException e) {
            e.printStackTrace();
            fromConfig = e.getMessage();
        }
        return fromConfig;
    }

    public String getFromBind() {
        String fromBind = null;
        try {
            fromBind = (String) new InitialContext().lookup("myApp/test2");
        } catch (NamingException e) {
            e.printStackTrace();
            fromBind = e.getMessage();
        }
        return fromBind;
    }
}
