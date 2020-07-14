/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration for acmeCA-2.0 feature.
 */
public class AcmeCA extends ConfigElement {

    private List<String> accountContact;

    private String accountKeyFile;

    private AcmeRevocationChecker acmeRevocationChecker;

    private AcmeTransportConfig acmeTransportConfig;

    private String challengePollTimeout; // Duration

    private String subjectDN;

    private String directoryURI;

    private List<String> domain;

    private String domainKeyFile;

    private String orderPollTimeout; // Duration

    private String validFor; // Duration

    private String renewBeforeExpiration;

    private String certCheckerSchedule;

    private String certCheckerErrorSchedule;

    private boolean disableMinRenewWindow;

    private boolean disableRenewOnNewHistory;

    private Long renewCertMin;

    /**
     * @return the accountContact
     */
    public List<String> getAccountContact() {
        return (accountContact == null) ? (accountContact = new ArrayList<String>()) : accountContact;
    }

    /**
     * @return the accountKeyFile
     */
    public String getAccountKeyFile() {
        return accountKeyFile;
    }

    /**
     * @return the acmeRevocationChecker
     */
    public AcmeRevocationChecker getAcmeRevocationChecker() {
        return acmeRevocationChecker;
    }

    /**
     * @return the acmeTransportConfig
     */
    public AcmeTransportConfig getAcmeTransportConfig() {
        return acmeTransportConfig;
    }

    /**
     * @return the challengePollTimeout
     */
    public String getChallengePoll() {
        return challengePollTimeout;
    }

    /**
     * @return the directoryURI
     */
    public String getDirectoryURI() {
        return directoryURI;
    }

    /**
     * @return the domainKeyFile
     */
    public String getDomainKeyFile() {
        return domainKeyFile;
    }

    /**
     * @return the domain
     */
    public List<String> getDomain() {
        return domain;
    }

    /**
     * @return the orderPollTimeout
     */
    public String getOrderPoll() {
        return orderPollTimeout;
    }

    /**
     * @return the subjectDN
     */
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     * @return the validFor
     */
    public String getValidFor() {
        return validFor;
    }

    /**
     * @param accountContact the accountContact to set
     */
    @XmlElement(name = "accountContact")
    public void setAccountContact(List<String> accountContact) {
        this.accountContact = accountContact;
    }

    /**
     * @param accountKeyFile the accountKeyFile to set
     */
    @XmlAttribute(name = "accountKeyFile")
    public void setAccountKeyFile(String accountKeyFile) {
        this.accountKeyFile = accountKeyFile;
    }

    /**
     * @param acmeRevocationChecker the acmeRevocationChecker to set
     */
    @XmlElement(name = "acmeRevocationChecker")
    public void setAcmeRevocationChecker(AcmeRevocationChecker acmeRevocationChecker) {
        this.acmeRevocationChecker = acmeRevocationChecker;
    }

    /**
     * @param acmeTransportConfig the acmeTransportConfig to set
     */
    @XmlElement(name = "acmeTransportConfig")
    public void setAcmeTransportConfig(AcmeTransportConfig acmeTransportConfig) {
        this.acmeTransportConfig = acmeTransportConfig;
    }

    /**
     * @param challengeRetryWait the challengeRetryWait to set
     */
    @XmlAttribute(name = "challengePollTimeout")
    public void setChallengePoll(String challengePollTimeout) {
        this.challengePollTimeout = challengePollTimeout;
    }

    /**
     * @param domainKeyFile the domainKeyFile to set
     */
    @XmlAttribute(name = "domainKeyFile")
    public void setDomainKeyFile(String domainKeyFile) {
        this.domainKeyFile = domainKeyFile;
    }

    /**
     * @param directoryURI the directoryURI to set
     */
    @XmlAttribute(name = "directoryURI")
    public void setDirectoryURI(String directoryURI) {
        this.directoryURI = directoryURI;
    }

    /**
     * @param domain the domain to set
     */
    @XmlElement(name = "domain")
    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    /**
     * @param orderPollTimeout the orderPollTimeout to set
     */
    @XmlAttribute(name = "orderPollTimeout")
    public void setOrderPoll(String orderPollTimeout) {
        this.orderPollTimeout = orderPollTimeout;
    }

    /**
     * @param subjectDN the subjectDN to set
     */
    @XmlAttribute(name = "subjectDN")
    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    /**
     * @param validFor the validFor to set
     */
    @XmlAttribute(name = "validFor")
    public void setValidFor(String validFor) {
        this.validFor = validFor;
    }

    /**
     * @return the renewBeforeExpiration
     */
    public String getRenewBeforeExpiration() {
        return renewBeforeExpiration;
    }

