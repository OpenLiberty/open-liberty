/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(configurationPolicy = IGNORE, property = "service.vendor=IBM")
public class WASObjectFactoryBuilder implements ObjectFactoryBuilder {

    private static final TraceNLS nls = TraceNLS.getTraceNLS(WSContext.class, "com.ibm.ws.jndi.internal.resources.JNDIMessages");

    @Override
    @FFDCIgnore({ ClassNotFoundException.class, Throwable.class, IllegalAccessException.class, InstantiationException.class })
    // FFDC is processed explicitly
    public ObjectFactory createObjectFactory(Object o, Hashtable<?, ?> envmt) throws NamingException {

        ObjectFactory of = null;

        String className = null;

        if (o instanceof Reference) {
            className = ((Reference) o).getFactoryClassName();
        } else if (o instanceof Referenceable) {
            className = ((Referenceable) o).getReference().getFactoryClassName();
        }

        if (className != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends ObjectFactory> clazz = (Class<? extends ObjectFactory>) Class.forName(className, true, getClassLoader());
                of = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                // try current classloader:
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends ObjectFactory> clazz = (Class<? extends ObjectFactory>) Class.forName(className);
                    of = clazz.newInstance();
                } catch (Throwable t) {
                    //ignore - FFDC for original exception is more useful
                }
                if (of == null) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.osgi.jndi.fep.WASObjectFactoryBuilder.createObjectFactory", "44", this, new Object[] { o, envmt });
                    String errorString = (nls.getFormattedMessage("jndi.objectfactory.no.class", new Object[] { o.getClass().getName(), className },
                                                                  "Could not find class to create ObjectFactory "
                                                                                  + className + " for class "
                                                                                  + o.getClass().getName()));
                    NamingException ne = new NamingException(errorString);
                    ne.initCause(e);
                    throw ne;
                }
            } catch (IllegalAccessException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.osgi.jndi.fep.WASObjectFactoryBuilder.createObjectFactory", "51", this, new Object[] { o, envmt });
                String errorString = (nls.getFormattedMessage("jndi.objectfactory.create.failed", new Object[] { o.getClass().getName(), className },
                                                              "Unable to create ObjectFactory for "
                                                                              + o.getClass().getName()
                                                                              + " with factory class name " + className));
                NamingException ne = new NamingException(errorString);
                ne.initCause(e);
                throw ne;
            } catch (InstantiationException e) {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.osgi.jndi.fep.WASObjectFactoryBuilder.createObjectFactory", "58", this, new Object[] { o, envmt });
                String errorString = (nls.getFormattedMessage("jndi.objectfactory.create.failed", new Object[] { o.getClass().getName(), className },
                                                              "Unable to create ObjectFactory for "
                                                                              + o.getClass().getName()
                                                                              + " with factory class name " + className));
                NamingException ne = new NamingException(errorString);
                ne.initCause(e);
                throw ne;
            }
        }

        return of;
    }

    private ClassLoader getClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run()
            {
                return Thread.currentThread().getContextClassLoader();
            }

        });
    }
}
