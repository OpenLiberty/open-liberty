/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.mp.concurrent.context;

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;

@Trivial
public class DeferredClearedWeldContextSnapshot extends WeldContextSnapshot {

    private final CDIService cdiService;

    public DeferredClearedWeldContextSnapshot(CDIService cdiService) {
        super(false, null);
        this.cdiService = cdiService;
    }

    @Override
    public ThreadContextController begin() {
        if (!cdiService.isCurrentModuleCDIEnabled()) {
            // If the module where this task runs is also not CDI enabled, no-op
            return () -> {
                // no-op ThreadContextController
            };
        } else {
            // The submitting module was not CDI enabled, but the task is running in a module
            // that is CDI enabled, so propagate a cleared CDI context state using the current BM
            this.manager = (WeldManager) cdiService.getCurrentModuleBeanManager();
            return super.begin();
        }
    }
}
