/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.ejb;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.naming.LocalColonEJBNamingHelper;
import com.ibm.ws.jndi.WSContextBase;
import com.ibm.ws.jndi.WSName;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * A URL {@link Context} for the "local:" namespace. This is a read-only
 * namespace so many {@link Context} operations are not supported and will throw {@link OperationNotSupportedException}s. There is no binding in this
 * implementation, the equivalent action is for a container to register a {@link LocalColonEJBNamingHelper} with information about JNDI objects. This
 * implementation checks the helper services for non-null information about a
 * JNDI name and then calls the specified resource factory to retrieve the named
 * Object.
 */
public class LocalColonURLContext extends WSContextBase implements Context {
    static final TraceComponent tc = Tr.register(LocalColonURLContext.class);

    // The environment for this instance of the Context
    private final Map<String, Object> environment = new ConcurrentHashMap<String, Object>();
    private final ConcurrentServiceReferenceSet<LocalColonEJBNamingHelper> helperServices;

    // We're faking a local:ejb subcontext with a boolean
    private boolean ejbSubContext = false;

    /**
     * Constructor for use by the LocalColonURLContextFactory.
     *
     * @param env
     *            Map<String,Object> of environment parameters for this Context
     */
    @SuppressWarnings("unchecked")
    public LocalColonURLContext(Hashtable<?, ?> environment, ConcurrentServiceReferenceSet<LocalColonEJBNamingHelper> helperServices) {
        this.environment.putAll((Map<? extends String, ? extends Object>) environment);
        this.helperServices = helperServices;
    }

    // Copy constructor for when the lookup string is blank or just local namespace
    public LocalColonURLContext(LocalColonURLContext copy) {
        this.environment.putAll(copy.environment);
        this.helperServices = copy.helperServices;
    }

    // Copy constructor for subcontext when the lookup string is local:ejb
    public LocalColonURLContext(LocalColonURLContext copy, boolean ejbSubContext) {
        this.environment.putAll(copy.environment);
        this.helperServices = copy.helperServices;
        this.ejbSubContext = ejbSubContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object addToEnvironment(String s, Object o) throws NamingException {
        return this.environment.put(s, o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws NamingException {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        Hashtable<Object, Object> envmt = new Hashtable<Object, Object>();
        envmt.putAll(environment);
        return envmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return "local:";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeFromEnvironment(String s) throws NamingException {
        return this.environment.remove(s);
    }

    /**
     * Since the local: URL {@link Context} in this implementation does not
     * support binding, the lookup is lazy and performs as follows.
     * <OL>
     * <LI>Call all the helper services which have registered under the {@link LocalColonEJBNamingHelper} interface in the SR.
     * <LI>If a non-null object is returned from a helper then return
     * that object, as it is the resource being looked up.
     * </OL>
     *
     * Throws NameNotFoundException if no matching Object is found.
     *
     * @param n
     *            {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NamingException {@inheritDoc}
     */
    @Override
    protected Object lookup(WSName name) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (name.isEmpty()) {
            return new LocalColonURLContext(this);
        }

        String lookup = name.toString();

        // Clean up lookups with multiple namespaces in front.
        boolean endsInNamespace = lookup.endsWith(":");
        String[] lookupArray = lookup.split(":");
        if (endsInNamespace) {
            lookup = lookupArray[lookupArray.length - 1] + ":";
        } else if (lookupArray.length > 1) {
            lookup = lookupArray[lookupArray.length - 2] + ":" + lookupArray[lookupArray.length - 1];
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "namespace parsed lookup: " + lookup);
        }

        // They could be looking up just namespace contexts
        if (lookup.equals("local:")) {
            return new LocalColonURLContext(this);
        }

        if (lookup.equals("local:ejb")) {
            return new LocalColonURLContext(this, true);
        }

        if (lookup.startsWith("ejblocal:")) {
            return new InitialContext().lookup(lookup);
        }

        Object toReturn = null;

        /**
         * if they are doing a lookup from our context they don't have to have
         * local: in front, but they can also look us up from the initial context
         * with local: in front. Our helper service stores the binding without
         * the namespace context in front so just remove it if present.
         */
        String lookupModified = lookup;
        boolean startedWithLocal = false;
        if (lookupModified.startsWith("local:")) {
            lookupModified = lookupModified.substring(6);
            startedWithLocal = true;
        }

        /**
         * if we're in the local:ejb add ejb/ in front of the lookup
         */
        if (ejbSubContext && !startedWithLocal)
            lookupModified = "ejb/" + lookupModified;

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "parsed lookup: " + lookupModified);
        }

        for (Iterator<LocalColonEJBNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            LocalColonEJBNamingHelper helperService = it.next();
            toReturn = helperService.getObjectInstance(lookupModified);

            if (toReturn != null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NamingHelper found object: " + toReturn);
                }
                break;
            }
        }

        if (toReturn == null) {
            throw new NameNotFoundException(NameNotFoundException.class.getName() + ": " + name.toString());
        }

        return toReturn;
    }

    @Override
    protected void bind(WSName name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected void rebind(WSName name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected void rename(WSName oldName, WSName newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected void unbind(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected NamingEnumeration<NameClassPair> list(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected NamingEnumeration<Binding> listBindings(WSName name) throws NamingException {
        HashSet<Binding> bindings = new HashSet<Binding>();
        NamingEnumeration<NameClassPair> pairs = list(name);
        if (pairs.hasMore()) {
            // At this point we know n is a context with bindings
            Context ctx = (Context) lookup(name);
            while (pairs.hasMore()) {
                NameClassPair pair = pairs.next();
                Binding binding = new Binding(pair.getName(), pair.getClassName(), ctx.lookup(pair.getName()));
                bindings.add(binding);
            }
        }

        return new EJBNamingEnumeration<Binding>(bindings);
    }

    @Override
    protected void destroySubcontext(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected Context createSubcontext(WSName name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    protected Object lookupLink(WSName name) throws NamingException {
        return lookup(name);
    }

    @Override
    protected NameParser getNameParser(WSName name) throws NamingException {
        return null;
    }
}
