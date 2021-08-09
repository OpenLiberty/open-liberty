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
package javax.faces.lifecycle;

import java.util.Map;
import javax.faces.context.FacesContext;

/**
 * @since 2.2
 */
public abstract class ClientWindow
{
    
    public static final String CLIENT_WINDOW_MODE_PARAM_NAME = 
            "javax.faces.CLIENT_WINDOW_MODE";
    
    private static final String CLIENT_WINDOW_RENDER_MODE_DISABLED = 
            "org.apache.myfaces.CLIENT_WINDOW_URL_QUERY_PARAMETER_DISABLED";
    
    public abstract void decode(FacesContext context);
    
    public abstract String getId();
    
    public abstract Map<String,String> getQueryURLParameters(FacesContext context);
    
    public boolean isClientWindowRenderModeEnabled(FacesContext context)
    {
        // By default is enabled, so it is easier to check the opposite.
        return !Boolean.TRUE.equals(
                context.getAttributes().get(CLIENT_WINDOW_RENDER_MODE_DISABLED));
    }
    
    public void disableClientWindowRenderMode(FacesContext context)
    {
        context.getAttributes().put(CLIENT_WINDOW_RENDER_MODE_DISABLED, Boolean.TRUE);
    }
    
    public void enableClientWindowRenderMode(FacesContext context)
    {
        context.getAttributes().put(CLIENT_WINDOW_RENDER_MODE_DISABLED, Boolean.FALSE);
    }
}
