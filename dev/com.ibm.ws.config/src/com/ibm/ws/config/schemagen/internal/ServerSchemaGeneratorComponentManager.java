/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.metatype.SchemaGenerator;
import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public class ServerSchemaGeneratorComponentManager implements StaticComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((ServerSchemaGeneratorImpl) instance).activate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((ServerSchemaGeneratorImpl) instance).deactivate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((ServerSchemaGeneratorImpl) instance).setLocationAdmin((ServiceReference<WsLocationAdmin>) params[0]);
        } else if ("setSchemaGen".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((ServerSchemaGeneratorImpl) instance).setSchemaGen((ServiceReference<SchemaGenerator>) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((ServerSchemaGeneratorImpl) instance).unsetLocationAdmin((ServiceReference<WsLocationAdmin>) params[0]);
        } else if ("unsetSchemaGen".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((ServerSchemaGeneratorImpl) instance).unsetSchemaGen((ServiceReference<SchemaGenerator>) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

}
