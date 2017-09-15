/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.WSNameUtil.normalize;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jndi.internal.JNDIServiceBinderManager.JNDIServiceBinderHolder;

/**
 * This provider should be instantiated (at least) once for each requesting bundle,
 * and should be considered to be for the sole use of that bundle. We keep track
 * of the requesting bundle and pass this to any created context objects.
 */
@Component(service = { InitialContextFactory.class, ObjectFactory.class, WSContextFactory.class },
           servicefactory = true,
           configurationPolicy = IGNORE,
           property = "service.vendor=IBM")
public class WSContextFactory implements InitialContextFactory, ObjectFactory {
    /** The bundle that is using this {@link InitialContextFactory}. */
    private BundleContext userContext;

    /** called by DS to activate this component */
    protected void activate(ComponentContext cc) {
        Bundle usingBundle = cc.getUsingBundle();
        userContext = usingBundle.getBundleContext();
    }

    ///////////////////////////////////////////
    // InitialContextFactory implementation //
    /////////////////////////////////////////

    /** {@inheritDoc} */
    // Sanction the implicit cast from Hashtable to Hashtable<String, Object>
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public WSContext getInitialContext(Hashtable env) throws NamingException {
        return new WSContext(userContext, JNDIServiceBinderHolder.HELPER.root, env);
    }

    ///////////////////////////////////
    // ObjectFactory implementation //
    /////////////////////////////////

    @Override
    public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt) throws Exception {
        @SuppressWarnings("unchecked")
        Hashtable<String, Object> env = (Hashtable<String, Object>) envmt;
        return (o instanceof Reference) ? resolve((Reference) o, env) : null;
    }

    private WSContext resolve(Reference ref, Hashtable<String, Object> env) throws NamingException {
        if (ref.getClassName().equals(WSContext.class.getName())) {
            RefAddr addr = ref.get("jndi");
            Object content = addr.getContent();
            if (content instanceof String) {
                String name = (String) content;
                Object o = JNDIServiceBinderHolder.HELPER.root.lookup(normalize(name));
                try {
                    ContextNode node = (ContextNode) o;
                    return new WSContext(userContext, node, env);
                } catch (ClassCastException e) {
                    // Auto-FFDC happens here!
                    throw new NotContextException(name);
                }
            }
        }
        return null;
    }

    static Reference makeReference(WSContext ctx) throws NamingException {
        RefAddr addr = new StringRefAddr("jndi", "" + ctx.myNode.fullName);
        return new Reference(WSContext.class.getName(), addr, WSContextFactory.class.getName(), null);
    }

}
