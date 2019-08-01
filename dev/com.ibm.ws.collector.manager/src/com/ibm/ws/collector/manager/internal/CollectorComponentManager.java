/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.collector.manager.internal;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;

/**
 *
 */
public class CollectorComponentManager implements StaticComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((CollectorManagerImpl) instance).activate((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((CollectorManagerImpl) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((CollectorManagerImpl) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setConfigurationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ConfigurationAdmin.class);
            ((CollectorManagerImpl) instance).setConfigurationAdmin((ConfigurationAdmin) params[0]);
        } else if ("setSource".equals(name)) {
            Object[] params = parameters.getParameters(Source.class);
            ((CollectorManagerImpl) instance).setSource((Source) params[0]);
        } else if ("setHandler".equals(name)) {
            Object[] params = parameters.getParameters(Handler.class);
            ((CollectorManagerImpl) instance).setHandler((Handler) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetConfigurationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ConfigurationAdmin.class);
            ((CollectorManagerImpl) instance).unsetConfigurationAdmin((ConfigurationAdmin) params[0]);
        } else if ("unsetSource".equals(name)) {
            Object[] params = parameters.getParameters(Source.class);
            ((CollectorManagerImpl) instance).unsetSource((Source) params[0]);
        } else if ("unsetHandler".equals(name)) {
            Object[] params = parameters.getParameters(Handler.class);
            ((CollectorManagerImpl) instance).unsetHandler((Handler) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

}
