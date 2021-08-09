/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * JavaURLContextFactory is an implementation of {@link ObjectFactory} that is
 * intended to be registered in the SR with the osgi.jndi.url.scheme=java
 * property. As such it is called for handling JNDI names starting with "java:".
 * 
 */
public class JavaURLContextFactory implements ObjectFactory {
    private final ConcurrentServiceReferenceSet<JavaColonNamingHelper> helperServices = new ConcurrentServiceReferenceSet<JavaColonNamingHelper>("helpers");

    public void addHelper(ServiceReference<JavaColonNamingHelper> reference) {
        helperServices.addReference(reference);
    }

    public void removeHelper(ServiceReference<JavaColonNamingHelper> reference) {
        helperServices.removeReference(reference);
    }

    public void activate(ComponentContext cc) {
        helperServices.activate(cc);
    }

    public void deactivate(ComponentContext cc) {
        helperServices.deactivate(cc);
    }

    /**
     * Creates a JavaURLContext for resolving java: urls. It should only be
     * called by an OSGi JNDI spec implementation. There is no support for
     * non-null Name and Context parameters for this method in accordance with
     * the OSGi specification for JNDI.
     * 
     * <UL>
     * <LI>If the parameter o is null a new {@link JavaURLContext} is returned.
     * <LI>If o is a URL String then the Object returned is the result of
     * looking up the String on a {@link JavaURLContext}.
     * <LI>If o is an array of URL Strings then an {@link OperationNotSupportedException} is thrown as there is no
     * sub-context support in this implementation from which to lookup multiple
     * names.
     * <LI>If o is any other Object an {@link OperationNotSupportedException} is
     * thrown.
     * </UL>
     * 
     * @param o
     *            {@inheritDoc}
     * @param n
     *            must be null (OSGi JNDI spec)
     * @param c
     *            must be null (OSGi JNDI spec)
     * @param envmt
     *            {@inheritDoc}
     * @return
     * @throws OperationNotSupportedException
     *             if the Object passed in is not null or a String.
     */
    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        // by OSGi JNDI spec Name and Context should be null
        // if they are not then this code is being called in
        // the wrong way
        if (n != null || c != null)
            return null;
        // Object is String, String[] or null
        // Hashtable contains any environment properties
        if (o == null) {
            return new JavaURLContext(envmt, helperServices);
        } else if (o instanceof String) {
            return new JavaURLContext(envmt, helperServices).lookup((String) o);
        } else {
            throw new OperationNotSupportedException();
        }
    }

    /**
     * This method should only be called by the JavaURLContextReplacer class for
     * de-serializing an instance of JavaURLContext. The name parameter can be
     * null.
     */
    JavaURLContext createJavaURLContext(Hashtable<?, ?> envmt, Name name) {
        return new JavaURLContext(envmt, helperServices, name);
    }
}
