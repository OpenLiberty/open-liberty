/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import java.util.List;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.ValidationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;

/**
 * Common messaging/logging tool extensions for SAML
 */
//placeholder - added any SAML specific tooling here

public class SAMLMessageTools extends CommonMessageTools {

    private final Class<?> thisClass = SAMLMessageTools.class;
    public ValidationData vData = new ValidationData(SAMLConstants.ALL_TEST_ACTIONS);

    public List<validationData> addForbiddenExpectation(String action, List<validationData> expectations) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
            //    		expectations = vData.addSuccessStatusCodes(null, action);
            //    		expectations = vData.addResponseStatusExpectation(expectations, action, Constants.FORBIDDEN_STATUS)  ;
        }
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_FORBIDDEN);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.OK_MESSAGE);
        //        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.FORBIDDEN) ;
        return expectations;
    }

    public List<validationData> addrsSamlUnauthorizedExpectation(String action, List<validationData> expectations) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes(null, action);
            expectations = vData.addResponseStatusExpectation(expectations, action, Constants.UNAUTHORIZED_STATUS);
        }
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, null);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_HEADER, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_UNAUTHORIZED);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.UNAUTHORIZED_MESSAGE);
        //        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.FORBIDDEN) ;
        return expectations;
    }

    public List<validationData> addAuthenticationFailedExpectation(String action, List<validationData> expectations) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes(null, action);
            expectations = vData.addResponseStatusExpectation(expectations, action, Constants.FORBIDDEN_STATUS);
        }
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.AUTHENTICATION_FAILED);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.FORBIDDEN);
        return expectations;
    }

    public List<validationData> addNoSPExpectation(String action, List<validationData> expectations) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes(null, action);
            expectations = vData.addResponseStatusExpectation(expectations, action, Constants.OK_STATUS);
        }
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.HTTP_ERROR_FORBIDDEN);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLMessageConstants.CWWKS5004E_SP_NOT_CONFIGURED);
        return expectations;
    }

    public List<validationData> addNotFoundExpectation(String action, List<validationData> expectations) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes(null, action);
            expectations = vData.addResponseStatusExpectation(expectations, action, Constants.NOT_FOUND_STATUS);
        }
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.NOT_FOUND_ERROR);
        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not fail to have access.", null, SAMLConstants.NOT_FOUND_UPPERCASE);
        return expectations;
    }

    public String buildPublicCredString(String keyValue, String value) throws Exception {
        String identString = ".*" + SAMLConstants.PUBLIC_CREDENTIAL_STRING + ".*" + keyValue + "=" + value + ".*";
        Log.info(thisClass, "buildPublicCredString", identString);
        return identString;
    }

    public List<validationData> addIdentifierExpectations(String action, SAMLTestSettings settings, List<validationData> expectations, String user, String group, String userUnique, String realm) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }
        if (user.equals(SAMLConstants.SAML_TOKEN_ASSERTION)) {
            expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected userIdentier value", null, buildPublicCredString(SAMLConstants.SAML_USER_IDENTIFIER, settings.getIdpUserName()));
        } else {
            if (user.equals(SAMLConstants.NAME_ID)) {
                expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected userIdentier value", null, SAMLConstants.NOT_FOUND_ERROR);
            } else {
                if (user.equals(SAMLConstants.EXCEPTION)) {
                }
            }

        }
        if (group.equals(SAMLConstants.OMITTED)) {
            expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_DOES_NOT_CONTAIN, "Found groupIdentier and should NOT have", null, SAMLConstants.SAML_GROUP_IDENTIFIER + "=");
        } else {
            if (group.equals(SAMLConstants.SAML_TOKEN_ASSERTION)) {
                expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find groupIdentier value", null, buildPublicCredString(SAMLConstants.SAML_GROUP_IDENTIFIER, ".*" + SAMLConstants.TFIM_TEST_GROUP));
            } else {
                if (group.equals(SAMLConstants.LOCAL_REGISTRY)) {
                    expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected realmIdentier value", null, SAMLConstants.LOCAL_REGISTRY);// fix
                } else {
                    if (group.equals(SAMLConstants.EXCEPTION)) {
                    }
                }
            }
        }
        if (userUnique.equals(SAMLConstants.SAML_TOKEN_ASSERTION)) {
            expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected userUniqueIdentifier", null, buildPublicCredString(SAMLConstants.SAML_USERUNIQUE_IDENTIFIER, "user:" + settings.getIdpIssuer() + "/" + settings.getIdpUserName()));
        } else {
            if (userUnique.equals(SAMLConstants.TFIM_REGISTRY)) {
                expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected realmIdentier value", null, buildPublicCredString(SAMLConstants.SAML_USERUNIQUE_IDENTIFIER, "user:" + SAMLConstants.TFIM_REGISTRY_REALM));
            } else {
                if (userUnique.equals(SAMLConstants.LOCAL_REGISTRY)) {
                    expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected realmIdentier value", null, SAMLConstants.LOCAL_REGISTRY);// fix
                } else {
                    if (userUnique.equals(SAMLConstants.EXCEPTION)) {
                    }
                }
            }

        }
        if (realm.equals(SAMLConstants.SAML_TOKEN_ASSERTION)) {
            expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected realmIdentier value", null, buildPublicCredString(SAMLConstants.SAML_REALM_IDENTIFIER, SAMLConstants.TFIM_REGISTRY_REALM));
        } else {
            if (realm.equals(SAMLConstants.SAML_TOKEN_ISSUER)) {
                expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected realmIdentier value", null, buildPublicCredString(SAMLConstants.SAML_REALM_IDENTIFIER, settings.getIdpIssuer()));
            } else {
                if (realm.equals(SAMLConstants.LOCAL_REGISTRY)) {
                    expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not find expected realmIdentier value", null, SAMLConstants.LOCAL_REGISTRY);// fix
                } else {
                    if (user.equals(SAMLConstants.EXCEPTION)) {
                    }
                }

            }
        }

        return expectations;
    }

    public List<validationData> addAccessIdCheck(String action, SAMLTestSettings settings, List<validationData> expectations, String type) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }
        if (type.equals(SAMLConstants.SAML_TOKEN_ISSUER)) {
            expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected accessId", null, buildPublicCredString(SAMLConstants.SAML_ACCESS_ID, "user:" + settings.getIdpIssuer() + "/" + settings.getIdpUserName()));
        } else {
            if (type.equals(SAMLConstants.NAME_ID)) {
                expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected accessId value", null, buildPublicCredString(SAMLConstants.SAML_ACCESS_ID, "user:BasicRealm/testuser"));
            } else {
                if (type.equals(SAMLConstants.TFIM_REGISTRY)) {
                    //                    expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected accessId value", null, buildPublicCredString(SAMLConstants.SAML_ACCESS_ID, "user:" + SAMLConstants.TFIM_REGISTRY_REALM));
                    expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected accessId value", null, buildPublicCredString(SAMLConstants.SAML_ACCESS_ID, "user:.*" + "testuser"));
                } else {
                    if (type.equals(SAMLConstants.LOCAL_REGISTRY)) {
                        expectations = vData.addExpectation(expectations, action, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find expected accessId value", null, buildPublicCredString(SAMLConstants.SAML_ACCESS_ID, "user:" + SAMLConstants.LOCAL_REGISTRY_REALM + "/" + settings.getIdpUserName()));
                    } else {
                        if (type.equals(SAMLConstants.EXCEPTION)) {
                        }
                    }
                }
            }

        }

        return expectations;
    }

}
