/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package com.ibm.ws.injectionengine.osgi.internal;

import java.io.PrintWriter;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.logging.Introspector;

@Component(service = Introspector.class)
public class InjectionIntrospector implements Introspector {
    private OSGiInjectionEngineImpl injectionEngine;

    @Reference(service = OSGiInjectionEngineImpl.class)
    protected void setInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    protected void unsetInjectionEngine(OSGiInjectionEngineImpl injectionEngine) {}

    @Override
    public String getIntrospectorName() {
        return "InjectionIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Injection java: namespace dump";
    }

    @Override
    public void introspect(PrintWriter writer) {
        writer.println();
        writer.println("======================================================================================");
        writer.println("Beginning of Dump");
        writer.println("======================================================================================");
        writer.println();

        OSGiInjectionScopeData globalData = injectionEngine.getInjectionScopeData(null);
        globalData.introspect(writer);

        writer.println("======================================================================================");
        writer.println("End of Dump");
        writer.println("======================================================================================");
    }
}
