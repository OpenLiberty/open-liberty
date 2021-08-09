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

package com.ibm.websphere.security.auth;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.kernel.security.thread.ThreadIdentityException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * <p>
 * The <code>WSSubject</code> class is provided to workaround a design oversight in
 * Java 2 Security. When integrating JAAS doAs with Java 2 Security doPrivileged
 * Programming Model, the doPrivileged call did not propagate the Subject object.
 * This workaround provides doAs and doAsPrivileged static functions in which the
 * Subject is set as the invocation subject. Then WSSubject.doAS and doAsPrivileged
 * methods will invoke the corresponding Subject.doAs and doAsPrivilged methods.
 * This workaround can provide the desired remote EJB doAs invocation behavior.
 * The workaround can function correctly regardless whether the actual fix get
 * into JDK 1.3.1 or not.
 * </p>
 * 
 * <p>
 * Please note, <code>null</code> Subject or the Subject does not contain an instance of
 * <code>com.ibm.websphere.security.cred.WSCredential</code> in the public credential set
 * of the Subject then an Unauthenticated credential is set as the Invocation
 * credential in the <code>doAs{Privileged}()</code> methods call.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 8.5
 * @since WAS 5.0
 * @ibm-api
 */
public final class WSSubject {

    private static final TraceComponent tc =
                    Tr.register(WSSubject.class, null, null);

    private static final AuthPermission DOAS_PERM = new AuthPermission("doAs");
    private static final AuthPermission DOASPRIVILEGED_PERM = new AuthPermission("doAsPrivileged");
    private static final AuthPermission GETCALLERSUBJECT_PERM = new AuthPermission("wssecurity.getCallerSubject");
    private static final AuthPermission GETRUNASSUBJECT_PERM = new AuthPermission("wssecurity.getRunAsSubject");
    private static final AuthPermission SETRUNASSUBJECT_PERM = new AuthPermission("wssecurity.setRunAsSubject");

    private final static AtomicServiceReference<SubjectManagerService> smServiceRef =
                    new AtomicServiceReference<SubjectManagerService>(SubjectManagerService.KEY_SUBJECT_MANAGER_SERVICE);

    protected void setSubjectManagerService(ServiceReference<SubjectManagerService> reference) {
        smServiceRef.setReference(reference);
    }

    protected void unsetSubjectManagerService(ServiceReference<SubjectManagerService> reference) {
        smServiceRef.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        smServiceRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        smServiceRef.deactivate(cc);
    }

    public static Object doAs(Subject subject, java.security.PrivilegedAction action) {
        return doAs(subject, action, false);
    }

