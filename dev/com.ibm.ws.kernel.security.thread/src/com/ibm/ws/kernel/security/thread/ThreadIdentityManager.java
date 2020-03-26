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
package com.ibm.ws.kernel.security.thread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.security.thread.ThreadIdentityService;

/**
 * Convenience class for setting the thread identity.
 */
public class ThreadIdentityManager {

    private static final TraceComponent tc = Tr.register(ThreadIdentityManager.class, "Security");
    private static final String thisClass = ThreadIdentityManager.class.getName();

    /**
     * ThreadLocal used to detect potential infinite recursion in the
     * tracing code.
     */
    private static final ThreadLocal<Boolean> recursionMarker = new ThreadLocal<Boolean>();

    /**
     * The ThreadIdentityService references. The references are set by the
     * ThreadIdentityManagerConfigurator.
     */
    private static final List<ThreadIdentityService> threadIdentityServices = new CopyOnWriteArrayList<>();

    /**
     * The J2CIdentityService references. The references are set by the ThreadIdentityManagerConfigurator.
     */
    private static final List<J2CIdentityService> j2cIdentityServices = new CopyOnWriteArrayList<>();

    private static final Object emptyToken = Collections.EMPTY_MAP;

    /**
     * Add a ThreadIdentityService reference. This method is called by
     * ThreadIdentityManagerConfigurator when a ThreadIdentityService shows
     * up in the OSGI framework.
     * 
     * @param tis
     */
    public static void addThreadIdentityService(ThreadIdentityService tis) {
        if (tis != null) {
            threadIdentityServices.add(tis);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "A ThreadIdentityService implementation was added.", tis.getClass().getName());
            }
        }
    }

    /**
     * Add a J2CIdentityService reference. This method is called by
     * ThreadIdentityManagerConfigurator when a J2CIdentityService shows
     * up in the OSGI framework.
     * 
     * @param j2cIdentityService
     */
    public static void addJ2CIdentityService(J2CIdentityService j2cIdentityService) {
        if (j2cIdentityService != null) {
            j2cIdentityServices.add(j2cIdentityService);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "A J2CIdentityService implementation was added.", j2cIdentityService.getClass().getName());
            }
        }
    }

    /**
     * Remove a ThreadIdentityService reference. This method is called by
     * ThreadIdentityManagerConfigurator when a ThreadIdentityService leaves
     * the OSGI framework.
     * 
     * @param tis
     */
    public static void removeThreadIdentityService(ThreadIdentityService tis) {
        if (tis != null) {
            threadIdentityServices.remove(tis);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "A ThreadIdentityService implementation was removed.", tis.getClass().getName());
            }
        }
    }

    /**
     * Remove a J2CIdentityService reference. This method is called by
     * ThreadIdentityManagerConfigurator when a J2CIdentityService leaves
     * the OSGI framework.
     * 
     * @param j2cIdentityService
     */
    public static void removeJ2CIdentityService(J2CIdentityService j2cIdentityService) {
        if (j2cIdentityService != null) {
            j2cIdentityServices.remove(j2cIdentityService);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "A J2CIdentityService implementation was removed.", j2cIdentityService.getClass().getName());
            }
        }
    }

    /**
     * Remove all the ThreadIdentityService references. This method is called by
     * ThreadIdentityManagerConfigurator when the ThreadIdentityService service tracker is closed.
     */
    public static void removeAllThreadIdentityServices() {
        threadIdentityServices.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "All the ThreadIdentityService implementations were removed.");
        }
    }

    /**
     * Remove all the J2CIdentityService references. This method is called by
     * ThreadIdentityManagerConfigurator when the J2CIdentityService service tracker is closed.
     */
    public static void removeAllJ2CIdentityServices() {
        j2cIdentityServices.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "All the J2CIdentityService implementations were removed.");
        }
    }

    /**
     * Returns true if application thread identity or J2C thread identity support
     * is enabled for any of the registered instances.
     * 
     * @return true if thread identity management is enabled; false otherwise.
     */
    public static boolean isThreadIdentityEnabled() {
        return isAppThreadIdentityEnabled() || isJ2CThreadIdentityEnabled();
    }

    /**
     * Returns true if application thread identity is enabled
     * for any of the registered ThreadIdentityService instances.
     * 
     * @return true if application thread identity is enabled; false otherwise.
     */
    public static boolean isAppThreadIdentityEnabled() {
        for (ThreadIdentityService tis : threadIdentityServices) {
            if (tis.isAppThreadIdentityEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if J2C thread identity is enabled
     * for any of the registered ThreadIdentityService instances.
     * 
     * @return true if J2C thread identity is enabled; false otherwise.
     */
    public static boolean isJ2CThreadIdentityEnabled() {
        for (J2CIdentityService j2cIdentityService : j2cIdentityServices) {
            if (j2cIdentityService.isJ2CThreadIdentityEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the subject's identity as the thread identity.
     * 
     * This method is used by J2C callers. It checks whether J2C thread
     * identity is enabled for the registered J2CIdentityService instance(s)
     * before setting the identity.
     * 
     * @return A token representing the identity previously on the thread.
     *         This token must be passed to the subsequent reset call.
     *         Returns null if thread identity support is disabled.
     * @throws ThreadIdentityException
     */
    public static Object setJ2CThreadIdentity(Subject subject) throws ThreadIdentityException {
        LinkedHashMap<J2CIdentityService, Object> token = null;
        for (J2CIdentityService j2cIdentityService : j2cIdentityServices) {
            if (j2cIdentityService.isJ2CThreadIdentityEnabled()) {
                try {
                    Object tokenReturnedFromSet = j2cIdentityService.set(subject);
                    if (tokenReturnedFromSet != null) {
                        if (token == null) {
                            token = new LinkedHashMap<J2CIdentityService, Object>();
                        }
                        token.put(j2cIdentityService, tokenReturnedFromSet);
                    }
                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, thisClass, "272");
                    resetCheckedInternal(token, e);
                }
            }
        }
        return token;
    }

    /**
     * Set the subject's identity as the thread identity.
     * 
     * This method is used by app containers. It checks whether app thread
     * identity is enabled for the registered ThreadIdentityService instance(s)
     * before setting the identity.
     * 
     * @return A token representing the identity previously on the thread.
     *         This token must be passed to the subsequent reset call.
     *         Returns null if thread identity support is disabled.
     * @throws ThreadIdentityException
     */
    public static Object setAppThreadIdentity(Subject subject) throws ThreadIdentityException {
        LinkedHashMap<ThreadIdentityService, Object> token = null;
        for (ThreadIdentityService tis : threadIdentityServices) {
            if (tis.isAppThreadIdentityEnabled()) {
                try {
                    Object tokenReturnedFromSet = tis.set(subject);
                    if (tokenReturnedFromSet != null) {
                        if (token == null) {
                            token = new LinkedHashMap<ThreadIdentityService, Object>();
                        }
                        token.put(tis, tokenReturnedFromSet);
                    }
                } catch (Exception e) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, thisClass, "251");
                    resetCheckedInternal(token, e);
                }
            }
        }
        return token;
    }

    /**
     * Check for recursion.
     * 
     * As a rule, ThreadIdentityManager should NEVER be re-entered. However, it can
     * be called from the Tr code (during log rollover, for example), so if the
     * ThreadIdentityService issues another Tr (or if ANY code called by ThreadIdentityService
     * issues a Tr), it will likely cause infinite recursion.
     * 
     * If we have NOT recursed, then this method will return false and set the recursionMarker.
     * Subsequent calls to this method on this thread will return true.
     * 
     * Once the recursion-safe block of code is finished, the recursionMarker must be reset
     * with a call to resetRecursionCheck.
     * 
     * TODO: despite the recursion safety, odd things may still happen in the logging code.
     * E.g, on log rollover, we may end up with an extra intermittent log between the
     * first call to runAsServer and the 2nd call (which NO-OPs due to the recursion check).
     * Not sure how to fix this oddness.
     * 
     * And for the record, I am not at all satisfied with this hack-ish solution, but at
     * the moment I lack any better ideas.
     * 
     * Note: Problems may occur if we decide to do log rollover whilst in the middle of
     * setting the thread identity. E.g, suppose we're running as unprivileged user
     * "rob", then we try to access the classloader. The classloader is wrapped
     * with a call to ThreadIdentityManager.runAsServer. We enter runAsServer, set
     * the recursion marker, then prior to setting the identity, we issue a trace
     * record which causes the logging code to rollover. The rollover tries to
     * issue a runAsServer, but that call is NO-OPed by the recursion check. So the
     * runAsServer in the rollover code never runs, and we try to create the rollover
     * log file using id "rob", which fails.
     * 
     * In general, bad things happen when log-rollover and ThreadIdentityManagement are
     * both enabled. I'm not sure if it's possible to avoid that badness, so we might
     * end up having to document the badness and/or prevent log rollover when
     * ThreadIdentityManagement is enabled.
     * 
     * @return false, if we have NOT recursed. The recursionMarker is set.
     *         true, if we have recursed.
     */
    private static boolean checkForRecursionAndSet() {
        if (recursionMarker.get() == null) {
            recursionMarker.set(Boolean.TRUE);
            return false;
        } else {
            return true; // recursion detected.
        }
    }

    /**
     * Reset the recursionMarker.
     */
    private static void resetRecursionCheck() {
        recursionMarker.remove();
    }

    /**
     * Set the server's identity as the thread identity.
     * 
     * @return A token representing the identity previously on the thread.
     *         This token must be passed to the subsequent reset call.
     */
    public static Object runAsServer() {
        LinkedHashMap<ThreadIdentityService, Object> token = null;

        if (!checkForRecursionAndSet()) {
            try {
                for (ThreadIdentityService tis : threadIdentityServices) {
                    if (tis.isAppThreadIdentityEnabled()) {
                        if (token == null) {
                            token = new LinkedHashMap<ThreadIdentityService, Object>();
                        }
                        token.put(tis, tis.runAsServer());
                    }
                }
            } finally {
                resetRecursionCheck();
            }
        }
        return token == null ? emptyToken : token;
    }

    /**
     * Reset the identity on the thread.
     * 
     * @param token The token returned by a previous call to set the thread identity.
     *            The token represents the previous identity on the thread.
     * @throws ThreadIdentityException
     */
    public static void resetChecked(Object token) throws ThreadIdentityException {
        resetCheckedInternal(token, null);
    }

    /**
     * Reset the identity on the thread.
     * 
     * @param token The token returned by a previous call to set the thread identity.
     *            The token represents the previous identity on the thread.
     * @param firstException This is the first exception that may have been thrown by set()
     * @throws ThreadIdentityException
     */
    @SuppressWarnings({ "rawtypes" })
    private static void resetCheckedInternal(Object token, Exception firstException) throws ThreadIdentityException {
        Exception cachedException = firstException;
        if (threadIdentityServices.isEmpty() == false || j2cIdentityServices.isEmpty() == false) {
            if (!checkForRecursionAndSet()) {
                try {
                    if (token != null) {
                        Map tokenMap = (Map) token;
                        int size = tokenMap.size();
                        if (size > 0) {
                            @SuppressWarnings("unchecked")
                            List<Map.Entry> identityServicesToTokensMap = new ArrayList<Map.Entry>(tokenMap.entrySet());

                            for (int idx = size - 1; idx >= 0; idx--) {
                                Map.Entry entry = identityServicesToTokensMap.get(idx);
                                Object identityService = entry.getKey();
                                Object identityServiceToken = entry.getValue();
                                try {
                                    if (identityService instanceof ThreadIdentityService) {
                                        ((ThreadIdentityService) identityService).reset(identityServiceToken);
                                    } else if (identityService instanceof J2CIdentityService) {
                                        ((J2CIdentityService) identityService).reset(identityServiceToken);
                                    }
                                } catch (Exception e) {
                                    com.ibm.ws.ffdc.FFDCFilter.processException(e, thisClass, "385");
                                    if (cachedException == null)
                                    {
                                        cachedException = e;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    resetRecursionCheck();
                    if (cachedException != null)
                    {
                        throw new ThreadIdentityException(cachedException);
                    }
                }
            }
        }
    }

    /**
     * Get a J2C subject based on the invocation subject.
     * Use first subject that is not null.
     */
    public static Subject getJ2CInvocationSubject() {
        Subject j2cSubject = null;
        for (J2CIdentityService j2cIdentityService : j2cIdentityServices) {
            if (j2cIdentityService.isJ2CThreadIdentityEnabled()) {
                Subject subject = j2cIdentityService.getJ2CInvocationSubject();
                if (subject != null) {
                    j2cSubject = subject;
                    break;
                }
            }
        }
        return j2cSubject;
    }

    /**
     * Reset the identity on the thread and
     * do not propagate any ThreadIdentityExcepion.
     * 
     * @see #resetChecked(Object)
     */
    public static void reset(Object token) {
        if (token != emptyToken && token != null) {
            try {
                resetCheckedInternal(token, null);
            } catch (ThreadIdentityException tie) {
            }
        }
    }
}
