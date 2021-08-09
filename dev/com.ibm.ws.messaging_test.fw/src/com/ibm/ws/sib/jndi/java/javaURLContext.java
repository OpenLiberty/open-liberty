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
package com.ibm.ws.sib.jndi.java;

import java.util.Hashtable;

import javax.naming.CannotProceedException;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ResolveResult;

import com.ibm.ws.sib.jndi.HierCtx;

/* ************************************************************************** */
/**
 * A Summary of a goes here. Followed by a paragraph of
 * general description.
 * 
 */
/* ************************************************************************** */
public class javaURLContext implements Context {
    private Hashtable _environment;

    /* -------------------------------------------------------------------------- */
    /*
     * javaURLContext constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new javaURLContext.
     * 
     * @param environment
     */
    public javaURLContext(Hashtable environment) {
        _environment = environment;
    }

    protected Context getContinuationContext(Name n) throws NamingException {
        Object obj = lookup(n.get(0));
        CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(obj);
        cpe.setEnvironment(_environment);
        return NamingManager.getContinuationContext(cpe);
    }

    protected ResolveResult getRootURLContext(String url) throws NamingException {
        if (!url.startsWith("java:"))
            throw new IllegalArgumentException(url + " is not a valid java: URL");

        CompositeName remaining = new CompositeName();
        if (url.length() > 5) {
            remaining.add(url.substring(5));
        }

        Context context = HierCtx.getStaticNamespace(_environment);
        return new ResolveResult(context, remaining);
    }

    /**
     * Returns the suffix of the url. The result should be identical to
     * that of calling getRootURLContext().getRemainingName(), but
     * without the overhead of doing anything with the prefix like
     * creating a context.
     *<p>
     * This method returns a Name instead of a String because to give
     * the provider an opportunity to return a Name (for example,
     * for weakly separated naming systems like COS naming).
     *<p>
     * The default implementation uses skips 'prefix', calls
     * UrlUtil.decode() on it, and returns the result as a single component
     * CompositeName.
     * Subclass should override if this is not appropriate.
     * This method is used only by rename().
     * If rename() is supported for a particular URL scheme,
     * getRootURLContext(), getURLPrefix(), and getURLSuffix()
     * must be in sync wrt how URLs are parsed and returned.
     *<p>
     * For many URL schemes, this method is very similar to URL.getFile(),
     * except getFile() will return a leading slash in the
     * 2nd, 3rd, and 4th cases. For schemes like "ldap" and "iiop",
     * the leading slash must be skipped before the name is an acceptable
     * format for operation by the Context methods. For schemes that treat the
     * leading slash as significant (such as "file"),
     * the subclass must override getURLSuffix() to get the correct behavior.
     * Remember, the behavior must match getRootURLContext().
     * 
     * URL Suffix
     * foo://host:port <empty string>
     * foo://host:port/rest/of/name rest/of/name
     * foo:///rest/of/name rest/of/name
     * foo:/rest/of/name rest/of/name
     * foo:rest/of/name rest/of/name
     */
    protected Name getURLSuffix(String prefix, String url) throws NamingException {
        String suffix = url.substring(prefix.length());
        if (suffix.length() == 0) {
            return new CompositeName();
        }

        if (suffix.charAt(0) == '/') {
            suffix = suffix.substring(1); // skip leading slash
        }

        // Note: Simplified implementation; a real implementation should
        // transform any URL-encoded characters into their Unicode char
        // representation
        return new CompositeName().add(suffix);
    }

    /**
     * Finds the prefix of a URL.
     * Default implementation looks for slashes and then extracts
     * prefixes using String.substring().
     * Subclass should override if this is not appropriate.
     * This method is used only by rename().
     * If rename() is supported for a particular URL scheme,
     * getRootURLContext(), getURLPrefix(), and getURLSuffix()
     * must be in sync wrt how URLs are parsed and returned.
     *<p>
     * URL Prefix
     * foo://host:port foo://host:port
     * foo://host:port/rest/of/name foo://host:port
     * foo:///rest/of/name foo://
     * foo:/rest/of/name foo:
     * foo:rest/of/name foo:
     */
    protected String getURLPrefix(String url) throws NamingException {
        int start = url.indexOf(":");

        if (start < 0) {
            throw new OperationNotSupportedException("Invalid URL: " + url);
        }
        ++start; // skip ':'

        if (url.startsWith("//", start)) {
            start += 2; // skip double slash

            // find last slash
            int posn = url.indexOf("/", start);
            if (posn >= 0) {
                start = posn;
            } else {
                start = url.length(); // rest of URL
            }
        }

        // else 0 or 1 initial slashes; start is unchanged
        return url.substring(0, start);
    }