    /**
     * @param renewBeforeExpiration the renewBeforeExpiration to set
     */
    @XmlAttribute(name = "renewBeforeExpiration")
    public void setRenewBeforeExpiration(String renewBeforeExpiration) {
        this.renewBeforeExpiration = renewBeforeExpiration;
    }

    public String getCertCheckerSchedule() {
        return certCheckerSchedule;
    }

    @XmlAttribute(name = "certCheckerSchedule")
    public void setCertCheckerSchedule(String certCheckerSchedule) {
        this.certCheckerSchedule = certCheckerSchedule;
    }

    public String getCertCheckerErrorSchedule() {
        return certCheckerErrorSchedule;
    }

    @XmlAttribute(name = "certCheckerErrorSchedule")
    public void setCertCheckerErrorSchedule(String certCheckerErrorSchedule) {
        this.certCheckerErrorSchedule = certCheckerErrorSchedule;
    }

    @XmlAttribute(name = "disableMinRenewWindow")
    public void setDisableMinRenewWindow(boolean disableMinRenewWindow) {
        this.disableMinRenewWindow = disableMinRenewWindow;
    }

    public boolean isDisableMinRenewWindow() {
        return disableMinRenewWindow;
    }

    @XmlAttribute(name = "disableRenewOnNewHistory")
    public void setDisableRenewOnNewHistory(boolean disableRenewOnNewHistory) {
        this.disableRenewOnNewHistory = disableRenewOnNewHistory;
    }

    public boolean isDisableRenewOnNewHistory() {
        return disableRenewOnNewHistory;
    }

    @XmlAttribute(name = "renewCertMin")
    public void setRenewCertMin(long renewCertMin) {
        this.renewCertMin = renewCertMin;
    }

