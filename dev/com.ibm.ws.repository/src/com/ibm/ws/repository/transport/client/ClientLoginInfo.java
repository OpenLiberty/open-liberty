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

package com.ibm.ws.repository.transport.client;

public class ClientLoginInfo {

    private String userId;
    private String password;
    private String apiKey;
    private String repositoryUrl;

    private String softlayerUserId;
    private String softlayerPassword;

    private String attachmentBasicAuthUserId;
    private String attachmentBasicAuthPassword;

    private String userAgent;

    private LoginInfoClientProxy proxy;

    public ClientLoginInfo(String userId, String password, String apiKey, String repositoryUrl,
                           String softlayerUserId, String softlayerPassword, String attachmentBasicAuthUserId, String attachmentBasicAuthPassword, String userAgent) {
        this.userId = userId;
        this.password = password;
        this.apiKey = apiKey;
        this.repositoryUrl = repositoryUrl;
        this.softlayerUserId = softlayerUserId;
        this.softlayerPassword = softlayerPassword;
        this.attachmentBasicAuthUserId = attachmentBasicAuthUserId;
        this.attachmentBasicAuthPassword = attachmentBasicAuthPassword;
        this.userAgent = userAgent;
    }

    public ClientLoginInfo(String userId, String password, String apiKey, String repositoryUrl,
                           String softlayerUserId, String softlayerPassword) {
        this(userId, password, apiKey, repositoryUrl, softlayerUserId, softlayerPassword, null, null, null);
    }

    public ClientLoginInfo(String userId, String password, String apiKey, String repositoryUrl) {
        this(userId, password, apiKey, repositoryUrl, null, null, null, null, null);
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

    public void setProxy(LoginInfoClientProxy proxy) {
        this.proxy = proxy;
    }

    public LoginInfoClientProxy getProxy() {
        return this.proxy;
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
}
