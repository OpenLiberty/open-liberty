/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.context;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.adapter.ldap.LdapHelper;

/**
 * This is a wrapper class for an {@link InitialLdapContext} that contains the ability to timeout the connection. The timeout
 * is lazily forced on access.
 *
 * <p/>
 * This class does not implement the {@link DirContext} interface nor extend the {@link InitialLdapContext} class
 * so that we can purposely trace and time all JNDI calls.
 *
 * <p/>
 * TODO This class would be a good place to move the search and attributes caching.
 */
@Trivial
public class TimedDirContext {

    /** Constant for JNDI_CALL trace */
    public static final String JNDI_CALL = "JNDI_CALL ";

    /** {@link TraceComponent} for the class. */
    private static final TraceComponent tc = Tr.register(TimedDirContext.class);

    /**
     * The {@link InitialLdapContext} instance.
     */
    private final InitialLdapContext context;

    /** The context creation time stamp. */
    private long iCreateTimestampSeconds;

    /** The context pool time stamp. */
    private long iPoolTimestampSeconds;

    private final String iProviderURL;

    /**
     * Construct a new {@link TimedDirContext} instance.
     *
     * @param environment The environment.
     * @param connCtls Connection controls.
     * @param createTimestamp The creation time stamp to use (in seconds).
     * @throws NamingException If a {@link NamingException} was encountered.
     *
     * @see InitialLdapContext#InitialLdapContext(Hashtable, Control[])
     */
    @Sensitive
    public TimedDirContext(@Sensitive Hashtable<?, ?> environment, Control[] connCtls, long createTimestamp) throws NamingException {
        context = new InitialLdapContext(environment, connCtls);
        iCreateTimestampSeconds = createTimestamp;
        iPoolTimestampSeconds = createTimestamp;
        iProviderURL = (String) environment.get(DirContext.PROVIDER_URL);
    }

    /** @see InitialLdapContext#close() */
    @FFDCIgnore({ NamingException.class })
    public void close() throws NamingException {
        final String METHODNAME = "close()";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME);
            begin = System.currentTimeMillis();
            context.close();
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /** @see InitialLdapContext#createSubcontext(Name, Attributes) */
    @FFDCIgnore({ NamingException.class })
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        final String METHODNAME = "createSubcontext(Name,Attributes)";

        long begin = 0;
        NamingException ne = null;
        DirContext results = null;

