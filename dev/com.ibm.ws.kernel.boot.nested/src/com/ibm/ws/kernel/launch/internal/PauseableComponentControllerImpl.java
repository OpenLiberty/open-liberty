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
package com.ibm.ws.kernel.launch.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentController;
import com.ibm.ws.kernel.launch.service.PauseableComponentControllerRequestFailedException;

/**
 * This service provides a means of delivering actions to Listeners.
 *
 * Pause and resume are supported through this service. It maintains a
 * List of Listeners that it will drive specific actions to.
 * <p>
 * Currently, this Service is used by the server script and by the z/OS CommandHandler
 * functions interact with this server's Listeners that participate by providing
 * an implementation for the PauseResumeListener interface.
 * <p>
 * Implementation for a PauseableComponentController service.
 */

public class PauseableComponentControllerImpl implements PauseableComponentController {

    private static final TraceComponent tc = Tr.register(PauseableComponentControllerImpl.class);

    private ServiceTracker<PauseableComponent, PauseableComponent> tracker;

    PauseableComponentControllerImpl(BundleContext systemBundleCtx) {

        if (systemBundleCtx != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Activating PauseableComponentController service");
            }

            // Initialize a ServerTracker for the PauseResumeListener services
            tracker = new ServiceTracker<PauseableComponent, PauseableComponent>(systemBundleCtx, PauseableComponent.class, null);
            tracker.open();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#pause()
     */
    @Override
    public void pause() throws PauseableComponentControllerRequestFailedException {

        Tr.info(tc, "info.server.pause.all.request.received");

        Set<String> failed = new HashSet<String>();

        if (tracker.getTrackingCount() == 0) {
            Tr.warning(tc, "warning.server.pause.no.targets");
            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.pause.no.targets"));
        }

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {
                            try {
                                pauseableComponent.pause();
                            } catch (Throwable t) {
                                // Catch anything and mark a failed Add it to the failed list.
                                failed.add(pauseableComponent.getName());
                            }
                        }
                    }

                    break;
                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }

