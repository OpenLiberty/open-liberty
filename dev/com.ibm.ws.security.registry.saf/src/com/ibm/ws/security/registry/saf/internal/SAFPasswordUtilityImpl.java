/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.saf.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.audit.Audit;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.saf.SAFServiceResult;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.jni.NativeMethodUtils;
import com.ibm.wsspi.security.registry.saf.SAFPasswordChangeException;
import com.ibm.wsspi.security.registry.saf.SAFPasswordUtility;

/**
 *
 */
@Component(name = "com.ibm.ws.security.registry.passUtil",
           immediate = true,
           reference = { @Reference(name = "NativeServiceCHGPASS", service = com.ibm.ws.zos.core.NativeService.class,
                                    target = "(&(native.service.name=CHGPASS)(is.authorized=true))") },
           property = { "service.vendor=IBM" })
public class SAFPasswordUtilityImpl implements SAFPasswordUtility {

    private static final TraceComponent tc = Tr.register(SAFPasswordUtilityImpl.class);

    /**
     * The NativeMethodManager service for loading native methods.
     */
    private NativeMethodManager nativeMethodManager;

    private SAFDelegatingUserRegistry safDelegatingUserRegistry;

    private String applId;

    private boolean reportPasswordChangeDetails;

    /**
     * Indicates whether the SAF product supports mixed-case passwords.
     */
    private boolean isMixedCasePWEnabled = false;

    /**
     * Invoked by OSGi when service is activated.
     */
    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass());
        }
        updateConfig(cc, props);
    }

    /**
     * Invoked by OSGi when service is deactivated.
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
    }

    /**
     * Invoked by OSGi when the <safRegistry> configuration has changed.
     */
    @Modified
    protected void modify(ComponentContext cc, Map<String, Object> props) {
        updateConfig(cc, props);
    }

    @Reference(target = "(com.ibm.ws.security.registry.type=SAF)")
    protected void setSAFDelegatingUserRegistry(UserRegistry userRegistry) {
        if (userRegistry instanceof SAFDelegatingUserRegistry) {
            this.safDelegatingUserRegistry = (SAFDelegatingUserRegistry) userRegistry;
        }
    }

    protected void unsetSAFDelegatingUserRegistry(UserRegistry userRegistry) {
        this.safDelegatingUserRegistry = null;
    }

    @Reference
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
        this.nativeMethodManager.registerNatives(SAFPasswordUtilityImpl.class);

        isMixedCasePWEnabled = ntv_isMixedCasePasswordEnabled();
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * This method is called whenever the SAF authorization config is updated.
     */
    protected void updateConfig(ComponentContext cc, Map<String, Object> props) {
        // Refresh reportPasswordChangeDetails
        if (safDelegatingUserRegistry != null) {
            reportPasswordChangeDetails = safDelegatingUserRegistry.getReportPasswordChangeDetailsConfig();
        }
    }

    @Override
    public void passwordChange(String userid, @Sensitive String oldPassword, @Sensitive String newPassword) throws SAFPasswordChangeException, IllegalArgumentException {

        //Validate userName and Passwords are valid. Throws an illegalArgumentException if invalid
        validateParameters(userid, 8, "userId");
        validateParameters(oldPassword, 100, "currentPassword");
        validateParameters(newPassword, 100, "newPassword");

        //Normalize password if mixedCase is enabled
        String normalizedOldPassword = normalizePassword(oldPassword);
        String normalizedNewPassword = normalizePassword(newPassword);

        if (safDelegatingUserRegistry != null)
            applId = safDelegatingUserRegistry.getProfilePrefix();

        if (applId == null)
            throw new IllegalArgumentException("WLP z/OS System Security Access Domain (WZSSAD) could not be found");

        int rc = 0;
        SAFServiceResult safServiceResult = null;

        safServiceResult = new SAFServiceResult();
        rc = ntv_changePassword(safServiceResult.getBytes(),
                                NativeMethodUtils.convertToEBCDIC(applId),
                                NativeMethodUtils.convertToEBCDIC(userid.toString()),
                                NativeMethodUtils.convertToEBCDIC(normalizedOldPassword.toString()),
                                NativeMethodUtils.convertToEBCDIC(normalizedNewPassword.toString()));

        // Cut audit report for password change request if audit enabled
        Audit.audit(Audit.EventID.SECURITY_SAF_AUTHZ,
                    safServiceResult.getSAFReturnCode(),
                    safServiceResult.getRacfReturnCode(),
                    safServiceResult.getRacfReasonCode(),
                    userid,
                    null,
                    null,
                    (rc == 0) ? true : false, //set the outcome
                    null,
                    applId,
                    null,
                    null,
                    "changePassword");

        if (rc != 0) {
            if (reportPasswordChangeDetails) {
                // The reportPasswordChangeDetails flag defaults to false and is set in config by
                // <safAuthorization reportPasswordChangeDetails="true" />.
                // Create exception with details
                int safReturnCode = safServiceResult.getSAFReturnCode();
                int racfReturnCode = safServiceResult.getRacfReturnCode();
                int racfReasonCode = safServiceResult.getRacfReasonCode();

                throw new SAFPasswordChangeException(safReturnCode, racfReturnCode, racfReasonCode, userid, applId);
            } else {
                // Create blank exception, values initialize to default
                throw new SAFPasswordChangeException(userid, applId);
            }
        }

    }

    /**
     * Normalize the given password for the SAF service call.
     * If mixed-case PW is disabled and PW.length() <=8, UPPERCASE it.
     * Otheriwse, return it as is.
     * Note: If >8chars it is not a password and should be checked as a passphrase.
     *
     * @throws NullPointerException if password is null.
     */
    @Sensitive
    protected String normalizePassword(@Sensitive String password) {
        return (!isMixedCasePWEnabled && password.length() <= 8) ? password.toUpperCase() : password;
    }

    /**
     * This method checks if the given param
     *
     * @param param
     * @param length
     * @param value
     * @throws IllegalArgumentException
     */
    @Sensitive
    private void validateParameters(@Sensitive String param, int length, String value) throws IllegalArgumentException {
        if (param == null || param.length() < 1 || param.length() > length) {
            throw new IllegalArgumentException(value + " is not valid. It is either empty or exceeds the character limit.");
        }
    }

    /**
     * Native utility to let the user change their SAF password. Check whether or not the given
     * SAF credential has authority to access the given resource (SAF profile) under the given class
     * for the given applid. The required authority is indicated by accessLevel.
     *
     * The change password is made against the underlying SAF product, using native SAF
     * authorized services (RACROUTE REQUEST=VERIFY).
     *
     * Defined in security_saf_authz.c.
     *
     * @param safServiceResult Output parm where SAF return/reason codes are copied back to Java.
     * @param applId           The APPLNAME.
     * @param username
     * @param oldPassword
     * @param newPassword
     *
     * @return 0 if credential is authorized; otherwise a non-zero error code. See jsafServiceResult
     *         for SAF failure codes.
     */
    protected native int ntv_changePassword(byte[] safServiceResult,
                                            byte[] applId,
                                            byte[] username,
                                            byte[] oldPassword,
                                            byte[] newPassword);

    /**
     * Determine whether the SAF product supports mixed-case passwords.
     */
    protected native boolean ntv_isMixedCasePasswordEnabled();

}
