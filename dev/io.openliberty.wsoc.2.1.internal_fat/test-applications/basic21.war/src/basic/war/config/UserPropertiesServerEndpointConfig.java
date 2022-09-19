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
package basic.war.config;

import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* 
 *  The ServerEndpointConfig which contain the inital list of user properties. 
 */
public class UserPropertiesServerEndpointConfig implements ServerEndpointConfig {
    
    private static final Map<String, Object> SERVER_USER_PROPERTIES =  new HashMap<>();
    
    static {
      SERVER_USER_PROPERTIES.put("SERVER-1", new Object());
    }
    
    public Map<String, Object> getUserProperties() {
      return SERVER_USER_PROPERTIES;
    }
    
    public Class<?> getEndpointClass() {
      return basic.war.UserPropertiesServerEP.class;
    }
    
    public String getPath() {
      return "/userproperties";
    }
    
    public List<String> getSubprotocols() {
      return Collections.emptyList();
    }
    
    public List<Extension> getExtensions() {
      return Collections.emptyList();
    }
    
    public ServerEndpointConfig.Configurator getConfigurator() {
      return (ServerEndpointConfig.Configurator)new UserPropertiesConfigurator();
    }
    
    public List<Class<? extends Encoder>> getEncoders() {
      return Collections.emptyList();
    }
    
    public List<Class<? extends Decoder>> getDecoders() {
      return Collections.emptyList();
    }
  }
