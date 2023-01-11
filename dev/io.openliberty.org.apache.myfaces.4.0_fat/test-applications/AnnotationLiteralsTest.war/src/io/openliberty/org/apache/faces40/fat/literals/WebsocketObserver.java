/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

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

/**
 * This class forces CDI to create a @Push @WebsocketEvent.Opened and @WebsocketEvent.Closed beans for testing
 */
@Named
@ApplicationScoped
public class WebsocketObserver implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(WebsocketObserver.class.getName());

    @Inject
    @Push(channel = EventBean.channelName)
    private PushContext myChannel;

    public void onOpen(@Observes @Opened WebsocketEvent event) {
        log.log(Level.INFO, "Channel {0} was opened successfully!", event.getChannel());
    }

    public void onClose(@Observes @Closed WebsocketEvent event) {
        log.log(Level.INFO, "Channel {0} was closed successfully!", event.getChannel());
    }
}
