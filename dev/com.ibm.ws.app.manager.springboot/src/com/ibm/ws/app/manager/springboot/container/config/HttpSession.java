/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * Defines configuration attributes of the HTTP Session Manager
 *
 */
public class HttpSession extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_STORAGE_REF = "storageRef";
    private String storageRef;

    public final static String XML_ATTRIBUTE_NAME_SSL_TRACKING_ENABLED = "sslTrackingEnabled";
    private Boolean sslTrackingEnabled;

    public final static String XML_ATTRIBUTE_NAME_URL_REWRITING_ENABLED = "urlRewritingEnabled";
    private Boolean urlRewritingEnabled;

    public final static String XML_ATTRIBUTE_NAME_PROTOCOL_SWITCH_REWRITING_ENABLED = "protocolSwitchRewritingEnabled";
    private Boolean protocolSwitchRewritingEnabled;

    public final static String XML_ATTRIBUTE_NAME_COOKIES_ENABLED = "cookiesEnabled";
    private Boolean cookiesEnabled;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_NAME = "cookieName";
    private String cookieName;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_DOMAIN = "cookieDomain";
    private String cookieDomain;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_MAX_AGE = "cookieMaxAge";
    private Integer cookieMaxAge;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_PATH = "cookiePath";
    private String cookiePath;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_SECURE = "cookieSecure";
    private Boolean cookieSecure;

    public final static String XML_ATTRIBUTE_NAME_COOKIE_HTTP_ONLY = "cookieHttpOnly";
    private Boolean cookieHttpOnly;

    public final static String XML_ATTRIBUTE_NAME_MAX_IN_MEMORY_SESSION_COUNT = "maxInMemorySessionCount";
    private Integer maxInMemorySessionCount;

    public final static String XML_ATTRIBUTE_NAME_ALLOW_OVERFLOW = "allowOverflow";
    private Boolean allowOverflow;

    public final static String XML_ATTRIBUTE_NAME_INVALIDATION_TIMEOUT = "invalidationTimeout";
    private Integer invalidationTimeout;

    public final static String XML_ATTRIBUTE_NAME_SECURITY_INTEGRATION_ENABLED = "securityIntegrationEnabled";
    private Boolean securityIntegrationEnabled;

    public final static String XML_ATTRIBUTE_NAME_ID_LENGTH = "idLength";
    private Integer idLength;

    public final static String XML_ATTRIBUTE_NAMEUSE_CONTEXT_ROOT_AS_COOKIE_PATH = "useContextRootAsCookiePath";
    private Boolean useContextRootAsCookiePath;

    public final static String XML_ATTRIBUTE_NAME_ALWAYS_ENCODE_URL = "alwaysEncodeUrl";
    private Boolean alwaysEncodeUrl;

    public final static String XML_ATTRIBUTE_NAME_CLONE_ID = "cloneId";
    private String cloneId;

    public final static String XML_ATTRIBUTE_NAME_CLONE_SEPARATOR = "cloneSeparator";
    private String cloneSeparator;

    public final static String XML_ATTRIBUTE_NAME_DEBUG_CROSSOVER = "debugCrossover";
    private Boolean debugCrossover;

    public final static String XML_ATTRIBUTE_NAME_FORCE_INVALIDATION_MULTIPLE = "forceInvalidationMultiple";
    private Integer forceInvalidationMultiple;

    public final static String XML_ATTRIBUTE_NAME_ID_REUSE = "idReuse";
    private Boolean idReuse;

    public final static String XML_ATTRIBUTE_NAME_NO_ADDITIONAL_INFO = "noAdditionalInfo";
    private Boolean noAdditionalInfo;

    public final static String XML_ATTRIBUTE_NAME_REAPER_POLL_INTERVAL = "reaperPollInterval";
    private Long reaperPollInterval;

    public final static String XML_ATTRIBUTE_NAME_REWRITE_ID = "rewriteId";
    private String rewriteId;

    public final static String XML_ATTRIBUTE_NAME_SECURITY_USER_IGNORE_CASE = "securityUserIgnoreCase";
    private Boolean securityUserIgnoreCase;

    public final static String XML_ATTRIBUTE_NAME_THROW_SECURITY_EXCEPTION_ON_GET_SESSION_FALSE = "throwSecurityExceptionOnGetSessionFalse";
    private Boolean throwSecurityExceptionOnGetSessionFalse;

    public final static String XML_ATTRIBUTE_NAME_ALLOW_SERIALIZE_ACCESS = "allowSerializedAccess";
    private Boolean allowSerializedAccess;

    public final static String XML_ATTRIBUTE_NAME_ACCES_ON_TIMEOUT = "accessOnTimeout";
    private Boolean accessOnTimeout;

    public final static String XML_ATTRIBUTE_NAME_MAX_WAIT_TIME = "maxWaitTime";
    private Integer maxWaitTime;

    public String getStorageRef() {
        return storageRef;
    }

    public void setStorageRef(String storageRef) {
        this.storageRef = storageRef;
    }

    /**
     * @return the number of minutes before sessions become eligible for invalidation
     */
    public Integer getInvalidationTimeout() {
        return this.invalidationTimeout;
    }

    /**
     * @param invalidationTimeout the number of minutes before sessions become eligible for invalidation
     */
    public void setInvalidationTimeout(Integer invalidationTimeout) {
        this.invalidationTimeout = invalidationTimeout;
    }

    public Boolean isSslTrackingEnabled() {
        return this.sslTrackingEnabled;
    }

    public void setSslTrackingEnabled(Boolean sslTrackingEnabled) {
        this.sslTrackingEnabled = sslTrackingEnabled;
    }

    public Boolean isUrlRewritingEnabled() {
        return this.urlRewritingEnabled;
    }

    public void setUrlRewritingEnabled(Boolean urlRewritingEnabled) {
        this.urlRewritingEnabled = urlRewritingEnabled;
    }

    public Boolean isProtocolSwitchRewritingEnabled() {
        return this.protocolSwitchRewritingEnabled;
    }

    public void setProtocolSwitchRewritingEnabled(Boolean protocolSwitchRewritingEnabled) {
        this.protocolSwitchRewritingEnabled = protocolSwitchRewritingEnabled;
    }

    public Boolean isCookiesEnabled() {
        return this.cookiesEnabled;
    }

    public void setCookiesEnabled(Boolean cookiesEnabled) {
        this.cookiesEnabled = cookiesEnabled;
    }

    public String getCookieName() {
        return this.cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = ConfigElement.getValue(cookieName);
    }

    public String getCookieDomain() {
        return this.cookieDomain;
    }

    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = ConfigElement.getValue(cookieDomain);
    }

    public Integer getCookieMaxAge() {
        return this.cookieMaxAge;
    }

    public void setCookieMaxAge(Integer cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    public String getCookiePath() {
        return this.cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = ConfigElement.getValue(cookiePath);
    }

    public Boolean isCookieSecure() {
        return this.cookieSecure;
    }

    public void setCookieSecure(Boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public Integer getMaxInMemorySessionCount() {
        return this.maxInMemorySessionCount;
    }

    public void setMaxInMemorySessionCount(Integer maxInMemorySessionCount) {
        this.maxInMemorySessionCount = maxInMemorySessionCount;
    }

    public Boolean isAllowOverflow() {
        return this.allowOverflow;
    }

    public void setAllowOverflow(Boolean allowOverflow) {
        this.allowOverflow = allowOverflow;
    }

    public Boolean isSecurityIntegrationEnabled() {
        return this.securityIntegrationEnabled;
    }

    public void setSecurityIntegrationEnabled(Boolean securityIntegrationEnabled) {
        this.securityIntegrationEnabled = securityIntegrationEnabled;
    }

    public Integer getIdLength() {
        return this.idLength;
    }

    public void setIdLength(Integer idLength) {
        this.idLength = idLength;
    }

    public Boolean getUseContextRootAsCookiePath() {
        return useContextRootAsCookiePath;
    }

    public void setUseContextRootAsCookiePath(Boolean useContextRootAsCookiePath) {
        this.useContextRootAsCookiePath = useContextRootAsCookiePath;
    }

    public Boolean getAlwaysEncodeUrl() {
        return alwaysEncodeUrl;
    }

    public void setAlwaysEncodeUrl(Boolean alwaysEncodeUrl) {
        this.alwaysEncodeUrl = alwaysEncodeUrl;
    }

    public String getCloneId() {
        return cloneId;
    }

    public void setCloneId(String cloneId) {
        this.cloneId = cloneId;
    }

    public String getCloneSeparator() {
        return cloneSeparator;
    }

    public void setCloneSeparator(String cloneSeparator) {
        this.cloneSeparator = cloneSeparator;
    }

    public Boolean getDebugCrossover() {
        return debugCrossover;
    }

    public void setDebugCrossover(Boolean debugCrossover) {
        this.debugCrossover = debugCrossover;
    }

    public Integer getForceInvalidationMultiple() {
        return forceInvalidationMultiple;
    }

    public void setForceInvalidationMultiple(Integer forceInvalidationMultiple) {
        this.forceInvalidationMultiple = forceInvalidationMultiple;
    }

    public Boolean getIdReuse() {
        return idReuse;
    }

    public void setIdReuse(Boolean idReuse) {
        this.idReuse = idReuse;
    }

    public Boolean getNoAdditionalInfo() {
        return noAdditionalInfo;
    }

    public void setNoAdditionalInfo(Boolean noAdditionalInfo) {
        this.noAdditionalInfo = noAdditionalInfo;
    }

    public Long getReaperPollInterval() {
        return reaperPollInterval;
    }

    public void setReaperPollInterval(Long reaperPollInterval) {
        this.reaperPollInterval = reaperPollInterval;
    }

    public String getRewriteId() {
        return rewriteId;
    }

    public void setRewriteId(String rewriteId) {
        this.rewriteId = rewriteId;
    }

    public Boolean getSecurityUserIgnoreCase() {
        return securityUserIgnoreCase;
    }

    public void setSecurityUserIgnoreCase(Boolean securityUserIgnoreCase) {
        this.securityUserIgnoreCase = securityUserIgnoreCase;
    }

    public Boolean getSslTrackingEnabled() {
        return sslTrackingEnabled;
    }

    public Boolean getUrlRewritingEnabled() {
        return urlRewritingEnabled;
    }

    public Boolean getProtocolSwitchRewritingEnabled() {
        return protocolSwitchRewritingEnabled;
    }

    public Boolean getCookiesEnabled() {
        return cookiesEnabled;
    }

    public Boolean getCookieSecure() {
        return cookieSecure;
    }

    public Boolean getCookieHttpOnly() {
        return this.cookieHttpOnly;
    }

    public void setCookieHttpOnly(Boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    public Boolean getAllowOverflow() {
        return allowOverflow;
    }

    public Boolean getSecurityIntegrationEnabled() {
        return securityIntegrationEnabled;
    }

    public Boolean getThrowSecurityExceptionOnGetSessionFalse() {
        return throwSecurityExceptionOnGetSessionFalse;
    }

    public void setThrowSecurityExceptionOnGetSessionFalse(Boolean throwSecurityExceptionOnGetSessionFalse) {
        this.throwSecurityExceptionOnGetSessionFalse = throwSecurityExceptionOnGetSessionFalse;
    }

    public Boolean getAllowSerializedAccess() {
        return allowSerializedAccess;
    }

    public void setAllowSerializedAccess(Boolean allowSerializedAccess) {
        this.allowSerializedAccess = allowSerializedAccess;
    }

    public Boolean getAccessOnTimeout() {
        return accessOnTimeout;
    }

    public void setAccessOnTimeout(Boolean accessOnTimeout) {
        this.accessOnTimeout = accessOnTimeout;
    }

    public Integer getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(Integer maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("HttpSession{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (allowOverflow != null)
            buf.append("allowOverflow=\"" + allowOverflow + "\" ");
        if (cookieDomain != null)
            buf.append("cookieDomain=\"" + cookieDomain + "\" ");
        if (cookieMaxAge != null)
            buf.append("cookieMaxAge=\"" + cookieMaxAge + "\" ");
        if (cookieName != null)
            buf.append("cookieName=\"" + cookieName + "\" ");
        if (cookiePath != null)
            buf.append("cookiePath=\"" + cookiePath + "\" ");
        if (cookieSecure != null)
            buf.append("cookieSecure=\"" + cookieSecure + "\" ");
        if (cookiesEnabled != null)
            buf.append("cookiesEnabled=\"" + cookiesEnabled + "\" ");
        if (idLength != null)
            buf.append("idLength=\"" + idLength + "\" ");
        if (invalidationTimeout != null)
            buf.append("invalidationTimeout=\"" + invalidationTimeout + "\" ");
        if (maxInMemorySessionCount != null)
            buf.append("maxInMemorySessionCount=\"" + maxInMemorySessionCount + "\" ");
        if (protocolSwitchRewritingEnabled != null)
            buf.append("protocolSwitchRewritingEnabled=\"" + protocolSwitchRewritingEnabled + "\" ");
        if (securityIntegrationEnabled != null)
            buf.append("securityIntegrationEnabled=\"" + securityIntegrationEnabled + "\" ");
        if (sslTrackingEnabled != null)
            buf.append("sslTrackingEnabled=\"" + sslTrackingEnabled + "\" ");
        if (urlRewritingEnabled != null)
            buf.append("urlRewritingEnabled=\"" + urlRewritingEnabled + "\" ");
        if (useContextRootAsCookiePath != null)
            buf.append("useContextRootAsCookiePath=\"" + useContextRootAsCookiePath + "\" ");
        if (alwaysEncodeUrl != null)
            buf.append("alwaysEncodeUrl=\"" + alwaysEncodeUrl + "\" ");
        if (cloneId != null)
            buf.append("cloneId=\"" + cloneId + "\" ");
        if (cloneSeparator != null)
            buf.append("cloneSeparator=\"" + cloneSeparator + "\" ");
        if (debugCrossover != null)
            buf.append("debugCrossover=\"" + debugCrossover + "\" ");
        if (forceInvalidationMultiple != null)
            buf.append("forceInvalidationMultiple=\"" + forceInvalidationMultiple + "\" ");
        if (idReuse != null)
            buf.append("idReuse=\"" + idReuse + "\" ");
        if (noAdditionalInfo != null)
            buf.append("noAdditionalInfo=\"" + noAdditionalInfo + "\" ");
        if (reaperPollInterval != null)
            buf.append("reaperPollInterval=\"" + reaperPollInterval + "\" ");
        if (rewriteId != null)
            buf.append("rewriteId=\"" + rewriteId + "\" ");
        if (securityUserIgnoreCase != null)
            buf.append("securityUserIgnoreCase=\"" + securityUserIgnoreCase + "\" ");
        if (throwSecurityExceptionOnGetSessionFalse != null)
            buf.append("throwSecurityExceptionOnGetSessionFalse=\"" + throwSecurityExceptionOnGetSessionFalse + "\" ");
        if (allowSerializedAccess != null)
            buf.append("allowSerializedAccess=\"" + allowSerializedAccess + "\" ");
        if (accessOnTimeout != null)
            buf.append("accessOnTimeout=\"" + accessOnTimeout + "\" ");
        if (maxWaitTime != null)
            buf.append("maxWaitTime=\"" + maxWaitTime + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
