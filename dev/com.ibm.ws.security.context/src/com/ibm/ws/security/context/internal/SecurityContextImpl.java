/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.context.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.authentication.helper.AuthenticateUserHelper;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This class establishes the security context for a unit of work.
 * 
 * An instance of this object is created by the work-queueing thread via
 * SecurityContextProviderImpl.captureThreadContext().
 * 
 * The thread that executes the work then calls taskStarting() and taskStopping()
 * on this object, to set up and remove the security context, respectively.
 * 
 * The security context is serializable, which means it can be flattened and
 * re-inflated, possibly on another machine.
 */
public class SecurityContextImpl implements ThreadContext {
    private static final long serialVersionUID = 2674866355469888259L;

    private static final TraceComponent tc = Tr.register(SecurityContextImpl.class);
    protected final static String DESERIALIZE_LOGINCONTEXT_DEFAULT = JaasLoginConfigConstants.SYSTEM_DESERIALIZE_CONTEXT; //TODO: add deserialize

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    private static final String CALLER_PRINCIPAL = "C", INVOCATION_PRINCIPAL = "I", SUBJECTS_ARE_EQUAL = "E", JAAS_LOGIN_CONTEXT = "J",
                    CALLER_SUBJECT_CACHE_KEY = "CK", INVOCATION_SUBJECT_CACHE_KEY = "IK";

    /**
     * Fields to serialize
     * 
     * The field names are used by the readObject/writeObject operations as the key to lookup the serializable
     * data in the stream. If you change field names, they must also be changed in readObject/writeObject.
     * 
     * CAUTION! Deleting a field or changing the type of a field will be an incompatible change, and should be avoided at all cost.
     * 
     * See <a
     * href="http://docs.oracle.com/javase/7/docs/platform/serialization/spec/version.html#6519">http://docs.oracle.com/javase/7/docs/platform/serialization/spec/version.html
     * #6519</a>
     */
    private static final ObjectStreamField[] serialPersistentFields =
                    new ObjectStreamField[] {
                                             new ObjectStreamField(CALLER_PRINCIPAL, WSPrincipal.class),
                                             new ObjectStreamField(INVOCATION_PRINCIPAL, WSPrincipal.class),
                                             new ObjectStreamField(SUBJECTS_ARE_EQUAL, boolean.class),
                                             new ObjectStreamField(JAAS_LOGIN_CONTEXT, String.class),
                                             new ObjectStreamField(CALLER_SUBJECT_CACHE_KEY, String.class),
                                             new ObjectStreamField(INVOCATION_SUBJECT_CACHE_KEY, String.class) };

    /**
     * Constant for error message. Unable to serialize custom cache key
     */
    private static final String SEC_CONTEXT_UNABLE_TO_SERIALIZE = "SEC_CONTEXT_DESERIALIZE_AUTHN_ERROR";

    /**
     * @serialField The invocation subject principal associated with this security context.
     */
    protected WSPrincipal invocationPrincipal = null;

    /**
     * @serialField The caller subject principal associated with this security context.
     */
    protected WSPrincipal callerPrincipal = null;

    /**
     * @serialField True if Caller and Invocation subjects are equal
     */
    private boolean subjectsAreEqual = false;

    /**
     * @serialField The name of the jaasLoginContext to use for the deserialization login
     */
    private String jaasLoginContextEntry = null;

    /**
     * The invocation subject associated with this security context.
     */
    protected transient Subject invocationSubject = null;

    /**
     * The caller subject associated with this security context.
     */
    protected transient Subject callerSubject = null;

    /**
     * The previous invocation subject on the thread prior to applying this
     * security context. The previous subjects are restored when this security
     * context is removed.
     * 
     * The previous subjects are determined at execute time; therefore they are
     * not serialized with the security context.
     */
    private transient Subject prevInvocationSubject = null;

    /**
     * The previous caller subject on the thread prior to applying this security
     * context. The previous subjects are restored when this security
     * context is removed.
     * 
     * The previous subjects are determined at execute time; therefore they are
     * not serialized with the security context.
     */
    private transient Subject prevCallerSubject = null;

    /**
     * For getting/setting the Invocation and Caller Subjects.
     */
    private transient SubjectManager subjectManager = null;

    /**
     * For checking if subject is unauthenticated
     */
    protected transient SubjectHelper subjectHelper = null;

    /*
     * Authentication Cache service
     * 12/08 RZ: This is no longer required
     */
    // private transient AuthCacheService authCache = null;

