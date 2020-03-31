/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Defines configuration attributes of the HTTP Session Manager
 *
 * @author Tim Burns
 *
 */
public class HttpSession extends ConfigElement {

    private String storageRef;
    private Boolean sslTrackingEnabled;
    private Boolean urlRewritingEnabled;
    private Boolean protocolSwitchRewritingEnabled;
    private Boolean cookiesEnabled;
    private String cookieName;
    private String cookieDomain;
    private Integer cookieMaxAge;
    private String cookiePath;
    private Boolean cookieSecure;
    private Boolean cookieHttpOnly;
    private String cookieSameSite;
    private Integer maxInMemorySessionCount;
    private Boolean allowOverflow;
    private Integer invalidationTimeout;
    private Boolean securityIntegrationEnabled;
    private Integer idLength;
    private Boolean useContextRootAsCookiePath;
    private Boolean alwaysEncodeUrl;
    private String cloneId;
    private String cloneSeparator;
    private Boolean debugCrossover;
    private Integer forceInvalidationMultiple;
    private Boolean idReuse;
    private Boolean noAdditionalInfo;
    private Long reaperPollInterval;
    private String rewriteId;
    private Boolean securityUserIgnoreCase;
    private Boolean throwSecurityExceptionOnGetSessionFalse;
    private Boolean allowSerializedAccess;
    private Boolean accessOnTimeout;
    private Integer maxWaitTime;

    @XmlTransient
    public void setStorage(HttpSessionDatabase httpSessionDatabase) {
        this.setStorageRef(httpSessionDatabase == null ? null : httpSessionDatabase.getId());
    }

    public String getStorageRef() {
        return storageRef;
    }

    @XmlAttribute
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
    @XmlAttribute
    public void setInvalidationTimeout(Integer invalidationTimeout) {
        this.invalidationTimeout = invalidationTimeout;
    }

    public Boolean isSslTrackingEnabled() {
        return this.sslTrackingEnabled;
    }

    @XmlAttribute
    public void setSslTrackingEnabled(Boolean sslTrackingEnabled) {
        this.sslTrackingEnabled = sslTrackingEnabled;
    }

    public Boolean isUrlRewritingEnabled() {
        return this.urlRewritingEnabled;
    }

    @XmlAttribute
    public void setUrlRewritingEnabled(Boolean urlRewritingEnabled) {
        this.urlRewritingEnabled = urlRewritingEnabled;
    }

    public Boolean isProtocolSwitchRewritingEnabled() {
        return this.protocolSwitchRewritingEnabled;
    }

    @XmlAttribute
    public void setProtocolSwitchRewritingEnabled(Boolean protocolSwitchRewritingEnabled) {
        this.protocolSwitchRewritingEnabled = protocolSwitchRewritingEnabled;
    }

    public Boolean isCookiesEnabled() {
        return this.cookiesEnabled;
    }

    @XmlAttribute
    public void setCookiesEnabled(Boolean cookiesEnabled) {
        this.cookiesEnabled = cookiesEnabled;
    }

    public String getCookieName() {
        return this.cookieName;
    }

    @XmlAttribute
    public void setCookieName(String cookieName) {
        this.cookieName = ConfigElement.getValue(cookieName);
    }

    public String getCookieDomain() {
        return this.cookieDomain;
    }

