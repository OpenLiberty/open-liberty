/*******************************************************************************
 * Copyright (c) 2012, 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static com.ibm.ws.jndi.internal.JNDIServiceBinder.createServiceProperties;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.PartialResultException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.Adapter;
import com.ibm.ws.jndi.WSContextBase;
import com.ibm.ws.jndi.WSName;
import com.ibm.ws.jndi.WSNameParser;
import com.ibm.ws.jndi.WSNamingEnumeration;

final class WSContext extends WSContextBase implements Context, Referenceable {
    private final static TraceComponent tc = Tr.register(WSContext.class);
    private final BundleContext userContext;
    private final Hashtable<String, Object> env;
    private final WSContextFactory creatingFactory;
    final ContextNode myNode;

    public WSContext(BundleContext userContext, ContextNode node, Hashtable<String, Object> env, WSContextFactory creatingFactory) {
        this.userContext = userContext;
        this.myNode = node;
        this.env = env;
        this.creatingFactory = creatingFactory;
    }

    private ServiceRegistration<?> bindIntoServiceRegistry(WSName wsName, final Object o) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Binding object into registry with name for bundle", o, wsName, userContext.getBundle().getSymbolicName());
        String className = o == null ? null : o.getClass().getName();
        final Hashtable<String, Object> props = createServiceProperties(wsName, className);
        ServiceRegistration<?> reg = AccessController.doPrivileged(new PrivilegedAction<ServiceRegistration<?>>() {
            @Override
            public ServiceRegistration<?> run() {
                return userContext.registerService(Object.class, o, props);
            }
        });
        if (tc.isDebugEnabled())
            Tr.debug(tc, "bind succeeded", reg);
        return reg;
    }

    private Object makeReference(WSName subname, Object o) throws NamingException {
        // if we have a referenceable, convert it to a reference
        if (o instanceof Referenceable) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "converting object to reference", o);
            o = ((Referenceable) o).getReference();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "converted object to reference", o);
        }
        // put whatever we are binding into the service registry
        return bindIntoServiceRegistry(subname, o);
    }

    /**
     * Perform any needed conversions on an object retrieved from the context tree.
     *
     * @param o the object to be resolved
     * @return the resolved object
     * @throws Exception
     * @throws NamingException
     */
    @FFDCIgnore({ NamingException.class })
    @Sensitive
    Object resolveObject(Object o, WSName subname) throws NamingException {
        ServiceReference<?> ref = null;
        try {
            if (o instanceof ContextNode)
                return new WSContext(userContext, (ContextNode) o, env, creatingFactory);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Resolving object", o);

            if (o instanceof ServiceReference) {
                ref = (ServiceReference<?>) o;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "External service registry entry.");
            } else if (o instanceof AutoBindNode) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "AutoBindNode entry.");
                AutoBindNode abNode = (AutoBindNode) o;
                ref = (ServiceReference<?>) abNode.getLastEntry();
                // null means the node was removed in another thread.
                if (ref == null) {
                    // Use the same semantics as ContextNode.lookup.
                    throw new NameNotFoundException(subname.toString());
                }
            } else if (o instanceof ServiceRegistration) {
                ref = ((ServiceRegistration<?>) o).getReference();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Programmatic JNDI entry.");
            }

            boolean getObjectInstance;
            if (ref == null) {
                getObjectInstance = true;
            } else {
                o = getReference(ref);
                if (o == null) {
                    throw new NamingException(Tr.formatMessage(tc, "jndi.servicereference.failed", subname.toString()));
                }

                Object origin = ref.getProperty(JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Retrieved service from registry", o,
                             JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN + "=" + origin,
                             Constants.OBJECTCLASS + "=" + Arrays.toString((String[]) ref.getProperty(Constants.OBJECTCLASS)));
                getObjectInstance = JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN_VALUE.equals(origin) ||
                                    contains((String[]) ref.getProperty(Constants.OBJECTCLASS), Reference.class.getName());
            }

            if (getObjectInstance) {
                // give JNDI a chance to resolve any references
                try {
                    Object oldO = o;
                    o = NamingManager.getObjectInstance(o, subname, this, env);
                    if (o != oldO)
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Resolved object through NamingManager"); // remove logging object o since it might contain the sensitive information.
                } catch (NamingException e) {
                    throw e;
                } catch (Exception e) {
                    // FFDC and proceed
                }
            }
            return o;
        } catch (NamingException e) {
            throw e;
        } catch (Exception e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }
    }

    String resolveObjectClassName(Object o) {
        if (o instanceof ContextNode) {
            return Context.class.getName();
        }

        ServiceReference<?> ref = null;
        if (o instanceof ServiceReference) {
            ref = (ServiceReference<?>) o;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "External service registry entry.");
        } else if (o instanceof AutoBindNode) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "AutoBindNode entry.");
            AutoBindNode abNode = (AutoBindNode) o;
            ref = (ServiceReference<?>) abNode.getLastEntry();
        } else if (o instanceof ServiceRegistration) {
            ref = ((ServiceRegistration<?>) o).getReference();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Programmatic JNDI entry.");
        }

        if (ref != null) {
            Object origin = ref.getProperty(JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN);
            String className = (String) ref.getProperty(JNDIServiceBinder.OSGI_JNDI_SERVICE_CLASS);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Service reference",
                         JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN + "=" + origin,
                         JNDIServiceBinder.OSGI_JNDI_SERVICE_CLASS + "=" + className,
                         Constants.OBJECTCLASS + "=" + Arrays.toString((String[]) ref.getProperty(Constants.OBJECTCLASS)));

            if (className != null || JNDIServiceBinder.OSGI_JNDI_SERVICE_ORIGIN_VALUE.equals(origin)) {
                return className;
            }

            String[] objectClass = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            if (contains(objectClass, Reference.class.getName())) {
                Reference jndiRef = (Reference) (o = getReference(ref));
                return jndiRef.getClassName();
            }

            return objectClass[0];
        }

        return o == null ? null : o.getClass().getName();
    }

    private static boolean contains(String[] array, String find) {
        for (String value : array)
            if (find.equals(value))
                return true;
        return false;
    }

    @Override
    public Object addToEnvironment(String s, Object o) throws NamingException {
        return env.put(s, o);
    }

    @Override
    protected void bind(WSName subname, Object o) throws NamingException {
        myNode.bind(subname, makeReference(subname, o));
    }

    @Override
    public void close() {}

    @Override
    protected Context createSubcontext(WSName n) throws NamingException {
        // try to create the new slot - this throws the right exception if it fails
        ContextNode subnode = myNode.createSubcontext(n);
        // that succeeded, so return a new context pointing to that slot
        return new WSContext(userContext, subnode, env, creatingFactory);
    }

    @Override
    protected void destroySubcontext(WSName n) throws NamingException {
        myNode.destroySubcontext(n);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return env;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return "" + myNode.fullName;
    }

    @Override
    protected NameParser getNameParser(WSName n) throws NamingException {
        if (n == null || n.isEmpty())
            return new WSNameParser(myNode.root);
        // n is not empty, so try to get the parser from the context at n
        Object obj = lookup(n);
        if (obj instanceof Context)
            return ((Context) obj).getNameParser("");
        if (obj == null)
            throw new NameNotFoundException("" + myNode.fullName.plus(n));
        throw new NotContextException("" + myNode.fullName.plus(n));
    }

    @Override
    protected NamingEnumeration<NameClassPair> list(final WSName subname) throws NamingException {
        return new WSNamingEnumeration<NameClassPair>(myNode.getChildren(subname).entrySet(), new Adapter<Entry<String, Object>, NameClassPair>() {
            @Override
            public NameClassPair adapt(Entry<String, Object> entry) {
                String className = resolveObjectClassName(entry.getValue());
                return new NameClassPair(entry.getKey(), className);
            }
        });
    }

    @Override
    protected NamingEnumeration<Binding> listBindings(final WSName subname) throws NamingException {
        return new WSNamingEnumeration<Binding>(myNode.getChildren(subname).entrySet(), new Adapter<Entry<String, Object>, Binding>() {
            @Override
            public Binding adapt(Entry<String, Object> entry) throws NamingException {
                return new Binding(entry.getKey(), resolveObject(entry.getValue(), subname.plus(entry.getKey())));
            }
        });
    }

    @Override
    @Sensitive
    protected Object lookup(WSName subname) throws NamingException {
        // 9099: detect empty lookup on a root context and return a new InitialContext with the same environment instead
        if (subname.isEmpty()&&getNameInNamespace().isEmpty()) {
          return new javax.naming.InitialContext(env);
        }
        Object localObject = myNode.lookup(subname);
        return resolveObject(localObject, subname);
    }

    @Override
    @FFDCIgnore({ ClassCastException.class, NullPointerException.class })
    protected Object lookupLink(WSName subname) throws NamingException {
        Context target = this;
        int i = 0;
        try {
            for (/* int i = 0 */; i < subname.size() - 1; i++)
                target = (Context) target.lookup(subname.get(i));
            if (target == null)
                throw new NullPointerException();
        } catch (ClassCastException e) {
            throw new NotContextException("" + myNode.fullName.plus(subname.getPrefix(i + 1)));
        } catch (NullPointerException e) {
            throw new NameNotFoundException("" + myNode.fullName.plus(subname.getPrefix(i + 1)));
        }
        // Now we have the target context, return the final lookup result
        return target.lookup(subname.getLast());
    }

    @Override
    @FFDCIgnore({ NameNotFoundException.class })
    protected void rebind(WSName subname, Object o) throws NamingException {
        // try removing first - need to ensure we don't remove externals
        try {
            unbind(subname);
        } catch (NameNotFoundException nnfe) {
            // Swallow exception. This is intentional as items
            // we are rebinding don't have to already exist
        }
        myNode.rebind(subname, makeReference(subname, o));
    }

    @Override
    public Object removeFromEnvironment(String s) throws NamingException {
        return env.remove(s);
    }

    @Override
    protected void rename(WSName nOld, WSName nNew) throws NamingException {
        // retrieve existing entry
        Object entry = myNode.lookup(nOld);
        checkNotExternal(nOld, entry, "rename");
        if (entry instanceof ServiceRegistration) {
            ServiceRegistration<?> reg = (ServiceRegistration<?>) entry;
            String className = (String) reg.getReference().getProperty(JNDIServiceBinder.OSGI_JNDI_SERVICE_CLASS);
            reg.setProperties(createServiceProperties(myNode.fullName.plus(nNew), className));
            // try to add new binding
            myNode.bind(nNew, entry);
        } else if (entry instanceof ContextNode) {
            ContextNode oldNode = (ContextNode) entry;
            // create a new node and move over all the movable items
            ContextNode newNode = myNode.createSubcontext(nNew);
            if (!!!moveContents(oldNode, newNode))
                // if it failed we need to let the caller know
                throw new PartialResultException(myNode.fullName + ".rename(" + nOld + ", " + nNew + ")");
        } else {
            // there shouldn't be any other types of object in there!
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected object type found in internal tree", entry);
            // treat it as though there were nothing there
            throw new NameNotFoundException("" + myNode.fullName.plus(nOld));
        }
        // if we succeeded, we need to remove the old binding
        myNode.ensureNotBound(nOld, entry);
    }

    /**
     * Move all the contents of the node and the node itself, as far as possible.
     * Do NOT move any external entries (i.e. anything inserted directly into the
     * service registry), since we cannot actually remove these from the service
     * registry itself, and that store is what takes precedence.
     */
    private boolean moveContents(ContextNode oldNode, ContextNode newNode) throws InvalidNameException, NameAlreadyBoundException, NameNotFoundException, NotContextException {
        boolean emptied = true;
        Iterator<Entry<String, Object>> entries = oldNode.children.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, Object> entry = entries.next();
            WSName name = new WSName(entry.getKey());
            Object child = entry.getValue();
            if (child instanceof AutoBindNode) {
                emptied = false;
            } else if (child instanceof ServiceRegistration) {
                ServiceRegistration<?> reg = (ServiceRegistration<?>) child;
                String className = (String) reg.getReference().getProperty(JNDIServiceBinder.OSGI_JNDI_SERVICE_CLASS);
                reg.setProperties(createServiceProperties(newNode.fullName.plus(name), className));
                newNode.bind(name, reg);
                entries.remove();
            } else if (child instanceof ContextNode) {
                ContextNode oldChildNode = (ContextNode) child;
                ContextNode newChildNode = newNode.createSubcontext(name);
                if (moveContents(oldChildNode, newChildNode))
                    // if it was emptied, remove the node
                    // TODO - potential race condition here - need to prevent any additions during clean
                    entries.remove();
                else
                    // if it wasn't emptied we need to keep its parent too
                    emptied = false;
            } else {
                // there shouldn't be any other types of object in there!
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Unexpected object type found in internal tree", oldNode.fullName + WSName.SEPARATOR + entry.getKey(), child);
                // remove it anyway, and don't copy it to the new location
                entries.remove();
            }
        }
        return emptied;
    }

    @Override
    protected void unbind(WSName subname) throws NamingException {
        Object entry = myNode.lookup(subname);
        checkNotExternal(subname, entry, "unbind");
        if (entry instanceof ServiceRegistration) {
            ServiceRegistration<?> reg = (ServiceRegistration<?>) entry;
            unregister(reg);
        } else if (entry instanceof ContextNode) {
            ContextNode node = (ContextNode) entry;
            if (!!!scrubContents(node))
                throw new PartialResultException(myNode.fullName + ".unbind(" + subname + ")");
        } else {
            try {
                reportUnexpectedType(subname, entry);
            } catch (NameNotFoundException nnfe) {
                // Added try catch so that injection adds an FFDC
                throw nnfe;
            }
        }
        // remove the binding
        myNode.ensureNotBound(subname, entry);
    }

    /**
     * Remove all the contents of the node as far as possible.
     * Do NOT remove any external entries (i.e. anything inserted directly into the
     * service registry), since we cannot actually remove these from the service
     * registry itself, and that store is what takes precedence.
     */
    static boolean scrubContents(ContextNode node) {
        boolean emptied = true;
        Iterator<Entry<String, Object>> entries = node.children.entrySet().iterator();
        while (entries.hasNext()) {
            Entry<String, Object> entry = entries.next();
            Object child = entry.getValue();
            if (child instanceof AutoBindNode) {
                emptied = false;
            } else if (child instanceof ServiceRegistration) {
                ServiceRegistration<?> reg = (ServiceRegistration<?>) child;
                unregister(reg);
                entries.remove();
            } else if (child instanceof ContextNode) {
                ContextNode childNode = (ContextNode) child;
                if (scrubContents(childNode))
                    // if it was emptied, remove the node
                    // TODO - potential race condition here - need to prevent any additions during clean
                    entries.remove();
                else
                    // if it wasn't emptied we need to keep its parent too
                    emptied = false;
            } else {
                // there shouldn't be any other types of object in there!
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Unexpected object type found in internal tree", node.fullName + WSName.SEPARATOR + entry.getKey(), child);
                // remove it anyway
                entries.remove();
            }
        }
        return emptied;
    }

    @FFDCIgnore(IllegalStateException.class)
    private static void unregister(ServiceRegistration<?> reg) {
        try {
            reg.unregister();
        } catch (IllegalStateException ignored) {
            // Something beat us to it unregistering this entry.
            // This is not a big deal and can safely be ignored.
        }
    }

    private void checkNotExternal(WSName subname, Object entry, String opName) throws OperationNotSupportedException, InvalidNameException {
        if (entry instanceof ServiceReference) {
            // this entry was created by something else registering a service
            // so we cannot update the properties via JNDI - only the
            // registering bundle can update the properties
            throw new OperationNotSupportedException(opName + ": " + myNode.fullName.plus(subname));
        }
    }

    private void reportUnexpectedType(WSName subname, Object entry) throws NameNotFoundException, InvalidNameException {
        // there shouldn't be any other types of object in there!
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Unexpected object type found in internal tree", myNode.fullName + WSName.SEPARATOR + subname, entry);
        // treat it as though there were nothing there
        throw new NameNotFoundException("" + myNode.fullName.plus(subname));
    }

    @Override
    public Reference getReference() throws NamingException {
        return WSContextFactory.makeReference(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "" + myNode;
    }

    private Object getReference(ServiceReference<?> ref) {
        return creatingFactory.getService(ref);
    }
}
