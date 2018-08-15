/*
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.social;

import com.ibm.wsspi.wab.configure.WABConfiguration;

public interface SocialLoginWebappConfig extends WABConfiguration {

    public String getSocialMediaSelectionPageUrl();

    public boolean isLocalAuthenticationEnabled();

}
