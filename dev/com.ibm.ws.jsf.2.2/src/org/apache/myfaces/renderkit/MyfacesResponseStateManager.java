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
package org.apache.myfaces.renderkit;

import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;

/**
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @version $Revision: 1539875 $ $Date: 2013-11-08 00:13:51 +0000 (Fri, 08 Nov 2013) $
 * 
 */
public abstract class MyfacesResponseStateManager extends ResponseStateManager
{

    /**
     * Execute additional operations like save the state on a cache when server
     * side state saving is used.
     */
    public void saveState(FacesContext facesContext, Object state)
    {
    }
    
    /**
     * Indicates if the call to ResponseStateManager.writeState should be done after the view is fully rendered.
     * Usually this is required for client side state saving, but it is not for server side state saving, because
     * ResponseStateManager.writeState could render a just a marker and then StateManager.saveState could be called,
     * preventing use an additional buffer. 
     * 
     * @param facesContext
     * @return
     */
    public boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext)
    {
        return true;
    }
}
