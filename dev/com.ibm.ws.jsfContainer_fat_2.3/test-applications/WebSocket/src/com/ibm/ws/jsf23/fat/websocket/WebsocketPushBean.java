/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.websocket;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.WebsocketEvent;
import javax.faces.event.WebsocketEvent.Closed;
import javax.faces.event.WebsocketEvent.Opened;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@ApplicationScoped
public class WebsocketPushBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(WebsocketPushBean.class.getName());

    @Inject
    @Push
    private PushContext myChannel;

    public void send() {
        myChannel.send("Message from the server via push!");
    }

    public void onOpen(@Observes @Opened WebsocketEvent event) {
        String channel = event.getChannel();
        LOGGER.log(Level.INFO, "Channel {0} was opened successfully!", channel);
    }

    public void onClose(@Observes @Closed WebsocketEvent event) {
        String channel = event.getChannel();
        LOGGER.log(Level.INFO, "Channel {0} was closed successfully!", channel);
    }

}
