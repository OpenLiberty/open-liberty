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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * A URL {@link Context} for the "java:" namespace. This is a read-only
 * namespace so many {@link Context} operations are not supported and will throw {@link OperationNotSupportedException}s. There is no binding in this
 * implementation, the equivalent action is for a container to register a {@link JavaColonNamingHelper} with information about JNDI objects. This
 * implementation checks the helper services for non-null information about a
 * JNDI name and then calls the specified resource factory to retrieve the named
 * Object.
 * 
 */
public class JavaURLContext implements Context {

    private final ConcurrentServiceReferenceSet<JavaColonNamingHelper> helperServices;

    // The environment for this instance of the Context
    private final Map<String, Object> environment = new ConcurrentHashMap<String, Object>();

    // A JavaURLNameParser
    private static final JavaURLNameParser parser = new JavaURLNameParser();

    private static CompositeName newCompositeName(String nameString) throws NameNotFoundException {
        CompositeName name;
        try {
            name = new CompositeName(nameString);
        } catch (InvalidNameException ine) {
            // For parity with tWAS a composite name that is mis-constructed should throw a
            // NameNotFoundException rather than an InvalidNameException.
            // See test.jndi.url.context.servlet.JndiServlet.testInvalidName() for more details.
            NameNotFoundException nnfe = new NameNotFoundException(nameString);
            nnfe.initCause(ine);
            throw nnfe;
        }
        return name;
    }

    /**
     * Constructor for use by the JavaURLContextFactory.
     * 
     * @param envmt
     *            Map<String,Object> of environment parameters for this Context
     */
    @SuppressWarnings("unchecked")
    JavaURLContext(Hashtable<?, ?> envmt, ConcurrentServiceReferenceSet<JavaColonNamingHelper> helperServices) {
        this.environment.putAll((Map<? extends String, ? extends Object>) envmt);
        //this context does not have a base name
        this.base = null;
        this.helperServices = helperServices;
    }

    //the base name of the context
    private final Name base;

    /**
     * Constructor for use by the JavaURLContextFactory - but only for serialization/deserialization.
     */
    @SuppressWarnings("unchecked")
    JavaURLContext(Hashtable<?, ?> envmt, ConcurrentServiceReferenceSet<JavaColonNamingHelper> helperServices, Name base) {
        this.environment.putAll((Map<? extends String, ? extends Object>) envmt);
        this.base = base;
        this.helperServices = helperServices;
    }

    private JavaURLContext(Name base, JavaURLContext copy) {
        this.environment.putAll(copy.environment);
        this.base = base;
        this.helperServices = copy.helperServices;
    }

