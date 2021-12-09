/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.context;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.context.internal.SubjectRegistryThreadContext;

/**
 * The SubjectRegistryManager sets and gets caller/invocation subject information
 * off the thread and provides the ability to clear the subject registry info off the thread.
 * See {@link SubjectRegistryThreadContext} for more information.
 */
public class SubjectRegistryManager {

    private static ThreadLocal<SubjectRegistryThreadContext> threadLocal = new SecurityThreadLocal();

    /**
     * Gets the subject registry thread context that is unique per thread.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the subject registry thread context.
     */
    @Trivial
    private static SubjectRegistryThreadContext getSubjectRegistryThreadContext() {
        ThreadLocal<SubjectRegistryThreadContext> currentThreadLocal = getThreadLocal();
        SubjectRegistryThreadContext subjectRegistryThreadContext = currentThreadLocal.get();
        if (subjectRegistryThreadContext == null) {
            subjectRegistryThreadContext = new SubjectRegistryThreadContext();
            currentThreadLocal.set(subjectRegistryThreadContext);
        }
        return subjectRegistryThreadContext;
    }

    /**
     * Set whether or not the subject is from the SAF registry.
     *
     * @param isSAF True if the subject is from the SAF registry.
     */
    public static void setSubjectIsSAF(boolean isSAF) {
        if (!isZOS()) {
            return;
        }
        SubjectRegistryThreadContext subjectRegistryThreadContext = getSubjectRegistryThreadContext();
        subjectRegistryThreadContext.setIsSAF(isSAF);
    }

    /**
     * Start subject registry detection. This is only required if a SAF
     * registry is configured.
     *
     * @param isSAFRegistryConfigured
     */
    public static void startSubjectRegistryDetectionOnZOS() {
        if (!isZOS()) {
            return;
        }
        SubjectRegistryThreadContext subjectRegistryThreadContext = getSubjectRegistryThreadContext();
        subjectRegistryThreadContext.detect();
    }

    /**
     * Clear the subject registry detection. This is only required if a SAF
     * registry is configured.
     *
     * @param isSAFRegistryConfigured
     */
    public static void clearSubjectRegistryDetectionOnZOS() {
        if (!isZOS()) {
            return;
        }
        SubjectRegistryThreadContext subjectRegistryThreadContext = getSubjectRegistryThreadContext();
        subjectRegistryThreadContext.donotdetect();
    }

    /**
     * Determine if a SAF credential should be created.
     * The instances when a SAF credential should be
     * created are:
     * <ol>
     * <li>SAF registry is configured and subject is from the SAF registry</li>
     * <li>mapDistributedIdentities is true</li>
     * <li>Subject is from the OS registry (and not logging in)</li>
     * </ol>
     *
     * @return True if a SAF credential should be created and false if it should not
     */
    public static boolean isCreateSAFCredential() {
        if (!isZOS()) {
            return false;
        }
        SubjectRegistryThreadContext subjectRegistryThreadContext = getSubjectRegistryThreadContext();
        return subjectRegistryThreadContext.isCreateSAFCredential();
    }

    /**
     * Gets the thread local object.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the thread local object.
     */
    @Trivial
    private static ThreadLocal<SubjectRegistryThreadContext> getThreadLocal() {
        return threadLocal;
    }

    /**
     * Initialize the thread local object.
     */
    private static final class SecurityThreadLocal extends ThreadLocal<SubjectRegistryThreadContext> {
        @Override
        protected SubjectRegistryThreadContext initialValue() {
            return new SubjectRegistryThreadContext();
        }
    }

    /**
     * Check if this is z/OS
     *
     * @return true if on z/OS
     */
    private static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
    }
}
