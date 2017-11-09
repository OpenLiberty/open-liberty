/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat_helper;

/**
 *
 */
public class MessagesConstants {

 // Values to be verified in messages
    protected static final String MSG_JASPI_AUTHENTICATION_FAILED = "CWWKS1652A:.*";
    protected static final String PROVIDER_AUTHENTICATION_FAILED = "Invalid user or password";
    protected static final String MSG_AUTHORIZATION_FAILED = "CWWKS9104A:.*";
    protected static final String MSG_JACC_AUTHORIZATION_FAILED = "CWWKS9124A:.*";

    protected static final String MSG_JASPI_PROVIDER_ACTIVATED = "CWWKS1653I";
    protected static final String MSG_JASPI_PROVIDER_DEACTIVATED = "CWWKS1654I";

    protected static final String MSG_JACC_SERVICE_STARTING = "CWWKS2850I";
    protected static final String MSG_JACC_SERVICE_STARTED = "CWWKS2851I";
}
