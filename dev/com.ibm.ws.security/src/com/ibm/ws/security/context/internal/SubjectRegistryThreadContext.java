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
package com.ibm.ws.security.context.internal;

/**
 * The thread context that holds the registry information for a subject.
 * This is used to determine whether or not to create a SAF credential
 * after authenticating. The instances when a SAF credential should be
 * created are:
 * <ol>
 * <li>SAF registry is configured and subject is from the SAF registry</li>
 * <li>mapDistributedIdentities is true</li>
 * <li>Subject is from the OS registry (and not logging in)</li>
 * </ol>
 *
 * The subject's registry is determined by following the subject's login
 * path. In {@link UsernameAndPasswordLoginModule#login()}, start registry
 * detection in a try/catch (DETECT). In getUserSecurityName, the SAFRegistry or
 * SAFAuthorizedRegistry will be called if the user is from the SAF registry.
 * Set the thread context in those classes to IS_SAF.
 * <p>
 * When SAFCredentialsServiceImpl.setCredential is called, the thread context
 * is checked. If the value is DONT_DETECT, the subject is not logging in.
 * This is the case for OS registry users, and they should create a credential.
 * If the value is DETECT, the SAFRegistry classes were not accessed, meaning
 * the subject is not from the SAF registry. In this case, do not attempt to
 * create the credential. Lastly, IS_SAF means create the credential.
 */
public class SubjectRegistryThreadContext {

    /**
     * This is used to determine where we are in a user's login.
     * When login begins, we DETECT. Once we see we are in the
     * SAF registry, set IS_SAF or NOT_SAF. After login completes, DONT_DETECT.
     */
    private enum SAFDetectEnum {
        DONT_DETECT,
        DETECT,
        IS_SAF,
        NOT_SAF
    }

    /**
     * This is used to determine where we are in a user's login.
     * When login begins, we DETECT. Once we see we are in the
     * SAF registry, set IS_SAF. After login completes, DONT_DETECT.
     */
    private SAFDetectEnum SAFDetector = SAFDetectEnum.DONT_DETECT;

    /**
     * Set the SAFDetector when the registry is known.
     *
     * @param isSAF True if the user is from the SAF registry.
     */
    public void setIsSAF(boolean isSAF) {
        if (SAFDetector == SAFDetectEnum.DETECT) {
            if (isSAF) {
                SAFDetector = SAFDetectEnum.IS_SAF;
            } else {
                SAFDetector = SAFDetectEnum.NOT_SAF;
            }
        }
    }

    /**
     * Stop detection. This is called when login is complete or
     * there was an error.
     */
    public void donotdetect() {
        SAFDetector = SAFDetectEnum.DONT_DETECT;
    }

    /**
     * Start detection. This is called at the beginning of a login.
     * This means the subject is not the initial OS subject.
     */
    public void detect() {
        SAFDetector = SAFDetectEnum.DETECT;
    }

    /**
     * Determine if the credential should be created. The credential
     * should be created if detection has not started (DONT_DETECT)
     * or if we have a SAF user.
     *
     * @return True if the credential should be created and False if
     *         it should not.
     */
    public boolean isCreateSAFCredential() {
        return SAFDetector == SAFDetectEnum.DONT_DETECT || SAFDetector == SAFDetectEnum.IS_SAF;
    }

}
