/*******************************************************************************n * Copyright (c) 2019 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License v1.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-v10.htmln *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

/**
 *
 */
public class MetaTypeIntrospectionComponentManager implements StaticComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setMetaTypeRegistry".equals(name)) {
            Object[] params = parameters.getParameters(MetaTypeRegistry.class);
            ((MetaTypeIntrospection) instance).setMetaTypeRegistry((MetaTypeRegistry) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
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