    /**
     * Determines whether two URLs are the same.
     * Default implementation uses String.equals().
     * Subclass should override if this is not appropriate.
     * This method is used by rename().
     */
    protected boolean urlEquals(String url1, String url2) {
        return url1.equals(url2);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * lookup method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#lookup(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Object lookup(Name name) throws NamingException {
        if (name.size() == 1) {
            return lookup(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.lookup(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * lookup method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#lookup(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Object lookup(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.lookup(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * bind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     * @param name
     * @param obj
     * @throws javax.naming.NamingException
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.size() == 1) {
            bind(name.get(0), obj);
        } else {
            Context context = getContinuationContext(name);
            try {
                context.bind(name.getSuffix(1), obj);
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * bind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#bind(java.lang.String, java.lang.Object)
     * @param name
     * @param obj
     * @throws javax.naming.NamingException
     */
    public void bind(String name, Object obj) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            context.bind(result.getRemainingName(), obj);
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * rebind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     * @param name
     * @param obj
     * @throws javax.naming.NamingException
     */
    public void rebind(Name name, Object obj) throws NamingException {
        if (name.size() == 1) {
            rebind(name.get(0), obj);
        } else {
            Context context = getContinuationContext(name);
            try {
                context.rebind(name.getSuffix(1), obj);
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * rebind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     * @param name
     * @param obj
     * @throws javax.naming.NamingException
     */
    public void rebind(String name, Object obj) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            context.rebind(result.getRemainingName(), obj);
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * unbind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#unbind(javax.naming.Name)
     * @param name
     * @throws javax.naming.NamingException
     */
    public void unbind(Name name) throws NamingException {
        if (name.size() == 1) {
            unbind(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                context.unbind(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * unbind method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#unbind(java.lang.String)
     * @param name
     * @throws javax.naming.NamingException
     */
    public void unbind(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            context.unbind(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * rename method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     * @param oldName
     * @param newName
     * @throws javax.naming.NamingException
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        if ((oldName.size() == 1) && (newName.size() == 1)) {
            rename(oldName.get(0), newName.get(0));
        } else if ((newName.size() == 1) || (oldName.size() == 1)) {
            throw new NamingException("Unsupported rename from " + oldName + " to " + newName);
        } else {
            if (!oldName.get(0).equals(newName.get(0))) {
                throw new NamingException("Unsupported rename from " + oldName + " to " + newName);
            }

            Context context = getContinuationContext(oldName);
            try {
                context.rename(oldName.getSuffix(1), newName.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * rename method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     * @param oldName
     * @param newName
     * @throws javax.naming.NamingException
     */
    public void rename(String oldName, String newName) throws NamingException {
        String oldPrefix = getURLPrefix(oldName);
        String newPrefix = getURLPrefix(newName);
        if (!urlEquals(oldPrefix, newPrefix)) {
            throw new OperationNotSupportedException("Renaming using different URL prefixes not supported : " + oldName + " " + newName);
        }

        ResolveResult res = getRootURLContext(oldName);
        Context ctx = (Context) res.getResolvedObj();
        try {
            ctx.rename(res.getRemainingName(), getURLSuffix(newPrefix, newName));
        } finally {
            ctx.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * list method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#list(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NamingEnumeration list(Name name) throws NamingException {
        if (name.size() == 1) {
            return list(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.list(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * list method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#list(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NamingEnumeration list(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.list(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * listBindings method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.size() == 1) {
            return listBindings(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.listBindings(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * listBindings method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#listBindings(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.listBindings(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * destroySubcontext method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     * @param name
     * @throws javax.naming.NamingException
     */
    public void destroySubcontext(Name name) throws NamingException {
        if (name.size() == 1) {
            destroySubcontext(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                context.destroySubcontext(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * destroySubcontext method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     * @param name
     * @throws javax.naming.NamingException
     */
    public void destroySubcontext(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            context.destroySubcontext(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * createSubcontext method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Context createSubcontext(Name name) throws NamingException {
        if (name.size() == 1) {
            return createSubcontext(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.createSubcontext(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * createSubcontext method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Context createSubcontext(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.createSubcontext(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * lookupLink method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Object lookupLink(Name name) throws NamingException {
        if (name.size() == 1) {
            return lookupLink(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.lookupLink(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * lookupLink method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#lookupLink(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public Object lookupLink(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.lookupLink(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getNameParser method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NameParser getNameParser(Name name) throws NamingException {
        if (name.size() == 1) {
            return getNameParser(name.get(0));
        } else {
            Context context = getContinuationContext(name);
            try {
                return context.getNameParser(name.getSuffix(1));
            } finally {
                context.close();
            }
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getNameParser method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#getNameParser(java.lang.String)
     * @param name
     * @return
     * @throws javax.naming.NamingException
     */
    public NameParser getNameParser(String name) throws NamingException {
        ResolveResult result = getRootURLContext(name);
        Context context = (Context) result.getResolvedObj();
        try {
            return context.getNameParser(result.getRemainingName());
        } finally {
            context.close();
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * composeName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
     * @param name
     * @param prefix
     * @return
     * @throws javax.naming.NamingException
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = (Name) prefix.clone();
        result.addAll(name);
        return result;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * composeName method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     * @param name
     * @param prefix
     * @return
     * @throws javax.naming.NamingException
     */
    public String composeName(String name, String prefix) throws NamingException {
        if (prefix.equals("")) {
            return name;
        } else if (name.equals("")) {
            return prefix;
        } else {
            return (prefix + "/" + name);
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * addToEnvironment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#addToEnvironment(java.lang.String, java.lang.Object)
     * @param propName
     * @param propVal
     * @return
     * @throws javax.naming.NamingException
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        _environment = (_environment == null) ? new Hashtable(11, 0.75f) : (Hashtable) _environment.clone();
        return _environment.put(propName, propVal);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * removeFromEnvironment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     * @param propName
     * @return
     * @throws javax.naming.NamingException
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        if (_environment == null) {
            return null;
        }
        _environment = (Hashtable) _environment.clone();
        return _environment.remove(propName);
    }

    /* -------------------------------------------------------------------------- */
    /*
     * getEnvironment method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#getEnvironment()
     * @return
     * @throws javax.naming.NamingException
     */
    public Hashtable getEnvironment() throws NamingException {
        return _environment;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * close method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#close()
     * @throws javax.naming.NamingException
     */
    public void close() throws NamingException {}

    /* -------------------------------------------------------------------------- */
    /*
     * getNameInNamespace method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see javax.naming.Context#getNameInNamespace()
     * @return
     * @throws javax.naming.NamingException
     */
    public String getNameInNamespace() throws NamingException {
        return ""; // A URL context's name is ""
    }
}
