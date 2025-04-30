/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.ejb;

import java.util.Collection;
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
import com.ibm.ws.container.service.naming.EJBLocalNamingHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSContextBase;
import com.ibm.ws.jndi.WSName;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * A URL {@link Context} for the "ejblocal:" namespace. This is a read-only
 * namespace so many {@link Context} operations are not supported and will throw {@link OperationNotSupportedException}s. There is no binding in this
 * implementation, the equivalent action is for a container to register a {@link EJBLocalNamingHelper} with information about JNDI objects. This
 * implementation checks the helper services for non-null information about a
 * JNDI name and then calls the specified resource factory to retrieve the named
 * Object.
 */
public class EJBLocalURLContext extends WSContextBase implements Context {
    static final TraceComponent tc = Tr.register(EJBLocalURLContext.class);

    // The environment for this instance of the Context
    private final Map<String, Object> environment = new ConcurrentHashMap<String, Object>();
    private final ConcurrentServiceReferenceSet<EJBLocalNamingHelper> helperServices;

    // The sub-context, if this context represents a sub-context of ejblocal:
    private final String subContext;

    /**
     * Constructor for use by the EJBLocalURLContextFactory.
     *
     * @param env
     *                Map<String,Object> of environment parameters for this Context
     */
    @SuppressWarnings("unchecked")
    public EJBLocalURLContext(Hashtable<?, ?> environment, ConcurrentServiceReferenceSet<EJBLocalNamingHelper> helperServices) {
        this.environment.putAll((Map<? extends String, ? extends Object>) environment);
        this.helperServices = helperServices;
        this.subContext = "";
    }

    // Copy constructor for when the lookup string is blank or just ejblocal namespace
    public EJBLocalURLContext(EJBLocalURLContext copy) {
        this.environment.putAll(copy.environment);
        this.helperServices = copy.helperServices;
        this.subContext = copy.subContext;
    }

    // Copy constructor for when the lookup string is a sub-context of the ejblocal namespace
    public EJBLocalURLContext(EJBLocalURLContext copy, String subContext) {
        this.environment.putAll(copy.environment);
        this.helperServices = copy.helperServices;
        this.subContext = subContext;
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
        return "ejblocal:" + subContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeFromEnvironment(String s) throws NamingException {
        return this.environment.remove(s);
    }

    /**
     * Since the ejblocal: URL {@link Context} in this implementation does not
     * support binding, the lookup is lazy and performs as follows.
     * <OL>
     * <LI>Call all the helper services which have registered under the {@link EJBLocalNamingHelper} interface in the SR.
     * <LI>If a non-null object is returned from a helper then return
     * that object, as it is the resource being looked up.
     * </OL>
     *
     * Throws NameNotFoundException if no matching Object is found.
     *
     * @param n
     *              {@inheritDoc}
     * @return {@inheritDoc}
     * @throws NamingException {@inheritDoc}
     */
    @Override
    @FFDCIgnore({ NameNotFoundException.class })
    protected Object lookup(WSName name) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (name.isEmpty()) {
            return new EJBLocalURLContext(this);
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

        // They could be looking up just our context
        if (lookup.equals("ejblocal:")) {
            return new EJBLocalURLContext(this);
        }

        if (lookup.startsWith("local:")) {
            return new InitialContext().lookup(lookup);
        }

        if (lookup.equals("/")) {
            // avoid message text with double slash at end
            String nameStr = "ejblocal:" + subContext + lookup;
            throw new NameNotFoundException(NameNotFoundException.class.getName() + ": " + nameStr);
        }

        Object toReturn = null;

        /**
         * if they are doing a lookup from our context they don't have to have
         * ejblocal: in front, but they can also look us up from the initial context
         * with ejblocal: in front. Our helper service stores the binding without
         * the namespace context in front so just remove it if present.
         *
         * otherwise if ejblocal: is not in front and this context represents a
         * sub-context of ejblocal, then prepend the sub-context.
         */
        String listModified = lookup;
        String lookupModified = lookup;
        if (lookupModified.startsWith("ejblocal:")) {
            lookupModified = lookupModified.substring(9);
            listModified = lookupModified;
        } else if (!subContext.isEmpty()) {
            lookupModified = subContext + "/" + lookupModified;
        }

        for (Iterator<EJBLocalNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            EJBLocalNamingHelper helperService = it.next();
            toReturn = helperService.getObjectInstance(lookupModified);

            if (toReturn != null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NamingHelper found object: " + toReturn);
                }
                break;
            }
        }

        /**
         * if they are doing a lookup of a sub-context, then just look for any instances
         * bound under that sub-contex; if found, return a new context for that sub-context.
         */
        if (toReturn == null) {
            try {
                if (list(listModified).hasMore()) {
                    toReturn = new EJBLocalURLContext(this, lookupModified);
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NamingHelper found sub-contexts: " + lookupModified + ", " + toReturn);
                    }
                }
            } catch (NameNotFoundException ex) {
                // normal when a sub-context is not found; ignore and report meaningful exception below
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NamingHelper failed to find sub-contexts: " + ex);
                }
            }
        }

        if (toReturn == null) {
            String nameStr = "ejblocal:" + lookupModified;
            throw new NameNotFoundException(nameStr);
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
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        HashSet<NameClassPair> allInstances = new HashSet<NameClassPair>();
        for (Iterator<EJBLocalNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            EJBLocalNamingHelper helperService = it.next();
            Collection<NameClassPair> instances = helperService.listInstances(subContext, name.toString());

            if (instances != null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NamingHelper found instances: " + instances);
                }
                allInstances.addAll(instances);
            }
        }

        // If nothing found, report name not found, unless listing the root context
        if (allInstances.isEmpty() && !(subContext.isEmpty() && name.isEmpty())) {
            throw new NameNotFoundException(name.toString());
        }

        return new EJBNamingEnumeration<NameClassPair>(allInstances);
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
