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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.WebsocketEvent;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Named;

@Named(value = "eventBean")
@ApplicationScoped
public class EventBean implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String channelName = "testChannel";

    public void logOpenWebsocket() {
        WebsocketEvent opened = CDI.current().select(new TypeLiteral<WebsocketEvent>() {}, WebsocketEvent.Opened.Literal.INSTANCE).get();
        String message = "WebsocketEvent.Opened=" + Boolean.valueOf(opened != null && opened.getChannel().equals(channelName));
        FacesContext.getCurrentInstance().addMessage("open:messages", new FacesMessage(message));
    }

    public void pushToChannel() {
        PushContext push = CDI.current().select(new TypeLiteral<PushContext>() {}, Push.Literal.of(channelName)).get();
        String message = "Push=" + Boolean.valueOf(push != null);
        FacesContext.getCurrentInstance().addMessage("push:messages", new FacesMessage(message));

        push.send("testMessage");
    }

    public void logCloseWebsocket() {
        WebsocketEvent closed = CDI.current().select(new TypeLiteral<WebsocketEvent>() {}, WebsocketEvent.Closed.Literal.INSTANCE).get();
        String message = "WebsocketEvent.Closed=" + Boolean.valueOf(closed != null && closed.getChannel().equals(channelName));
        FacesContext.getCurrentInstance().addMessage("close:messages", new FacesMessage(message));
    }
}
