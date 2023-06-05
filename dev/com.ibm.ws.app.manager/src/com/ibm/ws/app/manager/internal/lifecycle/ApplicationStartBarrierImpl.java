/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package com.ibm.ws.app.manager.internal.lifecycle;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

/**
 * Enable Declarative Services to enforce start sequencing.
 * Application handlers should depend on this service.
 * Services should implement {@link ApplicationPrereq}
 */
@Component(
           configurationPid = "com.ibm.ws.app.prereqs",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationStartBarrierImpl implements ApplicationStartBarrier {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(ApplicationStartBarrierImpl.class);

    @Activate
    public ApplicationStartBarrierImpl(@Reference(name = "ApplicationPrereq") List<ApplicationPrereq> applicationPrereqs) {
    }
}
