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
package com.ibm.ws.security.saml;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameIDType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.SecurityService;

public class Constants {
    @Trivial
    public static enum SamlSsoVersion {
        SAMLSSO20, SAMLSSO11
    };

    @Trivial
    public static enum EndpointType {
        SAMLMETADATA, ACS, REQUEST, RESPONSE, SLO, LOGOUT
    };

    @Trivial
    public static enum MapToUserRegistry {
        No, User, Group
    };

    @Trivial
    public static enum SignatureMethodAlgorithm {
        none, SHA256, SHA1
    }

    public final static String UTF8 = "UTF-8";

    public static final String ATTRIBUTE_SAML20_REQUEST = "Saml20Request";

    public final static String VERSION_SAML_SSO20 = "SAMLSso20";
    public final static String VERSION_SAML_SSO11 = "SAMLSso11";

    public final static String KEY_SAML_CONIFG = SsoConfig.class.getName();
    public final static String KEY_SAML_HANDLER = SsoHandler.class.getName();
    public final static String KEY_SAML_SERVICE = SsoSamlService.class.getName();
    public final static String KEY_SAML_REQUEST = SsoRequest.class.getName();
    public final static String KEY_SECURITY_SERVICE = SecurityService.class.getName();
    public final static String KEY_SECURITY_SUBJECT = "KEY_SUBJECT";

    public final static String RELAY_STATE = "RelayState";
    public static final String SAMLRequest = "SAMLRequest";
    public static final String SAMLResponse = "SAMLResponse";
    public static final String LogoutResponse = "LogoutResponse";
    public static final String LogoutRequest = "LogoutRequest";
    public static final String ArtifactResponse = "ArtifactResponse";

    public final static String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String COOKIE_NAME_WAS_SAML_ACS = "WASSamlACS_";
    public static final String COOKIE_NAME_SP_PREFIX = "WASSamlSP_";
    public static final String COOKIE_NAME_SAML_FRAGMENT = "WASSamlReq_";

    public static final String KEY_COOKIE_ID = "cId";
    public static final String KEY_COOKIE_PROVIDER_ID = "cProviderId";

    public static final String LOCAL_NAME_Assertion = "Assertion";
    public static final String LOCAL_NAME_Issuer = "Issuer";
    public static final String LOCAL_NAME_Signature = "Signature";
    public static final String LOCAL_NAME_Subject = "Subject";
    public static final String LOCAL_NAME_NameID = "NameID";
    public static final String LOCAL_NAME_SubjectConfirmation = "SubjectConfirmation";
    public static final String LOCAL_NAME_SubjectConfirmationData = "SubjectConfirmationData";
    public static final String LOCAL_NAME_Conditions = "Conditions";
    public static final String LOCAL_NAME_AudienceRestriction = "AudienceRestriction";
    public static final String LOCAL_NAME_Audience = "Audience";
    public static final String LOCAL_NAME_AuthnStatement = "AuthnStatement";
    public static final String LOCAL_NAME_AuthnContext = "AuthnContext";
    public static final String LOCAL_NAME_AuthnContextClassRef = "AuthnContextClassRef";
    public static final String LOCAL_NAME_AttributeStatement = "AttributeStatement";

    public static final String HTTP_ATTRIBUTE_COOKIE_SP = "com.ibm.ws.saml.cookie.sp";
    public static final String HTTP_ATTRIBUTE_SP_INITIATOR = "com.ibm.ws.saml.sp.initiator";
    public static final String SP_COOKIE_AND_SESSION_NOT_ON_OR_AFTER = "com.ibm.ws.saml.spcookie.session.not.on.or.after";
    public static final String SAML_SAMLEXCEPTION_FOUND = "com.ibm.ws.saml.samlexception.found";

    public static final String SAML20_CONTEXT_PATH = "/ibm/saml20/";

    public static final String SAML2_ARTIFACT_BINDING_URI = SAMLConstants.SAML2_ARTIFACT_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";
    public static final String SAML2_POST_BINDING_URI = SAMLConstants.SAML2_POST_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    public static final String SAML2_POST_SIMPLE_SIGN_BINDING_URI = SAMLConstants.SAML2_POST_SIMPLE_SIGN_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign";
    public static final String SAML2_REDIRECT_BINDING_URI = SAMLConstants.SAML2_REDIRECT_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
    public static final String SAML2_SOAP11_BINDING_URI = SAMLConstants.SAML2_SOAP11_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:SOAP";
    public static final String SAML2_PAOS_BINDING_URI = SAMLConstants.SAML2_PAOS_BINDING_URI;//"urn:oasis:names:tc:SAML:2.0:bindings:PAOS";
    public static final String SAML20P_NS = SAMLConstants.SAML20P_NS; //"urn:oasis:names:tc:SAML:2.0:protocol"

