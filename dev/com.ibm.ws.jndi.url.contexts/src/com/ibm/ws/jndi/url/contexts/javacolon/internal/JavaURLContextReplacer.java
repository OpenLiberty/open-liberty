/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.io.IOException;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationObjectReplacer;

/**
 *
 */
@Component(service = { SerializationObjectReplacer.class, DeserializationObjectResolver.class })
public class JavaURLContextReplacer implements SerializationObjectReplacer, DeserializationObjectResolver {
    private JavaURLContextFactory factory;

    @Reference(target = "(osgi.jndi.url.scheme=java)")
    protected void setJavaURLContextFactory(ObjectFactory factory) {
        this.factory = (JavaURLContextFactory) factory;
    }

    protected void unsetJavaURLContextFactory(ObjectFactory factory) {
        this.factory = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.serialization.DeserializationObjectResolver#resolveObject(java.lang.Object)
     */
    @Override
    public Object resolveObject(@Sensitive Object object) throws IOException {
        if (object instanceof JavaURLContextReplacement) {
            try {
                JavaURLContextReplacement replacement = (JavaURLContextReplacement) object;
                JavaURLContext ctx = factory.createJavaURLContext(replacement.getEnv(), new JavaURLName(replacement.getBase()));
                return ctx;
            } catch (NamingException ex) {
                throw new IOException(ex.getMessage(), ex);
            }

        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.serialization.SerializationObjectReplacer#replaceObject(java.lang.Object)
     */
    @Override
    public Object replaceObject(@Sensitive Object object) {
        if (object instanceof JavaURLContext) {
            JavaURLContext ctx = (JavaURLContext) object;
            JavaURLContextReplacement replacement = new JavaURLContextReplacement();
            replacement.setBase(ctx.getBase().toString());
            try {
                replacement.setEnv(ctx.getEnvironment());
            } catch (NamingException ex) {
                //should never occur, but will log ffdc if it does - and just user a blank hashtable to avoid NPEs
                replacement.setEnv(new Hashtable<Object, Object>());
            }
            return replacement;
        }
        return null;
    }
}
