/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.saf.internal;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.helper.AuthenticateUserHelper;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.authorization.saf.SAFRoleMapper;
import com.ibm.ws.security.delegation.DelegationProvider;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;

/**
 * SAF delegation provider.
 *
 * Used for EJB methods that have RunAs(role). The delegation provider returns the
 * Subject (user) associated with the given role for the given app.
 *
 * The mvsUserId associated with the given app/role is listed in the APPLDATA field
 * of the EJBROLE profile corresponding to the given app/role.
 *
 * This class first extracts the mvsUserid from the APPLDATA field, then runs
 * it thru the AuthenticationService (asserted - there's no password here) to build
 * a Subject around it.
 *
 */
public class SAFDelegationProvider implements DelegationProvider {

    /**
     * TraceComponent for issuing messages.
     */
    private static final TraceComponent tc = Tr.register(SAFDelegationProvider.class);

    /**
     * For loading native methods.
     */
    private NativeMethodManager nativeMethodManager;

    /**
     * For mapping appname/rolename to an EJBROLE profile name.
     */
    private SAFRoleMapper safRoleMapper;

    /**
     * For accessing the AuthenticationService
     */
    private SecurityService securityService;

    public String delegationUser = "";

    /**
     * DS inject
     */
    protected void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * DS inject. Also registers native methods.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        this.nativeMethodManager.registerNatives(SAFDelegationProvider.class);
    }

    /**
     * DS inject
     */
    protected void setSAFRoleMapper(SAFRoleMapper safRoleMapper) {
        this.safRoleMapper = safRoleMapper;
    }

    /**
     *
     * RACROUTE EXTRACT the APPLDATA from the EJBROLE profile corresponding to the
     * given roleName and appName. The APPLDATA contains the mvsUserId that is
     * mapped to this app/role. Run the mvsUserId thru the AuthenticationService
     * to create an ASSERTED credential and Subject.
     *
     * {@inheritDoc}
     *
     * @see com.ibm.ws.security.delegation.DelegationProvider#getRunAsSubject(java.lang.String, java.lang.String)
     */
    @Override
    public Subject getRunAsSubject(String roleName, String appName) throws AuthenticationException {

        String mvsUserId = getAppldata(safRoleMapper.getProfileFromRole(appName, roleName));
        delegationUser = mvsUserId;

        if (mvsUserId == null || mvsUserId.trim().isEmpty()) {
            return null;
        } else {
            AuthenticateUserHelper authHelper = new AuthenticateUserHelper();
            return authHelper.authenticateUser(securityService.getAuthenticationService(),
                                               mvsUserId,
                                               JaasLoginConfigConstants.SYSTEM_WEB_INBOUND);
        }
    }

    /**
     * @return the APPLDATA field from the given profile in the EJBROLE class
     */
    private String getAppldata(String profileName) throws AuthenticationException {

        SAFServiceResult safServiceResult = new SAFServiceResult();

        byte[] appldata = ntv_racrouteExtract(NativeMethodUtils.convertToEBCDIC("EJBROLE"),
                                              NativeMethodUtils.convertToEBCDIC(profileName),
                                              NativeMethodUtils.convertToEBCDIC("APPLDATA"),
                                              safServiceResult.getBytes());

        if (appldata == null) {

            // NLS-ify the ex msg.
            String exMsg = Tr.formatMessage(tc, "CANNOT_READ_APPLDATA", profileName, safServiceResult.getMessage());

            throw new AuthenticationException(exMsg, safServiceResult.getSAFException());
        }

        return NativeMethodUtils.convertToASCII(appldata);
    }

    /**
     * TODO: this could be moved to a common JNI class, shared by SAFRegistry.ntv_getRealm
     * (which also does a RACROUTE EXTRACT)
     *
     */
    protected native byte[] ntv_racrouteExtract(byte[] className,
                                                byte[] profileName,
                                                byte[] fieldName,
                                                byte[] safServiceResults);

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.delegation.DelegationProvider#getDelegationUser()
     */
    @Override
    public String getDelegationUser() {
        return delegationUser;
    }
}
