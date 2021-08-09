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
package org.apache.myfaces.webapp;

import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.faces.context.FacesContext;

/**
 * EL context listener which installs the faces context (if present) into el context and dispatches el context events to
 * faces application el context listeners.
 * 
 * @author Mathias Broekelmann (latest modification by $Author: mbr $)
 * @version $Revision: 514285 $ $Date: 2007-03-04 00:15:00 +0000 (Sun, 04 Mar 2007) $
 */
public class FacesELContextListener implements ELContextListener
{
    public void contextCreated(ELContextEvent ece)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null)
        {
            ece.getELContext().putContext(FacesContext.class, facesContext);

            for (ELContextListener listener : facesContext.getApplication().getELContextListeners())
            {
                listener.contextCreated(ece);
            }
        }
    }

}
