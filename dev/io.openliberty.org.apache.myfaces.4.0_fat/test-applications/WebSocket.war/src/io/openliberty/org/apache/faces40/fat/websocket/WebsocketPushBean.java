/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.org.apache.faces40.fat.websocket;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.faces.event.WebsocketEvent;
import jakarta.faces.event.WebsocketEvent.Closed;
import jakarta.faces.event.WebsocketEvent.Opened;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
        LOGGER.log(Level.INFO, "Method send was invoked successfully!");
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
