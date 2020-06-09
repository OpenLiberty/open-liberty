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

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>ldapRegistry</li>
 * </ul>
 */
public class LdapRegistry extends ConfigElement {

    private LdapFilters activedFilters;
    private AttributeConfiguration attributeConfiguration;
    private String baseDN;
    private String bindDN;
    private String bindPassword;
    private String certificateFilter;
    private String certificateMapMode;
    private String connectTimeout;
    private String readTimeout;
    private ContextPool contextPool;
    private LdapFilters customFilters;
    private LdapFilters domino50Filters;
    private LdapFilters edirectoryFilters;
    private ConfigElementList<FailoverServers> failoverServers;
    private GroupProperties groupProperties;
    private String host;
    private LdapFilters idsFilters;
    private Boolean ignoreCase;
    private LdapFilters iplanetFilters;
    private Boolean jndiOutputEnabled;
    private LdapCache ldapCache;
    private ConfigElementList<LdapEntityType> ldapEntityTypes;
    private String ldapType;
    private String name; // PRIVATE
    private LdapFilters netscapeFilters;
    private String port; // Integer in metatype, but need to support properties.
    private Integer primaryServerQueryTimeInterval;
    private String realm;
    private Boolean recursiveSearch;
    private String referal; // PRIVATE
    private String referral;
    private ConfigElementList<BaseEntry> registryBaseEntries;
    private Boolean returnToPrimaryServer;
    private Boolean reuseConnection;
    private String searchTimeout;
    private LdapFilters securewayFilters;
    private Boolean sslEnabled;
    private String sslRef;
    private Integer searchPageSize; // PRIVATE
    private String certificateMapperId;
    private String timestampFormat;

    /**
     * @return the activedFilters
     */
    public LdapFilters getActivedFilters() {
        return activedFilters;
    }

    /**
     * @return the attributeConfiguration
     */
    public AttributeConfiguration getAttributeConfiguration() {
        return attributeConfiguration;
    }

    /**
     * @return the baseDN
     */
    public String getBaseDN() {
        return baseDN;
    }

    /**
     * @return the bindDN
     */
    public String getBindDN() {
        return bindDN;
    }

    /**
     * @return the bindPassword
     */
    public String getBindPassword() {
        return bindPassword;
    }

    /**
     * @return the certificateFilter
     */
    public String getCertificateFilter() {
        return certificateFilter;
    }

    /**
     * @return the certificateMapMode
     */
    public String getCertificateMapMode() {
        return certificateMapMode;
    }

    /**
     * @return the certificateMapperId
     */
    public String getCertificateMapperId() {
        return certificateMapperId;
    }

    /**
     * @return the connectTimeout
     */
    public String getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @return the readTimeout
     */
    public String getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return the contextPool
     */
    public ContextPool getContextPool() {
        return contextPool;
    }

    /**
     * @return the customFilters
     */
    public LdapFilters getCustomFilters() {
        return customFilters;
    }

    /**
     * @return the domino50Filters
     */
    public LdapFilters getDomino50Filters() {
        return domino50Filters;
    }

    /**
     * @return the edirectoryFilters
     */
    public LdapFilters getEdirectoryFilters() {
        return edirectoryFilters;
    }

    /**
     * @return the failoverServers
     */
    public ConfigElementList<FailoverServers> getFailoverServers() {
        return (failoverServers == null) ? (failoverServers = new ConfigElementList<FailoverServers>()) : failoverServers;
    }

