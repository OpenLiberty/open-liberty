/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.backchannelLogout.fat.utils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;

/**
 * This class supplies support methods to the back channel logout tests.
 */

public class VariationSettings extends CommonTest {

    public static Class<?> thisClass = VariationSettings.class;
    public String loginMethod = Constants.OIDC;
    public String logoutMethodTested = Constants.END_SESSION;
    public String sessionLogoutEndpoint = null;
    public String reqLogoutServer = null; // req.logout can be called on the OP or the client
    public boolean isEndSessionEndpointInvoked = false;
    public boolean isLogoutEndpointInvoked = false; // do we need a slo logout flow flag?
    public boolean isRevokeEndpointInvoked = false;
    public boolean isRPReqLogoutInvoked = false;
    public boolean isOPReqLogoutInvoked = false;
    public boolean usesSAML = false;
    public boolean flowUsesBCL = false;
    public String finalAppWithPostRedirect = null;
    public String finalAppWithoutPostRedirect = null;

    /**
     * There are 3 main setups and each has 1-3 different ways that we can log out - this method will set some flags to tell the
     * test cases and supporting methods what type of config we have and the logout that we're expecting
     * 1) OIDC Provider with an openidconnect client
     * --- invoke end_session on the OP to log out
     * --- invoke a test app on the rp to invoke http logout then invoke end_session on the OP
     * --- invoke a test app on the rp to invoke http logout then invoke logout on the OP
     * 2) OIDC Provider with social client
     * --- invoke end_session on the OP to log out
     * --- invoke a test app on the social client to invoke http logout then invoke end_session on the OP
     * --- invoke a test app on the social client to invoke http logout then invoke logout on the OP
     * 3) OIDC Provider with SAML with an openidconnect client
     * --- invoke IDP logout on the IDP (Shibboleth server) to log out
     **/
    public VariationSettings(String currentRepeatAction) throws Exception {

        finalAppWithPostRedirect = Constants.postLogoutJSessionIdApp;
        sessionLogoutEndpoint = null;
        reqLogoutServer = null;

        Log.info(thisClass, "setConfigBasedOnRepeat", "Current repeat action: " + currentRepeatAction);

        if (currentRepeatAction.toUpperCase().contains(Constants.SAML)) {
            loginMethod = Constants.SAML;
            usesSAML = true;
        } else {
            if (currentRepeatAction.contains(SocialConstants.SOCIAL)) {
                loginMethod = SocialConstants.SOCIAL;
            } else {
                loginMethod = Constants.OIDC;
            }
        }
        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            if (currentRepeatAction.contains(Constants.OIDC_RP) || currentRepeatAction.contains(SocialConstants.SOCIAL)) {
                reqLogoutServer = Constants.OIDC_RP;
                isRPReqLogoutInvoked = true;
            } else {
                reqLogoutServer = Constants.OIDC__OP;
                isOPReqLogoutInvoked = true;
            }
            logoutMethodTested = Constants.HTTP_SESSION;
            if (currentRepeatAction.contains(Constants.END_SESSION)) {
                sessionLogoutEndpoint = Constants.END_SESSION;
                finalAppWithoutPostRedirect = Constants.defaultLogoutPage;
                finalAppWithPostRedirect = Constants.defaultLogoutPage;
                isEndSessionEndpointInvoked = true;
                flowUsesBCL = true;
                isOPReqLogoutInvoked = true;
            } else {
                if (currentRepeatAction.contains(Constants.LOGOUT_ENDPOINT)) {
                    if (currentRepeatAction.toUpperCase().contains(Constants.SAML)) {
                        finalAppWithoutPostRedirect = "/ibm/saml20/spOP/slo"; // all tests should be using the same provider spOP
                        isLogoutEndpointInvoked = true;
                    } else {
                        finalAppWithoutPostRedirect = Constants.LOGOUT_ENDPOINT;
                    }
                    sessionLogoutEndpoint = Constants.LOGOUT_ENDPOINT;
                    // override for configs that use post redrects - using the logout endpoint on the OP will NOT result in a call to them
                    finalAppWithPostRedirect = Constants.LOGOUT_ENDPOINT;
                    isLogoutEndpointInvoked = true;
                    flowUsesBCL = true;
                    isOPReqLogoutInvoked = true;
                } else {
                    finalAppWithPostRedirect = "/simpleLogoutTestApp/simpleLogout";
                    finalAppWithoutPostRedirect = "/simpleLogoutTestApp/simpleLogout";
                }
            }
        } else {
            //            if (currentRepeatAction.contains(Constants.SAML)) {
            //                //                logoutMethodTested = Constants.SAML;
            //                finalAppWithoutPostRedirect = Constants.samlLogoutPage;
            //            } // else {
            if (currentRepeatAction.contains(Constants.END_SESSION)) {
                logoutMethodTested = Constants.END_SESSION;
                finalAppWithoutPostRedirect = Constants.defaultLogoutPage;
                isEndSessionEndpointInvoked = true;
                flowUsesBCL = true;
                isOPReqLogoutInvoked = true;
            } else {
                if (currentRepeatAction.contains(Constants.REVOCATION_ENDPOINT)) {
                    logoutMethodTested = Constants.REVOCATION_ENDPOINT;
                    finalAppWithoutPostRedirect = Constants.defaultLogoutPage;
                    isRevokeEndpointInvoked = true;
                    flowUsesBCL = false;
                    isOPReqLogoutInvoked = false;
                } else {
                    if (currentRepeatAction.contains(Constants.SAML_IDP_INITIATED_LOGOUT)) {
                        logoutMethodTested = Constants.SAML_IDP_INITIATED_LOGOUT;
                        finalAppWithoutPostRedirect = Constants.samlLogoutPage;
                        isOPReqLogoutInvoked = true;
                        flowUsesBCL = true;
                    } else {
                        if (currentRepeatAction.contains(Constants.IBM_SECURITY_LOGOUT)) {
                            logoutMethodTested = Constants.IBM_SECURITY_LOGOUT;
                            isOPReqLogoutInvoked = true; 
                            finalAppWithoutPostRedirect = ".*/ibm_security_logout";
                            finalAppWithPostRedirect = finalAppWithoutPostRedirect;
                            flowUsesBCL = true;
                        } else {
                            logoutMethodTested = Constants.LOGOUT_ENDPOINT;
                            flowUsesBCL = true;
                            isOPReqLogoutInvoked = true;
                            if (currentRepeatAction.toUpperCase().contains(Constants.SAML)) {
                                finalAppWithoutPostRedirect = "/ibm/saml20/spOP/slo"; // all tests should be using the same provider spOP
                                isLogoutEndpointInvoked = true;
                            } else {
                                finalAppWithoutPostRedirect = "/oidc/endpoint/.*/logout";
                                // logout doesn't redirect to the post logout uri
                                finalAppWithPostRedirect = finalAppWithoutPostRedirect;
                            }
                        }
                    }
                }
            }
        }

