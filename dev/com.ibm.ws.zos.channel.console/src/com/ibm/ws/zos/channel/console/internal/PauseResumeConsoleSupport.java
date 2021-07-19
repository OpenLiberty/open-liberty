/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.console.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentController;
import com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException;

/**
 * Support various z/OS console commands affecting PauseableComponents.
 *
 * serialized singleton.
 */
public enum PauseResumeConsoleSupport {

    INSTANCE;

    /**
     * trace variable
     */
    //private static final TraceComponent tc = Tr.register(PauseResumeConsoleSupport.class, "zConsole");
    private static final TraceComponent tc = Tr.register(PauseResumeConsoleSupport.class);

    /**
     * PauseableComponentController to manage pause/resume for PauseableComponents..
     */
    private PauseableComponentController m_pauseableComponentController;

    /**
     * Deliver pause request to interested PauseableComponents.
     *
     * @param args optional PauseableComponent targets.
     * @return 0 if all went well, 8 if an error was encountered.
     */
    @FFDCIgnore(PauseableComponentControllerRequestFailedException.class)
    public int pause(String args) {
        if (this.m_pauseableComponentController == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "pause: PauseableComponentController not yet injected");
            }

            Tr.error(tc, "ERROR.PAUSEABLE.COMPONENT.NOTAVAILABLE", "pause");
            return 8;
        }

        // FYI: the passed parms to Controller should be just the string of ids.
        // So, if "f <server>,pause,target='id1,id2,id3'", we should pass a String of (id1,id2,id3).  No target=
        // wrapping single quotes.  The caller should've stripped them.

        if (args == null) {
            // Pause on server
            try {
                this.m_pauseableComponentController.pause();
            } catch (PauseableComponentControllerRequestFailedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "pause(): PauseableComponentController threw to us:" + e);
                }
                return 8;
            }
        } else {
            // Pause with target(s)
            try {
                this.m_pauseableComponentController.pause(args);
            } catch (PauseableComponentControllerRequestFailedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "pause(target): PauseableComponentController threw to us:" + e);
                }
                return 8;
            }
        }

        return 0;
    }

    /**
     * Deliver the resume request to interested PauseableComponents.
     *
     * @param args optional parameters.
     * @return 0 if all went well, 8 if an error was encountered.
     */
    @FFDCIgnore(PauseableComponentControllerRequestFailedException.class)
    public int resume(String args) {
        if (this.m_pauseableComponentController == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "resume: PauseableComponentController not yet injected");
            }

            Tr.error(tc, "ERROR.PAUSEABLE.COMPONENT.NOTAVAILABLE", "resume");
            return 8;
        }

        if (args == null) {
            // Resume on server
            try {
                this.m_pauseableComponentController.resume();
            } catch (PauseableComponentControllerRequestFailedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "resume: PauseableComponentController threw to us:" + e);
                }
                return 8;
            }
        } else {
            try {
                // Resume with target(s)
                this.m_pauseableComponentController.resume(args);
            } catch (PauseableComponentControllerRequestFailedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "resume(target): PauseableComponentController threw to us:" + e);
                }
                return 8;
            }
        }

        return 0;
    }

    /**
     * Deliver status request to PauseableComponentController.
     *
     * @return 0 if all went well, 4 if server status is active, 8 if an error was encountered.
     */
    public int status() {
        if (this.m_pauseableComponentController == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "status: PauseableComponentController not yet injected");
            }

            Tr.error(tc, "ERROR.PAUSEABLE.COMPONENT.NOTAVAILABLE", "status");

            return 8;
        }

        // f bbgzsrv,status call for server status
        if (this.m_pauseableComponentController.isPaused()) {
            return 0;
        } else {
            return 4;
        }
    }

    /**
     * Get display information about specific participating PauseableComponents.
     *
     * @param args target components.
     * @return 0 if all went, 8 if an error was encountered.
     */
    public int statusTarget(List<String> responses, String targetArg) {
        if (this.m_pauseableComponentController == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "status,target: PauseableComponentController not yet injected");
            }

            Tr.error(tc, "ERROR.PAUSEABLE.COMPONENT.NOTAVAILABLE", "status,target");

            return 8;
        }

        LinkedHashMap<String, String> targetMap = new LinkedHashMap<String, String>();

        if (targetArg != null && !targetArg.equals("")) {
            String targetsList[] = targetArg.split(",");

            // Prime the status for each target with the "not found" message.
            for (String target : targetsList) {
                StringBuffer pcNotFound = new StringBuffer();
                pcNotFound.append("TARGET ").append(target).append(" was not found.");

                targetMap.put(target, pcNotFound.toString());
            }
        } else {
            if (targetMap.isEmpty()) {
                responses.add("parsed targets resulted in an empty list");
                return 8;
            }
        }

        // Add each pauseable component to this list. If the List of pauseable components get modified
        // while we are iterating we start over, skip anyone already in this list.
        Set<PauseableComponent> processedList = new LinkedHashSet<PauseableComponent>();

        while (true) {
            String currentPC_name = null;
            try {

                for (PauseableComponent pauseableComponent : this.m_pauseableComponentController.getPauseableComponents()) {

                    currentPC_name = pauseableComponent.getName();
                    if (processedList.add(pauseableComponent)) {

                        // Remove from target list...returns true if found and removed
                        if (targetMap.containsKey(currentPC_name)) {

                            // myHttpEndpoint is paused|active.
                            String pausedState = pauseableComponent.isPaused() ? "paused" : "active";

                            StringBuffer pcStatus = new StringBuffer();
                            pcStatus.append("TARGET ").append(currentPC_name).append(" is ").append(pausedState).append(".");

                            targetMap.put(currentPC_name, pcStatus.toString());
                        }
                    }
                }
                // At this point, the pauseable components were found and processed. Push built status back.
                for (String response : targetMap.values()) {
                    responses.add(response);
                }

                break;

            } catch (Throwable te) {
                // Someone modified our list of services or something else. Build a response for the current PC in case we don't
                // process it on the our retry to process the remaining PCs.
                if (currentPC_name != null) {
                    StringBuffer pcException = new StringBuffer();
                    pcException.append("TARGET ").append(currentPC_name).append(" failure occurred obtaining status. Check server logs for details.");

                    targetMap.put(currentPC_name, pcException.toString());
                }
            }
        }

        return 0;
    }

    /**
     * Get display information about current participating PauseableComponents.
     *
     * @param args optional parameters.
     * @return 0 if all went, 8 if an error was encountered.
     */
    public int details(List<String> responses) {
        if (this.m_pauseableComponentController == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "status,details: PauseableComponentController not yet injected");
            }

            Tr.error(tc, "ERROR.PAUSEABLE.COMPONENT.NOTAVAILABLE", "status,details");

            return 8;
        }

        List<String> pcResponses = new ArrayList<String>();

        // Add each pauseable component to this list. If the List of pauseable components get modified
        // while we are iterating we start over, skip anyone already in this list.
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        while (true) {
            try {

                for (PauseableComponent pauseableComponent : this.m_pauseableComponentController.getPauseableComponents()) {

                    if (processedList.add(pauseableComponent)) {

                        // myHttpEndpoint(PAUSED): host:*, state:4, httpsPort:38811, httpPort:38801
                        HashMap<String, String> extendedInfo = pauseableComponent.getExtendedInfo();

                        String pausedState = pauseableComponent.isPaused() ? "paused" : "active";

                        StringBuffer pcStatus = new StringBuffer();
                        pcStatus.append(pauseableComponent.getName()).append("(").append(pausedState).append("):");
                        String separator = " ";

                        for (String key : extendedInfo.keySet()) {
                            pcStatus.append(separator).append(key).append(":").append(extendedInfo.get(key));
                            separator = ", ";
                        }

                        pcResponses.add(pcStatus.toString());
                    }
                }
                // At this point, there were pauseable components found and all are paused.
                break;

            } catch (Throwable te) {
                // Someone modified our list of services or something else. Retry to process remaining PCs.
            }
        }

        if (pcResponses == null || pcResponses.isEmpty()) {
            responses.add("No results returned from pauseable components\n");
        } else {
            responses.addAll(pcResponses);
        }

        return 0;
    }

    /**
     * Save reference to the pause/resume request coordinator.
     *
     * @param m_pauseableComponentController
     */
    public void setPauseableComponentController(PauseableComponentController m_pauseableComponentController) {
        this.m_pauseableComponentController = m_pauseableComponentController;
    }
}
