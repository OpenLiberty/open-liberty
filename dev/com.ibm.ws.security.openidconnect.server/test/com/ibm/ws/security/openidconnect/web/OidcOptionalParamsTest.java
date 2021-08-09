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
package com.ibm.ws.security.openidconnect.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.oauth20.OAuth20BadParameterFormatException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

import test.common.SharedOutputManager;

public class OidcOptionalParamsTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Before
    public void setUp() {
    }

    /*
     * test getlParameters method with no match.
     * expect result is empty AttributeList.
     */
    @Test
    public void testGetOptionalParametersNoMatch() {

        MockServletRequest req = new MockServletRequest();
        req.setParameter("somekey", "somevalue");

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }
        String[] retValue = al.getAttributeValuesByNameAndType(OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT, OAuth20Constants.ATTRTYPE_REQUEST);
        assertNull(retValue);
    }

    /*
     * test getlParameters method with nonce parameter.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersNonceParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "q1w2e3r4t";
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_NONCE;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }
        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(1, retValue.length);
        assertEquals(paramValue, retValue[0]);
    }

    /*
     * test getlParameters method with display parameter.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersDisplayParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = OIDCConstants.OIDC_AUTHZ_PARAM_DISPLAY_PAGE;
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_DISPLAY;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }

        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(1, retValue.length);
        assertEquals(paramValue, retValue[0]);
    }

    /*
     * test getlParameters method with max_age parameter.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersMaxAgeParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "123";
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_MAX_AGE;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }
        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(1, retValue.length);
        assertEquals(paramValue, retValue[0]);
    }

    /*
     * test getlParameters method with id_token_hint parameter.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersIdTokenHintParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "idtokenhint:adfadfadsewrwer213423";
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_ID_TOKEN_HINT;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }
        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(1, retValue.length);
        assertEquals(paramValue, retValue[0]);
    }

    /*
     * test getlParameters method with login_hint parameter.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersLoginHintParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "loginhint:adfadfadsewrwer213423";
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_LOGIN_HINT;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }
        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(1, retValue.length);
        assertEquals(paramValue, retValue[0]);
    }

    /*
     * test getlParameters method with multiple parameters of prompt value.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersPromptValidParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT_LOGIN + " " + OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT_CONSENT;
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT;
        req.setParameter(paramName, paramValue);
        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }

        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(2, retValue.length);
        int index1 = paramValue.indexOf(retValue[0]);
        int index2 = paramValue.indexOf(retValue[1]);
        assertTrue((index1 >= 0) && (index2 >= 0) && (index1 != index2));
    }

    /*
     * test getlParameters method with invalid prompt value maltiple parameter.
     * expect result is throwing exceptiont.
     */
    @Test
    public void testGetOptionalParametersPromptinvalidParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT_LOGIN + " " + OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT_NONE;
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_PROMPT;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        try {
            AttributeList al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            Locale locale = new Locale("en-us");
            String encoding = "utf-8";
            String formattedMessage = e.formatSelf(locale, encoding);
            assertEquals("The exception message must be formatted.",
                         "CWOAU0042E: The authorization request [prompt] parameter value: [login+none] is not valid because it has a value of 'none' in addition to other values.",
                         formattedMessage);
            return;
        }

        fail();
    }

    /*
     * test getlParameters method with multiple parameters of ui_locales value.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersUiLocalesParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "en_us ja_jp en_ca de"; //0+6+12+18 = 36 which is sum of offset.
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_UI_LOCALES;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }

        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(4, retValue.length);
        int index1 = paramValue.indexOf(retValue[0]);
        int index2 = paramValue.indexOf(retValue[1]);
        int index3 = paramValue.indexOf(retValue[2]);
        int index4 = paramValue.indexOf(retValue[3]);
        assertTrue((index1 + index2 + index3 + index4) == 36);
    }

    /*
     * test getlParameters method with multiple parameters of claims_locales value.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersClaimsLocalesParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "en_us ja_jp en_ca de"; //0+6+12+18 = 36 which is sum of offset.
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_CLAIMS_LOCALES;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }

        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(4, retValue.length);
        int index1 = paramValue.indexOf(retValue[0]);
        int index2 = paramValue.indexOf(retValue[1]);
        int index3 = paramValue.indexOf(retValue[2]);
        int index4 = paramValue.indexOf(retValue[3]);
        assertTrue((index1 + index2 + index3 + index4) == 36);
    }

    /*
     * test getlParameters method with multiple parameters of acr values.
     * expect result is valid AttributeList.
     */
    @Test
    public void testGetOptionalParametersClaimsAcrValueParam() {

        MockServletRequest req = new MockServletRequest();
        String paramValue = "1 2 basic_auth"; //0+2+4 = 6 which is sum of offset.
        String paramName = OIDCConstants.OIDC_AUTHZ_PARAM_ACR_VALUES;
        req.setParameter(paramName, paramValue);

        OidcOptionalParams oop = new OidcOptionalParams();
        AttributeList al = null;
        try {
            al = oop.getParameters(req);
        } catch (OAuth20BadParameterFormatException e) {
            fail();
        }

        String[] retValue = al.getAttributeValuesByNameAndType(paramName, OAuth20Constants.ATTRTYPE_REQUEST);
        assertEquals(3, retValue.length);
        int index1 = paramValue.indexOf(retValue[0]);
        int index2 = paramValue.indexOf(retValue[1]);
        int index3 = paramValue.indexOf(retValue[2]);
        assertTrue((index1 + index2 + index3) == 6);
    }
}