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
package com.ibm.ws.security.credentials.saf.internal;

/**
 * SAFCredentialsServiceImpl implementation that mocks all native methods. This class is
 * used for testing SAFCredentials on non-z/OS platforms.
 *
 * This class extends SAFCredentialsServiceImpl and overrides only the native methods.
 * All native methods invoked against this object are forwarded to the mock
 * SAFCredentialsServiceTest object (SAFCredentialsServiceTest.scsmock).
 *
 */
public class SAFCredentialsServiceImplMockNative extends SAFCredentialsServiceImpl {

    @Override
    protected boolean ntv_isMixedCasePWEnabled() {
        return SAFCredentialsServiceTest.scsmock.ntv_isMixedCasePWEnabled();
    }

    @Override
    protected byte[] ntv_createPasswordCredential(byte[] userSecurityName,
                                                  byte[] password,
                                                  byte[] auditString,
                                                  byte[] applname,
                                                  byte[] safResult) {

        return SAFCredentialsServiceTest.scsmock.ntv_createPasswordCredential(userSecurityName,
                                                                              password,
                                                                              auditString,
                                                                              applname,
                                                                              safResult);
    }

    @Override
    protected int ntv_deleteCredential(byte[] safCredentialTokenBytes) {

        return SAFCredentialsServiceTest.scsmock.ntv_deleteCredential(safCredentialTokenBytes);
    }

    @Override
    protected byte[] ntv_createAssertedCredential(byte[] userSecurityName,
                                                  byte[] auditString,
                                                  byte[] apllname,
                                                  int msgSuppress,
                                                  byte[] safResult) {

        return SAFCredentialsServiceTest.scsmock.ntv_createAssertedCredential(userSecurityName,
                                                                              auditString,
                                                                              apllname,
                                                                              msgSuppress,
                                                                              safResult);
    }

    @Override
    protected byte[] ntv_createCertificateCredential(byte[] ecert,
                                                     int certLen,
                                                     byte[] auditString,
                                                     byte[] apllname,
                                                     byte[] outputUserName,
                                                     byte[] safResult) {

        return SAFCredentialsServiceTest.scsmock.ntv_createCertificateCredential(ecert,
                                                                                 certLen,
                                                                                 auditString,
                                                                                 apllname,
                                                                                 outputUserName,
                                                                                 safResult);
    }

    @Override
    protected void ntv_flushPenaltyBoxCache() {
        SAFCredentialsServiceTest.scsmock.ntv_flushPenaltyBoxCache();
    }

    @Override
    protected void ntv_setPenaltyBoxDefaults(boolean suppressAuthFailureMessages) {
        SAFCredentialsServiceTest.scsmock.ntv_setPenaltyBoxDefaults(suppressAuthFailureMessages);
    }

}
