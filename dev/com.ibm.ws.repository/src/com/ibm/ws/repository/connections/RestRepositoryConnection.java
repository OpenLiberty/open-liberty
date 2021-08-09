/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.connections;

import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RestClient;

public class RestRepositoryConnection extends AbstractRepositoryConnection implements RepositoryConnection {

    private String repositoryUrl;
    private String userId;
    private String password;
    private String apiKey;

    private String softlayerUserId;
    private String softlayerPassword;

    private String attachmentBasicAuthUserId;
    private String attachmentBasicAuthPassword;

    private String userAgent;

    private RestRepositoryConnectionProxy proxy;

    public RestRepositoryConnection(String repositoryUrl) {
        this(null, null, null, repositoryUrl);
    }

    public RestRepositoryConnection(String userId, String password, String apiKey, String repositoryUrl) {
        this(userId, password, apiKey, repositoryUrl, null, null, null, null);
    }

    public RestRepositoryConnection(String userId, String password, String apiKey, String repositoryUrl,
                                    String softlayerUserId, String softlayerPassword) {
        this(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, null, null);
    }

    public RestRepositoryConnection(String userId, String password, String apiKey, String repositoryUrl,
                                    String softlayerUserId, String softlayerPassword, String attachmentBasicAuthUserId, String gsaPassword) {
        this.userId = userId;
        this.password = password;
        this.apiKey = apiKey;
        this.repositoryUrl = repositoryUrl;
        this.softlayerUserId = softlayerUserId;
        this.softlayerPassword = softlayerPassword;
        this.attachmentBasicAuthUserId = attachmentBasicAuthUserId;
        this.attachmentBasicAuthPassword = gsaPassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getSoftlayerUserId() {
        return softlayerUserId;
    }

    public void setSoftlayerUserId(String softlayerUserId) {
        this.softlayerUserId = softlayerUserId;
    }

    public String getSoftlayerPassword() {
        return softlayerPassword;
    }

    public void setSoftlayerPassword(String softlayerPassword) {
        this.softlayerPassword = softlayerPassword;
    }

    public String getAttachmentBasicAuthUserId() {
        return attachmentBasicAuthUserId;
    }

    public void setAttachmentBasicAuthUserId(String attachmentBasicAuthUserId) {
        this.attachmentBasicAuthUserId = attachmentBasicAuthUserId;
    }

    public String getAttachmentBasicAuthPassword() {
        return attachmentBasicAuthPassword;
    }

    public void setAttachmentBasicAuthPassword(String attachmentBasicAuthPassword) {
        this.attachmentBasicAuthPassword = attachmentBasicAuthPassword;
    }

    public com.ibm.ws.repository.transport.client.ClientLoginInfo getClientLoginInfo() {
        com.ibm.ws.repository.transport.client.ClientLoginInfo clientLogin = new com.ibm.ws.repository.transport.client.ClientLoginInfo(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, attachmentBasicAuthUserId, attachmentBasicAuthPassword, userAgent);
        if (proxy != null) {
            clientLogin.setProxy(proxy.getLoginInfoClientProxy());
        }
        return clientLogin;
    }

    public void setProxy(RestRepositoryConnectionProxy proxy) {
        this.proxy = proxy;
    }

    public RestRepositoryConnectionProxy getProxy() {
        return proxy;
    }

    /**
     * Returns a URL which represent the URL that can be used to view the asset in Massive. This is more
     * for testing purposes, the assets can be access programatically via various methods on this class.
     *
     * @param asset - an Asset
     * @return String - the asset URL
     */
    public String getAssetURL(String id) {
        String url = getRepositoryUrl() + "/assets/" + id + "?";
        if (getUserId() != null) {
            url += "userId=" + getUserId();
        }
        if (getUserId() != null && getPassword() != null) {
            url += "&password=" + getPassword();
        }
        if (getApiKey() != null) {
            url += "&apiKey=" + getApiKey();
        }
        return url;
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * <p>Sets the user agent that is being used to access the Massive client. This follows the HTTP user-agent header specification here:</p>
     * <p><a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43">http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.43</a></p>
     * <p>For example:</p>
     * <code>com.ibm.ws.st.ui/8.5.5.4</code>
     *
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /** {@inheritDoc} */
    @Override
    public String getRepositoryLocation() {
        return getRepositoryUrl();
    }

    @Override
    public RepositoryReadableClient createClient() {
        return new RestClient(getClientLoginInfo());
    }

}
