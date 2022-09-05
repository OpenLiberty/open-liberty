/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.push;

import org.apache.myfaces.push.cdi.WebsocketApplicationSessionHolder;
import java.io.IOException;
import java.io.Serializable;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.faces.event.WebsocketEvent;
import jakarta.faces.event.WebsocketEvent.Closed;
import jakarta.faces.event.WebsocketEvent.Opened;
import jakarta.faces.push.PushContext;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

/**
 *
 */
public class EndpointImpl extends Endpoint
{

    public static final String JAKARTA_FACES_PUSH_PATH = PushContext.URI_PREFIX + "/{channel}";

    public static final String PUSH_CHANNEL_PARAMETER = "channel";
    
    private static final AnnotationLiteral<Opened> OPENED = 
            new AnnotationLiteral<Opened>() 
            {
                private static final long serialVersionUID = 2789324L;
            };
    private static final AnnotationLiteral<Closed> CLOSED = 
            new AnnotationLiteral<Closed>() 
            {
                private static final long serialVersionUID = 38450203L;
            };

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        // Get the channel and the channel id
        String channel = session.getPathParameters().get(PUSH_CHANNEL_PARAMETER);
        String channelToken = session.getQueryString();

        // Locate holder
        // Note in this point there is no session scope because there is no HttpSession available,
        // but on the handshake there is. So, everything below should use CDI application scoped
        // beans only.

        if (Boolean.TRUE.equals(config.getUserProperties().get(WebsocketConfigurator.WEBSOCKET_VALID)) &&
                WebsocketApplicationSessionHolder.addOrUpdateSession(channelToken, session))
        {
            session.setMaxIdleTimeout((Long) config.getUserProperties().getOrDefault(
                    WebsocketConfigurator.MAX_IDLE_TIMEOUT, 300000L));

            Serializable user = (Serializable) session.getUserProperties().get(WebsocketConfigurator.WEBSOCKET_USER);

            BeanManager beanManager = CDI.current().getBeanManager();
            beanManager.getEvent().select(OPENED).fire(new WebsocketEvent(channel, user, null));

            session.getUserProperties().put(
                    WebsocketSessionClusterSerializedRestore.WEBSOCKET_SESSION_SERIALIZED_RESTORE, 
                    new WebsocketSessionClusterSerializedRestore(channelToken));
        }
        else
        {
            try
            {
                session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION,
                        "Websocket connection not registered in current session"));
            }
            catch (IOException ex)
            {
                onError(session, ex);
            }
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason)
    {
        // Get the channel and the channel id
        String channel = session.getPathParameters().get(PUSH_CHANNEL_PARAMETER);
        String channelToken = session.getQueryString();

        Serializable user = (Serializable) session.getUserProperties().get(WebsocketConfigurator.WEBSOCKET_USER);

        // In this point in some cases (close reason 1006) CDI does not work and you cannot lookup application
        // scope beans, because the context could not be properly initialized. In that case we should try to
        // propagate the event but if an error happens, just ensure the Session is properly disposed.
        try
        {
            BeanManager beanManager = CDI.current().getBeanManager();
            beanManager.getEvent().select(CLOSED).fire(new WebsocketEvent(channel, user, closeReason.getCloseCode()));
        }
        catch(Exception e)
        {
            //No op because it is expected something could go wrong.
        }
        finally
        {
            WebsocketApplicationSessionHolder.removeSession(channelToken);
        }
    }

    @Override
    public void onError(Session session, Throwable ex)
    {
        if (session.isOpen())
        {
            session.getUserProperties().put(Throwable.class.getName(), ex);
        }
    }
}
