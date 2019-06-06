/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.consumer.expectations;

import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.jwt.fat.consumer.JWTConsumerConstants;
import com.ibm.ws.security.jwt.fat.consumer.actions.JwtConsumerActions;

@SuppressWarnings("restriction")
public class JwtConsumerExpectation extends ResponseFullExpectation {

    public enum ValidationMsgType {
        SPECIFIC_CLAIM_API, CLAIM_LIST_MEMBER, CLAIM_FROM_LIST
    }

    /**
     * @param testAction
     * @param searchLocation
     * @param checkType
     * @param searchFor
     * @param failureMsg
     */
    public JwtConsumerExpectation(String errorId, String configId) {
        super(null, JWTConsumerConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + configId + ".+" + errorId, "Response did not show the expected " + errorId + " failure.");
    }

    public JwtConsumerExpectation(String checkType, String searchFor, String failureMsg) {
        super(checkType, searchFor, failureMsg);
    }

    public JwtConsumerExpectation(String key, Object value, ValidationMsgType claimType) {
        super(null, JWTConsumerConstants.STRING_MATCHES, null, "Response from test step " + JwtConsumerActions.ACTION_INVOKE_JWT_CONSUMER + " did not match expected value.");

        // based on what we're trying to check, override the validation value and the check type
        switch (claimType) {
        case SPECIFIC_CLAIM_API:
            checkType = JWTConsumerConstants.STRING_MATCHES;
            validationValue = buildClaimApiString(key, value);
            break;

        case CLAIM_LIST_MEMBER:
            if (value == null) {
                checkType = JWTConsumerConstants.STRING_DOES_NOT_CONTAIN;
            } else {
                checkType = JWTConsumerConstants.STRING_MATCHES;
            }
            validationValue = buildJsonClaimString(key, value);
            break;

        case CLAIM_FROM_LIST:
            if (value == null) {
                checkType = JWTConsumerConstants.STRING_DOES_NOT_CONTAIN;
            } else {
                checkType = JWTConsumerConstants.STRING_MATCHES;
            }
            validationValue = buildJsonAllClaimString(key, value);
            break;

        default:
            break;
        }
    }

    public String buildClaimApiString(String keyLogName, Object value) {

        //        Log.info(thisClass, "buildClaimApiString - generic", "value: " + value + " " + value.getClass());
        String builtString = JWTConsumerConstants.JWT_CONSUMER_CLAIM + keyLogName + ".*" + value;

        return builtString;
    }

    public String buildJsonClaimString(String key, Object value) {

        String builtString = "garbage";
        if (value == null) {
            builtString = key + ":";
        } else {
            String newValue = value.toString().replace("[", "").replace("]", "");
            builtString = JWTConsumerConstants.JWT_CONSUMER_CLAIM + JWTConsumerConstants.JWT_BUILDER_JSON + "\\{" + ".*" + key + ".*" + newValue + ".*\\}";
        }
        return builtString;
    }