        printVariationSettings();

    }

    public void printVariationSettings() {

        Log.info(thisClass, "printVariationSettings", "loginMethod: " + loginMethod);
        Log.info(thisClass, "printVariationSettings", "logoutMethodTested: " + logoutMethodTested);
        Log.info(thisClass, "printVariationSettings", "sessionLogoutEndpoint: " + sessionLogoutEndpoint);
        Log.info(thisClass, "printVariationSettings", "reqLogoutServer: " + reqLogoutServer);
        Log.info(thisClass, "printVariationSettings", "usesSAML: " + Boolean.toString(usesSAML));
        Log.info(thisClass, "printVariationSettings", "finalAppWithPostRedirect: " + finalAppWithPostRedirect);
        Log.info(thisClass, "printVariationSettings", "finalAppWithoutPostRedirect: " + finalAppWithoutPostRedirect);
        Log.info(thisClass, "printVariationSettings", "isEndSessionEndpointInvoked: " + Boolean.toString(isEndSessionEndpointInvoked));
        Log.info(thisClass, "printVariationSettings", "isLogoutEndpointInvoked: " + Boolean.toString(isLogoutEndpointInvoked));
        Log.info(thisClass, "printVariationSettings", "isRevokeEndpointInvoked: " + Boolean.toString(isRevokeEndpointInvoked));
        Log.info(thisClass, "printVariationSettings", "isRPReqLogoutInvoked: " + Boolean.toString(isRPReqLogoutInvoked));
        Log.info(thisClass, "printVariationSettings", "isOPReqLogoutInvoked: " + Boolean.toString(isOPReqLogoutInvoked));
        Log.info(thisClass, "printVariationSettings", "flowUsesBCL: " + Boolean.toString(flowUsesBCL));

    }
}
