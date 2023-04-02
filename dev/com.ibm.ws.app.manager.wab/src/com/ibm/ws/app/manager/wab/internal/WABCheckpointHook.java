/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.internal;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 * A checkpoint hook that ensures that all system WABs are finished being
 * deployed to the web container.
 * This is a separate component from the WABInstaller to allow it to have
 * a maximum service ranking so that it is called early when doing a
 * checkpoint. This is necessary to make sure all WABs are finished before
 * continuing to call the hooks registered statically with the CheckpointPhase
 * class. In particular the hooks registered by
 * GeneratePluginConfigListener.submitGeneratePluginTask(). Otherwise a
 * an ordering issue can happen if the static hooks are acquired before
 * we wait for all the WABs to complete. This can cause the plugin
 * generation to start processing on the checkpoint side.
 * This hook makes sure we wait for all WABs before continuing on to any
 * static hooks.
 */
@Component(service = CheckpointHook.class,
           property = { Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE,
                        CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true" })
public class WABCheckpointHook implements CheckpointHook {
    private final WABInstaller installer;

    @Activate
    public WABCheckpointHook(@Reference WABInstaller installer) {
        this.installer = installer;
    }

    @Override
    public void prepare() {
        installer.check();
    }
}