    /**
     * @return the groupProperties
     */
    public GroupProperties getGroupProperties() {
        return groupProperties;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the idsFilters
     */
    public LdapFilters getIdsFilters() {
        return idsFilters;
    }

    /**
     * @return the ignoreCase
     */
    public Boolean getIgnoreCase() {
        return ignoreCase;
    }

    /**
     * @return the iplanetFilters
     */
    public LdapFilters getIplanetFilters() {
        return iplanetFilters;
    }

    /**
     * @return the jndiOutputEnabled
     */
    public Boolean getJndiOutputEnabled() {
        return jndiOutputEnabled;
    }

    /**
     * @return the ldapCache
     */
    public LdapCache getLdapCache() {
        return ldapCache;
    }

    /**
     * @return the ldapEntityTypes
     */
    public ConfigElementList<LdapEntityType> getLdapEntityTypes() {
        return (ldapEntityTypes == null) ? (ldapEntityTypes = new ConfigElementList<LdapEntityType>()) : ldapEntityTypes;
    }

    /**
     * @return the ldapType
     */
    public String getLdapType() {
        return ldapType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the netscapeFilters
     */
    public LdapFilters getNetscapeFilters() {
        return netscapeFilters;
    }

    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @return the primaryServerQueryTimeInterval
     */
    public Integer getPrimaryServerQueryTimeInterval() {
        return primaryServerQueryTimeInterval;
    }

    /**
     * @return the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @return the recursiveSearch
     */
    public Boolean getRecursiveSearch() {
        return recursiveSearch;
    }

    /**
     * @return the referal
     */
    public String getReferal() {
        return referal;
    }

    /**
     * @return the referral
     */
    public String getReferral() {
        return referral;
    }

    /**
     * @return the registryBaseEntry
     */
    public ConfigElementList<BaseEntry> getRegistryBaseEntries() {
        return (registryBaseEntries == null) ? (registryBaseEntries = new ConfigElementList<BaseEntry>()) : registryBaseEntries;
    }

    /**
     * @return the returnToPrimaryServer
     */
    public Boolean getReturnToPrimaryServer() {
        return returnToPrimaryServer;
    }

    /**
     * @return the reuseConnection
     */
    public Boolean getReuseConnection() {
        return reuseConnection;
    }

    /**
     * @return the searchPageSize
     */
    public Integer getSearchPageSize() {
        return searchPageSize;
    }

    /**
     * @return the searchTimeout
     */
    public String getSearchTimeout() {
        return searchTimeout;
    }

    /**
     * @return the securewayFilters
     */
    public LdapFilters getSecurewayFilters() {
        return securewayFilters;
    }

    /**
     * @return the sslEnabled
     */
    public Boolean getSslEnabled() {
        return sslEnabled;
    }

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @return the timestampFormat
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }

    /**
     * @param activedFilters the activedFilters to set
     */
    @XmlElement(name = "activedFilters")
    public void setActivedFilters(LdapFilters activedFilters) {
        this.activedFilters = activedFilters;
    }

    /**
     * @param attributeConfiguration the attributeConfiguration to set
     */
    @XmlElement(name = "attributeConfiguration")
    public void setAttributeConfiguration(AttributeConfiguration attributeConfiguration) {
        this.attributeConfiguration = attributeConfiguration;
    }

    /**
     * @param baseDN the baseDN to set
     */
    @XmlAttribute(name = "baseDN")
    public void setBaseDN(String baseDN) {
        this.baseDN = baseDN;
    }

    /**
     * @param bindDN the bindDN to set
     */
    @XmlAttribute(name = "bindDN")
    public void setBindDN(String bindDN) {
        this.bindDN = bindDN;
    }

    /**
     * @param bindPassword the bindPassword to set
     */
    @XmlAttribute(name = "bindPassword")
    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    /**
     * @param certificateFilter the certificateFilter to set
     */
    @XmlAttribute(name = "certificateFilter")
    public void setCertificateFilter(String certificateFilter) {
        this.certificateFilter = certificateFilter;
    }

    /**
     * @param certificateMapMode the certificateMapMode to set
     */
    @XmlAttribute(name = "certificateMapMode")
    public void setCertificateMapMode(String certificateMapMode) {
        this.certificateMapMode = certificateMapMode;
    }

    /**
     * @param certificateMapperId the certificateMapperId to set
     */
    @XmlAttribute(name = "certificateMapperId")
    public void setCertificateMapperId(String certificateMapperId) {
        this.certificateMapperId = certificateMapperId;
    }

    /**
     * @param connectTimeout the connectTimeout to set
     */
    @XmlAttribute(name = "")
    public void setConnectTimeout(String connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * @param connectTimeout the connectTimeout to set
     */
    @XmlAttribute(name = "readTimeout")
    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * @param contextPool the contextPool to set
     */
    @XmlElement(name = "contextPool")
    public void setContextPool(ContextPool contextPool) {
        this.contextPool = contextPool;
    }

    /**
     * @param customFilters the customFilters to set
     */
    @XmlElement(name = "customFilters")
    public void setCustomFilters(LdapFilters customFilters) {
        this.customFilters = customFilters;
    }

    /**
     * @param domino50Filters the domino50Filters to set
     */
    @XmlElement(name = "domino50Filters")
    public void setDomino50Filters(LdapFilters domino50Filters) {
        this.domino50Filters = domino50Filters;
    }

    /**
     * @param edirectoryFilters the edirectoryFilters to set
     */
    @XmlElement(name = "edirectoryFilters")
    public void setEdirectoryFilters(LdapFilters edirectoryFilters) {
        this.edirectoryFilters = edirectoryFilters;
    }

    /**
     * @param failoverServers the failoverServers to set
     */
    @XmlElement(name = "failoverServers")
    public void setFailoverServers(ConfigElementList<FailoverServers> failoverServers) {
        this.failoverServers = failoverServers;
    }

    /**
     * Convenience method to set the the list of failover servers list to a single entry.
     *
     * @param failoverServers The single instance of {@link FailoverServers} to set.
     */
    public void setFailoverServer(FailoverServers failoverServers) {
        this.failoverServers = new ConfigElementList<FailoverServers>();
        this.failoverServers.add(failoverServers);
    }

    /**
     * @param groupProperties the groupProperties to set
     */
    @XmlElement(name = "groupProperties")
    public void setGroupProperties(GroupProperties groupProperties) {
        this.groupProperties = groupProperties;
    }

    /**
     * @param host the host to set
     */
    @XmlAttribute(name = "host")
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @param idsFilters the idsFilters to set
     */
    @XmlElement(name = "idsFilters")
    public void setIdsFilters(LdapFilters idsFilters) {
        this.idsFilters = idsFilters;
    }

    /**
     * @param ignoreCase the ignoreCase to set
     */
    @XmlAttribute(name = "ignoreCase")
    public void setIgnoreCase(Boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * @param iplanetFilters the iplanetFilters to set
     */
    @XmlElement(name = "iplanetFilters")
    public void setIplanetFilters(LdapFilters iplanetFilters) {
        this.iplanetFilters = iplanetFilters;
    }

    /**
     * @param jndiOutputEnabled the jndiOutputEnabled to set
     */
    @XmlAttribute(name = "jndiOutputEnabled")
    public void setJndiOutputEnabled(Boolean jndiOutputEnabled) {
        this.jndiOutputEnabled = jndiOutputEnabled;
    }

    /**
     * @param ldapCache the ldapCache to set
     */
    @XmlElement(name = "ldapCache")
    public void setLdapCache(LdapCache ldapCache) {
        this.ldapCache = ldapCache;
    }

    /**
     * @param ldapEntityType the ldapEntityTypes to set
     */
    @XmlElement(name = "ldapEntityType")
    public void setLdapEntityTypes(ConfigElementList<LdapEntityType> ldapEntityTypes) {
        this.ldapEntityTypes = ldapEntityTypes;
    }

    /**
     * @param ldapType the ldapType to set
     */
    @XmlAttribute(name = "ldapType")
    public void setLdapType(String ldapType) {
        this.ldapType = ldapType;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param netscapeFilters the netscapeFilters to set
     */
    @XmlElement(name = "netscapeFilters")
    public void setNetscapeFilters(LdapFilters netscapeFilters) {
        this.netscapeFilters = netscapeFilters;
    }

    /**
     * @param port the port to set
     */
    @XmlAttribute(name = "port")
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @param primaryServerQueryTimeInterval the primaryServerQueryTimeInterval to set
     */
    @XmlAttribute(name = "primaryServerQueryTimeInterval")
    public void setPrimaryServerQueryTimeInterval(Integer primaryServerQueryTimeInterval) {
        this.primaryServerQueryTimeInterval = primaryServerQueryTimeInterval;
    }

    /**
     * @param realm the realm to set
     */
    @XmlAttribute(name = "realm")
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * @param recursiveSearch the recursiveSearch to set
     */
    @XmlAttribute(name = "recursiveSearch")
    public void setRecursiveSearch(Boolean recursiveSearch) {
        this.recursiveSearch = recursiveSearch;
    }

    /**
     * @param referal the referal to set
     */
    @XmlAttribute(name = "referal")
    public void setReferal(String referal) {
        this.referal = referal;
    }

    /**
     * @param referral the referral to set
     */
    @XmlAttribute(name = "referral")
    public void setReferral(String referral) {
        this.referral = referral;
    }

    /**
     * @param registryBaseEntries the registryBaseEntries to set
     */
    @XmlElement(name = "registryBaseEntry")
    public void setRegistryBaseEntries(ConfigElementList<BaseEntry> registryBaseEntries) {
        this.registryBaseEntries = registryBaseEntries;
    }

    /**
     * @param returnToPrimaryServer the returnToPrimaryServer to set
     */
    @XmlAttribute(name = "returnToPrimaryServer")
    public void setReturnToPrimaryServer(Boolean returnToPrimaryServer) {
        this.returnToPrimaryServer = returnToPrimaryServer;
    }

    /**
     * @param reuseConnection the reuseConnection to set
     */
    @XmlAttribute(name = "reuseConnection")
    public void setReuseConnection(Boolean reuseConnection) {
        this.reuseConnection = reuseConnection;
    }

    /**
     * @param searchPageSize the searchPageSize to set
     */
    @XmlAttribute(name = "searchPageSize")
    public void setSearchPageSize(Integer searchPageSize) {
        this.searchPageSize = searchPageSize;
    }

    /**
     * @param searchTimeout the searchTimeout to set
     */
    @XmlAttribute(name = "searchTimeout")
    public void setSearchTimeout(String searchTimeout) {
        this.searchTimeout = searchTimeout;
    }

    /**
     * @param securewayFilters the securewayFilters to set
     */
    @XmlElement(name = "securewayFilters")
    public void setSecurewayFilters(LdapFilters securewayFilters) {
        this.securewayFilters = securewayFilters;
    }

    /**
     * @param sslEnabled the sslEnabled to set
     */
    @XmlAttribute(name = "sslEnabled")
    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute(name = "sslRef")
    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @param timestampFormat the timestampFormat to set
     */
    @XmlAttribute(name = "timestampFormat")
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (activedFilters != null) {
            sb.append("activedFilters=\"").append(activedFilters).append("\" ");;
        }
        if (attributeConfiguration != null) {
            sb.append("attributeConfiguration=\"").append(attributeConfiguration).append("\" ");;
        }
        if (baseDN != null) {
            sb.append("baseDN=\"").append(baseDN).append("\" ");;
        }
        if (bindDN != null) {
            sb.append("bindDN=\"").append(bindDN).append("\" ");;
        }
        if (bindPassword != null) {
            sb.append("bindPassword=\"").append(bindPassword).append("\" ");;
        }
        if (certificateFilter != null) {
            sb.append("certificateFilter=\"").append(certificateFilter).append("\" ");;
        }
        if (certificateMapMode != null) {
            sb.append("certificateMapMode=\"").append(certificateMapMode).append("\" ");;
        }
        if (certificateMapperId != null) {
            sb.append("certificateMapperId=\"").append(certificateMapperId).append("\" ");;
        }
        if (connectTimeout != null) {
            sb.append("connectTimeout=\"").append(connectTimeout).append("\" ");;
        }
        if (readTimeout != null) {
            sb.append("readTimeout=\"").append(readTimeout).append("\" ");;
        }
        if (contextPool != null) {
            sb.append("contextPool=\"").append(contextPool).append("\" ");;
        }
        if (customFilters != null) {
            sb.append("customFilters=\"").append(customFilters).append("\" ");;
        }
        if (domino50Filters != null) {
            sb.append("domino50Filters=\"").append(domino50Filters).append("\" ");;
        }
        if (edirectoryFilters != null) {
            sb.append("edirectoryFilters=\"").append(edirectoryFilters).append("\" ");;
        }
        if (failoverServers != null) {
            sb.append("failoverServers=\"").append(failoverServers).append("\" ");;
        }
        if (groupProperties != null) {
            sb.append("groupConfiguration=\"").append(groupProperties).append("\" ");;
        }
        if (host != null) {
            sb.append("host=\"").append(host).append("\" ");;
        }
        if (idsFilters != null) {
            sb.append("idsFilters=\"").append(idsFilters).append("\" ");;
        }
        if (ignoreCase != null) {
            sb.append("ignoreCase=\"").append(ignoreCase).append("\" ");;
        }
        if (iplanetFilters != null) {
            sb.append("iplanetFilters=\"").append(iplanetFilters).append("\" ");;
        }
        if (jndiOutputEnabled != null) {
            sb.append("jndiOutputEnabled=\"").append(jndiOutputEnabled).append("\" ");;
        }
        if (ldapCache != null) {
            sb.append("ldapCache=\"").append(ldapCache).append("\" ");;
        }
        if (ldapEntityTypes != null) {
            sb.append("ldapEntityTypes=\"").append(ldapEntityTypes).append("\" ");;
        }
        if (ldapType != null) {
            sb.append("ldapType=\"").append(ldapType).append("\" ");;
        }
        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (netscapeFilters != null) {
            sb.append("netscapeFilters=\"").append(netscapeFilters).append("\" ");;
        }
        if (port != null) {
            sb.append("port=\"").append(port).append("\" ");;
        }
        if (primaryServerQueryTimeInterval != null) {
            sb.append("primaryServerQueryTimeInterval=\"").append(primaryServerQueryTimeInterval).append("\" ");;
        }
        if (realm != null) {
            sb.append("realm=\"").append(realm).append("\" ");;
        }
        if (recursiveSearch != null) {
            sb.append("recursiveSearch=\"").append(recursiveSearch).append("\" ");;
        }
        if (referal != null) {
            sb.append("referal=\"").append(referal).append("\" ");;
        }
        if (referral != null) {
            sb.append("referral=\"").append(referral).append("\" ");;
        }
        if (registryBaseEntries != null) {
            sb.append("registryBaseEntries=\"").append(registryBaseEntries).append("\" ");;
        }
        if (returnToPrimaryServer != null) {
            sb.append("returnToPrimaryServer").append(returnToPrimaryServer).append("\" ");;
        }
        if (reuseConnection != null) {
            sb.append("reuseConnection=\"").append(reuseConnection).append("\" ");;
        }
        if (searchTimeout != null) {
            sb.append("searchTimeout=\"").append(searchTimeout).append("\" ");;
        }
        if (securewayFilters != null) {
            sb.append("securewayFilters=\"").append(securewayFilters).append("\" ");;
        }
        if (sslEnabled != null) {
            sb.append("sslEnabled=\"").append(sslEnabled).append("\" ");;
        }
        if (sslRef != null) {
            sb.append("sslRef=\"").append(sslRef).append("\" ");;
        }
        if (timestampFormat != null) {
            sb.append("timestampFormat=\"").append(timestampFormat).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
