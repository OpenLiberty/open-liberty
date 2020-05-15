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

    private Integer challengeRetries;

    private String challengeRetryWait; // Duration

    private String subjectDN;

    private String directoryURI;

    private List<String> domain;

    private String domainKeyFile;

    private Integer orderRetries;

    private String orderRetryWait; // Duration

    private String validFor; // Duration

    private String renewBeforeExpiration;

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
     * @return the challengeRetries
     */
    public Integer getChallengeRetries() {
        return challengeRetries;
    }

    /**
     * @return the challengeRetryWait
     */
    public String getChallengeRetryWait() {
        return challengeRetryWait;
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
     * @return the orderRetries
     */
    public Integer getOrderRetries() {
        return orderRetries;
    }

    /**
     * @return the orderRetryWait
     */
    public String getOrderRetryWait() {
        return orderRetryWait;
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
     * @param challengeRetries the challengeRetries to set
     */
    @XmlAttribute(name = "challengeRetries")
    public void setChallengeRetries(Integer challengeRetries) {
        this.challengeRetries = challengeRetries;
    }

    /**
     * @param challengeRetryWait the challengeRetryWait to set
     */
    @XmlAttribute(name = "challengeRetryWait")
    public void setChallengeRetryWait(String challengeRetryWait) {
        this.challengeRetryWait = challengeRetryWait;
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
     * @param orderRetries the orderRetries to set
     */
    @XmlAttribute(name = "orderRetries")
    public void setOrderRetries(Integer orderRetries) {
        this.orderRetries = orderRetries;
    }

    /**
     * @param orderRetryWait the orderRetryWait to set
     */
    @XmlAttribute(name = "orderRetryWait")
    public void setOrderRetryWait(String orderRetryWait) {
        this.orderRetryWait = orderRetryWait;
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
        if (challengeRetries != null) {
            sb.append("challengeRetries=\"").append(challengeRetries).append("\" ");;
        }
        if (challengeRetryWait != null) {
            sb.append("challengeRetryWait=\"").append(challengeRetryWait).append("\" ");;
        }
        if (directoryURI != null) {
            sb.append("directoryURI=\"").append(directoryURI).append("\" ");;
        }
        if (domainKeyFile != null) {
            sb.append("domainKeyFile=\"").append(domainKeyFile).append("\" ");;
        }
        if (domain != null) {
            sb.append("domain=\"").append(domain).append("\" ");;
        }
        if (orderRetries != null) {
            sb.append("orderRetries=\"").append(orderRetries).append("\" ");;
        }
        if (orderRetryWait != null) {
            sb.append("orderRetryWait=\"").append(orderRetryWait).append("\" ");;
        }
        if (subjectDN != null) {
            sb.append("subjectDN=\"").append(subjectDN).append("\" ");;
        }
        if (validFor != null) {
            sb.append("validFor=\"").append(validFor).append("\" ");;
        }
        if (renewBeforeExpiration != null) {
            sb.append("renewBeforeExpiration=\"").append(renewBeforeExpiration).append("\" ");;
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
