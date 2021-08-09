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

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;

public abstract class LifecycleWrapper extends Lifecycle implements FacesWrapper<Lifecycle>
{
    
    public void render(FacesContext context) throws FacesException
    {
        getWrapped().render(context);
    }

    public void removePhaseListener(PhaseListener listener)
    {
        getWrapped().removePhaseListener(listener);
    }

    public PhaseListener[] getPhaseListeners()
    {
        return getWrapped().getPhaseListeners();
    }

    public void execute(FacesContext context) throws FacesException
    {
        getWrapped().execute(context);
    }

    public void attachWindow(FacesContext context)
    {
        getWrapped().attachWindow(context);
    }

    public void addPhaseListener(PhaseListener listener)
    {
        getWrapped().addPhaseListener(listener);
    }
    
    public abstract Lifecycle getWrapped();
    
}
