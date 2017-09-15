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

import java.util.HashMap;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

/**
 * This class encapsulates the logic used to call PhaseListeners. It was needed because of issue 9 of the JSF 1.2 spec.
 * See section 11.3 for more details.
 * 
 * @author Stan Silvert
 */
class PhaseListenerManager
{
    private Lifecycle lifecycle;
    private FacesContext facesContext;
    private PhaseListener[] phaseListeners;

    // Tracks success in the beforePhase. Listeners that throw an exception
    // in beforePhase or were never called because a previous listener threw
    // an exception should not have its afterPhase called
    private Map<PhaseId, boolean[]> listenerSuccessMap = new HashMap<PhaseId, boolean[]>();

    /** Creates a new instance of PhaseListenerManager */
    PhaseListenerManager(Lifecycle lifecycle, FacesContext facesContext, PhaseListener[] phaseListeners)
    {
        this.lifecycle = lifecycle;
        this.facesContext = facesContext;
        this.phaseListeners = phaseListeners;
    }

    private boolean isListenerForThisPhase(PhaseListener phaseListener, PhaseId phaseId)
    {
        int listenerPhaseId = phaseListener.getPhaseId().getOrdinal();
        return (listenerPhaseId == PhaseId.ANY_PHASE.getOrdinal() || listenerPhaseId == phaseId.getOrdinal());
    }

    void informPhaseListenersBefore(PhaseId phaseId)
    {
        boolean[] beforePhaseSuccess = new boolean[phaseListeners.length];
        listenerSuccessMap.put(phaseId, beforePhaseSuccess);

        PhaseEvent event = new PhaseEvent(facesContext, phaseId, lifecycle);

        for (int i = 0; i < phaseListeners.length; i++)
        {
            PhaseListener phaseListener = phaseListeners[i];
            if (isListenerForThisPhase(phaseListener, phaseId))
            {
                try
                {
                    phaseListener.beforePhase(event);
                    beforePhaseSuccess[i] = true;
                }
                catch (Throwable e)
                {
                    beforePhaseSuccess[i] = false; // redundant - for clarity
                    
                    // JSF 2.0: publish exceptions instead of logging them.
                    
                    publishException (e, phaseId, ExceptionQueuedEventContext.IN_BEFORE_PHASE_KEY);
                    
                    return;
                }
            }
        }
    }

    void informPhaseListenersAfter(PhaseId phaseId)
    {
        boolean[] beforePhaseSuccess = listenerSuccessMap.get(phaseId);
        
        if (beforePhaseSuccess == null)
        {
            // informPhaseListenersBefore method was not called : maybe an exception in LifecycleImpl.executePhase  
            return;
        }

        PhaseEvent event = null;

        for (int i = phaseListeners.length - 1; i >= 0; i--)
        {
            PhaseListener phaseListener = phaseListeners[i];
            if (isListenerForThisPhase(phaseListener, phaseId) && beforePhaseSuccess[i])
            {
                if (event == null)
                {
                    event = new PhaseEvent(facesContext, phaseId, lifecycle);
                }
                try
                {
                    phaseListener.afterPhase(event);
                }
                catch (Throwable e)
                {
                    // JSF 2.0: publish exceptions instead of logging them.
                    
                    publishException (e, phaseId, ExceptionQueuedEventContext.IN_AFTER_PHASE_KEY);
                }
            }
        }

    }
    
    private void publishException (Throwable e, PhaseId phaseId, String key)
    {
        ExceptionQueuedEventContext context = new ExceptionQueuedEventContext (facesContext, e, null, phaseId);
        
        context.getAttributes().put (key, Boolean.TRUE);
        
        facesContext.getApplication().publishEvent (facesContext, ExceptionQueuedEvent.class, context);
    }
}
