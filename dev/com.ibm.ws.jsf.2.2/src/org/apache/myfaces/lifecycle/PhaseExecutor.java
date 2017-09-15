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
package org.apache.myfaces.lifecycle;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;

/**
 * Implements the PhaseExecutor for a lifecycle
 * 
 * @author Nikolay Petrov (latest modification by $Author: jakobk $)
 * @version $Revision: 949866 $ $Date: 2010-05-31 21:12:04 +0000 (Mon, 31 May 2010) $
 */
abstract class PhaseExecutor
{

    /**
     * Executes a phase of the JavaServer(tm) Faces lifecycle, like UpdateModelValues. The <code>execute</code> method
     * is called by the lifecylce implementation's private <code>executePhase</code>.
     * 
     * @param facesContext
     *            The <code>FacesContext</code> for the current request we are processing
     * @return <code>true</code> if execution should be stopped
     */
    public abstract boolean execute(FacesContext facesContext);

    /**
     * Returns the <code>PhaseId</code> for which the implemented executor is invoked
     * 
     * @return
     */
    public abstract PhaseId getPhase();
    
    /**
     * This method will be called by LifecycleImpl before the phase associated with this
     * PhaseExecutor actually starts (before the before-PhaseListeners are called). 
     * Thus the PhaseExecutor implementation will be able to do some pre-phase initialisation work.
     * 
     * @param facesContext
     * @since 2.0.1
     */
    public void doPrePhaseActions(FacesContext facesContext)
    {
        // default: nothing
    }
    
}
