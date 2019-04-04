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
package com.ibm.ws.cdi.mp.context;

import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.weld.manager.api.WeldManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;

@Component(immediate = true, name = "com.ibm.ws.cdi.context.provider")
public class WeldContextProvider implements ThreadContextProvider {

    private static final NoopSnapshot NO_OP_SNAPSHOT = new NoopSnapshot();

    @Reference
    CDIService cdiService;

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        if (!cdiService.isCurrentModuleCDIEnabled())
            return NO_OP_SNAPSHOT;
        return new WeldContextSnapshot(true, (WeldManager) cdiService.getCurrentModuleBeanManager());
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        if (!cdiService.isCurrentModuleCDIEnabled())
            return NO_OP_SNAPSHOT;
        return new WeldContextSnapshot(false, (WeldManager) cdiService.getCurrentModuleBeanManager());
    }

    @Override
    @Trivial
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }

    @Trivial
    private static class NoopSnapshot implements ThreadContextSnapshot, ThreadContextController {
        @Override
        public ThreadContextController begin() {
            return this;
        }

        @Override
        public void endContext() throws IllegalStateException {
        }
    }
}
