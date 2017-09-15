/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.resource.ResourceFactory;

@Component(service = { ObjectFactory.class, ResourceFactoryObjectFactory.class })
public class ResourceFactoryObjectFactory implements ObjectFactory {
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        if (!(o instanceof ResourceFactoryReference)) {
            return null;
        }

        ResourceFactoryReference reference = (ResourceFactoryReference) o;
        ResourceFactory resourceFactory = reference.getResourceFactory();
        return resourceFactory.createResource(null);
    }
}