        try {
            traceJndiBegin(METHODNAME, name, attrs);
            begin = System.currentTimeMillis();
            results = context.createSubcontext(name, attrs);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /** @see InitialLdapContext#destroySubcontext(Name) */
    @FFDCIgnore({ NamingException.class })
    public void destroySubcontext(Name name) throws NamingException {
        final String METHODNAME = "destroySubcontext(Name)";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME, name);
            begin = System.currentTimeMillis();
            context.destroySubcontext(name);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /** @see InitialLdapContext#getAttributes(Name, String[]) */
    @FFDCIgnore({ NamingException.class })
    public Attributes getAttributes(Name name, String[] attrs) throws NamingException {
        final String METHODNAME = "getAttributes(Name,String[])";

        long begin = 0;
        NamingException ne = null;
        Attributes results = null;

        try {
            traceJndiBegin(METHODNAME, name, attrs);
            begin = System.currentTimeMillis();
            results = context.getAttributes(name, attrs);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /**
     * Get the context creation time stamp.
     *
     * @return The time stamp in seconds.
     */
    public long getCreateTimestamp() {
        return iCreateTimestampSeconds;
    }

    /** @see InitialLdapContext#getEnvironment() */
    @Sensitive
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        /*
         * Don't trace as it may contain the bind password.
         */
        return context.getEnvironment();
    }

    /** @see InitialLdapContext#getNameParser(String) */
    @FFDCIgnore({ NamingException.class })
    public NameParser getNameParser(String name) throws NamingException {
        final String METHODNAME = "getNameParser(String)";

        long begin = 0;
        NamingException ne = null;
        NameParser results = null;

        try {
            traceJndiBegin(METHODNAME, name);
            begin = System.currentTimeMillis();
            results = context.getNameParser(name);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /**
     * Get the pool time stamp.
     *
     * @return The time stamp in seconds.
     */
    public long getPoolTimestamp() {
        return iPoolTimestampSeconds;
    }

    /** @see InitialLdapContext#getResponseControls() */
    @FFDCIgnore({ NamingException.class })
    public Control[] getResponseControls() throws NamingException {
        final String METHODNAME = "getResponseControls()";

        long begin = 0;
        NamingException ne = null;
        Control[] results = null;

        try {
            traceJndiBegin(METHODNAME);
            begin = System.currentTimeMillis();
            results = context.getResponseControls();
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), (Object[]) results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /** @see InitialLdapContext#modifyAttributes(Name, int, Attributes) */
    @FFDCIgnore({ NamingException.class })
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        final String METHODNAME = "modifyAttributes(Name,int,Attributes)";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME, name, mod_op, attrs);
            begin = System.currentTimeMillis();
            context.modifyAttributes(name, mod_op, attrs);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /** @see InitialLdapContext#modifyAttributes(Name, ModificationItem[]) */
    @FFDCIgnore({ NamingException.class })
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        final String METHODNAME = "modifyAttributes(Name,ModificationItem[])";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME, name, mods);
            begin = System.currentTimeMillis();
            context.modifyAttributes(name, mods);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /** @see InitialLdapContext#rename(String, String) */
    @FFDCIgnore({ NamingException.class })
    public void rename(String oldName, String newName) throws NamingException {
        final String METHODNAME = "rename(String,String)";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME, oldName, newName);
            begin = System.currentTimeMillis();
            context.rename(oldName, newName);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /** @see InitialLdapContext#search(Name, String, Object[], SearchControls) */
    @FFDCIgnore({ NamingException.class })
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        final String METHODNAME = "search(Name,String,Object[],SearchControls)";

        long begin = 0;
        NamingException ne = null;
        NamingEnumeration<SearchResult> results = null;

        try {

            traceJndiBegin(METHODNAME, name, filterExpr, filterArgs, LdapHelper.printSearchControls(cons));
            begin = System.currentTimeMillis();
            results = context.search(name, filterExpr, filterArgs, cons);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /** @see InitialLdapContext#search(Name, String, SearchControls)) */
    @FFDCIgnore({ NamingException.class })
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, SearchControls cons) throws NamingException {
        final String METHODNAME = "search(Name,String,SearchControls)";

        long begin = 0;
        NamingException ne = null;
        NamingEnumeration<SearchResult> results = null;

        try {
            traceJndiBegin(METHODNAME, name, filterExpr, LdapHelper.printSearchControls(cons));
            begin = System.currentTimeMillis();
            results = context.search(name, filterExpr, cons);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /** @see InitialLdapContext#search(String, String, SearchControls) */
    @FFDCIgnore({ NamingException.class })
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, SearchControls cons) throws NamingException {
        final String METHODNAME = "search(String,String,SearchControls)";

        long begin = 0;
        NamingException ne = null;
        NamingEnumeration<SearchResult> results = null;

        try {
            traceJndiBegin(METHODNAME, name, filterExpr, cons);
            begin = System.currentTimeMillis();
            results = context.search(name, filterExpr, cons);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin), results);
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }

        return results;
    }

    /**
     * Set the context creation time stamp.
     *
     * @param createTimestamp The time stamp in seconds.
     */
    public void setCreateTimestamp(long createTimestamp) {
        iCreateTimestampSeconds = createTimestamp;
    }

    /**
     * Set the pool time stamp.
     *
     * @param poolTimestamp The time stamp in seconds.
     */
    public void setPoolTimeStamp(long poolTimestamp) {
        iPoolTimestampSeconds = poolTimestamp;
    }

    /** @see InitialLdapContext#setRequestControls(Control[]) */
    @FFDCIgnore({ NamingException.class })
    public void setRequestControls(Control[] requestControls) throws NamingException {
        final String METHODNAME = "setRequestControls(Control[])";

        long begin = 0;
        NamingException ne = null;

        try {
            traceJndiBegin(METHODNAME, (Object[]) requestControls);
            begin = System.currentTimeMillis();
            context.setRequestControls(requestControls);
        } catch (NamingException e) {
            ne = e;
            throw e;
        } finally {
            /*
             * Always attempt to trace the result.
             */
            if (ne == null) {
                traceJndiReturn(METHODNAME, (System.currentTimeMillis() - begin));
            } else {
                traceJndiThrow(METHODNAME, (System.currentTimeMillis() - begin), ne);
            }
        }
    }

    /**
     * Trace a message with "JNDI_CALL' that includes the parameters sent to the JNDI call.
     *
     * @param methodname The name of the method that is requesting the trace.
     * @param objs The parameters to trace.
     */
    private void traceJndiBegin(String methodname, Object... objs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String providerURL = "UNKNOWN";
            try {
                providerURL = (String) getEnvironment().get(Context.PROVIDER_URL);
            } catch (NamingException ne) {
                /* Ignore. */
            }
            Tr.debug(tc, JNDI_CALL + methodname + " [" + providerURL + "]", objs);
        }
    }

    /**
     * Trace a message with "JNDI_CALL' that includes the returned objects from that JNDI call.
     *
     * @param methodname The name of the method that is requesting the trace.
     * @param duration The duration of the call in milliseconds.
     * @param objs The objects to trace.
     */
    private void traceJndiReturn(String methodname, long duration, Object... objs) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, JNDI_CALL + methodname + " [" + duration + " ms]", objs);
        }
    }

    /**
     * Trace a message with "JNDI_CALL' that includes the resulting exception from that JNDI call.
     *
     * @param methodname The name of the method that is requesting the trace.
     * @param duration The duration of the call in milliseconds.
     * @param ne The {@link NamingException}.
     */
    private void traceJndiThrow(String methodname, long duration, NamingException ne) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, JNDI_CALL + methodname + " [" + duration + " ms] " + ne.getMessage(), ne);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "{iProviderURL=" + iProviderURL + ", iCreateTimestampSeconds=" + iCreateTimestampSeconds + ", iPoolTimeStampSeconds=" + iPoolTimestampSeconds
               + "}";
    }
}