        //Check if we had any failures and throw an exception back with a list of failed pauseable components.
        if (!failed.isEmpty()) {

            Tr.error(tc, "error.server.pause.failed", Arrays.toString(failed.toArray()));

            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "error.server.pause.failed", Arrays.toString(failed.toArray())));

        } else {

            Tr.info(tc, "info.server.pause.request.completed");
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#pause(java.lang.String)
     */
    @Override
    public void pause(String targets) throws PauseableComponentControllerRequestFailedException {

        Tr.info(tc, "info.server.pause.request.received", targets);

        Set<String> foundTargets = new HashSet<String>();

        Set<String> targetList = createTargetList(targets);

        if (targetList.isEmpty()) {
            Tr.warning(tc, "warning.server.pause.invalid.targets");
            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.pause.invalid.targets"));
        }

        Set<String> failed = new HashSet<String>();

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {

                            if (targetList.contains(pauseableComponent.getName())) {
                                foundTargets.add(pauseableComponent.getName());

                                try {
                                    pauseableComponent.pause();

                                } catch (Throwable t) {
                                    // Catch anything and mark a failed Add it to the failed list.
                                    failed.add(pauseableComponent.getName());
                                }
                            }
                        }
                    }

                    break;
                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }

        //Check which (if any) targets were not found
        boolean targetsNotFound = false;
        targetList.removeAll(foundTargets);
        if (!targetList.isEmpty()) {
            targetsNotFound = true;
            Tr.warning(tc, "warning.server.pause.missing.targets", Arrays.toString(targetList.toArray()));
        }

        //Check if we had any failures and throw an exception back with a list of failed pauseable components.
        if (!failed.isEmpty()) {

            Tr.error(tc, "error.server.pause.failed", Arrays.toString(failed.toArray()));

            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "error.server.pause.failed", Arrays.toString(failed.toArray())));

        } else {
            Tr.info(tc, "info.server.pause.request.completed");

            if (targetsNotFound) {
                throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.pause.missing.targets", Arrays.toString(targetList.toArray())));
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#resume()
     */
    @Override
    public void resume() throws PauseableComponentControllerRequestFailedException {

        Tr.info(tc, "info.server.resume.all.request.received");

        Set<String> failed = new HashSet<String>();

        if (tracker.getTrackingCount() == 0) {
            Tr.warning(tc, "warning.server.resume.no.targets");
            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.resume.no.targets"));
        }

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {
                            try {
                                pauseableComponent.resume();
                            } catch (Throwable t) {
                                // Catch anything and mark a failed Add it to the failed list.
                                failed.add(pauseableComponent.getName());
                            }
                        }
                    }

                    break;
                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }

        //Check if we had any failures and throw an exception back with a list of failed pauseable components.
        if (!failed.isEmpty()) {

            Tr.error(tc, "error.server.resume.failed", Arrays.toString(failed.toArray()));

            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "error.server.resume.failed", Arrays.toString(failed.toArray())));

        } else {

            Tr.info(tc, "info.server.resume.request.completed");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#resume(java.lang.String)
     */
    @Override
    public void resume(String targets) throws PauseableComponentControllerRequestFailedException {

        Tr.info(tc, "info.server.resume.request.received", targets);

        Set<String> foundTargets = new HashSet<String>();

        Set<String> targetList = createTargetList(targets);

        if (targetList.isEmpty()) {
            Tr.warning(tc, "warning.server.resume.invalid.targets");
            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.resume.invalid.targets"));
        }

        Set<String> failed = new HashSet<String>();

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {

                            if (targetList.contains(pauseableComponent.getName())) {
                                foundTargets.add(pauseableComponent.getName());

                                try {
                                    pauseableComponent.resume();

                                } catch (Throwable t) {
                                    // Catch anything and mark a failed Add it to the failed list.
                                    failed.add(pauseableComponent.getName());
                                }
                            }
                        }
                    }

                    break;
                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }

        //Check which (if any) targets were not found
        boolean targetsNotFound = false;
        targetList.removeAll(foundTargets);
        if (!targetList.isEmpty()) {
            targetsNotFound = true;
            Tr.warning(tc, "warning.server.resume.missing.targets", Arrays.toString(targetList.toArray()));
        }

        //Check if we had any failures and throw an exception back with a list of failed pauseable components.
        if (!failed.isEmpty()) {

            Tr.error(tc, "error.server.resume.failed", Arrays.toString(failed.toArray()));

            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "error.server.resume.failed", Arrays.toString(failed.toArray())));

        } else {
            Tr.info(tc, "info.server.resume.request.completed");

            if (targetsNotFound) {
                throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.resume.missing.targets", Arrays.toString(targetList.toArray())));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#isPaused()
     */
    @Override
    public boolean isPaused() {

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    if (tracker.getTrackingCount() == 0) {
                        // No pauseable components.
                        return false;
                    }
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {
                            // All pauseable components need to be paused in order for the server to be called paused.
                            // If one component is not paused, the server is not paused.
                            if (!pauseableComponent.isPaused()) {
                                return false;
                            }
                        }
                    }
                    //At this point, there were pauseable components found and all are paused.
                    return true;

                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#isPaused(java.lang.String)
     */
    @Override
    public boolean isPaused(String targets) throws PauseableComponentControllerRequestFailedException {

        Set<String> foundTargets = new HashSet<String>();

        Set<String> targetList = createTargetList(targets);

        //Add each pauseable component to this list. If the tracked values get modified
        //while we are iterating and we start over, skip anyone already in this list
        Set<PauseableComponent> processedList = new HashSet<PauseableComponent>();

        // Sync with other methods changing/querying states for PauseableComponents
        synchronized (this) {
            while (true) {
                try {
                    for (PauseableComponent pauseableComponent : tracker.getTracked().values()) {

                        if (processedList.add(pauseableComponent)) {

                            if (targetList.contains(pauseableComponent.getName())) {
                                foundTargets.add(pauseableComponent.getName());

                                if (!pauseableComponent.isPaused()) {
                                    return false;
                                }
                            }
                        }
                    }

                    break;
                } catch (Throwable t) {
                    // Someone modified our list of services. Retry.
                }
            }
        }

        //Check which (if any) targets were not found
        targetList.removeAll(foundTargets);
        if (!targetList.isEmpty()) {
            // We could not find targets specified. Return false: not everyone was paused
            Tr.warning(tc, "warning.server.status.missing.targets", Arrays.toString(targetList.toArray()));
            throw new PauseableComponentControllerRequestFailedException(Tr.formatMessage(tc, "warning.server.status.missing.targets", Arrays.toString(targetList.toArray())));

        } else {
            return true;
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.kernel.launch.service.PauseableComponentController#getPauseableComponents()
     */
    @Override
    public Collection<PauseableComponent> getPauseableComponents() {

        return tracker.getTracked().values();

    }

    /**
     * Args should be something like: target=singapore. We want to strip the keyword and equals sign
     * off. Also, remove single quotes too.
     *
     * @param args
     * @return
     */
    private Set<String> createTargetList(String targets) throws PauseableComponentControllerRequestFailedException {

        Set<String> targetSet = new HashSet<String>();

        if (targets != null && !targets.equals("")) {
            String targetsList[] = targets.split(",");
            targetSet.addAll(Arrays.asList(targetsList));
        }

        return targetSet;
    }

}