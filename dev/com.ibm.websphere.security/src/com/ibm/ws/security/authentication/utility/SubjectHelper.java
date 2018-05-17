/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.utility;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;

public class SubjectHelper {
    private static final TraceComponent tc = Tr.register(SubjectHelper.class);

    /**
     * Check whether the subject is un-authenticated or not.
     *
     * @param subject {@code null} is supported.
     * @return Returns {@code true} if the Subject is either null
     *         or the UNAUTHENTICATED Subject. {@code false} otherwise.
     */
    public boolean isUnauthenticated(Subject subject) {
        if (subject == null) {
            return true;
        }

        WSCredential wsCred = getWSCredential(subject);
        if (wsCred == null) {
            return true;
        } else {
            return wsCred.isUnauthenticated();
        }
    }

    /**
     * Gets the realm from the subjects' WSCredential.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    public String getRealm(Subject subject) throws Exception {
        String realm = null;
        WSCredential credential = getWSCredential(subject);
        if (credential != null) {
            realm = credential.getRealmName();
        }
        return realm;
    }

    /**
     * Gets the WSCredential from the subject.
     *
     * @param subject {@code null} is not supported.
     * @return
     */
    public WSCredential getWSCredential(Subject subject) {
        WSCredential wsCredential = null;
        Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
        Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
        if (wsCredentialsIterator.hasNext()) {
            wsCredential = wsCredentialsIterator.next();
        }
        return wsCredential;
    }

