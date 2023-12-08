/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.context.custom;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class KakfaCustomContextTestBean {

    public static final String DEFAULT_IN = "default-context-service-in";
    public static final String DEFAULT_OUT = "default-contest-service-out";

    public static final String PROPAGATE_ALL_IN = "propagate-all-in";
    public static final String PROPAGATE_ALL_OUT = "propagate-all-out";

    public static final String PROPAGATE_NONE_IN = "propagate-none-in";
    public static final String PROPAGATE_NONE_OUT = "propagate-none-out";

    public static final String PROPAGATE_APP_IN = "propagate-app-in";
    public static final String PROPAGATE_APP_OUT = "propagate-app-out";

    @Incoming(DEFAULT_IN)
    @Outgoing(DEFAULT_OUT)
    public String configuredDefaultStream(String input) {
        return processMessage(input);
    }

    @Incoming(PROPAGATE_ALL_IN)
    @Outgoing(PROPAGATE_ALL_OUT)
    public String propagateAllStream(String input) {
        return processMessage(input);
    }

    @Incoming(PROPAGATE_NONE_IN)
    @Outgoing(PROPAGATE_NONE_OUT)
    public String propagateNoneStream(String input) {
        return processMessage(input);
    }

    @Incoming(PROPAGATE_APP_IN)
    @Outgoing(PROPAGATE_APP_OUT)
    public String propagateAppOnlyStream(String input) {
        return processMessage(input);
    }

    private String processMessage(String input) {
        try {
            return input + "-" + getAppName() + "-" + isTcclSet();
        } catch (Exception e) {
            return e.toString();
        }
    }

    private String getAppName() {
        try {
            return (String) new InitialContext().lookup("java:app/AppName");
        } catch (NamingException e) {
            return "noapp";
        }
    }

    private boolean isTcclSet() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        // Check if Liberty's special classloader is the TCCL
        // Unfortunately, when the TCCL is not set, we get a context classloader which delegates based on the classes on the stack so this is the easiest way to determine whether we have a regular TCCL or not
        if (tccl.getClass().getName().endsWith("ThreadContextClassLoader")) {
            return true;
        } else {
            return false;
        }
    }

}
