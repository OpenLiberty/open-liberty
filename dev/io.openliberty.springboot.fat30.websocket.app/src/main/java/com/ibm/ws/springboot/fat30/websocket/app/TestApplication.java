/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.fat30.websocket.app;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.AbstractHandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.ibm.ws.springboot.fat30.websocket.echo.ServerEchoWebSocketEndpoint;


@SpringBootApplication
@EnableWebSocket
public class TestApplication extends SpringBootServletInitializer implements WebSocketConfigurer {
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(TestApplication.class);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

	@Bean
	public ServerEchoWebSocketEndpoint serverEchoWebSocketEndpoint() {
		return new ServerEchoWebSocketEndpoint();
	}

	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	
	@Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(customWebSocketHandler(), "/customHandler").setHandshakeHandler(customHandshakeHandler());
    }

    @Bean
    public TextWebSocketHandler customWebSocketHandler() {
        return new TextWebSocketHandler() {
            @Override
            public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                session.sendMessage(new TextMessage("Did you say: " + message.getPayload()));
            }
        };
    }

    @Bean
    public AbstractHandshakeHandler customHandshakeHandler() {
        return new DefaultHandshakeHandler();
    }	
}
