/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.lifecycle;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

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
    configurationPid = "com.ibm.ws.app.startbarrier",
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationStartBarrierImpl implements ApplicationStartBarrier {
    private static final TraceComponent tc = Tr.register(ApplicationStartBarrierImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    void setApplicationPrereq(ApplicationPrereq applicationPrereq) {}

    @Activate
    public void activate() {} // for tracing purposes only — trace is auto-injected
}

