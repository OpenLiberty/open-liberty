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
package com.ibm.ws.security.spnego.fat.config;

public class MessageConstants {

//    public final static String SPNEGO_CONFIGURATION_PROCESSED_CWWKS4300I = "CWWKS4300I: The SPNEGO configuration .* was successfully processed.";
    public final static String SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I = "CWWKS4301I: The SPNEGO configuration .* was successfully modified.";
    public final static String KRBCONFIGFILE_NOT_SPECIFIED_CWWKS4302I = "CWWKS4302I: The Kerberos configuration file is not specified in the server.xml file, the default ";
    public final static String KRBCONFIGFILE_NOT_FOUND_CWWKS4303E = "CWWKS4303E: The specified Kerberos configuration file ";
    public final static String KEYTAB_NOT_SPECIFIED_CWWKS4304I = "CWWKS4304I: The Kerberos keytab file is not specified in the server.xml file, the default ";
    public final static String KEYTAB_NOT_FOUND_CWWKS4305E = "CWWKS4305E: The specified Kerberos keytab file ";
    public final static String SPNEGO_NOT_SUPPORTED_CWWKS4306E = "CWWKS4306E: <html><head><title>SPNEGO authentication is not supported.</title></head> ";
    public final static String NTLM_TOKEN_RECEIVED_CWWKS4307E = "CWWKS4307E: <html><head><title>An NTLM Token was received.</title></head> ";
    public final static String SPNEGO_AUTHENTICATION_ERROR_CWWKS4323E = "CWWKS4324E: <html><head><title>SPNEGO authentication failed. Contact your system administrator to resolve the problem.</title></head> ";
    public final static String CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E = "CWWKS4308E: Can not create a GSSCredential for service principal name: ";
    public final static String CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E = "CWWKS4309E: Can not create a GSSCredential for any of the service principal names. All requests will not use SPNEGO authentication.";
    public final static String GSSCREDENTIALS_NOT_RECEIVED_FOR_USER_CWWKS4310W = "CWWKS4310W: The client delegated GSSCredentials were expected to be received but were not found for user: ";
    public final static String SPN_NOT_SPECIFIED_CWWKS4314I = "CWWKS4314I: The servicePrincipalNames attribute is not specified in the server.xml file or its value is empty; the default ";
    public final static String GSSCREDENTIAL_NOT_FOUND_FOR_SPN_CWWKS4315E = "CWWKS4315E: Can not find a GSSCredential for the service principal name ";
    public final static String MULTIPLE_SPN_FOR_ONE_HOSTNAME_CWWKS4316W = "CWWKS4316W: The servicePrincipalNames have more than one SPN for host name ";
    public final static String MALFORMED_CUSTOM_ERROR_PAGE_CWWKS4317E = "CWWKS4317E: The custom error page URL ";
    public final static String CANNOT_LOAD_CUSTOM_ERROR_PAGE_CWWKS4318E = "CWWKS4318E: Can not load the custom error page ";
    public final static String CANNOT_GET_CONTENT_CUSTOM_ERROR_PAGE_CWWKS4319E = "CWWKS4319E: Can not get the content type for the custom error page ";
    public final static String CANNOT_VALIDATE_SPNEGO_TOKEN_CWWKS4320E = "CWWKS4320E: The SPNEGO or Kerberos token included in the HttpServletRequest can not be validated ";

    public final static String MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E = "CWWKS4354E: A malformed IP address range was specified. Found ";
    public final static String AUTHENTICATION_FILTER_ELEMENT_NOT_SPECIFIED_CWWKS4357I = "CWWKS4357I: The authFilter element is not specified in the server.xml file.";
    public final static String AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I = "CWWKS4358I: The authentication filter .* configuration was successfully processed.";
    public final static String AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I = "CWWKS4359I: The authentication filter .* configuration was successfully modified.";
    public final static String AUTHENTICATION_FILTER_MISSING_ID_ATTRIBUTE_CWWKS4360E = "CWWKS4360E: The authFilter element specified in the server.xml file is missing the required id attribute ";

    public final static String S4U2SELF_JDK_COMPLIANCE_ERROR_CWWKF0032E = "The constrainedDelegation-1.0 feature requires a minimum Java runtime environment version of JavaSE 1.8.";
    public final static String S4U2SELF_COULD_NOT_RESOLVE_JAVA8_MODULE_CWWKE0702E = "CWWKE0702E: Could not resolve module: com.ibm.ws.security.kerberos.java8";
    public final static String S4U2PROXY_SELF_COULD_NOT_START_OSGI_SERVICE_CWWKS0012E = "CWWKS0012E: The constrained delegation OSGi service";
    public final static String S4U2PROXY_COULD_NOT_RESOLVE_MODULE_CWWKE0702E = "CWWKE0702E: Could not resolve module: com.ibm.ws.security.kerberos.java8";

    public final static String S4U2SELF_COULD_NOT_IMPERSONATE_USER_CWWKS4340E = "CWWKS4340E: Can not impersonate the user";
    public final static String S4U2SELF_IS_NOT_ENABLED_CWWKS4342E = "CWWKS4342E: Can not process method .* because the constrained delegation S4U2self is not enabled.";
    public final static String S4U2PROXY_SELF_S4U2PROXY_IS_NOT_ENABLED_CWWKS4343E = "CWWKS4343E: Can not process method .* because the constrained delegation S4U2proxy is not enabled.";
    public final static String FEATURE_UPDATE_COMPLETE_CWWKF0008I = "CWWKF0008I: Feature update completed in ";

    //JDK 11 Message Expectation
    public final static String JDK11_INVALID_CHARACTER_CWWKE0701E = "CWWKE0701E: .* Empty nameString not allowed";

}