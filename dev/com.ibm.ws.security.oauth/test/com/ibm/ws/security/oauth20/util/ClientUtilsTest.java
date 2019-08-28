/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 *
 */
public class ClientUtilsTest extends TestCase {

    @Test
    public void testUriRewriteOidcBaseClient() {
        String providerRewriteOriginal1 = "${http://www.newValue1.com}";
        String providerRewriteReplacement1 = "http://www.evenNewerValue1.com";

        String providerRewriteOriginal2 = "${http://www.newValue2.com}";
        String providerRewriteReplacement2 = "http://www.evenNewerValue2.com";

        HashMap<String, String> rewriteMap = new HashMap<String, String>();
        rewriteMap.put(providerRewriteOriginal1, providerRewriteReplacement1);
        rewriteMap.put(providerRewriteOriginal2, providerRewriteReplacement2);

        ClientUtils.uriRewrites.put(AbstractOidcRegistrationBaseTest.COMPONENT_ID, rewriteMap);

        OidcBaseClient testOidcBaseClient = AbstractOidcRegistrationBaseTest.getSampleOidcBaseClient();

        JsonArray providerRewriteOriginalArray = new JsonArray();
        providerRewriteOriginalArray.add(new JsonPrimitive(providerRewriteOriginal1));
        providerRewriteOriginalArray.add(new JsonPrimitive(providerRewriteOriginal2));

        testOidcBaseClient.setRedirectUris(providerRewriteOriginalArray);

        OidcBaseClient uriReWrittenOidcBaseClient = ClientUtils.uriRewrite(testOidcBaseClient);

        assertNotNull(testOidcBaseClient);
        assertNotNull(uriReWrittenOidcBaseClient);
        assertEquals("URI was not rewritten for OidcBaseClient", uriReWrittenOidcBaseClient.getRedirectUris().get(0).getAsString(), providerRewriteReplacement1);
        assertEquals("Multiple URIs were not rewritten for OidcBaseClient", uriReWrittenOidcBaseClient.getRedirectUris().get(1).getAsString(), providerRewriteReplacement2);
    }

    @Test
    public void testUriRewriteBaseClient() {
        String providerRewriteOriginal1 = "${http://www.newValue1.com}";
        String providerRewriteReplacement1 = "http://www.evenNewerValue1.com";

        String providerRewriteOriginal2 = "${http://www.newValue2.com}";
        String providerRewriteReplacement2 = "http://www.evenNewerValue2.com";

        HashMap<String, String> rewriteMap = new HashMap<String, String>();
        rewriteMap.put(providerRewriteOriginal1, providerRewriteReplacement1);
        rewriteMap.put(providerRewriteOriginal2, providerRewriteReplacement2);

        ClientUtils.uriRewrites.put(AbstractOidcRegistrationBaseTest.COMPONENT_ID, rewriteMap);

        BaseClient testBaseClient = AbstractOidcRegistrationBaseTest.getSampleOidcBaseClient();

        JsonArray providerRewriteOriginalArray = new JsonArray();
        providerRewriteOriginalArray.add(new JsonPrimitive(providerRewriteOriginal1));
        providerRewriteOriginalArray.add(new JsonPrimitive(providerRewriteOriginal2));

        testBaseClient.setRedirectUris(providerRewriteOriginalArray);

        BaseClient uriReWrittenOidcBaseClient = ClientUtils.uriRewrite(testBaseClient);

        assertNotNull(testBaseClient);
        assertNotNull(uriReWrittenOidcBaseClient);
        assertEquals("URI was not rewritten for BaseClient", uriReWrittenOidcBaseClient.getRedirectUris().get(0).getAsString(), providerRewriteReplacement1);
        assertEquals("Multiple URIs were not rewritten for BaseClient", uriReWrittenOidcBaseClient.getRedirectUris().get(1).getAsString(), providerRewriteReplacement2);
    }

    @Test
    public void testUriRewriteEmpty() {
        String providerRewriteOriginal1 = "${http://www.newValue1.com}";
        String providerRewriteReplacement1 = "http://www.evenNewerValue1.com";

        String providerRewriteOriginal2 = "${http://www.newValue2.com}";
        String providerRewriteReplacement2 = "http://www.evenNewerValue2.com";

        HashMap<String, String> rewriteMap = new HashMap<String, String>();
        rewriteMap.put(providerRewriteOriginal1, providerRewriteReplacement1);
        rewriteMap.put(providerRewriteOriginal2, providerRewriteReplacement2);

        ClientUtils.uriRewrites.put(AbstractOidcRegistrationBaseTest.COMPONENT_ID, rewriteMap);

        OidcBaseClient testOidcBaseClient = AbstractOidcRegistrationBaseTest.getSampleOidcBaseClient();
        testOidcBaseClient.setRedirectUris(null);

        OidcBaseClient uriReWrittenOidcBaseClient = ClientUtils.uriRewrite(testOidcBaseClient);

        assertNotNull(testOidcBaseClient);
        assertNotNull(uriReWrittenOidcBaseClient);

        //Ensure no rewrites or uris were added into a client that had no redirectUris to begin with
        assertEquals(uriReWrittenOidcBaseClient.getRedirectUris().size(), 0);
    }
}