    public long getRenewCertMin() {
        return renewCertMin == null ? 15000L : renewCertMin;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (accountContact != null) {
            sb.append("accountContact=\"").append(accountContact).append("\" ");;
        }
        if (accountKeyFile != null) {
            sb.append("accountKeyFile=\"").append(accountKeyFile).append("\" ");;
        }
        if (acmeRevocationChecker != null) {
            sb.append("acmeRevocationChecker=\"").append(acmeRevocationChecker).append("\" ");;
        }
        if (acmeTransportConfig != null) {
            sb.append("acmeTransportConfig=\"").append(acmeTransportConfig).append("\" ");;
        }
        if (certCheckerSchedule != null) {
            sb.append("certCheckerSchedule=\"").append(certCheckerSchedule).append("\" ");;
        }
        if (certCheckerErrorSchedule != null) {
            sb.append("certCheckerErrorSchedule=\"").append(certCheckerErrorSchedule).append("\" ");;
        }
        if (challengePollTimeout != null) {
            sb.append("challengePollTimeout=\"").append(challengePollTimeout).append("\" ");;
        }
        if (directoryURI != null) {
            sb.append("directoryURI=\"").append(directoryURI).append("\" ");;
        }
        if (disableMinRenewWindow) {
            sb.append("disableMinRenewWindow=\"").append(disableMinRenewWindow).append("\" ");;
        }
        if (disableRenewOnNewHistory) {
            sb.append("disableRenewOnNewHistory=\"").append(disableRenewOnNewHistory).append("\" ");;
        }
        if (renewCertMin != null) {
            sb.append("renewCertMin=\"").append(renewCertMin).append("\" ");;
        }
        if (domainKeyFile != null) {
            sb.append("domainKeyFile=\"").append(domainKeyFile).append("\" ");;
        }
        if (domain != null) {
            sb.append("domain=\"").append(domain).append("\" ");;
        }
        if (orderPollTimeout != null) {
            sb.append("orderPollTimeout=\"").append(orderPollTimeout).append("\" ");;
        }
        if (renewBeforeExpiration != null) {
            sb.append("renewBeforeExpiration=\"").append(renewBeforeExpiration).append("\" ");;
        }
        if (subjectDN != null) {
            sb.append("subjectDN=\"").append(subjectDN).append("\" ");;
        }
        if (validFor != null) {
            sb.append("validFor=\"").append(validFor).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }

    /**
     * Transport configuration for the acmeCA-2.0 feature.
     */
    public static class AcmeTransportConfig {

        private String protocol;

        private String trustStore;

        private String trustStorePassword;

        private String trustStoreType;

        private String httpConnectTimeout; // duration

        private String httpReadTimeout; // duration

        /**
         * @return the protocol
         */
        public String getProtocol() {
            return protocol;
        }

        /**
         * @return the trustStore
         */
        public String getTrustStore() {
            return trustStore;
        }

        /**
         * @return the trustStorePassword
         */
        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        /**
         * @return the trustStoreType
         */
        public String getTrustStoreType() {
            return trustStoreType;
        }

        /**
         * @return the httpConnectTimeout
         */
        public String getHttpConnectTimeout() {
            return httpConnectTimeout;
        }

        /**
         * @return the httpReadTimeout
         */
        public String getHttpReadTimeout() {
            return httpReadTimeout;
        }

        /**
         * @param protocol the protocol to set
         */
        @XmlAttribute(name = "protocol")
        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        /**
         * @param trustStore the trustStore to set
         */
        @XmlAttribute(name = "trustStore")
        public void setTrustStore(String trustStore) {
            this.trustStore = trustStore;
        }

        /**
         * @param trustStorePassword the trustStorePassword to set
         */
        @XmlAttribute(name = "trustStorePassword")
        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        /**
         * @param trustStoreType the trustStoreType to set
         */
        @XmlAttribute(name = "trustStoreType")
        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        /**
         * @param httpConnectTimeout the httpConnectTimeout to set
         */
        @XmlAttribute(name = "httpConnectTimeout")
        public void setHttpConnectTimeout(String httpConnectTimeout) {
            this.httpConnectTimeout = httpConnectTimeout;
        }

        /**
         * @param httpReadTimeout the httpReadTimeout to set
         */
        @XmlAttribute(name = "httpReadTimeout")
        public void setHttpReadTimeout(String httpReadTimeout) {
            this.httpReadTimeout = httpReadTimeout;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();

            sb.append(getClass().getSimpleName()).append("{ ");

            if (protocol != null) {
                sb.append("protocol=\"").append(protocol).append("\" ");;
            }
            if (trustStore != null) {
                sb.append("trustStore=\"").append(trustStore).append("\" ");;
            }
            if (trustStorePassword != null) {
                sb.append("trustStorePassword=\"").append(trustStorePassword).append("\" ");;
            }
            if (trustStoreType != null) {
                sb.append("trustStoreType=\"").append(trustStoreType).append("\" ");;
            }
            if (httpConnectTimeout != null) {
                sb.append("httpConnectTimeout=\"").append(httpConnectTimeout).append("\" ");;
            }
            if (httpReadTimeout != null) {
                sb.append("httpReadTimeout=\"").append(httpReadTimeout).append("\" ");;
            }

            sb.append("}");

            return sb.toString();
        }
    }

    /**
     * ACME certificate revocation checker for the acmeCA-2.0 feature.
     */
    public static class AcmeRevocationChecker {

        private Boolean enabled;

        private String ocspResponderUrl;

        private Boolean preferCRLs;

        private Boolean disableFallback;

        /**
         * @return the ocspResponderUrl
         */
        public Boolean getEnabled() {
            return enabled;
        }

        /**
         * @return the ocspResponderUrl
         */
        public String getOcspResponderUrl() {
            return ocspResponderUrl;
        }

        /**
         * @return the preferCRLs
         */
        public Boolean getPreferCRLs() {
            return preferCRLs;
        }

        /**
         * @return the disableFallback
         */
        public Boolean getDisableFallback() {
            return disableFallback;
        }

        /**
         * @param enabled the enabled to set
         */
        @XmlAttribute(name = "enabled")
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @param ocspResponderUrl the ocspResponderUrl to set
         */
        @XmlAttribute(name = "ocspResponderUrl")
        public void setOcspResponderUrl(String ocspResponderUrl) {
            this.ocspResponderUrl = ocspResponderUrl;
        }

        /**
         * @param preferCRLs the preferCRLs to set
         */
        @XmlAttribute(name = "preferCRLs")
        public void setPreferCRLs(Boolean preferCRLs) {
            this.preferCRLs = preferCRLs;
        }

        /**
         * @param disableFallback the disableFallback to set
         */
        @XmlAttribute(name = "disableFallback")
        public void setDisableFallback(Boolean disableFallback) {
            this.disableFallback = disableFallback;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();

            sb.append(getClass().getSimpleName()).append("{ ");

            if (enabled != null) {
                sb.append("enabled=\"").append(enabled).append("\" ");;
            }
            if (disableFallback != null) {
                sb.append("disableFallback=\"").append(disableFallback).append("\" ");;
            }
            if (ocspResponderUrl != null) {
                sb.append("ocspResponderUrl=\"").append(ocspResponderUrl).append("\" ");;
            }
            if (preferCRLs != null) {
                sb.append("preferCRLs=\"").append(preferCRLs).append("\" ");;
            }

            sb.append("}");

            return sb.toString();
        }
    }
};