    /**
     * @serialField Caller Subject Cache Key
     */
    private String callerSubjectCacheKey = null;

    /**
     * @serialField Invocation Subject Cache Key
     */
    private String invocationSubjectCacheKey = null;

    /**
     * CTOR. Initializes the SecurityContextImpl with the security context
     * of the current thread.
     * 
     * @param captureCurrentThreadContext indicates that we should capture security context from the current thread instead of establishing a default context.
     * @param jaasLoginContextEntry the name of the jaasLoginContextEntry to use for the deserialization login
     */
    public SecurityContextImpl(boolean captureCurrentThreadContext, String jaasLoginContextEntry/* , AuthCacheService cache */) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", new Object[] { captureCurrentThreadContext, jaasLoginContextEntry });
        this.jaasLoginContextEntry = jaasLoginContextEntry;
        subjectManager = new SubjectManager();
        subjectHelper = new SubjectHelper();
        // authCache = cache;
        if (captureCurrentThreadContext) {
            /*
             * Save refs to the subjects that are currently active on the thread. These
             * subjects represent the security context associated with this object. They
             * will be applied to the thread when taskStarting is invoked.
             */
            invocationSubject = subjectManager.getInvocationSubject();
            callerSubject = subjectManager.getCallerSubject();
        }
        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", new Object[] { "caller/invocation subjects", callerSubject, invocationSubject });
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            SecurityContextImpl copy = (SecurityContextImpl) super.clone();
            copy.prevCallerSubject = null;
            copy.prevInvocationSubject = null;
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Push the subjects associated with this security context onto the thread.
     */
    @Override
    public void taskStarting() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        prevInvocationSubject = subjectManager.getInvocationSubject();
        prevCallerSubject = subjectManager.getCallerSubject();

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "taskStarting", "previous caller/invocation subjects", prevCallerSubject, prevInvocationSubject);

        subjectManager.setInvocationSubject(invocationSubject);
        subjectManager.setCallerSubject(callerSubject);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "taskStarting", new Object[] { "new caller/invocation subjects", callerSubject, invocationSubject });
    }

    /**
     * Restore the subjects that were previously on the thread prior to applying this
     * security context.
     */
    @Override
    public void taskStopping() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "taskStopping", "restore caller/invocation subjects", prevCallerSubject, prevInvocationSubject);

        subjectManager.setCallerSubject(prevCallerSubject);
        subjectManager.setInvocationSubject(prevInvocationSubject);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "taskStopping");
    }

    /**
     * @return true if the two Subjects are equal
     */
    private boolean areSubjectsEqual(Subject subj1, Subject subj2) {
        return (subj1 == subj2);
    }

    /**
     * Serialize security context.
     * 
     * @param out The stream to which this object is serialized.
     * 
     * @throws IOException if there is more than one WSPrincipal in the subject or if I/O error occurs
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "writeObject",
                     new Object[] { "caller/invocation subjects:", callerSubject, invocationSubject, "jaasLoginContextEntry:", jaasLoginContextEntry });

        PutField fields = out.putFields();

        if (callerSubject != null && !subjectHelper.isUnauthenticated(callerSubject)) {
            fields.put(CALLER_PRINCIPAL, getWSPrincipal(callerSubject));
        }

        subjectsAreEqual = areSubjectsEqual(callerSubject, invocationSubject);
        fields.put(SUBJECTS_ARE_EQUAL, subjectsAreEqual);

        //only serialize invocation principal if it's different from the caller
        if (!subjectsAreEqual) {
            if (invocationSubject != null && !subjectHelper.isUnauthenticated(invocationSubject)) {
                fields.put(INVOCATION_PRINCIPAL, getWSPrincipal(invocationSubject));
            }
        }

        if (jaasLoginContextEntry != null) {
            fields.put(JAAS_LOGIN_CONTEXT, jaasLoginContextEntry);
        }

        // Serialize Subject Cache key
        try {
            serializeSubjectCacheKey(fields);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unable to serialize Subject Cache Key: " + e.getMessage());
            if (tc.isWarningEnabled())
                Tr.warning(tc, SEC_CONTEXT_UNABLE_TO_SERIALIZE);
        }

        out.writeFields();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "writeObject", new Object[] { "subjects are equal: ", subjectsAreEqual });
    }

    /**
     * Deserialize security context.
     * 
     * @param in The stream from which this object is read.
     * 
     * @throws IOException if there are I/O errors while reading from the underlying InputStream
     * @throws ClassNotFoundException if the class of a serialized object could not be found.
     */
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "readObject");

        GetField fields = in.readFields();

        readState(fields);

        subjectManager = new SubjectManager();

        if (trace && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "readObject",
                    new Object[] { "deserialized caller/invocation principals: ", callerPrincipal, invocationPrincipal,
                                  "subjects are equal? ", subjectsAreEqual, "jaasLoginContextEntry: ", jaasLoginContextEntry });
        }
    }

    /**
     * Read the security context
     * 
     * @param fields
     * @throws IOException if there are I/O errors while reading from the underlying InputStream
     */
    private void readState(GetField fields) throws IOException {
        //get caller principal
        callerPrincipal = (WSPrincipal) fields.get(CALLER_PRINCIPAL, null);

        //get boolean marking if subjects are equal
        subjectsAreEqual = fields.get(SUBJECTS_ARE_EQUAL, false);

        //only deserialize invocation principal if it's different from the caller
        if (!subjectsAreEqual) {
            //get invocation principal
            invocationPrincipal = (WSPrincipal) fields.get(INVOCATION_PRINCIPAL, null);
        } else {
            invocationPrincipal = callerPrincipal;
        }

        jaasLoginContextEntry = (String) fields.get(JAAS_LOGIN_CONTEXT, null);
        callerSubjectCacheKey = (String) fields.get(CALLER_SUBJECT_CACHE_KEY, null);
        invocationSubjectCacheKey = (String) fields.get(INVOCATION_SUBJECT_CACHE_KEY, null);
    }

    /**
     * Perform a login to recreate the full subject, given a WSPrincipal
     * 
     * @param wsPrincipal the deserialized WSPrincipal that will be used for creating the new subject
     * @param securityService the security service to use for authenticating the user
     * @param AtomicServiceReference<UnauthenticatedSubjectService> reference to the unauthenticated subject service for creating the unauthenticated subject
     * @param customCacheKey The custom cache key to look up the subject
     * @return the authenticated subject, or unauthenticated if there was an error during authentication or wsPrincipal was null
     */
    @FFDCIgnore(AuthenticationException.class)
    protected Subject recreateFullSubject(WSPrincipal wsPrincipal, SecurityService securityService,
                                          AtomicServiceReference<UnauthenticatedSubjectService> unauthenticatedSubjectServiceRef, String customCacheKey) {
        Subject subject = null;
        if (wsPrincipal != null) {
            String userName = wsPrincipal.getName();
            AuthenticateUserHelper authHelper = new AuthenticateUserHelper();
            if (jaasLoginContextEntry == null) {
                jaasLoginContextEntry = DESERIALIZE_LOGINCONTEXT_DEFAULT;
            }
            try {
                subject = authHelper.authenticateUser(securityService.getAuthenticationService(), userName, jaasLoginContextEntry, customCacheKey);
            } catch (AuthenticationException e) {
                Tr.error(tc, "SEC_CONTEXT_DESERIALIZE_AUTHN_ERROR", new Object[] { e.getLocalizedMessage() });
            }
        }
        if (subject == null) {
            subject = unauthenticatedSubjectServiceRef.getService().getUnauthenticatedSubject();
        }
        return subject;
    }

    /**
     * Get the WSPrincipal from the subject
     * 
     * @param subject
     * @return the WSPrincipal of the subject
     * @throws IOException if there is more than one WSPrincipal in the subject
     */
    protected WSPrincipal getWSPrincipal(Subject subject) throws IOException {
        WSPrincipal wsPrincipal = null;
        Set<WSPrincipal> principals = (subject != null) ? subject.getPrincipals(WSPrincipal.class) : null;
        if (principals != null && !principals.isEmpty()) {
            if (principals.size() > 1) {
                // Error - too many principals
                String principalNames = null;
                for (WSPrincipal principal : principals) {
                    if (principalNames == null)
                        principalNames = principal.getName();
                    else
                        principalNames = principalNames + ", " + principal.getName();
                }
                throw new IOException(Tr.formatMessage(tc, "SEC_CONTEXT_DESERIALIZE_TOO_MANY_PRINCIPALS", principalNames));
            } else {
                wsPrincipal = principals.iterator().next();
            }
        }
        return wsPrincipal;
    }

    /**
     * After deserializing the security context, re-inflate the subjects (need to add the missing credentials, since these don't get serialized)
     * 
     * @param securityService the security service to use for authenticating the user
     * @param unauthenticatedSubjectServiceRef reference to the unauthenticated subject service for creating the unauthenticated subject
     */
    protected void recreateFullSubjects(SecurityService securityService, AtomicServiceReference<UnauthenticatedSubjectService> unauthenticatedSubjectServiceRef) {
        callerSubject = recreateFullSubject(callerPrincipal, securityService, unauthenticatedSubjectServiceRef, callerSubjectCacheKey);
        if (!subjectsAreEqual) {
            invocationSubject = recreateFullSubject(invocationPrincipal, securityService, unauthenticatedSubjectServiceRef, invocationSubjectCacheKey);
        } else {
            invocationSubject = callerSubject;
        }
    }

    /**
     * Serialize the cache lookup keys for the caller and invocation subjects
     */
    private void serializeSubjectCacheKey(PutField fields) throws Exception {
        if (callerSubject != null) {
            Hashtable<String, ?> hashtable =
                            subjectHelper.getHashtableFromSubject(callerSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY });

            if (hashtable != null) {
                callerSubjectCacheKey = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);

/*
 * 12/08/14 RZ: Removed after discussions with Ajay and Ut. This is an unwanted functionality.
 * 
 * if (callerSubjectCacheKey != null && authCache != null) {
 * Subject lookedUpSubject = authCache.getSubject(callerSubjectCacheKey);
 * 
 * // If we looked up the wrong subject, then create a BasicAuthCacheKey
 * if (!lookedUpSubject.equals(callerSubject)) {
 * hashtable = subjectHelper.getHashtableFromSubject(callerSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_USERID,
 * AttributeNameConstants.WSCREDENTIAL_PASSWORD });
 * 
 * if (hashtable != null) {
 * String userid = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID);
 * String password = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);
 * 
 * if (password != null) {
 * callerSubjectCacheKey = BasicAuthCacheKeyProvider.createLookupKey(subjectHelper.getRealm(callerSubject), userid, password);
 * } else {
 * callerSubjectCacheKey = BasicAuthCacheKeyProvider.createLookupKey(subjectHelper.getRealm(callerSubject), userid);
 * }
 * }
 * }
 * }
 */
            }
        }

        if (callerSubjectCacheKey != null)
            fields.put(CALLER_SUBJECT_CACHE_KEY, callerSubjectCacheKey);

        //only serialize invocation subject if it's different from the caller
        if (!subjectsAreEqual) {
            if (invocationSubject != null) {
                Hashtable<String, ?> hashtable =
                                subjectHelper.getHashtableFromSubject(invocationSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY });
                if (hashtable != null) {
                    invocationSubjectCacheKey = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);

/*
 * 12/08/14 RZ: Removed after discussions with Ajay and Ut. This is an unwanted functionality.
 * 
 * if (invocationSubjectCacheKey != null && authCache != null) {
 * Subject lookedUpSubject = authCache.getSubject(invocationSubjectCacheKey);
 * 
 * // If we looked up the wrong subject, then create a BasicAuthCacheKey
 * if (!lookedUpSubject.equals(invocationSubject)) {
 * hashtable = subjectHelper.getHashtableFromSubject(invocationSubject, new String[] { AttributeNameConstants.WSCREDENTIAL_USERID,
 * AttributeNameConstants.WSCREDENTIAL_PASSWORD });
 * 
 * if (hashtable != null) {
 * String userid = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_USERID);
 * String password = (String) hashtable.get(AttributeNameConstants.WSCREDENTIAL_PASSWORD);
 * 
 * if (password != null) {
 * invocationSubjectCacheKey = BasicAuthCacheKeyProvider.createLookupKey(subjectHelper.getRealm(invocationSubject), userid, password);
 * } else {
 * invocationSubjectCacheKey = BasicAuthCacheKeyProvider.createLookupKey(subjectHelper.getRealm(invocationSubject), userid);
 * }
 * }
 * }
 * }
 */
                }
            }
        }

        if (invocationSubjectCacheKey != null)
            fields.put(INVOCATION_SUBJECT_CACHE_KEY, invocationSubjectCacheKey);
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder(100)
                        .append(getClass().getSimpleName()).append('@').append(Integer.toHexString(hashCode())).append(' ')
                        .append(callerSubject == null ? null : callerSubject.getPrincipals()).append(' ')
                        .append(invocationSubject == null ? null : invocationSubject.getPrincipals());
        return sb.toString();
    }
}