    public String buildJsonAllClaimString(String key, Object value) {

        String builtString = JWTConsumerConstants.JWT_CONSUMER_CLAIM + JWTConsumerConstants.JWT_BUILDER_JSON + JWTConsumerConstants.JWT_BUILDER_GETALLCLAIMS + ".*" + key + ".*" + value;

        return builtString;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.fat.common.expectations.Expectation#validate(java.lang.Object)
     */
    @Override
    protected void validate(Object contentToValidate) throws Exception {
        super.validate(contentToValidate);

    }

    /*******************************************
     * Sample output from JWTConsumerClient
     **********************************************/
    /*
     * [05/20/2019 10:33:39:187 CDT] 001 CommonFatLoggingUtils NoOPMangleJWT1ServerTests_test I Response (Full):
     * ******************* Start of JwtConsumerClient output
     * *******************
     * Using configId: jwtConsumer
     * Successfully created consumer for id [jwtConsumer]
     * Token part[0]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Token part[1]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Token part[2]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Built JWT Token: eyJraWQiOiJrZXlpZCIsImFsZyI6IkhTMjU2In0.
     * eyJhdWQiOiJodHRwczovL2xvY2FsaG9zdDo4OTIwL2p3dGNvbnN1bWVyY2xpZW50L0p3dENvbnN1bWVyQ2xpZW50IiwiaXNzIjoiY2xpZW50MDEiLCJpYXQiOjE1NTgzNjY0MTcsImV4cCI6MTU1ODM2NjcxNywic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBqd3RDb25zdW1lciIsInN1YiI6InRlc3R1c2VyIiwicmVhbG1OYW1lIjoiQmFzaWNSZWFsbSIsInRva2VuX3R5cGUiOiJCZWFyZXIifQ
     * .RBpTMnA8xq0FPWLVXH5Km3hOAWIx4IdHtbKysCuQl0A
     * Header: JSON: {"kid":"keyid","alg":"HS256"}
     * Header: JSON Header: Key: kid Value: keyid
     * Header: JSON Header: Key: alg Value: HS256
     * JWT Consumer Claim: Issuer: client01
     * JWT Consumer Claim: Subject: testuser
     * JWT Consumer Claim: Audience: [https://localhost:8920/jwtconsumerclient/JwtConsumerClient]
     * JWT Consumer Claim: Expiration: 1558366717
     * JWT Consumer Claim: NotBefore: -1
     * JWT Consumer Claim: IssuedAt: 1558366417
     * JWT Consumer Claim: JwtId: null
     * JWT Consumer Claim: AuthorizedParty: null
     * JWT Consumer Claim: JSON:
     * {"sub":"testuser","realmName":"BasicRealm","iss":"client01","token_type":"Bearer","aud":
     * "https://localhost:8920/jwtconsumerclient/JwtConsumerClient",
     * "scope":"openid profile jwtConsumer","exp":1558366717,"iat":1558366417}
     * JWT Consumer Claim: JSON: getAllClaims: Key: sub Value: testuser
     * JWT Consumer Claim: JSON: getAllClaims: Key: realmName Value: BasicRealm
     * JWT Consumer Claim: JSON: getAllClaims: Key: iss Value: client01
     * JWT Consumer Claim: JSON: getAllClaims: Key: token_type Value: Bearer
     * JWT Consumer Claim: JSON: getAllClaims: Key: aud Value: https://localhost:8920/jwtconsumerclient/JwtConsumerClient
     * JWT Consumer Claim: JSON: getAllClaims: Key: scope Value: openid profile jwtConsumer
     * JWT Consumer Claim: JSON: getAllClaims: Key: exp Value: 1558366717
     * JWT Consumer Claim: JSON: getAllClaims: Key: iat Value: 1558366417
     ******************* End of JwtConsumerClient output *******************
     * [05/20/2019 10:33:39:188 CDT] 001 CommonFatLoggingUtils NoOPMangleJWT1ServerTests_test
     * I @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ End Response
     * Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
     * [05/20/2019 10:33:39:188 CDT] 001 JwtConsumerActions NoOPMangleJWT1ServerTests_test I JWT consumer app response:
     ******************* Start of JwtConsumerClient output *******************
     * Using configId: jwtConsumer
     * Successfully created consumer for id [jwtConsumer]
     * Token part[0]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Token part[1]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Token part[2]:
     * {"aud":"https://localhost:8920/jwtconsumerclient/JwtConsumerClient","iss":"client01","iat":1558366417,"exp":1558366717,
     * "scope":"openid profile jwtConsumer","sub":"testuser",
     * "realmName":"BasicRealm","token_type":"Bearer"}
     * Built JWT Token: eyJraWQiOiJrZXlpZCIsImFsZyI6IkhTMjU2In0.
     * eyJhdWQiOiJodHRwczovL2xvY2FsaG9zdDo4OTIwL2p3dGNvbnN1bWVyY2xpZW50L0p3dENvbnN1bWVyQ2xpZW50IiwiaXNzIjoiY2xpZW50MDEiLCJpYXQiOjE1NTgzNjY0MTcsImV4cCI6MTU1ODM2NjcxNywic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBqd3RDb25zdW1lciIsInN1YiI6InRlc3R1c2VyIiwicmVhbG1OYW1lIjoiQmFzaWNSZWFsbSIsInRva2VuX3R5cGUiOiJCZWFyZXIifQ
     * .RBpTMnA8xq0FPWLVXH5Km3hOAWIx4IdHtbKysCuQl0A
     * Header: JSON: {"kid":"keyid","alg":"HS256"}
     * Header: JSON Header: Key: kid Value: keyid
     * Header: JSON Header: Key: alg Value: HS256
     * JWT Consumer Claim: Issuer: client01
     * JWT Consumer Claim: Subject: testuser
     * JWT Consumer Claim: Audience: [https://localhost:8920/jwtconsumerclient/JwtConsumerClient]
     * JWT Consumer Claim: Expiration: 1558366717
     * JWT Consumer Claim: NotBefore: -1
     * JWT Consumer Claim: IssuedAt: 1558366417
     * JWT Consumer Claim: JwtId: null
     * JWT Consumer Claim: AuthorizedParty: null
     * JWT Consumer Claim: JSON:
     * {"sub":"testuser","realmName":"BasicRealm","iss":"client01","token_type":"Bearer","aud":
     * "https://localhost:8920/jwtconsumerclient/JwtConsumerClient",
     * "scope":"openid profile jwtConsumer","exp":1558366717,"iat":1558366417}
     * JWT Consumer Claim: JSON: getAllClaims: Key: sub Value: testuser
     * JWT Consumer Claim: JSON: getAllClaims: Key: realmName Value: BasicRealm
     * JWT Consumer Claim: JSON: getAllClaims: Key: iss Value: client01
     * JWT Consumer Claim: JSON: getAllClaims: Key: token_type Value: Bearer
     * JWT Consumer Claim: JSON: getAllClaims: Key: aud Value: https://localhost:8920/jwtconsumerclient/JwtConsumerClient
     * JWT Consumer Claim: JSON: getAllClaims: Key: scope Value: openid profile jwtConsumer
     * JWT Consumer Claim: JSON: getAllClaims: Key: exp Value: 1558366717
     * JWT Consumer Claim: JSON: getAllClaims: Key: iat Value: 1558366417
     ******************* End of JwtConsumerClient output *******************
     */
}