    public static final String SAML2_CONSENT_UNSPECIFIED_URI = "urn:oasis:names:tc:SAML:2.0:consent:unspecified";

    public static final String SP_INITAL = "sp_initial_";
    public static final String IDP_INITAL = "idp_initial_";
    public static final String DEFAULT_NAME_ID_FORMAT = NameIDType.EMAIL;
    public static final String DEFAULT_IDP_NAME_ID_FORMAT = "email";

    public static final String COOKIE_WAS_REQUEST = "SAML20UnsolicitedState";
    public static final String WAS_IR_COOKIE = "WASInitialRequest_";

    public static final String NAME_ID_FORMAT_UNSPECIFIED = NameIDType.UNSPECIFIED; // "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
    public static final String NAME_ID_FORMAT_EMAIL = NameIDType.EMAIL; // "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
    public static final String NAME_ID_FORMAT_X509_SUBJECT = NameIDType.X509_SUBJECT; // "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
    public static final String NAME_ID_FORMAT_WIN_DOMAIN_QUALIFIED = NameIDType.WIN_DOMAIN_QUALIFIED; // "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
    public static final String NAME_ID_FORMAT_KERBEROS = NameIDType.KERBEROS; // "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos";
    public static final String NAME_ID_FORMAT_ENTITY = NameIDType.ENTITY; // "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    public static final String NAME_ID_FORMAT_PERSISTENT = NameIDType.PERSISTENT; // "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
    public static final String NAME_ID_FORMAT_TRANSIENT = NameIDType.TRANSIENT; // "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
    public static final String NAME_ID_FORMAT_ENCRYPTED = NameIDType.ENCRYPTED; // "urn:oasis:names:tc:SAML:2.0:nameid-format:encrypted";

    public static final String NAME_ID_SHORT_UNSPECIFIED = "unspecified";
    public static final String NAME_ID_SHORT_EMAIL = "email";
    public static final String NAME_ID_SHORT_X509_SUBJECT = "x509SubjectName";
    public static final String NAME_ID_SHORT_WIN_DOMAIN_QUALIFIED = "windowsDomainQualifiedName";
    public static final String NAME_ID_SHORT_KERBEROS = "kerberos";
    public static final String NAME_ID_SHORT_ENTITY = "entity";
    public static final String NAME_ID_SHORT_PERSISTENT = "persistent";
    public static final String NAME_ID_SHORT_TRANSIENT = "transient";
    public static final String NAME_ID_SHORT_ENCRYPTED = "encrypted";

    public static final String NAME_ID_SHORT_CUSTOMIZE = "customize";

    // expired date is 1970 April
    public static final String STR_COOKIE_EXPIRED = " expires=Fri, 13-Apr-1970 00:00:00 GMT";
    public static final String STR_COOKIE_DO_NOT_EXPIRED = " expires=0;";

    public static final String DEFAULT_SP_ID = "defaultSP";

    public static final String DEFAULT_ERROR_MSG_JSP = "errorMsg.jsp";
    public static final String DEFAULT_403_ERR_MSG = "HTTP Error 403 - Forbidden";

    public static final String WLP_USER_DIR = "${wlp.user.dir}";

    public static final String ENGINE_TYPE_METADATA = "metadata";
    public static final String ENGINE_TYPE_PKIX = "pkix";

    public static final String TRUST_ALL_ISSUERS = "ALL_ISSUERS"; // trust all IdP if it's specified in the trustedIssuers

    public static final String ANY_AUDIENCE = "ANY";

    public static final String HDR_NAME_Authorization = "Authorization";

    public static final String REQUIRED = "required";
    public static final String NONE = "none";
    public static final String TRUE = "true";

    public static final String SLOINPROGRESS = "SLOInProgress";
    public static final String SP_INITIATED_SLO_IN_PROGRESS = "SpSLOInProgress";
    public static final String REDIRECT_SLO_TO_IDP = "RedirectSLOToIdP";
}
