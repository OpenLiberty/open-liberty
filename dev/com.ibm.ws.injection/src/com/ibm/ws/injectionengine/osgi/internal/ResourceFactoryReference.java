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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Vector;

import javax.naming.RefAddr;
import javax.naming.Reference;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.resource.ResourceFactory;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;

@Trivial
public class ResourceFactoryReference extends Reference {
    private final static long serialVersionUID = 4798118416701402834L;
    /**
     * Used for serialization. If new fields are added/removed/modified from this
     * class, you will need to increment this class version and make appropriate
     * changes to readObject and writeObject to ensure backwards compatibility.
     */
    private final static short CLASS_VERSION = 1;
    private ResourceFactory resourceFactory;
    private Map<String, Object> properties; // used for re-construction on deserialization

    ResourceFactoryReference(String className, ResourceFactory resourceFactory, Map<String, Object> properties) {
        super(className, ResourceFactoryObjectFactory.class.getName(), null);
        this.resourceFactory = resourceFactory;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return Util.identity(this) + '[' +
               "type=" + getClassName() +
               ", resourceFactory=" + resourceFactory +
               ']';
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(CLASS_VERSION);
        out.writeObject(this.className);
        out.writeObject(this.classFactory);
        out.writeObject(this.classFactoryLocation);
        out.writeObject(this.addrs);
        out.writeObject(this.properties);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException {
        try {
            if (in.readShort() != CLASS_VERSION) {
                throw new InvalidObjectException("This ResourceFactoryReference class is not the correct version." +
                                                 "This clent should be upgraded");
            }

            this.className = (String) in.readObject();
            this.classFactory = (String) in.readObject();
            this.classFactoryLocation = (String) in.readObject();
            this.addrs = (Vector<RefAddr>) in.readObject();
            this.properties = (Map<String, Object>) in.readObject();

            OSGiInjectionEngineImpl ie = (OSGiInjectionEngineImpl) InjectionEngineAccessor.getInstance();
            ResourceFactoryBuilder builder = ie.getResourceFactoryBuilder(this.className);
            this.resourceFactory = builder.createResourceFactory(properties);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    ResourceFactory getResourceFactory() {
        return resourceFactory;
    }
}
