/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health20.fails.to.start.health.check.app;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class BasicMessagingBean {

    public static final String CHANNEL_IN = "test-in";
    public static final String CHANNEL_OUT = "test-out";

    @Incoming(CHANNEL_IN)
    @Outgoing(CHANNEL_OUT)
    public void receive(String in) {

    }

}