    /**
     * Gets a Hashtable of values from the Subject.
     *
     * @param subject {@code null} is not supported.
     * @param properties The properties to get.
     * @return
     */
    public Hashtable<String, ?> getHashtableFromSubject(final Subject subject, final String[] properties) {
        return AccessController.doPrivileged(new PrivilegedAction<Hashtable<String, ?>>() {
            @Override
            public Hashtable<String, ?> run() {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Looking for custom properties in public cred list.");
                }
                Set<Object> list_public = subject.getPublicCredentials();
                Hashtable<String, ?> hashtableFromPublic = getHashtable(list_public, properties);
                if (hashtableFromPublic != null) {
                    return hashtableFromPublic;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Looking for custom properties in private cred list.");
                }
                Set<Object> list_private = subject.getPrivateCredentials();
                Hashtable<String, ?> hashtableFromPrivate = getHashtable(list_private, properties);
                if (hashtableFromPrivate != null) {
                    return hashtableFromPrivate;
                }

                return null;
            }
        });
    }

    /**
     * Given a credential Set and an array of properties, find the Hashtable (if it exists) that has the
     * expected properties.
     *
     * @param creds {@code null} is not supported.
     * @param properties {@code null} is not supported.
     * @return The Hashtable with our properties, or null.
     */
    @SuppressWarnings("unchecked")
    private Hashtable<String, ?> getHashtable(Set<Object> creds, String[] properties) {
        int i = 0;
        for (Object cred : creds) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object[" + i + "] in credential list: " + cred);
            }
            if (cred instanceof Hashtable) {
                for (int j = 0; j < properties.length; j++) {
                    if (((Hashtable) cred).get(properties[j]) != null)
                        return (Hashtable) cred;
                }
            }
            i++;
        }
        return null;
    }

    /**
     * Gets a GSSCredential from a Subject
     */
    public static GSSCredential getGSSCredentialFromSubject(final Subject subject) {
        if (subject == null) {
            return null;
        }
        GSSCredential gssCredential = AccessController.doPrivileged(new PrivilegedAction<GSSCredential>() {
            @Override
            public GSSCredential run() {
                GSSCredential gssCredInSubject = null;
                Set<GSSCredential> privCredentials = subject.getPrivateCredentials(GSSCredential.class);
                if (privCredentials != null) {
                    Iterator<GSSCredential> privCredItr = privCredentials.iterator();
                    if (privCredItr.hasNext()) {
                        gssCredInSubject = privCredItr.next();
                    }
                }
                return gssCredInSubject;
            }
        });

        if (gssCredential == null) {
            KerberosTicket tgt = getKerberosTicketFromSubject(subject, null);
            if (tgt != null) {
                gssCredential = createGSSCredential(subject, tgt);
            }
        }

        return gssCredential;
    }

    /**
     * @param subject
     * @return
     */
    private static KerberosTicket getKerberosTicketFromSubject(final Subject subject, final String user) {
        KerberosTicket tgt = AccessController.doPrivileged(new PrivilegedAction<KerberosTicket>() {
            @Override
            public KerberosTicket run() {
                KerberosTicket kerberosTicketInSubject = null;
                Set<KerberosTicket> privCredentials = subject.getPrivateCredentials(KerberosTicket.class);
                if (privCredentials != null) {
                    Iterator<KerberosTicket> privCredItr = privCredentials.iterator();
                    while (privCredItr.hasNext()) {
                        KerberosTicket kTicket = privCredItr.next();
                        if (user == null) {
                            return kTicket;
                        } else if (kTicket.getClient().getName().startsWith(user))
                            return kTicket;
                    }
                }
                return kerberosTicketInSubject;
            }
        });
        return tgt;
    }

    public static boolean isSpnTGTInSubject(final Subject subject, final String user) {
        boolean result = false;
        KerberosTicket tgt = getKerberosTicketFromSubject(subject, user);
        if (tgt != null) {
            result = true;
        }
        return result;
    }

    public static boolean isTGTInSubjectValid(final Subject subject, final String user) {
        boolean result = false;
        KerberosTicket tgt = getKerberosTicketFromSubject(subject, user);
        if (tgt != null) {
            return tgt.isCurrent();
        }
        return result;
    }

    private static GSSCredential createGSSCredential(Subject subject, final KerberosTicket ticket) {
        GSSCredential gssCred = null;
        PrivilegedAction<Object> action = new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                GSSCredential inGssCrd = null;
                try {
                    String clientName = ticket.getClient().getName();
                    Oid krbOid = new Oid("1.2.840.113554.1.2.2");
                    if (clientName != null && clientName.length() > 0) {
                        GSSManager gssMgr = GSSManager.getInstance();
                        GSSName gssName = gssMgr.createName(clientName, GSSName.NT_USER_NAME, krbOid);
                        inGssCrd = gssMgr.createCredential(gssName.canonicalize(krbOid),
                                                           GSSCredential.INDEFINITE_LIFETIME,
                                                           krbOid, GSSCredential.INITIATE_AND_ACCEPT);
                    }

                } catch (GSSException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "getGSSCredential unexpected exception", e);
                }
                return inGssCrd;

            }
        };

        gssCred = (GSSCredential) WSSubject.doAs(subject, action);

        return gssCred;
    }

    /**
     * Creates a Hashtable of values in the Subject without tracing the Subject.
     *
     * @param subject {@code null} is not supported.
     * @return the hashtable containing the properties.
     */
    @Sensitive
    public Hashtable<String, Object> createNewHashtableInSubject(@Sensitive final Subject subject) {
        return AccessController.doPrivileged(new NewHashtablePrivilegedAction(subject));
    }

    /*
     * Class to avoid tracing Subject that otherwise is traced when using
     * an anonymous PrivilegedAction resulting in an ACE when application code
     * is in the call stack.
     */
    class NewHashtablePrivilegedAction implements PrivilegedAction<Hashtable<String, Object>> {

        private final Subject subject;

        @Trivial
        public NewHashtablePrivilegedAction(Subject subject) {
            this.subject = subject;
        }

        @Override
        public Hashtable<String, Object> run() {
            Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
            subject.getPrivateCredentials().add(hashtable);
            return hashtable;
        }

    }

    /**
     * Gets a Hashtable of values from the Subject without tracing the Subject or hashtable.
     *
     * @param subject {@code null} is not supported.
     * @return the hashtable containing the properties.
     */
    @Sensitive
    public Hashtable<String, ?> getSensitiveHashtableFromSubject(@Sensitive final Subject subject) {
        if (System.getSecurityManager() == null) {
            return getHashtableFromSubject(subject);
        } else {
            return AccessController.doPrivileged(new GetHashtablePrivilegedAction(subject));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Trivial
    private Hashtable<String, ?> getHashtableFromSubject(Subject subject) {
        Set s = subject.getPrivateCredentials(Hashtable.class);
        if (s == null || s.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Subject has no Hashtable, return null.");
            }
            return null;
        } else {
            return (Hashtable<String, ?>) s.iterator().next();
        }
    }

    /*
     * Class to avoid tracing Subject that otherwise is traced when using
     * an anonymous PrivilegedAction resulting in an ACE when application code
     * is in the call stack.
     */
    private class GetHashtablePrivilegedAction implements PrivilegedAction<Hashtable<String, ?>> {

        private final Subject subject;

        @Trivial
        public GetHashtablePrivilegedAction(Subject subject) {
            this.subject = subject;
        }

        @Trivial
        @Override
        public Hashtable<String, ?> run() {
            return getHashtableFromSubject(subject);
        }

    }

    /**
     * Gets a Hashtable of values from the Subject, but do not trace the hashtable
     *
     * @param subject {@code null} is not supported.
     * @param properties The properties to get.
     * @return the hashtable containing the properties.
     */
    @Sensitive
    public Hashtable<String, ?> getSensitiveHashtableFromSubject(@Sensitive final Subject subject, @Sensitive final String[] properties) {
        return AccessController.doPrivileged(new PrivilegedAction<Hashtable<String, ?>>() {
            @Override
            public Hashtable<String, ?> run() {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Looking for custom properties in public cred list.");
                }
                Set<Object> list_public = subject.getPublicCredentials();
                Hashtable<String, ?> hashtableFromPublic = getSensitiveHashtable(list_public, properties);
                if (hashtableFromPublic != null) {
                    return hashtableFromPublic;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Looking for custom properties in private cred list.");
                }
                Set<Object> list_private = subject.getPrivateCredentials();
                Hashtable<String, ?> hashtableFromPrivate = getSensitiveHashtable(list_private, properties);
                if (hashtableFromPrivate != null) {
                    return hashtableFromPrivate;
                }

                return null;
            }
        });
    }

    /*
     * The hashtable returned must contain all the properties being asked for.
     */
    @Trivial
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Hashtable<String, ?> getSensitiveHashtable(Set<Object> creds, String[] properties) {
        List<String> props = new ArrayList<String>();
        for (String property : properties) {
            props.add(property);
        }

        for (Object cred : creds) {
            if (cred instanceof Hashtable) {
                if (((Hashtable) cred).keySet().containsAll(props)) {
                    return (Hashtable) cred;
                }
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String setSystemProperty(final String propName, final String propValue) {

        String savedPropValue = (String) java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            @Override
            public String run() {
                String oldValue = System.getProperty(propName);
                System.setProperty(propName, propValue);
                return oldValue;
            }
        });
        if (tc.isDebugEnabled())
            Tr.debug(tc, propName + " property previous: " + ((savedPropValue != null) ? savedPropValue : "<null>") + " and now: " + propValue);

        return savedPropValue;
    }
}
