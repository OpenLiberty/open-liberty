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
package test.app.prereq;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;

public class Activator implements BundleActivator {

    private ServiceRegistration<ApplicationPrereq> reg;

    @Override
    public void start(BundleContext ctx) throws Exception {
        Prereq p = new Prereq();
        this.reg = ctx.registerService(ApplicationPrereq.class, p, null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        if (reg != null) {
            reg.unregister();
        }
    }

}
