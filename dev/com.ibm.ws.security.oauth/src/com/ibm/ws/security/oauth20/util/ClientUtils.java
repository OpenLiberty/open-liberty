/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.exception.OAuthProviderException;
import com.ibm.ws.security.oauth20.platform.PlatformServiceFactory;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;

public class ClientUtils {

    private static TraceComponent tc = Tr.register(ClientUtils.class, "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    public static final String CLIENT_XML_FILE = "base.clients.xml";

    public static final int DEFAULT_SECRET_LENGTH = 60;

    // key-value URI redirect variable mappings for each provider
    public static final HashMap<String, HashMap<String, String>> uriRewrites = new HashMap<String, HashMap<String, String>>();

    // Generate a client secret, to be used by stack products
    public static String generateClientSecret() {
        return generateClientSecret(DEFAULT_SECRET_LENGTH);
    }

    public static String generateClientSecret(int length) {
        return OAuthUtil.getRandom(length);
    }

    /*
     * Utilities to read and write the client XML
     */
    // public static synchronized List<BaseClient> loadClients() throws OAuthProviderException {
    // if (tc.isEntryEnabled())
    // Tr.entry(tc, "loadClients");
    //
    // File targetDir = new File(OAuth20ProviderUtils.OAuthConfigFileDir);
    // if (!targetDir.exists()) {
    // targetDir.mkdir();
    // }
    //
    // File f = new File(targetDir, CLIENT_XML_FILE);
    // List<BaseClient> clients = new ArrayList<BaseClient>();
    // if (f.exists()) {
    // ClientProviderXMLHandler handler = new ClientProviderXMLHandler(f);
    // try {
    // handler.parse();
    // } catch (ParserConfigurationException e) {
    // throw new OAuthProviderException(e);
    // } catch (IOException e) {
    // throw new OAuthProviderException(e);
    // } catch (SAXException e) {
    // throw new OAuthProviderException(e);
    // }
    // clients = handler.getClients();
    // }
    //
    // if (tc.isEntryEnabled())
    // Tr.exit(tc, "loadClients");
    //
    // return clients;
    // }

    public static void storeClients(Collection<BaseClient> clients) throws OAuthProviderException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "storeClients");

        // Quick and dirty for now, until the XML format gets finalized
        String ls = System.getProperty("line.separator");
        String xml = Constants.XML_HEADER + ls;
        xml += "<" + Constants.XML_CAT_CLIENT + ">" + ls;
        for (BaseClient client : clients) {
            xml += "  <";
            xml += Constants.XML_TAG_CLIENT + " "
                    + Constants.XML_ATTR_CLIENT_ID + "=\""
                    + client.getClientId() + "\" ";
            xml += Constants.XML_ATTR_CLIENT_COMPONENT + "=\""
                    + client.getComponentId() + "\" ";
            xml += Constants.XML_ATTR_CLIENT_SECRET + "=\""
                    + PasswordUtil.passwordEncode(client.getClientSecret())
                    + "\" ";
            xml += Constants.XML_ATTR_CLIENT_DISPLAYNAME + "=\""
                    + client.getClientName() + "\" ";
            xml += Constants.XML_ATTR_CLIENT_REDIRECT + "=\""
                    + client.getRedirectUris() + "\" ";
            xml += Constants.XML_ATTR_CLIENT_ENABLED + "=\""
                    + client.isEnabled() + "\">";
            xml += ls + "  </" + Constants.XML_TAG_CLIENT + ">" + ls;
        }
        xml += "</" + Constants.XML_CAT_CLIENT + ">" + ls;

        storeXmlClients(xml);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "storeClients");
    }

    public static synchronized void storeXmlClients(String xml) throws OAuthProviderException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "storeXmlClients");

        File targetDir = new File(OAuth20ProviderUtils.OAuthConfigFileDir);
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }

        File f = new File(targetDir, CLIENT_XML_FILE);

        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                throw new OAuthProviderException(e);
            }
        }

        if (!f.canWrite()) {
            throw new OAuthProviderException("Cannot write: " + f.getAbsolutePath());
        }
        BufferedWriter buffer;
        try {
            buffer = new BufferedWriter(new FileWriter(f));
            buffer.write(xml);
            buffer.close();
        } catch (IOException e) {
            throw new OAuthProviderException(e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "storeXmlClients");
    }

    public static synchronized void deleteClientFile() {
        File f = new File(OAuth20ProviderUtils.OAuthConfigFileDir, CLIENT_XML_FILE);
        f.delete();
    }

    /*
     * Store the URI redirect key,value map for this provider
     */
    public static synchronized boolean initRewrites(OAuthComponentConfiguration config) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initRewrites");

        String providerID = config.getUniqueId();
        String[] providerRewrites = config.getConfigPropertyValues(Constants.CLIENT_URI_SUBSTITUTIONS);
        boolean hasRewrites = initRewrites(providerID, providerRewrites);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initRewrites");

        return hasRewrites;
    }

    /*
     * Store the URI redirect key,value map for this provider
     */
    public static synchronized boolean initRewrites(String providerID, String[] providerRewrites) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initRewrites");

        boolean hasRewrites = (providerRewrites != null) && (providerRewrites.length > 0);

        if (hasRewrites) {
            HashMap<String, String> rewriteMap = new HashMap<String, String>();
            for (String key : providerRewrites) {
                String newValue = key;
                try {
                    newValue = PlatformServiceFactory.getPlatformService().getRewrite(key);
                } catch (OAuthProviderException e) {
                    e.printStackTrace();
                }
                rewriteMap.put(key, newValue);

            }
            uriRewrites.put(providerID, rewriteMap);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initRewrites");

        return hasRewrites;
    }

    /*
     * Rewrite the client redirect URI variables. This should only be called on
     * client read, to preserve the variable in the stored string
     */
    public static BaseClient uriRewrite(BaseClient client) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "uriRewrite");
        }

        BaseClient result = client;
        String providerName = client.getComponentId();
        result.setRedirectUris(getReWrittenUris(client.getRedirectUris(), providerName));

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "uriRewrite");
        }

        return result;
    }

    /*
     * Rewrite the client redirect URI variables. This should only be called on
     * client read, to preserve the variable in the stored string
     */
    public static OidcBaseClient uriRewrite(OidcBaseClient client) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "uriRewrite");
        }

        OidcBaseClient result = client;
        String providerName = client.getComponentId();
        result.setRedirectUris(getReWrittenUris(client.getRedirectUris(), providerName));

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "uriRewrite");
        }

        return result;
    }

    private static JsonArray getReWrittenUris(JsonArray originalUris, String providerName) {
        if (!OidcOAuth20Util.isNullEmpty(originalUris)) {
            JsonArray newRedirectUris = new JsonArray();

            for (int idx = 0; idx < originalUris.size(); idx++)
            {
                String uri = originalUris.get(idx).getAsString();

                if (uri.indexOf("${") >= 0) // check if can skip for performance
                {
                    HashMap<String, String> rewriteMap = uriRewrites.get(providerName);
                    if (rewriteMap != null)
                    {
                        for (String key : rewriteMap.keySet())
                        {
                            uri = uri.replace(key, rewriteMap.get(key));
                        }
                    }
                }

                newRedirectUris.add(new JsonPrimitive(uri));
            }
            return newRedirectUris;
        }
        return new JsonArray();
    }
}