    /**
     * <p><code>doAs</code> wraps the Subject.doAs to provide the correct
     * inter-EJB invocation behavior.
     * </p>
     * 
     * <p>
     * Please note, if a <code>null</code> Subject is passed in or the Subject does not contain an
     * instance of <code>com.ibm.websphere.security.cred.WSCredential</code> then an Unauthenticated
     * subject is set as the Invocation subject.
     * If setCaller argument is true, the "caller" subject is set with the given subject argument.
     * </p>
     * 
     * @return An <code>java.lang.Object</code>.
     * @exception SecurityException
     *                Thrown if there is no doAs and other required permissions.
     */
    public static Object doAs(Subject subject, java.security.PrivilegedAction action, boolean setCaller) {
        SecurityManager securitymanager = System.getSecurityManager();
        if (securitymanager != null) {
            securitymanager.checkPermission(DOAS_PERM);
        }
        if (action == null) {
            throw new IllegalArgumentException("null PrivilegedAction provided");
        }
        Subject callerSubject = null;
        SubjectCookie invocationCookie = setInvocationSubject(subject);
        if (setCaller) {
            callerSubject = setCallerSubject(subject);
        }
        try {
            return Subject.doAs(subject, action);
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "WSSubject.doAs(Subject, PrivilegedAction) Exception caught: " + t);
            throw new RuntimeException(t); //$A13
        } finally {
            restoreInvocationSubject(invocationCookie);
            if (setCaller) {
                restoreCallerSubject(callerSubject);
            }
        }
    }

    public static Object doAs(Subject subject, java.security.PrivilegedExceptionAction action)
                    throws java.security.PrivilegedActionException {
        return doAs(subject, action, false);
    }

    /**
     * <p><code>doAs</code> wraps the Subject.doAs to provide the correct
     * inter-EJB invocation behavior.
     * </p>
     * 
     * <p>
     * Please note, if a <code>null</code> Subject is passed in or the Subject does not contain an
     * instance of <code>com.ibm.websphere.security.cred.WSCredential</code> then an Unauthenticated
     * subject is set as the Invocation subject.
     * If setCaller argument is true, the "caller" subject is set with the given subject argument.
     * </p>
     * 
     * @return An <code>java.lang.Object</code>.
     * @exception SecurityException
     *                Thrown if there is no doAs and other required permissions.
     */
    public static Object doAs(Subject subject, java.security.PrivilegedExceptionAction action, boolean setCaller)
                    throws java.security.PrivilegedActionException {
        SecurityManager securitymanager = System.getSecurityManager();
        if (securitymanager != null) {
            securitymanager.checkPermission(DOAS_PERM);
        }
        if (action == null) {
            throw new IllegalArgumentException("null PrivilegedExceptionAction provided");
        }
        Subject callerSubject = null;
        SubjectCookie invocationCookie = setInvocationSubject(subject);
        if (setCaller) {
            callerSubject = setCallerSubject(subject);
        }
        try {
            return Subject.doAs(subject, action);
        } catch (PrivilegedActionException pae) {
            throw pae;
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "WSSubject.doAs(Subject, PrivilegedExceptionAction) Exception caught: " + t);
            throw new RuntimeException(t); //$A13
        } finally {
            restoreInvocationSubject(invocationCookie);
            if (setCaller) {
                restoreCallerSubject(callerSubject);
            }
        }
    }

    public static Object doAsPrivileged(Subject subject,
                                        java.security.PrivilegedAction action,
                                        java.security.AccessControlContext acc) {

        return doAsPrivileged(subject, action, acc, false);

    }

    /**
     * <p><code>doAsPrivileged</code> wraps the Subject.doAsPrivileged to provide the correct
     * inter-EJB invocation behavior.
     * </p>
     * 
     * <p>
     * Please note, if a <code>null</code> Subject is passed in or the Subject does not contain an
     * instance of <code>com.ibm.websphere.security.cred.WSCredential</code> then an Unauthenticated
     * subject is set as the Invocation subject.
     * If setCaller argument is true, the "caller" subject is set with the given subject argument.
     * </p>
     * 
     * @return An <code>java.lang.Object</code>.
     * @exception SecurityException
     *                Thrown if there is no doAs and other required permissions.
     */
    public static Object doAsPrivileged(Subject subject,
                                        java.security.PrivilegedAction action,
                                        java.security.AccessControlContext acc,
                                        boolean setCaller) {
        SecurityManager securitymanager = System.getSecurityManager();
        if (securitymanager != null) {
            securitymanager.checkPermission(DOASPRIVILEGED_PERM);
        }
        if (action == null) {
            throw new IllegalArgumentException("null PrivilegedAction provided");
        }
        Subject callerSubject = null;
        SubjectCookie invocationCookie = setInvocationSubject(subject);
        if (setCaller) {
            callerSubject = setCallerSubject(subject);
        }
        try {
            return Subject.doAsPrivileged(subject, action, acc);
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "WSSubject.doAsPrivileged(Subject, PrivilegedAction, AccessControlContext) Exception caught: " + t);
            throw new RuntimeException(t); //$A13
        } finally {
            restoreInvocationSubject(invocationCookie);
            if (setCaller) {
                restoreCallerSubject(callerSubject);
            }
        }
    }

    public static Object doAsPrivileged(Subject subject,
                                        java.security.PrivilegedExceptionAction action,
                                        java.security.AccessControlContext acc)
                    throws java.security.PrivilegedActionException {

        return doAsPrivileged(subject, action, acc, false);
    }

    /**
     * <p><code>doAsPrivileged</code> wraps the Subject.doAsPrivileged to provide the correct
     * inter-EJB invocation behavior.
     * </p>
     * 
     * <p>
     * Please note, if a <code>null</code> Subject is passed in or the Subject does not contains a
     * <code>com.ibm.websphere.security.cred.WSCredential</code> then an Unauthenticated subject
     * is set as the Invocation subject with the action.
     * If setCaller argument is true, the "caller" subject is set with the given subject argument.
     * </p>
     * 
     * @return An <code>java.lang.Object</code>.
     * @exception SecurityException
     *                Thrown if there is no doAs and other required permissions.
     */
    public static Object doAsPrivileged(Subject subject,
                                        java.security.PrivilegedExceptionAction action,
                                        java.security.AccessControlContext acc,
                                        boolean setCaller)
                    throws java.security.PrivilegedActionException {
        SecurityManager securitymanager = System.getSecurityManager();
        if (securitymanager != null) {
            securitymanager.checkPermission(DOASPRIVILEGED_PERM);
        }
        if (action == null) {
            throw new IllegalArgumentException("null PrivilegedExceptionAction provided");
        }
        Subject callerSubject = null;
        SubjectCookie invocationCookie = setInvocationSubject(subject);
        if (setCaller) {
            callerSubject = setCallerSubject(subject);
        }
        try {
            return Subject.doAsPrivileged(subject, action, acc);
        } catch (PrivilegedActionException pae) {
            throw pae;
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "WSSubject.doAsPrivileged(Subject, PrivilegedExceptionAction, AccessControlContext) Exception caught: " + t);
            throw new RuntimeException(t); //$A13
        } finally {
            restoreInvocationSubject(invocationCookie);
            if (setCaller) {
                restoreCallerSubject(callerSubject);
            }
        }
    }

    /**
     * <p>
     * This method returns a Subject contains the principal of the J2EE caller and the
     * J2EE caller credential. If there is no caller credential, a <code>null</code>
     * is returned.
     * </p>
     * 
     * <p>
     * If there is a caller credential in the current thread, it creates a new Subject
     * that contains a <code>com.ibm.websphere.security.auth.WSPrincipal</code> and a
     * <code>com.ibm.websphere.security.cred.WSCredential</code>.
     * </p>
     * 
     * <p>
     * This method is protected by Java 2 Security. If Java 2 Security is enabled, then
     * access will be denied if the application code is not granted the permission
     * <code>javax.security.auth.AuthPermission("wssecurity.getCallerSubject")</code>.
     * </p>
     * 
     * <p>
     * This is a <b>server</b> side call, i.e., should only be used by application code
     * running in an application server. If this method is called by the client
     * (application client or thin client), it returns <code>null</code>.
     * </p>
     * 
     * @return Subject contains the caller identity, <code>null</code> if there is no caller identity
     *         and if called by application client or thin client code.
     * @exception WSSecurityException
     *                failed to get the caller identity
     * @see com.ibm.websphere.security.auth.WSPrincipal
     * @see com.ibm.websphere.security.cred.WSCredential
     */
    @SuppressWarnings("unchecked")
    public static Subject getCallerSubject()
                    throws WSSecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GETCALLERSUBJECT_PERM);
        }
        Subject s = null;

        try {
            s = (Subject) java.security.AccessController.doPrivileged(getCallerSubjectAction);
        } catch (PrivilegedActionException pae) {
            WSSecurityException e = (WSSecurityException) pae.getException();
            throw e;
        }

        return s;
    }

    @SuppressWarnings("rawtypes")
    private static final PrivilegedExceptionAction getCallerSubjectAction = new PrivilegedExceptionAction() {
        @Override
        public Object run() throws WSSecurityException {
            SubjectManagerService sms = smServiceRef.getService();
            Subject s = null;
            if (sms != null) {
                s = sms.getCallerSubject();
                if (s != null)
                    s.setReadOnly();

            }
            return s;
        }
    };

    /**
     * <p>
     * This method returns a Subject contains the principal of the J2EE run as identity
     * and the J2EE run as credential. If there is no run as credential, a <code>null</code>
     * is returned.
     * </p>
     * 
     * <p>
     * If there is a run as credential in the current thread, it creates a new Subject
     * that contains a <code>com.ibm.websphere.security.auth.WSPrincipal</code> and a
     * <code>com.ibm.websphere.security.cred.WSCredential</code>.
     * </p>
     * 
     * <p>
     * This method is protected by Java 2 Security. If Java 2 Security is enabled, then
     * access will be denied if the application code is not granted the permission
     * <code>javax.security.auth.AuthPermission("wssecurity.getRunAsSubject")</code>.
     * </p>
     * 
     * @return Subject contains the run as identity, <code>null</code> if there is no run as identity
     *         and if called by application client or thin client code.
     * @exception WSSecurityException
     *                failed to get the run as identity
     * @see com.ibm.websphere.security.auth.WSPrincipal
     * @see com.ibm.websphere.security.cred.WSCredential
     */
    @SuppressWarnings("unchecked")
    public static Subject getRunAsSubject()
                    throws WSSecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GETRUNASSUBJECT_PERM);
        }
        Subject s = null;

        try {
            s = (Subject) java.security.AccessController.doPrivileged(getRunAsSubjectAction);
        } catch (PrivilegedActionException pae) {
            WSSecurityException e = (WSSecurityException) pae.getException();
            throw e;
        }

        return s;
    }

    @SuppressWarnings({ "rawtypes" })
    private static final PrivilegedExceptionAction getRunAsSubjectAction = new PrivilegedExceptionAction() {
        @Override
        public Object run() throws WSSecurityException {
            SubjectManagerService sms = smServiceRef.getService();
            Subject s = null;
            if (sms != null) {
                s = sms.getInvocationSubject();
                if (s != null)
                    s.setReadOnly();

            }
            return s;
        }
    };

    /**
     * <p>
     * This method set the Subject as the J2EE run as identity on the current execution thread.
     * </p>
     * 
     * <p>
     * This method is protected by Java 2 Security. If Java 2 Security is enabled, then
     * access will be denied if the application code is not granted the permission
     * <code>javax.security.auth.AuthPermission("wssecurity.setRunAsSubject")</code>.
     * </p>
     * 
     * @no return value.
     * @exception WSSecurityException
     *                failed to set the run as identity
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setRunAsSubject(final Subject subject)
                    throws WSSecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SETRUNASSUBJECT_PERM);
        }
        try {
            java.security.AccessController.doPrivileged(new PrivilegedExceptionAction() {
                @Override
                public Object run() throws WSSecurityException {
                    SubjectManagerService sms = smServiceRef.getService();
                    if (sms != null) {
                        sms.setInvocationSubject(subject);
                    }
                    return null;

                }
            });

        } catch (PrivilegedActionException pae) {
            WSSecurityException e = (WSSecurityException) pae.getException();
            throw e;
        }
    }

    /**
     * <p>
     * This convenient method returns the caller principal of the
     * current executing thread.
     * </p>
     * 
     * <p>
     * It will extract the caller from the received credentials of
     * the current thread. If the received credentials is null, then
     * a value of null is returned. In the EJB and Web container,
     * user should use the standard interface provided by the J2EE
     * specification to get the caller principal or caller name. This
     * method call provides a way for code executing outside the
     * containers to get the caller principal. The principal name
     * return is not qualified with the security realm name.
     * </p>
     * 
     * @return The principal name (without the security realm). If the
     *         received credential is null, then the value of null will
     *         be returned as the caller principal.
     */
    public static String getCallerPrincipal() {
        String caller = null;
        SubjectManagerService sms = smServiceRef.getService();
        if (sms != null) {
            Subject subject = sms.getCallerSubject();
            if (subject != null) {

                WSCredential wsCred = getWSCredential(subject);

                if (wsCred != null && !wsCred.isUnauthenticated()) {
                    try {
                        caller = wsCred.getSecurityName();
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Internal error: " + e);
                    }
                }
            }
        }
        return caller;
    }

    /**
     * <p>
     * This convenient method returns the SAF user id that is in the passed-in
     * Subject. The SAF user id is extracted from the PlatformCredential object,
     * which is referenced by the WSCredential of the subject. If the WSCredential
     * does not have any reference to a PlatformCredential in order to identify the SAF
     * user id, then return <code>null</code>.
     * </p>
     * 
     * @param subject the Subject to extract the SAF user id from
     * @return the SAF user id
     */
    public static String getSAFUserFromSubject(Subject subject) {
        String safUser = null;
        return safUser;
    }

    /**
     * <p>
     * This convenient method returns the root login exception caught in the system
     * login module, if one exists.
     * </p>
     * 
     * <p>
     * It will extract the exception from the current thread. You will get what
     * the login module sees as the root exception. This could be a nested exception.
     * You may need to extract exceptions from the exception returned until you
     * get the real root exception.
     * </p>
     * 
     * @return A Throwable containing the root login exception. If a login
     *         exception did not occur, null will be returned.
     */
    public static Throwable getRootLoginException() {
        return null;
    }

    /**
     * Set the invocation subject
     */
    private static SubjectCookie setInvocationSubject(Subject s) {
        final SubjectCookie cookie = new SubjectCookie();
        SubjectManagerService sms = smServiceRef.getService();
        if (sms != null) {
            if (s == null) {
                s = new Subject();
            }

            Subject currentInvocationSubject = sms.getInvocationSubject();
            sms.setInvocationSubject(s);

            cookie.subject = currentInvocationSubject;

            // If SYNC-TO-OS-THREAD support is enabled for the server and configured for this app,
            // then sync the Subject's identity to the thread.  The returned token is handed back
            // to the ThreadIdentityManager when we restore of the previous invocation subject
            // (under restoreInvocationSubject).
            try {
                cookie.token = ThreadIdentityManager.setAppThreadIdentity(s);
            } catch (ThreadIdentityException e) {
                throw new SecurityException(e);
            }
        }
        return cookie;
    }

    /**
     * Set the invocation subject
     */
    private static Subject setCallerSubject(Subject s) {
        Subject currentCallerSubject = null;
        SubjectManagerService sms = smServiceRef.getService();
        if (sms != null) {
            if (s == null) {
                s = new Subject();
            }
            currentCallerSubject = sms.getCallerSubject();
            sms.setCallerSubject(s);
        }
        return currentCallerSubject;
    }

    /**
     * Restore the invocation subject.
     */
    private static void restoreInvocationSubject(SubjectCookie cookie) {
        try {
            if (cookie.token != null) {
                // We sync'ed the subject's identity to the thread under setInvocationSubject. 
                // Now restore the previous identity using the token returned when we sync'ed.
                ThreadIdentityManager.resetChecked(cookie.token);
            }
        } catch (ThreadIdentityException e) {
            throw new SecurityException(e);
        } finally {
            SubjectManagerService sms = smServiceRef.getService();
            if (sms != null) {
                sms.setInvocationSubject(cookie.subject);
            }
        }
    }

    private static void restoreCallerSubject(Subject s) {
        SubjectManagerService sms = smServiceRef.getService();
        if (sms != null) {
            sms.setCallerSubject(s);
        }
    }

    private static WSCredential getWSCredential(Subject subject) {
        WSCredential wsCredential = null;
        Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
        Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
        if (wsCredentialsIterator.hasNext()) {
            wsCredential = wsCredentialsIterator.next();
        }
        return wsCredential;
    }

    /**
     * Container for the Subject and credential token that were
     * pushed before the doAs and need to be popped.
     */
    private final static class SubjectCookie {
        boolean prevSyncedState = false;
        Subject subject = null;
        Object token = null;

        SubjectCookie() {}

        @Override
        public String toString() {
            return super.toString() +
                   ";prevSyncedState=" + prevSyncedState +
                   ",token=" + token +
                   ",subject=" + subject;
        }
    }

    // Static class
    //private WSSubject() {}
}
