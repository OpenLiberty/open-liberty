/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.invalid.nolib;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class NoLibMessagingBean {

    public static final String CHANNEL_IN = "shared-lib-in";
    public static final String CHANNEL_OUT = "shared-lib-out";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public String reverseString(String in) {
        StringBuilder sb = new StringBuilder(in);
        sb.reverse();
        return sb.toString();
    }

}
