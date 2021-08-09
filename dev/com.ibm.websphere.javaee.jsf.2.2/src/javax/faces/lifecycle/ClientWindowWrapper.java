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
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;

/**
 *
 * @since 2.2
 */
public abstract class ClientWindowWrapper extends ClientWindow 
    implements FacesWrapper<ClientWindow>
{

    public void decode(FacesContext context)
    {
        getWrapped().decode(context);
    }

    public String getId()
    {
        return getWrapped().getId();
    }

    public Map<String, String> getQueryURLParameters(FacesContext context)
    {
        return getWrapped().getQueryURLParameters(context);
    }

    public boolean isClientWindowRenderModeEnabled(FacesContext context)
    {
        return getWrapped().isClientWindowRenderModeEnabled(context);
    }

    public void disableClientWindowRenderMode(FacesContext context)
    {
        getWrapped().disableClientWindowRenderMode(context);
    }

    public void enableClientWindowRenderMode(FacesContext context)
    {
        getWrapped().enableClientWindowRenderMode(context);
    }
    
    public abstract ClientWindow getWrapped();

}
