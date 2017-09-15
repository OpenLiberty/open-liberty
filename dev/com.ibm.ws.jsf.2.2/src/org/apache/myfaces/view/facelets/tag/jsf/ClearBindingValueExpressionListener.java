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
package org.apache.myfaces.view.facelets.tag.jsf;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;

/**
 * MyFaces specific listener used to clear all component binding if
 * normal navigation occur during a POST request.
 * 
 * @author Leonardo Uribe
 * @since 2.0.6
 *
 */
public class ClearBindingValueExpressionListener implements ComponentSystemEventListener, Serializable
{
    
    /**
     * 
     */
    private static final long serialVersionUID = -6066524284031941519L;
    
    public ClearBindingValueExpressionListener()
    {
        super();
    }

    public void processEvent(ComponentSystemEvent event)
    {
        try
        {
            event.getComponent().getValueExpression("binding").setValue(
                    FacesContext.getCurrentInstance().getELContext(), null);
        }
        catch(ELException e)
        {
            Logger log = Logger.getLogger(ClearBindingValueExpressionListener.class.getName());
            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "Cannot reset binding for: " + event.getComponent().getClientId(), e);
            }
        }
        catch(NullPointerException e)
        {
            Logger log = Logger.getLogger(ClearBindingValueExpressionListener.class.getName());
            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "Cannot reset binding for: " + event.getComponent().getClientId(), e);
            }
        }
    }

}