    Name getBase() {
        return base;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object addToEnvironment(String s, Object o) throws NamingException {
        return this.environment.put(s, o);
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void bind(Name n, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void bind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Name composeName(Name n, Name pfx) throws NamingException {
        return ((Name) pfx.clone()).addAll(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String composeName(String s, String pfx) throws NamingException {
        return composeName(newCompositeName(s), parser.parse(pfx)).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws NamingException {
        // NO-OP
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public Context createSubcontext(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public Context createSubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void destroySubcontext(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void destroySubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException();
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
     * Returns "java:"
     * 
     * @return {@link NamingConstants.JAVA_NS}
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return base == null ? NamingConstants.JAVA_NS : base.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameParser getNameParser(Name n) throws NamingException {
        return parser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameParser getNameParser(String s) throws NamingException {
        return parser;
    }

    /**    
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name n) throws NamingException {
        HashSet<NameClassPair> allInstances = new HashSet<NameClassPair>();
        NameUtil nameUtil = new NameUtil(n);

        // Special case subcontexts of java:comp/env since we don't expect helpers to return those
        if (nameUtil.getNamespace() == JavaColonNamespace.COMP) {
            allInstances.add(new NameClassPair("env", Context.class.getName()));
            allInstances.add(new NameClassPair("websphere", Context.class.getName()));
        } else if (NamingConstants.JAVA_NS.equals(nameUtil.getNamespace().toString())) {
            allInstances.add(new NameClassPair("comp", Context.class.getName()));
        }

        // loop through the helper services looking for results
        // Helpers are responsible for returning both bound objects and contexts
        for (Iterator<JavaColonNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            JavaColonNamingHelper helperService = it.next();
            allInstances.addAll(helperService.listInstances(nameUtil.getNamespace(), nameUtil.getNameInContext()));
        }

        //if there were no helper services and this is not a subcontext,
        //we won't find the name, so throw NameNotFound
        if (allInstances.isEmpty()) {
            throw new NameNotFoundException(n.toString());
        }

        return new JavaURLEnumeration<NameClassPair>(allInstances);
    }

    /**   
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
        return list(newCompositeName(s));
    }

    /**   
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name n) throws NamingException {

        HashSet<Binding> bindings = new HashSet<Binding>();
        NamingEnumeration<NameClassPair> pairs = list(n);
        if (pairs.hasMore()) {
            // At this point we know n is a context with bindings
            Context ctx = (Context) lookup(n);
            while (pairs.hasMore()) {
                NameClassPair pair = pairs.next();
                Binding binding = new Binding(pair.getName(), pair.getClassName(), ctx.lookup(pair.getName()));
                bindings.add(binding);
            }
        }

        return new JavaURLEnumeration<Binding>(bindings);
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
        return listBindings(newCompositeName(s));
    }

    /**
     * Since the java: URL {@link Context} in this implementation does not
     * support binding, the lookup is lazy and performs as follows.
     * <OL>
     * <LI>Call all the helper services which have registered under the {@link JavaColonNamingHelper} interface in the SR.
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
    public Object lookup(Name inName) throws NamingException {
        NameUtil nameUtil = new NameUtil(inName);

        Object toReturn = null;

        //if there wasn't a name to lookup return a Context
        if (nameUtil.getStringNameWithoutPrefix().equals(""))
            return new JavaURLContext(nameUtil.getName(), this);

        // loop through the helper services looking for a non-null result
        for (Iterator<JavaColonNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            JavaColonNamingHelper helperService = it.next();
            toReturn = helperService.getObjectInstance(nameUtil.getNamespace(), nameUtil.getNameInContext());
            if (toReturn != null) {
                break;
            }
        }

        if (toReturn != null) {
            // return the resource asked for
            return toReturn;
        }

        boolean hasPrefix = false;
        // loop through the helper services to know whether any object instance's name
        // is prefixed with the name specified
        for (Iterator<JavaColonNamingHelper> it = helperServices.getServices(); it.hasNext();) {
            JavaColonNamingHelper helperService = it.next();
            hasPrefix = helperService.hasObjectWithPrefix(nameUtil.getNamespace(), nameUtil.getNameInContext());
            if (hasPrefix) {
                break;
            }
        }

        if (!hasPrefix) {
            throw new NameNotFoundException(NameNotFoundException.class.getName() + ": " + nameUtil.getName().toString());
        }

        return new JavaURLContext(nameUtil.getName(), this);
    }

    /**
     * String form of lookup(Name n)
     */
    @Override
    public Object lookup(String s) throws NamingException {
        if (s == null)
            throw new InvalidNameException();
        //turn the String into a CompositeName and then use the Name form of the method
        //all necessary parsing etc will be done there
        return lookup(newCompositeName(s));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object lookupLink(Name n) throws NamingException {
        return lookup(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object lookupLink(String s) throws NamingException {
        return lookup(s);
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void rebind(Name n, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void rebind(String s, Object o) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeFromEnvironment(String s) throws NamingException {
        return this.environment.remove(s);
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void rename(Name nOld, Name nNew) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void rename(String sOld, String sNew) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void unbind(Name n) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * This operation is not supported in the java: read-only namespace.
     * 
     * @throws {@link OperationNotSupportedException}
     */
    @Override
    public void unbind(String s) throws NamingException {
        throw new OperationNotSupportedException();
    }

    private class NameUtil {

        private final Name name;
        private final JavaColonNamespace namespace;
        private final String nameInContext;
        private final String stringNameWithoutPrefix;

        /**
         * @param n
         */
        public NameUtil(Name n) throws NamingException {
            if (n == null)
                throw new InvalidNameException();
            //if there is already a base name, then we need to add n to it
            if (base != null)
                n = composeName(n, base);
            if (!!!(n instanceof JavaURLName))
                n = parser.parse(n);

            this.namespace = parser.getJavaNamespaceFromName(n);
            this.nameInContext = parser.getStringNameWithoutPrefix(n);
            this.stringNameWithoutPrefix = parser.getStringNameWithoutPrefix(n);
            this.name = n;
        }

        /**
         * @return
         */
        public String getStringNameWithoutPrefix() {
            return this.stringNameWithoutPrefix;
        }

        /**
         * @return
         */
        public String getNameInContext() {
            return this.nameInContext;
        }

        /**
         * @return
         */
        public JavaColonNamespace getNamespace() {
            return this.namespace;
        }

        /**
         * @return
         * @throws NamingException
         */
        public Name getName() throws NamingException {
            return this.name;
        }

    }

}
