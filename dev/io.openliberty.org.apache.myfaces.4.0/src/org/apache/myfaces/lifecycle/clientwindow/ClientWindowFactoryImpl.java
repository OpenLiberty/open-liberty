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
package org.apache.myfaces.lifecycle.clientwindow;

import org.apache.myfaces.util.token.TokenGenerator;
import jakarta.faces.context.FacesContext;
import jakarta.faces.lifecycle.ClientWindow;
import jakarta.faces.lifecycle.ClientWindowFactory;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.FacesConfigurator;
import org.apache.myfaces.util.WebConfigParamUtils;

/**
 *
 * @since 2.2
 * @author Leonardo Uribe
 */
public class ClientWindowFactoryImpl extends ClientWindowFactory
{
    @JSFWebConfigParam(since="2.2",defaultValue="url")
    public static final String INIT_PARAM_DEFAULT_WINDOW_MODE = 
        "org.apache.myfaces.DEFAULT_WINDOW_MODE";

    public static final String WINDOW_MODE_NONE = "none";
    public static final String WINDOW_MODE_URL = "url";
    public static final String WINDOW_MODE_URL_REDIRECT = "url-redirect";
    public static final String WINDOW_MODE_CLIENT = "client";
    
    private String windowMode;
    private TokenGenerator windowTokenGenerator;
    private ClientConfig clientConfig;
    private WindowContextConfig windowContextConfig;

    public ClientWindowFactoryImpl()
    {
        windowTokenGenerator = new TokenGenerator();
        clientConfig = new ClientConfig();
        windowContextConfig = new WindowContextConfig();
    }

    @Override
    public ClientWindow getClientWindow(FacesContext facesContext)
    {
        if (WINDOW_MODE_NONE.equals(getWindowMode(facesContext)))
        {
            //No need to do anything
            return null;
        }
        else
        {
            if (WINDOW_MODE_URL.equals(getWindowMode(facesContext)))
            {
                return new UrlClientWindow(windowTokenGenerator);
            }
            if (WINDOW_MODE_URL_REDIRECT.equals(getWindowMode(facesContext)))
            {
                return new UrlRedirectClientWindow(windowTokenGenerator);
            }
            else if (WINDOW_MODE_CLIENT.equals(getWindowMode(facesContext)))
            {
                return new CODIClientSideWindow(windowTokenGenerator, windowContextConfig, clientConfig);
            }

            return null;
        }
    }
    
    private String getWindowMode(FacesContext context)
    {
        if (windowMode == null)
        {
            if (FacesConfigurator.isEnableDefaultWindowMode(context))
            {
                String defaultWindowMode = WebConfigParamUtils.getStringInitParameter(
                        context.getExternalContext(), 
                        INIT_PARAM_DEFAULT_WINDOW_MODE, 
                        WINDOW_MODE_URL);
                windowMode = WebConfigParamUtils.getStringInitParameter(
                        context.getExternalContext(), 
                        ClientWindow.CLIENT_WINDOW_MODE_PARAM_NAME, 
                        defaultWindowMode);
            }
            else
            {
                windowMode = WebConfigParamUtils.getStringInitParameter(
                        context.getExternalContext(), 
                        ClientWindow.CLIENT_WINDOW_MODE_PARAM_NAME, 
                        WINDOW_MODE_NONE);
            }
        }
        return windowMode;
    }

}
