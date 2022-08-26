/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * For WebSockets 2.1 : https://github.com/jakartaee/websocket/issues/235
 * 
 * Wraps the ServerEndpointConfig object and produces an individualized copy of the user properties. 
 */
public class ServerEndpointConfigCopyPerSession implements ServerEndpointConfig {
    
    ServerEndpointConfig epc = null;
    Map<String, Object> copy = new HashMap<String, Object>();

    public ServerEndpointConfigCopyPerSession (ServerEndpointConfig epc){
        this.epc = epc;
        if(this.epc.getUserProperties() != null){
            for (Map.Entry<String,Object> entry : this.epc.getUserProperties().entrySet()){
                copy.put(entry.getKey(),entry.getValue());
            }
        }
    }

    public Map<String, Object> getUserProperties() {
        return copy;
    }

    public List<Class<? extends Decoder>> getDecoders() {
        return this.epc.getDecoders();
    }

    public List<Class<? extends Encoder>> getEncoders() {
        return this.epc.getEncoders();
    }

    public Class<?> getEndpointClass() {
        return this.epc.getEndpointClass();
    }

    public String getPath() {
        return this.epc.getPath();
    }

    public List<String> getSubprotocols() {
        return this.epc.getSubprotocols();
    }

    public  List<Extension> getExtensions() {
        return this.epc.getExtensions();
    }

    public ServerEndpointConfig.Configurator getConfigurator() { 
        return this.epc.getConfigurator();
    }
}