    @XmlAttribute
    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = ConfigElement.getValue(cookieDomain);
    }

    public Integer getCookieMaxAge() {
        return this.cookieMaxAge;
    }

    @XmlAttribute
    public void setCookieMaxAge(Integer cookieMaxAge) {
        this.cookieMaxAge = cookieMaxAge;
    }

    public String getCookiePath() {
        return this.cookiePath;
    }

    @XmlAttribute
    public void setCookiePath(String cookiePath) {
        this.cookiePath = ConfigElement.getValue(cookiePath);
    }

    public Boolean isCookieSecure() {
        return this.cookieSecure;
    }

    @XmlAttribute
    public void setCookieSecure(Boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public Integer getMaxInMemorySessionCount() {
        return this.maxInMemorySessionCount;
    }

    @XmlAttribute
    public void setMaxInMemorySessionCount(Integer maxInMemorySessionCount) {
        this.maxInMemorySessionCount = maxInMemorySessionCount;
    }

    public Boolean isAllowOverflow() {
        return this.allowOverflow;
    }

    @XmlAttribute
    public void setAllowOverflow(Boolean allowOverflow) {
        this.allowOverflow = allowOverflow;
    }

    public Boolean isSecurityIntegrationEnabled() {
        return this.securityIntegrationEnabled;
    }

    @XmlAttribute
    public void setSecurityIntegrationEnabled(Boolean securityIntegrationEnabled) {
        this.securityIntegrationEnabled = securityIntegrationEnabled;
    }

    public Integer getIdLength() {
        return this.idLength;
    }

    @XmlAttribute
    public void setIdLength(Integer idLength) {
        this.idLength = idLength;
    }

    public Boolean getUseContextRootAsCookiePath() {
        return useContextRootAsCookiePath;
    }

    @XmlAttribute
    public void setUseContextRootAsCookiePath(Boolean useContextRootAsCookiePath) {
        this.useContextRootAsCookiePath = useContextRootAsCookiePath;
    }

    public Boolean getAlwaysEncodeUrl() {
        return alwaysEncodeUrl;
    }

    @XmlAttribute
    public void setAlwaysEncodeUrl(Boolean alwaysEncodeUrl) {
        this.alwaysEncodeUrl = alwaysEncodeUrl;
    }

    public String getCloneId() {
        return cloneId;
    }

    @XmlAttribute
    public void setCloneId(String cloneId) {
        this.cloneId = cloneId;
    }

    public String getCloneSeparator() {
        return cloneSeparator;
    }

    @XmlAttribute
    public void setCloneSeparator(String cloneSeparator) {
        this.cloneSeparator = cloneSeparator;
    }

    public Boolean getDebugCrossover() {
        return debugCrossover;
    }

    @XmlAttribute
    public void setDebugCrossover(Boolean debugCrossover) {
        this.debugCrossover = debugCrossover;
    }

    public Integer getForceInvalidationMultiple() {
        return forceInvalidationMultiple;
    }

    @XmlAttribute
    public void setForceInvalidationMultiple(Integer forceInvalidationMultiple) {
        this.forceInvalidationMultiple = forceInvalidationMultiple;
    }

    public Boolean getIdReuse() {
        return idReuse;
    }

    @XmlAttribute
    public void setIdReuse(Boolean idReuse) {
        this.idReuse = idReuse;
    }

    public Boolean getNoAdditionalInfo() {
        return noAdditionalInfo;
    }

    @XmlAttribute
    public void setNoAdditionalInfo(Boolean noAdditionalInfo) {
        this.noAdditionalInfo = noAdditionalInfo;
    }

    public Long getReaperPollInterval() {
        return reaperPollInterval;
    }

    @XmlAttribute
    public void setReaperPollInterval(Long reaperPollInterval) {
        this.reaperPollInterval = reaperPollInterval;
    }

    public String getRewriteId() {
        return rewriteId;
    }

    @XmlAttribute
    public void setRewriteId(String rewriteId) {
        this.rewriteId = rewriteId;
    }

    public Boolean getSecurityUserIgnoreCase() {
        return securityUserIgnoreCase;
    }

    @XmlAttribute
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

    @XmlAttribute
    public void setCookieHttpOnly(Boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    public String getCookieSameSite() {
        return this.cookieSameSite;
    }

    @XmlAttribute
    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
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

    @XmlAttribute
    public void setThrowSecurityExceptionOnGetSessionFalse(Boolean throwSecurityExceptionOnGetSessionFalse) {
        this.throwSecurityExceptionOnGetSessionFalse = throwSecurityExceptionOnGetSessionFalse;
    }

    public Boolean getAllowSerializedAccess() {
        return allowSerializedAccess;
    }

    @XmlAttribute
    public void setAllowSerializedAccess(Boolean allowSerializedAccess) {
        this.allowSerializedAccess = allowSerializedAccess;
    }

    public Boolean getAccessOnTimeout() {
        return accessOnTimeout;
    }

    @XmlAttribute
    public void setAccessOnTimeout(Boolean accessOnTimeout) {
        this.accessOnTimeout = accessOnTimeout;
    }

    public Integer getMaxWaitTime() {
        return maxWaitTime;
    }

    @XmlAttribute
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
