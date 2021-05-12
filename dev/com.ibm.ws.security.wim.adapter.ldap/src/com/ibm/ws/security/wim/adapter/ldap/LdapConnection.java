/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import static com.ibm.websphere.security.wim.ConfigConstants.ATTRIBUTES_CACHE_CONFIG;
import static com.ibm.websphere.security.wim.ConfigConstants.CACHE_CONFIG;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_CONTEXT_POOL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_IGNORE_CASE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_ALLOW_WRITE_TO_SECONDARY_SERVERS;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_ATTRIBUTE_RANGE_STEP;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_ATTRIBUTE_SIZE_LIMIT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_BIND_DN;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_BIND_PASSWORD;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_CACHE_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_CACHE_TIME_OUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_CONNECT_TIMEOUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_DEREFALIASES;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_ENABLED;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_HOST;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_INIT_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_JNDI_OUTPUT_ENABLED;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_MAX_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_POOL_TIME_OUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_POOL_WAIT_TIME;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PORT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PREF_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_READ_TIMEOUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_REFERAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_REFERRAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_RETURN_TO_PRIMARY_SERVER;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_COUNT_LIMIT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_PAGE_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_RESULTS_SIZE_LIMIT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_TIME_OUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SSL_ENABLED;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_REUSE_CONNECTION;
import static com.ibm.websphere.security.wim.ConfigConstants.SEARCH_CACHE_CONFIG;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.kerberos.auth.KerberosService;
import com.ibm.ws.security.wim.FactoryManager;
import com.ibm.ws.security.wim.adapter.ldap.context.ContextManager;
import com.ibm.ws.security.wim.adapter.ldap.context.ContextManager.InitializeResult;
import com.ibm.ws.security.wim.adapter.ldap.context.TimedDirContext;
import com.ibm.ws.security.wim.env.ICacheUtil;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.MissingInitPropertyException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.IdentifierType;

@SuppressWarnings("restriction")
public class LdapConnection {

    /** Configuration key for an individual fail-over server. */
    private static final String SERVER2 = "server";

    /** Configuration key for all fail-over servers. */
    private static final String FAILOVER_SERVERS = "failoverServers";

    /** Register the class to trace service. */
    private final static TraceComponent tc = Tr.register(LdapConnection.class);

    /** The ID for the Ldap Repository. */
    private String iReposId = null;

    /** The key to extract the id of the configuration. The value returned by this is used as the repository id. */
    private static final String KEY_ID = "config.id";

    /** Flag to determine if the search results cache is enabled. */
    private boolean iSearchResultsCacheEnabled = true;

    /** Name of the search results cache. */
    private String iSearchResultsCacheName = "SearchResultsCache";

    /** The search results cache */
    private ICacheUtil iSearchResultsCache = null;

    /** Default search results cache size */
    private int iSearchResultsCacheSize = 2000;

    /**
     * Default search results cache timeout in milliseconds
     * If you see the history, this was changed from 1200 to 1200000
     * to correctly be milliseconds. WAS used 1200 seconds to setup
     * the cache, but Liberty cache impl is set directly in ms.
     * PI81954
     */
    private long iSearchResultsCacheTimeOut = 1200000;

    /** default search results cache size limit */
    private int iSearchResultSizeLmit = 2000;

    /** Flag to determine if the attributes cache is enabled. */
    private boolean iAttrsCacheEnabled = true;

    /** Name of the attributes cache. */
    private String iAttrsCacheName = "AttributesCache";

    /** The attributes cache */
    private ICacheUtil iAttrsCache = null;

    /** Default attributes cache size */
    private int iAttrsCacheSize = 4000;

    /**
     * Default attributes cache timeout in milliseconds
     * If you see the history, this was changed from 1200 to 1200000
     * to correctly be milliseconds. WAS used 1200 seconds to setup
     * the cache, but Liberty cache impl is set directly in ms.
     * PI81954
     */
    private long iAttrsCacheTimeOut = 1200000;

    /** default attributes cache size limit */
    private int iAttrsSizeLmit = 2000;

    /** The search result count limit. This is initialized as part of server initialization. */
    private int iCountLimit = 1000;

    /** The search result time limit. This is initialized as part of server initialization. */
    private int iTimeLimit = 60000;

    /** Instance of the Ldap Configuration manager attained from the Ldap Adapter. */
    private LdapConfigManager iLdapConfigMgr = null;

    /** What is the step size while retrieving range attributes. */
    private int iAttrRangeStep;

    /** Keyword to look for while retrieving range attributes. */
    private final static String ATTR_RANGE_KEYWORD = ";range=";

    /** Constant for Ldap query while retrieving range attributes. */
    private final static String ATTR_RANGE_QUERY = ";range={0}-{1}";

    /** Constant for Ldap query while retrieving the last step of range attributes. */
    private final static String ATTR_RANGE_LAST_QUERY = ";range={0}-*";

    /** The name of LDAP Distinguished Name. */
    private static final String LDAP_DN = "distinguishedName";

    /** Search Page size. Default to 0 */
    private int iPageSize = 0;

    /** The name parser */
    private NameParser iNameParser = null;

    /** Reference to the SSL Socket factory to be used. */
    protected String iSSLFactory = null;

    /** Ignore case for DN in attribute cache. */
    private boolean ignoreDNCase = true;

    /** ContextManager to use for managing LDAP contexts. */
    private ContextManager iContextManager;

    /** The KerberosService for use when bindAuthMechanism is GSSAPI (Kerberos), also loads the keytab/config if configured in the <kerberos> element */
    private KerberosService kerberosService = null;

    private ConfigurationAdmin configAdmin = null;

    /**
     * Returns a hash key for the name|filter|cons tuple used in the search
     * query-results cache.
     *
     * @param name The name of the object from which to retrieve attributes.
     * @param filter the filter used in the search.
     * @param cons The search controls used in the search.
     * @throws NamingException If a naming exception is encountered.
     */
    @Trivial
    private static String toKey(String name, String filter, SearchControls cons) {
        int length = name.length() + filter.length() + 100;
        StringBuffer key = new StringBuffer(length);
        key.append(name);
        key.append("|");
        key.append(filter);
        key.append("|");
        key.append(cons.getSearchScope());
        key.append("|");
        key.append(cons.getCountLimit());
        key.append("|");
        key.append(cons.getTimeLimit());

        String[] attrIds = cons.getReturningAttributes();
        if (attrIds != null) {
            for (int i = 0; i < attrIds.length; i++) {
                key.append("|");
                key.append(attrIds[i]);
            }
        }
        return key.toString();
    }

    /**
     * Returns a hash key for the name|filterExpr|filterArgs|cons tuple used in the search
     * query-results cache.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression used in the search.
     * @param filterArgs the filter arguments used in the search.
     * @param cons The search controls used in the search.
     * @throws NamingException If a naming exception is encountered.
     */
    @Trivial
    private static String toKey(String name, String filterExpr, Object[] filterArgs, SearchControls cons) {
        int length = name.length() + filterExpr.length() + filterArgs.length + 200;
        StringBuffer key = new StringBuffer(length);
        key.append(name);
        key.append("|");
        key.append(filterExpr);
        String[] attrIds = cons.getReturningAttributes();
        for (int i = 0; i < filterArgs.length; i++) {
            key.append("|");
            key.append(filterArgs[i]);
        }
        if (attrIds != null) {
            for (int i = 0; i < attrIds.length; i++) {
                key.append("|");
                key.append(attrIds[i]);
            }
        }
        return key.toString();
    }

    /**
     * Retrieves the parser associated with the root context.
     *
     * @return The {@link NameParser}.
     * @throws WIMException If the {@link NameParser} could not be queried from the LDAP server.
     */
    public NameParser getNameParser() throws WIMException {
        if (iNameParser == null) {
            TimedDirContext ctx = iContextManager.getDirContext();
            try {
                try {
                    iNameParser = ctx.getNameParser("");
                } catch (NamingException e) {
                    if (!ContextManager.isConnectionException(e)) {
                        throw e;
                    }
                    ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                    iNameParser = ctx.getNameParser("");
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)), e);
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            } finally {
                iContextManager.releaseDirContext(ctx);
            }
        }
        return iNameParser;
    }

    /**
     * Construct a new {@link LdapConnection} instance.
     *
     * @param ldapConfigMgr The {@link LdapConfigManager} to get configuration from.
     * @param ks            The {@link KerberosService} to get KRB5 configuration from (can be null).
     */
    public LdapConnection(LdapConfigManager ldapConfigMgr, KerberosService ks, ConfigurationAdmin configAdminRef) {
        iLdapConfigMgr = ldapConfigMgr;
        kerberosService = ks;
        configAdmin = configAdminRef;
    }

    /**
     * Initialize the {@link LdapConnection} instance.
     *
     * @param configProps Configuration properties to take configuration from.
     * @throws WIMException If initialization failed.
     */
    public void initialize(Map<String, Object> configProps) throws WIMException {

        iReposId = (String) configProps.get(KEY_ID);

        /*
         * Set ldapTimeout
         */
        if (configProps.containsKey(CONFIG_PROP_SEARCH_TIME_OUT)) {
            long val = Long.parseLong(String.valueOf(configProps.get(CONFIG_PROP_SEARCH_TIME_OUT)));
            iTimeLimit = (int) val;
        }

        /*
         * Set ldapCountLimit
         */
        if (configProps.containsKey(CONFIG_PROP_SEARCH_COUNT_LIMIT)) {
            iCountLimit = (int) configProps.get(CONFIG_PROP_SEARCH_COUNT_LIMIT);
        }

        /*
         * Set search page size
         */
        if (configProps.containsKey(CONFIG_PROP_SEARCH_PAGE_SIZE)) {
            iPageSize = (int) configProps.get(CONFIG_PROP_SEARCH_PAGE_SIZE);
        } else {
            if (LdapConstants.AD_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())) {
                iPageSize = 1000;
            }
        }

        /*
         * Set attribute range step
         */
        if (configProps.containsKey(CONFIG_PROP_ATTRIBUTE_RANGE_STEP)) {
            iAttrRangeStep = Integer.parseInt((String) configProps.get(CONFIG_PROP_ATTRIBUTE_RANGE_STEP));
        } else {
            if (LdapConstants.AD_LDAP_SERVER.equalsIgnoreCase(iLdapConfigMgr.getLdapType())) {
                iAttrRangeStep = 1000;
            }
        }

        /*
         * Ignore case.
         */
        ignoreDNCase = (Boolean) configProps.get(CONFIG_IGNORE_CASE);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Set ignoreDNCase as [" + ignoreDNCase + "]");

        /*
         * Initialize the ContextManager and the caches.
         */
        initializeContextManager(configProps);
        initializeCaches(configProps);
    }

    /**
     * Initialize the {@link ContextManager} to manage LDAP connections.
     *
     * @param configProps Configuration properties for the component.
     * @throws WIMException If there was an error initializing the {@link ContextManager}.
     */
    private void initializeContextManager(Map<String, Object> configProps) throws WIMException {

        iContextManager = new ContextManager();

        /*
         * Set SSL configuration
         */
        iContextManager.setSSLAlias((String) configProps.get("sslRef"));
        iContextManager.setSSLEnabled((Boolean) configProps.get(CONFIG_PROP_SSL_ENABLED));

        /*
         * Set the primary server.
         */
        iContextManager.setPrimaryServer((String) configProps.get(CONFIG_PROP_HOST), (Integer) configProps.get(CONFIG_PROP_PORT));

        /*
         * Set fail-over servers and fail-over behavior.
         */
        for (Map<String, Object> serverConfig : Nester.nest(FAILOVER_SERVERS, configProps)) {
            List<Map<String, Object>> servers = Nester.nest(SERVER2, serverConfig);
            for (Map<String, Object> server : servers) {
                iContextManager.addFailoverServer((String) server.get(CONFIG_PROP_HOST), (Integer) server.get(CONFIG_PROP_PORT));
            }
        }
        iContextManager.setWriteToSecondary((Boolean) configProps.get(CONFIG_PROP_ALLOW_WRITE_TO_SECONDARY_SERVERS));
        iContextManager.setReturnToPrimary((Boolean) configProps.get(CONFIG_PROP_RETURN_TO_PRIMARY_SERVER));
        iContextManager.setQueryInterval((Integer) configProps.get(CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL) * 60);

        String bindAuthMech = ((String) configProps.get(ConfigConstants.CONFIG_PROP_BIND_AUTH_MECH));
        if (bindAuthMech != null) {
            iContextManager.setBindAuthMechanism(bindAuthMech);
        }

        if (bindAuthMech == null || !bindAuthMech.equals(ConfigConstants.CONFIG_BIND_AUTH_KRB5)) {
            /*
             * Set the simple authentication credentials.
             */
            iContextManager.setSimpleCredentials((String) configProps.get(CONFIG_PROP_BIND_DN), (SerializableProtectedString) configProps.get(CONFIG_PROP_BIND_PASSWORD));
        } else {
            iContextManager.setKerberosCredentials(iReposId, kerberosService, (String) configProps.get(ConfigConstants.CONFIG_PROP_KRB5_PRINCIPAL),
                                                   (String) configProps.get(ConfigConstants.CONFIG_PROP_KRB5_TICKET_CACHE), configAdmin);
        }

        /*
         * Set the connection timeout.
         */
        iContextManager.setConnectTimeout((Long) configProps.get(CONFIG_PROP_CONNECT_TIMEOUT));

        /*
         * Set the connection timeout.
         */
        iContextManager.setReadTimeout((Long) configProps.get(CONFIG_PROP_READ_TIMEOUT));

        /*
         * Set JNDI packet output to system out
         */
        iContextManager.setJndiOutputEnabled((Boolean) configProps.get(CONFIG_PROP_JNDI_OUTPUT_ENABLED));

        /*
         * Determine referral handling behavior. Initially the attribute was spelled missing an 'r' so
         * for backwards compatibility, support customers who might still be using it. The "referal"
         * attribute has no default so unless it is set we won't use it.
         */
        String referal = (String) configProps.get(CONFIG_PROP_REFERAL);
        String referral = (String) configProps.get(CONFIG_PROP_REFERRAL);
        referral = referal != null ? referal : referral;
        iContextManager.setReferral(referral.toLowerCase());

        /*
         * Set alias dereferencing handling.
         */
        iContextManager.setDerefAliases((String) configProps.get(CONFIG_PROP_DEREFALIASES));

        /*
         * Set binary attribute names.
         */
        iContextManager.setBinaryAttributeNames(getBinaryAttributes());

        /*
         * Configure the context pool.
         */
        boolean enableContextPool = true;

        List<Map<String, Object>> poolConfigs = Nester.nest(CONFIG_CONTEXT_POOL, configProps);

        Map<String, Object> poolConfig = null;
        if (!poolConfigs.isEmpty()) {
            poolConfig = poolConfigs.get(0);
        }

        /*
         * Enable the context pool if the context pool is enabled and the
         * stand-alone LDAP property reuseConnection has not been set to false.
         */
        boolean reuseConn = (Boolean) configProps.get(CONFIG_REUSE_CONNECTION);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Set reuseConnection as [" + reuseConn + "]");
        }
        if (poolConfig != null) {
            enableContextPool = (Boolean) poolConfig.get(CONFIG_PROP_ENABLED);
        }
        if (!reuseConn) {
            enableContextPool = false;
        }

        Integer initPoolSize = null;
        Integer maxPoolSize = null;
        Integer prefPoolSize = null;
        Long poolTimeOut = null;
        Long poolWaitTime = null;

        if (enableContextPool) {
            if (poolConfig != null) {
                if (poolConfig.get(CONFIG_PROP_INIT_POOL_SIZE) != null) {
                    initPoolSize = (Integer) poolConfig.get(CONFIG_PROP_INIT_POOL_SIZE);
                }
                if (poolConfig.get(CONFIG_PROP_MAX_POOL_SIZE) != null) {
                    maxPoolSize = (Integer) poolConfig.get(CONFIG_PROP_MAX_POOL_SIZE);
                }
                if (poolConfig.get(CONFIG_PROP_PREF_POOL_SIZE) != null) {
                    prefPoolSize = (Integer) poolConfig.get(CONFIG_PROP_PREF_POOL_SIZE);
                }

                if (poolConfig.get(CONFIG_PROP_POOL_TIME_OUT) != null) {
                    poolTimeOut = (Long) poolConfig.get(CONFIG_PROP_POOL_TIME_OUT);

                    /**
                     * The metatype is set to long for this property and all
                     * values will be passed as milliseconds.
                     * A value of 0 means no timeout, leave at 0
                     * Between 0 and 1000ms, round up to 1s
                     * Otherwise round to nearest second
                     */
                    if (poolTimeOut > 0 && poolTimeOut <= 1000) {
                        poolTimeOut = 1l; // override to 1 second
                    } else {
                        poolTimeOut = roundToSeconds(poolTimeOut);
                    }
                }
                if (poolConfig.get(CONFIG_PROP_POOL_WAIT_TIME) != null) {
                    poolWaitTime = (Long) poolConfig.get((CONFIG_PROP_POOL_WAIT_TIME));
                }
            }
        }

        /*
         * Set the configuration on the ContextManager.
         */
        iContextManager.setContextPool(enableContextPool, initPoolSize, prefPoolSize, maxPoolSize, poolTimeOut, poolWaitTime);

        /*
         * Initialize the ContextManager.
         *
         * Only need to check for missing password as the configuration for LDAP would catch a missing
         * primary server. The password is optional and only required when the bind DN is configured.
         */
        InitializeResult result = iContextManager.initialize();
        if (result == InitializeResult.MISSING_PASSWORD) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_INI_PROPERTY, WIMMessageHelper.generateMsgParms(CONFIG_PROP_BIND_PASSWORD));
            throw new MissingInitPropertyException(WIMMessageKey.MISSING_INI_PROPERTY, msg);
        } else if (result == InitializeResult.MISSING_KRB5_PRINCIPAL_NAME) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_INI_PROPERTY, WIMMessageHelper.generateMsgParms(ConfigConstants.CONFIG_PROP_KRB5_PRINCIPAL));
            throw new MissingInitPropertyException(WIMMessageKey.MISSING_INI_PROPERTY, msg);
        }
    }

    /**
     * Initialize search and attribute caches.
     *
     * @param configProps Configuration properties for the component.
     */
    private void initializeCaches(Map<String, Object> configProps) {
        final String METHODNAME = "initializeCaches(DataObject)";

        /*
         * initialize the cache pool names
         */
        iAttrsCacheName = iReposId + "/" + iAttrsCacheName;
        iSearchResultsCacheName = iReposId + "/" + iSearchResultsCacheName;

        List<Map<String, Object>> cacheConfigs = Nester.nest(CACHE_CONFIG, configProps);
        Map<String, Object> attrCacheConfig = null;
        Map<String, Object> searchResultsCacheConfig = null;

        if (!cacheConfigs.isEmpty()) {
            Map<String, Object> cacheConfig = cacheConfigs.get(0);
            Map<String, List<Map<String, Object>>> cacheInfo = Nester.nest(cacheConfig, ATTRIBUTES_CACHE_CONFIG, SEARCH_CACHE_CONFIG);
            List<Map<String, Object>> attrList = cacheInfo.get(ATTRIBUTES_CACHE_CONFIG);
            if (!attrList.isEmpty()) {
                attrCacheConfig = attrList.get(0);
            }
            List<Map<String, Object>> searchList = cacheInfo.get(SEARCH_CACHE_CONFIG);
            if (!searchList.isEmpty()) {
                searchResultsCacheConfig = searchList.get(0);
            }

            if (attrCacheConfig != null) {
                iAttrsCacheEnabled = (Boolean) attrCacheConfig.get(CONFIG_PROP_ENABLED);
                if (iAttrsCacheEnabled) {
                    // Initialize the Attributes Cache size
                    iAttrsCacheSize = (Integer) attrCacheConfig.get(CONFIG_PROP_CACHE_SIZE);
                    iAttrsCacheTimeOut = (Long) attrCacheConfig.get(CONFIG_PROP_CACHE_TIME_OUT);
                    iAttrsSizeLmit = (Integer) attrCacheConfig.get(CONFIG_PROP_ATTRIBUTE_SIZE_LIMIT);
                    /*
                     * TODO:: Cache Distribution is not yet needed.
                     * String cacheDistPolicy = attrCacheConfig.getString(CONFIG_PROP_CACHE_DIST_POLICY);
                     * if (cacheDistPolicy != null) {
                     * iAttrsCacheDistPolicy = LdapHelper.getCacheDistPolicyInt(cacheDistPolicy);
                     * }
                     */
                }
            }

            if (searchResultsCacheConfig != null) {
                iSearchResultsCacheEnabled = (Boolean) searchResultsCacheConfig.get(CONFIG_PROP_ENABLED);
                if (iSearchResultsCacheEnabled) {
                    // Initialize the Search Results Cache size
                    iSearchResultsCacheSize = (Integer) searchResultsCacheConfig.get(CONFIG_PROP_CACHE_SIZE);
                    iSearchResultsCacheTimeOut = (Long) searchResultsCacheConfig.get(CONFIG_PROP_CACHE_TIME_OUT);
                    iSearchResultSizeLmit = (Integer) searchResultsCacheConfig.get(CONFIG_PROP_SEARCH_RESULTS_SIZE_LIMIT);

                    /*
                     * TODO:: Cache Distribution is not yet needed.
                     * String cacheDistPolicy = searchResultsCacheConfig.getProperties().get(ConfigConstants.CONFIG_PROP_CACHE_DIST_POLICY);
                     * if (cacheDistPolicy != null) {
                     * iSearchResultsCacheDistPolicy = LdapHelper.getCacheDistPolicyInt(cacheDistPolicy);
                     * }
                     */
                }
            }
        }
        if (iAttrsCacheEnabled) {
            createAttributesCache();

            if (iAttrsCache == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Attributes Cache: " + iAttrsCacheName + " is not available because cache is not available yet.");
                }
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Attributes Cache: " + iAttrsCacheName + " is disabled.");
            }
        }
        if (iSearchResultsCacheEnabled) {
            createSearchResultsCache();
            if (iSearchResultsCache == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Search Results Cache: " + iSearchResultsCacheName + " is not available because cache is not available yet.");
                }
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Search Results Cache: " + iSearchResultsCacheName + " is disabled.");
            }
        }
    }

    /**
     * Method to create the search results cache, if configured.
     */
    private void createSearchResultsCache() {
        final String METHODNAME = "createSearchResultsCache";

        if (iSearchResultsCacheEnabled) {
            if (FactoryManager.getCacheUtil().isCacheAvailable()) {
                iSearchResultsCache = FactoryManager.getCacheUtil().initialize("SearchResultsCache", iSearchResultsCacheSize, iSearchResultsCacheSize, iSearchResultsCacheTimeOut);
                if (iSearchResultsCache != null) {
                    if (tc.isDebugEnabled()) {
                        StringBuilder strBuf = new StringBuilder(METHODNAME);
                        strBuf.append(" \nSearch Results Cache: ").append(iSearchResultsCacheName).append(" is enabled:\n");
                        strBuf.append("\tCacheSize: ").append(iSearchResultsCacheSize).append("\n");
                        strBuf.append("\tCacheTimeOut: ").append(iSearchResultsCacheTimeOut).append("\n");
                        strBuf.append("\tCacheResultSizeLimit: ").append(iSearchResultSizeLmit).append("\n");
                        Tr.debug(tc, strBuf.toString());
                    }
                }
            }
        }
    }

    /**
     * Method to create the attributes cache, if configured.
     */
    private void createAttributesCache() {
        final String METHODNAME = "createAttributesCache";

        if (iAttrsCacheEnabled) {
            if (FactoryManager.getCacheUtil().isCacheAvailable()) {
                iAttrsCache = FactoryManager.getCacheUtil().initialize("AttributesCache", iAttrsCacheSize, iAttrsCacheSize, iAttrsCacheTimeOut);
                if (iAttrsCache != null) {
                    if (tc.isDebugEnabled()) {
                        StringBuilder strBuf = new StringBuilder(METHODNAME);
                        strBuf.append(" \nAttributes Cache: ").append(iAttrsCacheName).append(" is enabled:\n");
                        strBuf.append("\tCacheSize: ").append(iAttrsCacheSize).append("\n");
                        strBuf.append("\tCacheTimeOut: ").append(iAttrsCacheTimeOut).append("\n");
                        strBuf.append("\tCacheSizeLimit: ").append(iAttrsSizeLmit).append("\n");
                        Tr.debug(tc, strBuf.toString());
                    }
                }
            }
        }
    }

    /**
     * Method to return the instance of the search results cache.
     *
     * @return The search results cache.
     */
    private ICacheUtil getSearchResultsCache() {
        if (iSearchResultsCache == null) {
            createSearchResultsCache();
        }
        return iSearchResultsCache;
    }

    /**
     * Method to return the instance of the attributes cache.
     *
     * @return The attributes cache.
     */
    private ICacheUtil getAttributesCache() {
        if (iAttrsCache == null) {
            createAttributesCache();
        }
        return iAttrsCache;
    }

    /**
     * Clear both the searchResults and attributes caches. This can be a big
     * performance hit to populate so only use when we need to clear to prevent
     * stale access to users/groups/attributes.
     *
     */
    public void clearCaches() {
        try {
            if (iSearchResultsCache != null) {
                iSearchResultsCache.clear();
            }
            if (iAttrsCache != null) {
                iAttrsCache.clear();
            }
        } catch (Exception e) {
            /*
             * Unlikely to still hit an NPE here, but shouldn't be a fatal error if we couldn't clear a cache
             * that doesn't exist
             */
            if (tc.isEventEnabled()) {
                Tr.event(tc, "clearCaches Unexpected exception occurred while clearing the search and attributes cache", e);
            }
        }
    }
    /**
     * Method to invalidate the specified entry from the attributes cache. One or all
     * parameters can be set in a single call. If all parameters are null, then this
     * operation no-ops.
     *
     * @param DN The distinguished name of the entity to invalidate attributes on.
     * @param extId The external ID of the entity to invalidate attributes on.
     * @param uniqueName The unique name of the entity to invalidate attributes on.
     */
    public void invalidateAttributes(String DN, String extId, String uniqueName) {
        final String METHODNAME = "invalidateAttributes(String, String, String)";

        if (getAttributesCache() != null) {

            if (DN != null) {
                getAttributesCache().invalidate(toKey(DN));
            }
            if (extId != null) {
                getAttributesCache().invalidate(extId);
            }
            if (uniqueName != null) {
                getAttributesCache().invalidate(toKey(uniqueName));
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " " + iAttrsCacheName + " size: " + getAttributesCache().size());
            }

        }
    }

    /**
     * Remove all entries from the search cache.
     */
    public void invalidateSearchCache() {
        if (getSearchResultsCache() != null) {
            getSearchResultsCache().clear();
        }
    }

    /**
     * Remove all entries from the attributes cache.
     */
    public void invalidateAttributeCache() {
        if (getAttributesCache() != null) {
            getAttributesCache().clear();
        }
    }

    /**
     * Convert the input distinguished name to a key for lookup in the attributes cache.
     *
     * @param name The distinguished name.
     * @return The key.
     */
    @Trivial
    private String toKey(String name) {
        if (ignoreDNCase) {
            return name.toLowerCase();
        } else {
            return name;
        }
    }

    /**
     * Whether to ignore the case of distinguished names.
     *
     * @return True if case should be ignored.
     */
    @Trivial
    public boolean isIgnoreCase() {
        return ignoreDNCase;
    }

    /**
     * Get an LDAP entity by an identifier.
     *
     * @param id An {@link IdentifierType} entity.
     * @param inEntityTypes The acceptable entity types to look up.
     * @param propNames The property names to return in the LdapEntry.
     * @param getMbrshipAttr Whether to return the membership attribute.
     * @param getMbrAttr Whether to return the member attribute.
     * @return The {@link LdapEntry} corresponding to the identifier.
     * @throws WIMException If there was an error retrieving the LDAP entity.
     */
    public LdapEntry getEntityByIdentifier(IdentifierType id, List<String> inEntityTypes, List<String> propNames, boolean getMbrshipAttr,
                                           boolean getMbrAttr) throws WIMException {
        return getEntityByIdentifier(id.getExternalName(), id.getExternalId(), id.getUniqueName(),
                                     inEntityTypes, propNames, getMbrshipAttr, getMbrAttr);
    }

    /**
     * Get an LDAP entity by an identifier. One of 'dn', 'extId' or 'uniqueName' must be non-null.
     *
     * @param dn The distinguished name for the entity.
     * @param extId The external name for the entity.
     * @param uniqueName The unique name for the entity.
     * @param inEntityTypes The acceptable entity types to look up.
     * @param propNames The property names to return in the LdapEntry.
     * @param getMbrshipAttr Whether to return the membership attribute.
     * @param getMbrAttr Whether to return the member attribute.
     * @return The {@link LdapEntry} corresponding to the identifier.
     * @throws WIMException If there was an error retrieving the LDAP entity.
     */
    public LdapEntry getEntityByIdentifier(String dn, String extId, String uniqueName, List<String> inEntityTypes,
                                           List<String> propNames, boolean getMbrshipAttr, boolean getMbrAttr) throws WIMException {
        String[] attrIds = iLdapConfigMgr.getAttributeNames(inEntityTypes, propNames, getMbrshipAttr, getMbrAttr);
        Attributes attrs = null;
        if (dn == null && !iLdapConfigMgr.needTranslateRDN()) {
            dn = iLdapConfigMgr.switchToLdapNode(uniqueName);
        }
        // Changed the order of the if-else ladder. Please check against tWAS code for the original order
        if (dn != null) {
            attrs = checkAttributesCache(dn, attrIds);
        } else if (extId != null) {
            if (iLdapConfigMgr.isAnyExtIdDN()) {
                dn = LdapHelper.getValidDN(extId);
                if (dn != null) {
                    attrs = checkAttributesCache(dn, attrIds);
                } else if (uniqueName != null) {
                    attrs = getAttributesByUniqueName(uniqueName, attrIds, inEntityTypes);
                    dn = LdapHelper.getDNFromAttributes(attrs);
                } else {
                    String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(null));
                    throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
                }
            } else {
                attrs = getAttributesByUniqueName(extId, attrIds, inEntityTypes);
                dn = LdapHelper.getDNFromAttributes(attrs);
            }
        } else if (uniqueName != null) {
            attrs = getAttributesByUniqueName(uniqueName, attrIds, inEntityTypes);
            dn = LdapHelper.getDNFromAttributes(attrs);
        } else {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(null));
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
        }

        String entityType = iLdapConfigMgr.getEntityType(attrs, uniqueName, dn, extId, inEntityTypes);
        uniqueName = getUniqueName(dn, entityType, attrs);
        if (extId == null) {
            extId = iLdapConfigMgr.getExtIdFromAttributes(dn, entityType, attrs);
        }
        LdapEntry ldapEntry = new LdapEntry(dn, extId, uniqueName, entityType, attrs);

        return ldapEntry;
    }

    /**
     * Get the unique name for the specified distinguished name.
     *
     * @param dn The distinguished name.
     * @param entityType The entity type for the distinguished name.
     * @param attrs The attributes for the entity.
     * @return The unique name.
     * @throws WIMException If there was an error retrieving portions of the unique name.
     */
    private String getUniqueName(String dn, String entityType, Attributes attrs) throws WIMException {
        final String METHODNAME = "getUniqueName";
        String uniqueName = null;
        dn = iLdapConfigMgr.switchToNode(dn);
        if (iLdapConfigMgr.needTranslateRDN() && iLdapConfigMgr.needTranslateRDN(entityType)) {
            try {
                if (entityType != null) {
                    LdapEntity ldapEntity = iLdapConfigMgr.getLdapEntity(entityType);
                    if (ldapEntity != null) {
                        String[] rdnName = LdapHelper.getRDNAttributes(dn);
                        String[][] rdnWIMProps = ldapEntity.getWIMRDNProperties();
                        String[][] rdnWIMAttrs = ldapEntity.getWIMRDNAttributes();
                        String[][] rdnAttrs = ldapEntity.getRDNAttributes();
                        Attribute[] rdnAttributes = new Attribute[rdnWIMProps.length];
                        String[] rdnAttrValues = new String[rdnWIMProps.length];
                        for (int i = 0; i < rdnAttrs.length; i++) {
                            String[] rdnAttr = rdnAttrs[i];
                            boolean isRDN = false;
                            for (int j = 0; j < rdnAttr.length; j++) {
                                for (int k = 0; k < rdnName.length; k++) {
                                    if (rdnAttr[j].equalsIgnoreCase(rdnName[k])) {
                                        isRDN = true;
                                        break;
                                    }
                                }
                            }
                            if (isRDN) {
                                String[] rdnWIMProp = rdnWIMProps[i];
                                String[] rdnWIMAttr = rdnWIMAttrs[i];
                                boolean retrieveRDNs = false;
                                if (attrs == null) {
                                    retrieveRDNs = true;
                                } else {
                                    for (int k = 0; k < rdnWIMAttr.length; k++) {
                                        if (attrs.get(rdnWIMAttr[k]) == null) {
                                            retrieveRDNs = true;
                                            break;
                                        }
                                    }
                                }
                                if (retrieveRDNs) {
                                    attrs = getAttributes(dn, rdnWIMAttr);
                                }
                                for (int k = 0; k < rdnWIMAttr.length; k++) {
                                    rdnAttributes[k] = attrs.get(rdnWIMAttr[k]);
                                    if (rdnAttributes[k] != null) {
                                        rdnAttrValues[k] = (String) rdnAttributes[k].get();
                                    }
                                }
                                uniqueName = LdapHelper.replaceRDN(dn, rdnWIMProp, rdnAttrValues);
                            }
                        }
                    }
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
        if (uniqueName == null) {
            uniqueName = dn;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Translated uniqueName: " + uniqueName);
            }
        }
        return uniqueName;
    }

    /**
     * Get the specified attributes for the distinguished name.
     *
     * @param name The distinguished name.
     * @param attrIds The attribute IDs to retrieve.
     * @return The {@link Attributes} instance.
     * @throws WIMException If there was an error retrieving the attributes from the LDAP server.
     */
    @FFDCIgnore({ NamingException.class, NameNotFoundException.class })
    private Attributes getAttributes(String name, String[] attrIds) throws WIMException {
        Attributes attributes = null;

        if (iLdapConfigMgr.getUseEncodingInSearchExpression() != null)
            name = LdapHelper.encodeAttribute(name, iLdapConfigMgr.getUseEncodingInSearchExpression());

        if (iAttrRangeStep > 0) {
            attributes = getRangeAttributes(name, attrIds);
        } else {
            TimedDirContext ctx = iContextManager.getDirContext();
            try {
                try {
                    if (attrIds.length > 0) {
                        attributes = ctx.getAttributes(new LdapName(name), attrIds);
                    } else {
                        attributes = new BasicAttributes();
                    }
                } catch (NamingException e) {
                    if (!ContextManager.isConnectionException(e)) {
                        throw e;
                    }
                    ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                    if (attrIds.length > 0) {
                        attributes = ctx.getAttributes(new LdapName(name), attrIds);
                    } else {
                        attributes = new BasicAttributes();
                    }
                }
            } catch (NameNotFoundException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_ENTRY_NOT_FOUND, WIMMessageHelper.generateMsgParms(name, e.toString(true)));
                throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, msg, e);
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            } finally {
                iContextManager.releaseDirContext(ctx);
            }
        }
        return attributes;
    }

    /**
     * Get the specified attributes for the distinguished name taking into consideration range attributes.
     *
     * @param name The distinguished name.
     * @param attrIds The attribute IDs to retrieve.
     * @return The {@link Attributes} instance.
     * @throws WIMException If there was an error retrieving the attributes from the LDAP server.
     */
    @FFDCIgnore({ NameNotFoundException.class, NamingException.class })
    private Attributes getRangeAttributes(String name, String[] attrIds) throws WIMException {

        Attributes attributes = null;
        TimedDirContext ctx = iContextManager.getDirContext();
        try {
            try {
                attributes = ctx.getAttributes(new LdapName(name), attrIds);
            } catch (NamingException e) {
                if (!ContextManager.isConnectionException(e)) {
                    throw e;
                }
                ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                attributes = ctx.getAttributes(new LdapName(name), attrIds);
            }

            supportRangeAttributes(attributes, name, ctx);
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_ENTRY_NOT_FOUND, WIMMessageHelper.generateMsgParms(name, e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            iContextManager.releaseDirContext(ctx);
        }
        return attributes;
    }

    /**
     * Check the attributes cache for the attributes on the distinguished name.
     * If any of the attributes are missing, a call to the LDAP server will be made to retrieve them.
     *
     * @param name The distinguished name to look up the attributes in the cache for.
     * @param attrIds The attributes to retrieve.
     * @return The {@link Attributes}.
     * @throws WIMException If a call to the LDAP server was necessary and failed.
     */
    public Attributes checkAttributesCache(String name, String[] attrIds) throws WIMException {
        final String METHODNAME = "checkAttributesCache";

        Attributes attributes = null;
        // If attribute cache is available, look up cache first
        if (getAttributesCache() != null) {
            String key = toKey(name);
            Object cached = getAttributesCache().get(key);
            // Cache entry found
            if (cached != null && (cached instanceof Attributes)) {
                List<String> missAttrIdList = new ArrayList<String>(attrIds.length);
                Attributes cachedAttrs = (Attributes) cached;
                attributes = new BasicAttributes(true);
                for (int i = 0; i < attrIds.length; i++) {
                    Attribute attr = LdapHelper.getIngoreCaseAttribute(cachedAttrs, attrIds[i]);
                    if (attr != null) {
                        attributes.put(attr);
                    } else {
                        missAttrIdList.add(attrIds[i]);
                    }
                }
                // If no missed attributes, nothing need to do.
                // Otherwise, retrieve missed attributes and add back to cache
                if (missAttrIdList.size() > 0) {
                    String[] missAttrIds = missAttrIdList.toArray(new String[0]);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Miss cache: " + key + " " + WIMTraceHelper.printObjectArray(missAttrIds));
                    }
                    Attributes missAttrs = getAttributes(name, missAttrIds);
                    // Add missed attributes to attributes.
                    addAttributes(missAttrs, attributes);
                    // Add missed attributes to cache.
                    updateAttributesCache(key, missAttrs, cachedAttrs, missAttrIds);
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Hit cache: " + key);
                    }
                }
            }
            // No cache entry, call LDAP to retrieve all request attributes.
            else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Miss cache: " + key);
                }

                attributes = getAttributes(name, attrIds);
                // Add attributes to cache.
                updateAttributesCache(key, attributes, null, attrIds);
            }
        } else {
            // Attribute cache is not available, directly call LDAP server
            attributes = getAttributes(name, attrIds);
        }

        return attributes;
    }

    /**
     * Add attributes new attributes to an existing {@link Attributes} instance.
     *
     * @param sourceAttrs The attributes to add.
     * @param descAttrs The attributes to add to.
     */
    @Trivial
    private void addAttributes(Attributes sourceAttrs, Attributes descAttrs) {
        for (NamingEnumeration<?> neu = sourceAttrs.getAll(); neu.hasMoreElements();) {
            descAttrs.put((Attribute) neu.nextElement());
        }
    }

    /**
     * Update the attributes cache by adding a mapping of the unique name to distinguished name
     * and mapping the distinguished name to the updated attributes.
     *
     * @param uniqueNameKey The unique name to map the distinguished name to.
     * @param dn The distinguished name to map to the unique name. This will also be used to map
     *            the attributes to.
     * @param newAttrs The new attributes.
     * @param attrIds The attribute IDs.
     */
    private void updateAttributesCache(String uniqueNameKey, String dn, Attributes newAttrs, String[] attrIds) {
        final String METHODNAME = "updateAttributesCache(key,dn,newAttrs)";

        /*
         * Add uniqueName to DN mapping to cache
         */
        getAttributesCache().put(uniqueNameKey, dn, 1, iAttrsCacheTimeOut, 0, null);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " Update " + iAttrsCacheName + "(size: "
                         + getAttributesCache().size() + ")\n" + uniqueNameKey + ": " + dn);
        }

        /*
         * Add the DN to Attributes mapping to the cache.
         */
        String dnKey = toKey(dn);
        Object cached = getAttributesCache().get(dnKey);
        Attributes cachedAttrs = null;
        if (cached != null && cached instanceof Attributes) {
            cachedAttrs = (Attributes) cached;
        }
        updateAttributesCache(dnKey, newAttrs, cachedAttrs, attrIds);
    }

    /**
     * Update the cached attributes for the specified key. Only attribute IDs that are in the
     * "missAttrIds" array will be added into the cached attributes.
     *
     * @param key The key for the cached attributes. This is usually the distinguished name.
     * @param missAttrs The missing/new attributes.
     * @param cachedAttrs The cached attributes.
     * @param missAttrIds The missing/new attribute IDs.
     */
    private void updateAttributesCache(String key, Attributes missAttrs, Attributes cachedAttrs, String[] missAttrIds) {
        final String METHODNAME = "updateAttributesCache(key,missAttrs,cachedAttrs,missAttrIds)";
        if (missAttrIds != null) {
            boolean newattr = false; // differentiate between a new entry and an entry we'll update so we change the cache correctly and maintain the creation TTL.
            if (missAttrIds.length > 0) {
                if (cachedAttrs != null) {
                    cachedAttrs = (Attributes) cachedAttrs.clone();
                } else {
                    cachedAttrs = new BasicAttributes(true);
                    newattr = true;
                }

                for (int i = 0; i < missAttrIds.length; i++) {
                    boolean findAttr = false;
                    for (NamingEnumeration<?> neu = missAttrs.getAll(); neu.hasMoreElements();) {
                        Attribute attr = (Attribute) neu.nextElement();
                        if (attr.getID().equalsIgnoreCase(missAttrIds[i])) {
                            findAttr = true;
                            // If the size of the attributes is larger than the
                            // limit, do not cache it.
                            if (!(iAttrsSizeLmit > 0 && attr.size() > iAttrsSizeLmit)) {
                                cachedAttrs.put(attr);
                            }
                            break;
                        } else {
                            int pos = attr.getID().indexOf(";");
                            if (pos > 0 && missAttrIds[i].equalsIgnoreCase(attr.getID().substring(0, pos))) {
                                findAttr = true;
                                // If the size of the attributes is larger than
                                // the limit, do not cache it.
                                if (!(iAttrsSizeLmit > 0 && attr.size() > iAttrsSizeLmit)) {
                                    cachedAttrs.put(attr);
                                }
                                break;
                            }
                        }
                    }

                    if (!findAttr) {
                        Attribute nullAttr = new BasicAttribute(missAttrIds[i], null);
                        cachedAttrs.put(nullAttr);
                    }
                }
                if (newattr) { // only set the the TTL if we're putting in a new entry
                    getAttributesCache().put(key, cachedAttrs, 1, iAttrsCacheTimeOut, 0, null);
                } else {
                    getAttributesCache().put(key, cachedAttrs);
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Update " + iAttrsCacheName + "(size: " + getAttributesCache().size() + " newEntry: " + newattr + ")\n" + key
                                 + ": " + cachedAttrs);
                }
            }
        } else {
            updateAttributesCache(key, missAttrs, cachedAttrs);
        }
    }

    /**
     * Update the attributes cache for the specified key.
     *
     * @param key The key for the cached attributes. This is usually the distinguished name.
     * @param missAttrs The missing/new attributes.
     * @param cachedAttrs The cached attributes.
     */
    private void updateAttributesCache(String key, Attributes missAttrs, Attributes cachedAttrs) {
        final String METHODNAME = "updateAttributeCache(key,missAttrs,cachedAttrs)";
        if (missAttrs.size() > 0) {
            boolean newAttr = false; // differentiate between a new entry and an entry we'll update so we change the cache correctly and maintain the creation TTL.
            if (cachedAttrs != null) {
                cachedAttrs = (Attributes) cachedAttrs.clone();
            } else {
                cachedAttrs = new BasicAttributes(true);
                newAttr = true;
            }

            //Set extIdAttrs = iLdapConfigMgr.getExtIds();
            for (NamingEnumeration<?> neu = missAttrs.getAll(); neu.hasMoreElements();) {
                Attribute attr = (Attribute) neu.nextElement();
                // If the size of the attributes is larger than the limit, don not cache it.
                if (!(iAttrsSizeLmit > 0 && attr.size() > iAttrsSizeLmit)) {
                    cachedAttrs.put(attr);
                }
            }
            if (newAttr) {
                getAttributesCache().put(key, cachedAttrs, 1, iAttrsCacheTimeOut, 0, null);
            } else {
                getAttributesCache().put(key, cachedAttrs);
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Update " + iAttrsCacheName + "(size: " + getAttributesCache().size() + " newEntry: " + newAttr + ")\n" + key + ": " + cachedAttrs);
            }
        }
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter.
     * Performs the search as specified by the search controls.
     *
     * <p/>Will first query the search cache. If there is a cache miss, the search will be
     * sent to the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filter the filter expression to use for the search. May not be null.
     * @param scope The search scope. One of: {@link SearchControls#OBJECT_SCOPE},
     *            {@link SearchControls#ONELEVEL_SCOPE}, {@link SearchControls#SUBTREE_SCOPE}.
     * @param attrIds The identifiers of the attributes to return along with the entry.
     *            If null, return all attributes. If empty return no attributes.
     * @return The {@link NamingEnumeration} containing the search results.
     * @throws WIMException If the search failed.
     */
    public NamingEnumeration<SearchResult> search(String name, String filter, int scope, String[] attrIds) throws WIMException {
        SearchControls controls = new SearchControls(scope, iCountLimit, iTimeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, null, controls);
    }

    /**
     * Check the search cache for previously performed searches. If the result is not cached,
     * query the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression used in the search.
     * @param filterArgs the filter arguments used in the search.
     * @param cons The search controls used in the search.
     * @return The {@link CachedNamingEnumeration} if the search is still in the cache, null otherwise.
     * @throws WIMException If the search failed with an error.
     */
    private NamingEnumeration<SearchResult> checkSearchCache(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws WIMException {
        final String METHODNAME = "checkSearchCache";
        NamingEnumeration<SearchResult> neu = null;
        if (getSearchResultsCache() != null) {
            String key = null;
            if (filterArgs == null) {
                key = toKey(name, filterExpr, cons);
            } else {
                key = toKey(name, filterExpr, filterArgs, cons);
            }
            CachedNamingEnumeration cached = (CachedNamingEnumeration) getSearchResultsCache().get(key);

            if (cached == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Miss cache: " + key);
                }
                neu = search(name, filterExpr, filterArgs, cons, null);
                String[] reqAttrIds = cons.getReturningAttributes();
                neu = updateSearchCache(name, key, neu, reqAttrIds);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Hit cache: " + key);
                }
                neu = (CachedNamingEnumeration) cached.clone();
            }
        } else {
            neu = search(name, filterExpr, filterArgs, cons, null);
        }
        return neu;
    }

    /**
     * Update the search cache with search results.
     *
     * @param searchBase The base entry the search was made from.
     * @param key The key for the entry in the search cache.
     * @param neu The search results.
     * @param reqAttrIds The attribute IDs that were requested.
     * @return The {@link CachedNamingEnumeration} with the original search results.
     * @throws WIMSystemException If the results could not be cloned into the search cache.
     */
    @FFDCIgnore(NamingException.class)
    private NamingEnumeration<SearchResult> updateSearchCache(String searchBase, String key, NamingEnumeration<SearchResult> neu,
                                                              String[] reqAttrIds) throws WIMSystemException {
        final String METHODNAME = "updateSearchCache";
        CachedNamingEnumeration clone1 = new CachedNamingEnumeration();
        CachedNamingEnumeration clone2 = new CachedNamingEnumeration();
        int count = cloneSearchResults(neu, clone1, clone2);
        // Size limit 0 means no limit.
        if (iSearchResultSizeLmit == 0 || count < iSearchResultSizeLmit) {
            getSearchResultsCache().put(key, clone2, 1, iSearchResultsCacheTimeOut, 0, null);
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Update " + iSearchResultsCacheName + "(size: " + getSearchResultsCache().size() + ")\n" + key);

            // To reduce JNDI calls during get(), cache the entry into attribute cache
            if (getAttributesCache() != null) {
                try {
                    count = 0;
                    while (clone2.hasMore()) {
                        SearchResult result = clone2.nextElement();
                        String dnKey = LdapHelper.prepareDN(result.getName(), searchBase);
                        Object cached = getAttributesCache().get(dnKey);
                        Attributes cachedAttrs = null;
                        if (cached != null && cached instanceof Attributes) {
                            cachedAttrs = (Attributes) cached;
                        }

                        updateAttributesCache(dnKey, result.getAttributes(), cachedAttrs, reqAttrIds);
                        if (++count > 20) {
                            // cache only first 20 results from the search.
                            // caching all entries may thrash the attributeCache
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, METHODNAME + " attribute cache updated with " + (count - 1) + " entries. skipping rest.");

                            break;
                        }
                    }
                } catch (NamingException e) {
                    /* Ignore. */
                }
            }
        }
        return clone1;
    }

    /**
     * Clone the given {@link NamingNumeration}.
     *
     * @param results The results to clone.
     * @param clone1 {@link CachedNamingEnumeration} with the original results.
     * @param clone2 {@link CachedNamingEnumeration} with the cloned results.
     * @return The number of entries in the results.
     * @throws WIMSystemException If the results could not be cloned.
     */
    @FFDCIgnore(SizeLimitExceededException.class)
    private static int cloneSearchResults(NamingEnumeration<SearchResult> results, CachedNamingEnumeration clone1,
                                          CachedNamingEnumeration clone2) throws WIMSystemException {
        final String METHODNAME = "cloneSearchResults(NamingEnumeration, CachedNamingEnumeration, CachedNamingEnumeration)";
        int count = 0;
        try {
            while (results.hasMore()) {
                SearchResult result = results.nextElement();
                Attributes attrs = (Attributes) result.getAttributes().clone();
                SearchResult cachedResult = new SearchResult(result.getName(), null, null, attrs);
                clone1.add(result);
                clone2.add(cachedResult);
                count++;
            }
        } catch (SizeLimitExceededException e) {
            // if size limit is reached because of LDAP server limit, then log the result
            // and return the available results
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " " + e.toString(true), e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        }

        return count;
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter.
     * Performs the search as specified by the search controls.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain
     *            variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The
     *            value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent
     *            to an empty array.
     * @param cons the search controls that control the search. If null, the default search controls
     *            are used (equivalent to (new SearchControls())).
     * @param requestControls Request controls to set on the request; may be null.
     * @return The {@link NamingEnumeration} containing the search results.
     * @throws WIMException If the search failed.
     */
    private NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
                                                   SearchControls cons, Control requestControls[]) throws WIMException {
        final String METHODNAME = "search(String, String, Object[], SearchControls)";

        if (iPageSize == 0) {
            TimedDirContext ctx = iContextManager.getDirContext();
            NamingEnumeration<SearchResult> neu = null;
            try {
                try {
                    if (filterArgs == null) {
                        neu = ctx.search(new LdapName(name), filterExpr, cons);
                    } else {
                        neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                    }
                } catch (NamingException e) {
                    if (!ContextManager.isConnectionException(e)) {
                        throw e;
                    }
                    ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                    if (filterArgs == null) {
                        neu = ctx.search(new LdapName(name), filterExpr, cons);
                    } else {
                        neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                    }
                }
            } catch (NameNotFoundException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_ENTRY_NOT_FOUND, WIMMessageHelper.generateMsgParms(name, e.toString(true)));
                throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, msg, e);
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            } finally {
                iContextManager.releaseDirContext(ctx);
            }

            return neu;
        } else {
            TimedDirContext ctx = iContextManager.getDirContext();
            try {
                if (requestControls != null) {
                    ctx.setRequestControls(new Control[] { requestControls[0], new PagedResultsControl(iPageSize, false) });
                } else {
                    ctx.setRequestControls(new Control[] { new PagedResultsControl(iPageSize, false) });
                }

                byte[] cookie = null;
                int pageCount = 1;
                int count = 0;
                CachedNamingEnumeration allNeu = new CachedNamingEnumeration();
                int retries = 0; // The number of retries
                boolean tryAgain; // Whether to retry the search

                do {
                    tryAgain = false;

                    NamingEnumeration<SearchResult> neu = null;
                    try {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Search page: " + pageCount + ", Retries: " + retries);
                        }
                        if (filterArgs == null) {
                            neu = ctx.search(new LdapName(name), filterExpr, cons);
                        } else {
                            neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                        }

                        /*
                         * Save the results.
                         */
                        if (neu != null) {
                            pageCount++;
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Received search results, looping through elements. May include referral chasing.");
                            }

                            while (neu.hasMoreElements()) {
                                SearchResult sr = neu.nextElement();
                                if (iAttrRangeStep > 0) { // Should only enable this on ActiveDirectory, not supported on other Ldaps, added for OLGH 10144
                                    supportRangeAttributes(sr.getAttributes(), name, ctx);
                                }
                                allNeu.add(sr);
                                count++;
                            }

                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Received search results, looped through elements. Num of elements retrieved: " + count);
                            }

                            /*
                             * Get the cookie if there are more results to process.
                             */
                            Control[] resCtrls = ctx.getResponseControls();
                            if (resCtrls != null && resCtrls.length > 0) {
                                PagedResultsResponseControl resCtrl = (PagedResultsResponseControl) resCtrls[0];
                                cookie = resCtrl.getCookie();
                                if (cookie != null && cookie.length > 0) {
                                    ctx.setRequestControls(new Control[] { new PagedResultsControl(iPageSize, cookie, false) });
                                }
                            }
                        }

                    } catch (NamingException e) {

                        if (ContextManager.isConnectionException(e) && retries == 0) {

                            /*
                             * Some LDAP servers do not allow the paged searches to span new connections, so
                             * we need to start the paged search from the beginning. Clear the cookie and
                             * the results collected so far and start at page 1. We also need to be aware that
                             * the new connection could be to another (fail-over) server that has no context
                             * for the current paged search's cookie.
                             */
                            tryAgain = true;
                            retries++;
                            pageCount = 1;
                            cookie = null;
                            allNeu = new CachedNamingEnumeration();
                            count = 0;

                            /*
                             * Recreate the connection.
                             */
                            ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                            if (requestControls != null) {
                                ctx.setRequestControls(new Control[] { requestControls[0], new PagedResultsControl(iPageSize, false) });
                            } else {
                                ctx.setRequestControls(new Control[] { new PagedResultsControl(iPageSize, false) });
                            }

                        } else {
                            throw e;
                        }
                    }

                } while (tryAgain || (cookie != null && cookie.length > 0 && count < cons.getCountLimit()));
                return allNeu;
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            } catch (IOException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.GENERIC, WIMMessageHelper.generateMsgParms(e.toString()));
                throw new WIMSystemException(WIMMessageKey.GENERIC, msg, e);
            } finally {
                try {
                    ctx.setRequestControls(null);
                } catch (NamingException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + e.toString(true));
                }
                iContextManager.releaseDirContext(ctx);
            }
        }
    }

    /**
     * Get the attributes for the entity with the specified unique name. This method will make
     * use of the attribute cache when possible.
     *
     * @param uniqueName The unique name to get the attributes for.
     * @param attrIds The attribute IDs to retrieve.
     * @param entityTypes The entity types to consider. This includes all sub-types.
     * @return The entities.
     * @throws WIMException If there was an issue calling the LDAP server to get the attributes.
     */
    public Attributes getAttributesByUniqueName(String uniqueName, String[] attrIds, List<String> entityTypes) throws WIMException {

        String DN = null;
        Attributes attributes = null;
        uniqueName = iLdapConfigMgr.switchToLdapNode(uniqueName);
        boolean needTranslate = false;
        if (iLdapConfigMgr.needTranslateRDN()) {
            if (entityTypes != null && entityTypes.size() > 0) {
                for (int i = 0; i < entityTypes.size(); i++) {
                    if (iLdapConfigMgr.needTranslateRDN(entityTypes.get(i))) {
                        needTranslate = true;
                        break;
                    }
                }
            } else {
                needTranslate = true;
            }
        }
        if (!needTranslate) {
            DN = uniqueName;
            attributes = checkAttributesCache(DN, attrIds);
            attributes.put(LDAP_DN, DN);
            return attributes;
        }

        boolean readFromCache = false;
        String uniqueNameKey = "UNIQUENAME::" + toKey(uniqueName);
        if (getAttributesCache() != null) {
            readFromCache = true;
            Object cached = getAttributesCache().get(uniqueNameKey);

            if (cached != null) {
                if (cached instanceof String) {
                    /*
                     * This may be a uniqueName to distinguished name mapping entry. Use the distinguished
                     * name to look up the attributes.
                     */
                    DN = (String) cached;
                    try {
                        attributes = checkAttributesCache(DN, attrIds);
                        attributes.put(LDAP_DN, DN);
                        return attributes;

                    } catch (EntityNotFoundException e) {
                        // Name not found, invalidate this entry and continue doing search
                        getAttributesCache().invalidate(uniqueNameKey);
                    }
                }
            }
        }

        /*
         * We didn't find any cached attributes, so search for the entity and it's attributes.
         */
        String filter = "objectclass=*";
        SearchControls controls = new SearchControls();
        controls.setTimeLimit(iTimeLimit);
        controls.setCountLimit(iCountLimit);
        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setReturningAttributes(attrIds); // Only retrieve requested attributes
        controls.setReturningObjFlag(false);

        TimedDirContext ctx = iContextManager.getDirContext();
        NamingEnumeration<SearchResult> neu = null;
        try {
            try {
                neu = ctx.search(uniqueName, filter, controls);
            } catch (NamingException e) {
                if (!ContextManager.isConnectionException(e)) {
                    throw e;
                }
                ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                neu = ctx.search(uniqueName, filter, controls);
            }
            if (neu.hasMoreElements()) {
                SearchResult thisEntry = neu.nextElement();
                if (thisEntry != null) {
                    DN = LdapHelper.prepareDN(thisEntry.getNameInNamespace(), null);
                    attributes = thisEntry.getAttributes();
                    if (iAttrRangeStep > 0) {
                        supportRangeAttributes(attributes, DN, ctx);
                    }
                }
            }

        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(uniqueName));
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            iContextManager.releaseDirContext(ctx);
        }

        if (attributes != null) {
            if (readFromCache) {
                updateAttributesCache(uniqueNameKey, DN, attributes, attrIds);
            }
            attributes.put(LDAP_DN, DN);
        } else {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(uniqueName));
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg);
        }

        return attributes;
    }

    /**
     * Get dynamic groups.
     *
     * @param bases Search bases.
     * @param propNames Properties to return.
     * @param getMbrshipAttr Whether to request the membership attribute.
     * @return Map of group distinguished names to {@link LdapEntry}.
     * @throws WIMException If there was an error resolving the dynamic groups from the LDAP server.
     */
    public Map<String, LdapEntry> getDynamicGroups(String bases[], List<String> propNames, boolean getMbrshipAttr) throws WIMException {
        Map<String, LdapEntry> dynaGrps = new HashMap<String, LdapEntry>();
        String[] attrIds = iLdapConfigMgr.getAttributeNames(iLdapConfigMgr.getGroupTypes(), propNames, getMbrshipAttr, false);
        String[] dynaMbrAttrNames = iLdapConfigMgr.getDynamicMemberAttributes();
        String[] temp = attrIds;
        attrIds = new String[temp.length + dynaMbrAttrNames.length];
        System.arraycopy(temp, 0, attrIds, 0, temp.length);
        System.arraycopy(dynaMbrAttrNames, 0, attrIds, temp.length, dynaMbrAttrNames.length);
        String dynaGrpFitler = iLdapConfigMgr.getDynamicGroupFilter();
        for (int i = 0, n = bases.length; i < n; i++) {
            String base = bases[i];
            for (NamingEnumeration<SearchResult> urls = search(base, dynaGrpFitler, SearchControls.SUBTREE_SCOPE, attrIds); urls.hasMoreElements();) {
                javax.naming.directory.SearchResult thisEntry = urls.nextElement();
                if (thisEntry == null) {
                    continue;
                }
                String entryName = thisEntry.getName();
                if (entryName == null || entryName.trim().length() == 0) {
                    continue;
                }
                String dn = LdapHelper.prepareDN(entryName, base);
                javax.naming.directory.Attributes attrs = thisEntry.getAttributes();
                String extId = iLdapConfigMgr.getExtIdFromAttributes(dn, SchemaConstants.DO_GROUP, attrs);
                String uniqueName = getUniqueName(dn, SchemaConstants.DO_GROUP, attrs);
                LdapEntry ldapEntry = new LdapEntry(dn, extId, uniqueName, SchemaConstants.DO_GROUP, attrs);
                dynaGrps.put(ldapEntry.getDN(), ldapEntry);
            }
        }
        return dynaGrps;
    }

    /**
     * Determine whether the distinguished name is in the LDAP URL query.
     *
     * @param urls The {@link LdapURL}s to query.
     * @param dn The distinguished name to check.
     * @return True if the distinguished name is resolved by one of the {@link LdapURL}s.
     * @throws WIMException If there were issues resolving any of the LDAP URLs.
     */
    public boolean isMemberInURLQuery(LdapURL[] urls, String dn) throws WIMException {
        boolean result = false;
        String[] attrIds = {};
        String rdn = LdapHelper.getRDN(dn);
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                LdapURL ldapURL = urls[i];
                if (ldapURL.parsedOK()) {
                    String searchBase = ldapURL.get_dn();
                    // If dn is not under base, no need to do search
                    if (LdapHelper.isUnderBases(dn, searchBase)) {
                        int searchScope = ldapURL.get_searchScope();
                        String searchFilter = ldapURL.get_filter();
                        if (searchScope == SearchControls.SUBTREE_SCOPE) {
                            if (searchFilter == null) {
                                result = true;
                                break;
                            } else {
                                NamingEnumeration<SearchResult> nenu = search(dn, searchFilter, SearchControls.SUBTREE_SCOPE, attrIds);
                                if (nenu.hasMoreElements()) {
                                    result = true;
                                    break;
                                }
                            }
                        } else {
                            if (searchFilter == null) {
                                searchFilter = rdn;
                            } else {
                                searchFilter = "(&(" + searchFilter + ")(" + rdn + "))";
                            }

                            NamingEnumeration<SearchResult> nenu = search(searchBase, searchFilter, searchScope, attrIds);
                            if (nenu.hasMoreElements()) {
                                SearchResult thisEntry = nenu.nextElement();
                                if (thisEntry == null) {
                                    continue;
                                }
                                String returnedDN = LdapHelper.prepareDN(thisEntry.getName(), searchBase);
                                if (dn.equalsIgnoreCase(returnedDN)) {
                                    result = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter
     * and match the given entity types. Performs the search as specified by the search controls.
     *
     * <p/>Will first query the search cache. If there is a cache miss, the search will be
     * sent to the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain
     *            variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The
     *            value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent
     *            to an empty array.
     * @param scope The search scope. One of: {@link SearchControls#OBJECT_SCOPE},
     *            {@link SearchControls#ONELEVEL_SCOPE}, {@link SearchControls#SUBTREE_SCOPE}.
     * @param inEntityTypes The entity types to return.
     * @param propNames The property names to return for the entities.
     * @param getMbrshipAttr Whether to request the configured membership attribute to be returned.
     * @param getMbrAttr Whether to request the configured member attribute to be returned.
     * @return The {@link LdapEntry}s that match the search criteria.
     * @throws WIMException If the search failed with an error.
     */
    public Set<LdapEntry> searchEntities(String name, String filterExpr, Object[] filterArgs, int scope, List<String> inEntityTypes,
                                         List<String> propNames, boolean getMbrshipAttr, boolean getMbrAttr) throws WIMException {
        return searchEntities(name, filterExpr, filterArgs, scope, inEntityTypes, propNames, getMbrshipAttr, getMbrAttr,
                              iCountLimit, iTimeLimit);
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter.
     * Performs the search as specified by the search controls.
     *
     * <p/>Will first query the search cache. If there is a cache miss, the search will be
     * sent to the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filter the filter expression to use for the search. May not be null.
     * @param scope The search scope. One of: {@link SearchControls#OBJECT_SCOPE},
     *            {@link SearchControls#ONELEVEL_SCOPE}, {@link SearchControls#SUBTREE_SCOPE}.
     * @param attrIds The identifiers of the attributes to return along with the entry.
     *            If null, return all attributes. If empty return no attributes.
     * @param countLimit The maximum number of entries to return. If 0, return all entries that satisfy filter.
     * @param timeLimit The number of milliseconds to wait before returning. If 0, wait indefinitely.
     * @return The {@link NamingEnumeration} containing the search results.
     * @throws WIMException If the search failed with an error.
     */
    public NamingEnumeration<SearchResult> search(String name, String filter, int scope, String[] attrIds, int countLimit,
                                                  int timeLimit) throws WIMException {
        SearchControls controls = new SearchControls(scope, countLimit, timeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, null, controls);
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter.
     * Performs the search as specified by the search controls.
     *
     * <p/>Will first query the search cache. If there is a cache miss, the search will be
     * sent to the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain
     *            variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The
     *            value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent
     *            to an empty array.
     * @param scope The search scope. One of: {@link SearchControls#OBJECT_SCOPE},
     *            {@link SearchControls#ONELEVEL_SCOPE}, {@link SearchControls#SUBTREE_SCOPE}.
     * @param attrIds The identifiers of the attributes to return along with the entry.
     *            If null, return all attributes. If empty return no attributes.
     * @param countLimit The maximum number of entries to return. If 0, return all entries that satisfy filter.
     * @param timeLimit The number of milliseconds to wait before returning. If 0, wait indefinitely.
     * @return The {@link NamingEnumeration} containing the search results.
     * @throws WIMException If the search failed with an error.
     */
    public NamingEnumeration<SearchResult> search(String name, String filter, Object[] filterArgs, int scope, String[] attrIds,
                                                  int countLimit, int timeLimit) throws WIMException {
        SearchControls controls = new SearchControls(scope, countLimit, timeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, filterArgs, controls);
    }

    /**
     * Searches in the named context or object for entries that satisfy the given search filter
     * and match the given entity types. Performs the search as specified by the search controls.
     *
     * <p/>Will first query the search cache. If there is a cache miss, the search will be
     * sent to the LDAP server.
     *
     * @param name The name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain
     *            variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The
     *            value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent
     *            to an empty array.
     * @param scope The search scope. One of: {@link SearchControls#OBJECT_SCOPE},
     *            {@link SearchControls#ONELEVEL_SCOPE}, {@link SearchControls#SUBTREE_SCOPE}.
     * @param inEntityTypes The entity types to return.
     * @param propNames The property names to return for the entities.
     * @param getMbrshipAttr Whether to request the configured membership attribute to be returned.
     * @param getMbrAttr Whether to request the configured member attribute to be returned.
     * @param countLimit The maximum number of entries to return. If 0, return all entries that satisfy filter.
     * @param timeLimit The number of milliseconds to wait before returning. If 0, wait indefinitely.
     * @return The {@link LdapEntry}s that match the search criteria.
     * @throws WIMException If the search failed with an error.
     */
    public Set<LdapEntry> searchEntities(String name, String filter, Object[] filterArgs, int scope, List<String> inEntityTypes,
                                         List<String> propNames, boolean getMbrshipAttr, boolean getMbrAttr, int countLimit, int timeLimit) throws WIMException {
        String inEntityType = null;
        List<String> supportedProps = propNames;
        if (inEntityTypes != null && inEntityTypes.size() > 0) {
            inEntityType = inEntityTypes.get(0);
            supportedProps = iLdapConfigMgr.getSupportedProperties(inEntityType, propNames);
        }

        String[] attrIds = iLdapConfigMgr.getAttributeNames(inEntityTypes, supportedProps, getMbrshipAttr, getMbrAttr);

        NamingEnumeration<SearchResult> neu = null;
        if (filterArgs == null) {
            neu = search(name, filter, scope, attrIds, countLimit, timeLimit);
        } else {
            neu = search(name, filter, filterArgs, scope, attrIds, countLimit, timeLimit);
        }

        Set<LdapEntry> entities = this.populateResultSet(neu, name, scope, inEntityTypes, attrIds);

        return entities;
    }

    /**
     * Populate the {@link Set} with {@link LdapEntry}s from the {@link NamingEnumeration} results.
     *
     * @param neu The {@link NamingEnumeration}.
     * @param base The base entry for the search.
     * @param scope The scope of the search.
     * @param inEntityTypes The entity types to return.
     * @param attrIds The attribute IDs to return for the {@link LdapEntry}s.
     * @return The {@link Set} of {@link LdapEntry}s.
     * @throws WIMException
     */
    @FFDCIgnore(SizeLimitExceededException.class)
    private Set<LdapEntry> populateResultSet(NamingEnumeration<SearchResult> neu, String base, int scope, List<String> inEntityTypes, String[] attrIds) throws WIMException {
        final String METHODNAME = "populateResultSet";

        Set<LdapEntry> entities = new HashSet<LdapEntry>();
        try {
            while (neu.hasMore()) {
                SearchResult thisEntry = neu.nextElement();
                if (thisEntry == null) {
                    continue;
                }
                String entryName = thisEntry.getName();
                String dn = LdapHelper.prepareDN(entryName, base);
                if (scope != SearchControls.OBJECT_SCOPE && base.equalsIgnoreCase(dn))
                    continue;

                Attributes attrs = thisEntry.getAttributes();

                // Handle RACF(SDBM) attributes that are not fetched using search controls
                if (iLdapConfigMgr.isRacf()) {
                    if (attrs.size() == 0) {
                        attrs = getAttributes(dn, attrIds);
                    }
                }

                String entityType = iLdapConfigMgr.getEntityType(attrs, null, dn, null, inEntityTypes);

                // Skip if for RACF we found an incorrect entity type
                if (iLdapConfigMgr.isRacf()) {
                    boolean entityTypeMatches = false;
                    for (String inType : inEntityTypes) {
                        /*
                         * It would be better if we had a utility method to check if a type is a sub-type of another type.
                         */
                        if ("PersonAccount".equals(entityType)) {
                            entityTypeMatches = inType.equals("LoginAccount") || inType.equals("PersonAccount");
                        } else if ("Group".equals(entityType)) {
                            entityTypeMatches = inType.equals("Group");
                        }
                        if (entityTypeMatches) {
                            break;
                        }
                    }

                    if (!entityTypeMatches) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Excluding '" + dn + "' from result since it is unexpected entity type.");
                        }
                        continue;
                    }
                }

                String extId = iLdapConfigMgr.getExtIdFromAttributes(dn, entityType, attrs);
                String uniqueName = getUniqueName(dn, entityType, attrs);
                LdapEntry ldapEntry = new LdapEntry(dn, extId, uniqueName, entityType, attrs);
                entities.add(ldapEntry);
            }
        } catch (SizeLimitExceededException e) {
            // if size limit is reached because of LDAP server limit, then log
            // the result
            // and return the available results
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " " + e.toString(true));
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        }
        return entities;
    }

    /**
     * Get a list of all ancestor distinguished names for the input distinguished name. For example;
     * if the input distinguished name was "uid=user,o=ibm,c=us" the results would be ["o=ibm,c=us", "c=us"].
     *
     * @param DN The distinguished name to get the ancestor distinguished names for.
     * @param level the number of levels to return. If not set, all will be returned.
     * @return The list of ancestor DNs.
     * @throws WIMException If there was an error parsing the input DN.
     */
    public List<String> getAncestorDNs(String DN, int level) throws WIMException {
        if (DN == null || DN.trim().length() == 0) {
            return null;
        }
        try {
            NameParser nameParser = getNameParser();
            Name name = nameParser.parse(DN);
            int size = name.size();

            List<String> ancestorDNs = new ArrayList<String>();
            if (level == 0) {
                level = size;
            }

            for (int i = size - 1; i >= (size - level); i--) {
                name.remove(i);
                ancestorDNs.add(name.toString());
            }
            return ancestorDNs;
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        }
    }

    /**
     * For the given attributes, check if any of them are range attributes, and if they are, request the rest
     * of the attributes that fall outside the range.
     *
     * @param attributes The attributes returned (so far).
     * @param dn The distinguished name of the entity containing the attributes.
     * @param ctx The context to use to request the remainder of the range attributes.
     * @throws WIMException If there was an error recreating a context in the case of a connection error.
     * @throws NamingException If there was an error retrieving the attributes from the LDAP server.
     */
    @FFDCIgnore({ NumberFormatException.class, NamingException.class })
    private void supportRangeAttributes(Attributes attributes, String dn, TimedDirContext ctx) throws WIMException, NamingException {

        final String METHODNAME = "supportRangeAttributes";

        // Deal with range attributes
        for (NamingEnumeration<?> anu = attributes.getAll(); anu.hasMoreElements();) {
            Attribute attr = (Attribute) anu.nextElement();
            String attrName = attr.getID();

            int pos = attrName.indexOf(ATTR_RANGE_KEYWORD);
            if (pos > -1) {
                String attrId = attrName.substring(0, pos);
                Attribute newAttr = new BasicAttribute(attrId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Range attribute retrieved: " + attrName);
                }

                for (NamingEnumeration<?> neu2 = attr.getAll(); neu2.hasMoreElements();) {
                    newAttr.add(neu2.nextElement());
                }

                int rangeLow = iAttrRangeStep;
                try {
                    rangeLow = Integer.parseInt(attrName.substring(attrName.indexOf('-') + 1)) + 1;
                } catch (NumberFormatException e) {
                    // continue;
                }
                int rangeHigh = rangeLow + iAttrRangeStep - 1;
                boolean quitLoop = false;
                boolean lastQuery = false;
                do {
                    String attributeWithRange = null;
                    if (!lastQuery) {
                        Object[] args = { Integer.valueOf(rangeLow).toString(),
                                          Integer.valueOf(rangeHigh).toString()
                        };
                        attributeWithRange = attrId + MessageFormat.format(ATTR_RANGE_QUERY, args);
                    } else {
                        Object[] args = {
                                          Integer.valueOf(rangeLow).toString()
                        };
                        attributeWithRange = attrId + MessageFormat.format(ATTR_RANGE_LAST_QUERY, args);
                    }
                    Attributes results = null;
                    String[] rAttrIds = {
                                          attributeWithRange
                    };
                    try {
                        results = ctx.getAttributes(new LdapName(dn), rAttrIds);
                    } catch (NamingException e) {
                        if (ContextManager.isConnectionException(e)) {
                            ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                            results = ctx.getAttributes(new LdapName(dn), rAttrIds);
                        } else {
                            throw e;
                        }
                    }
                    Attribute result = results.get(attributeWithRange);
                    if (result == null) {
                        Object[] args = {
                                          Integer.valueOf(rangeLow).toString()
                        };
                        attributeWithRange = attrId + MessageFormat.format(ATTR_RANGE_LAST_QUERY, args);
                        result = results.get(attributeWithRange);
                        lastQuery = true;
                    }
                    if (result != null) {
                        for (NamingEnumeration<?> neu2 = result.getAll(); neu2.hasMoreElements();) {
                            newAttr.add(neu2.nextElement());
                        }

                        if (lastQuery) {
                            quitLoop = true;
                        } else {
                            rangeLow = rangeHigh + 1;
                            rangeHigh = rangeLow + iAttrRangeStep - 1;
                        }
                    } else {
                        lastQuery = true;
                    }
                } while (!quitLoop);
                attributes.put(newAttr);
                attributes.remove(attrName);
            }
        }
    }

    /**
     * Search using operational attribute specified in the parameter.
     *
     * @param dn The DN to search on
     * @param filter The LDAP filter for the search.
     * @param inEntityTypes The entity types to search for.
     * @param propNames The property names to return.
     * @param oprAttribute The operational attribute.
     * @return The search results or null if there are no results.
     * @throws WIMException If the entity types do not exist or the search failed.
     */
    public SearchResult searchByOperationalAttribute(String dn, String filter, List<String> inEntityTypes, List<String> propNames, String oprAttribute) throws WIMException {
        String inEntityType = null;
        List<String> supportedProps = propNames;
        if (inEntityTypes != null && inEntityTypes.size() > 0) {
            inEntityType = inEntityTypes.get(0);
            supportedProps = iLdapConfigMgr.getSupportedProperties(inEntityType, propNames);
        }

        String[] attrIds = iLdapConfigMgr.getAttributeNames(inEntityTypes, supportedProps, false, false);

        attrIds = Arrays.copyOf(attrIds, attrIds.length + 1);
        attrIds[attrIds.length - 1] = oprAttribute;

        NamingEnumeration<SearchResult> neu = null;
        neu = search(dn, filter, SearchControls.OBJECT_SCOPE, attrIds, iCountLimit, iTimeLimit);

        if (neu != null) {
            try {
                if (neu.hasMore()) {
                    return neu.next();
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }

        return null;
    }

    /**
     * Get the list of configure binary attributes.
     *
     * @return A white-space delimited string of binary attribute names, suitable for use
     *         configuring JNDI.
     */
    private String getBinaryAttributes() {

        // Add binary settings for all octet string attributes.
        StringBuffer binaryAttrNamesBuffer = new StringBuffer();

        // Check the ldap data type of the extId attribute.
        Map<String, LdapAttribute> attrMap = iLdapConfigMgr.getAttributes();

        for (String attrName : attrMap.keySet()) {
            LdapAttribute attr = attrMap.get(attrName);
            if (LdapConstants.LDAP_ATTR_SYNTAX_OCTETSTRING.equalsIgnoreCase(attr.getSyntax())) {
                binaryAttrNamesBuffer.append(attr.getName()).append(" ");
            }
        }

        return binaryAttrNamesBuffer.toString().trim();
    }

    /**
     * Modify the given LDAP name according to the specified modification items.
     *
     * @param dn The distinguished name to modify attributes on.
     * @param mod_op The operation to perform.
     * @param mods The modification items.
     * @throws NamingException If there was an issue writing the new attribute values.
     * @throws WIMException If there was an issue getting or releasing a context, or the context is on a
     *             fail-over server and writing to fail-over servers is prohibited, or the distinguished
     *             name does not exist.
     */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException, WIMException {
        TimedDirContext ctx = iContextManager.getDirContext();
        iContextManager.checkWritePermission(ctx);
        try {
            try {
                ctx.modifyAttributes(new LdapName(name), mods);
            } catch (NamingException e) {
                if (!ContextManager.isConnectionException(e)) {
                    throw e;
                }
                ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                ctx.modifyAttributes(new LdapName(name), mods);
            }
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } catch (NamingException e) {
            throw e;
        } finally {
            iContextManager.releaseDirContext(ctx);
        }
    }

    /**
     * Modify the attributes for the specified distinguished name.
     *
     * @param dn The distinguished name to modify attributes on.
     * @param mod_op The operation to perform.
     * @param attrs The attributes to modify.
     * @throws NamingException If there was an issue writing the new attribute values.
     * @throws WIMException If there was an issue getting or releasing a context, or the context is on a
     *             fail-over server and writing to fail-over servers is prohibited, or the distinguished
     *             name does not exist.
     */
    public void modifyAttributes(String dn, int mod_op, Attributes attrs) throws NamingException, WIMException {
        TimedDirContext ctx = iContextManager.getDirContext();
        iContextManager.checkWritePermission(ctx);
        try {
            try {
                ctx.modifyAttributes(new LdapName(dn), mod_op, attrs);
            } catch (NamingException e) {
                if (!ContextManager.isConnectionException(e)) {
                    throw e;
                }
                ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                ctx.modifyAttributes(new LdapName(dn), mod_op, attrs);
            }
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } catch (NamingException e) {
            throw e;
        }

        finally {
            iContextManager.releaseDirContext(ctx);
        }
    }

    /**
     * Rename an entity.
     *
     * @param dn The distinguished name to rename.
     * @param newDn The new distinguished name.
     * @throws WIMException If there was an issue getting or releasing a context, or the context is on a
     *             fail-over server and writing to fail-over servers is prohibited, or the distinguished
     *             name does not exist.
     */
    public void rename(String dn, String newDn) throws WIMException {
        TimedDirContext ctx = iContextManager.getDirContext();
        iContextManager.checkWritePermission(ctx);
        try {
            try {
                ctx.rename(dn, newDn);
            } catch (NamingException e) {
                if (!ContextManager.isConnectionException(e)) {
                    throw e;
                }
                ctx = iContextManager.reCreateDirContext(ctx, e.toString());
                ctx.rename(dn, newDn);
            }
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            iContextManager.releaseDirContext(ctx);
        }
    }

    /**
     * Return the searchTimeout.
     *
     * @return The search timeout.
     */
    public int getSearchTimeout() {
        return iTimeLimit;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":{");
        sb.append("Repository ID=").append(iReposId).append("\n");
        sb.append(", SSL Factory=").append(iSSLFactory).append("\n");
        sb.append(", Page Size=").append(iPageSize).append("\n");
        sb.append(", Attribute Range Step=").append(iAttrRangeStep).append("\n");
        sb.append(", Ignore DN Case=").append(ignoreDNCase).append("\n");
        sb.append(", Search Result Count Limit=").append(iCountLimit).append("\n");
        sb.append(", Search Result Time Limit=").append(iTimeLimit).append("\n");

        /* Attributes Cache */
        sb.append(", Attributes Cache{ Enabled=").append(iAttrsCacheEnabled);
        sb.append(", Name=").append(iAttrsCacheName);
        sb.append(", Size=").append(iAttrsCacheSize);
        sb.append(", Limit=").append(iAttrsSizeLmit);
        sb.append(", Timeout=").append(iAttrsCacheTimeOut);
        sb.append(" }");

        /* Search Cache */
        sb.append(", Search Results Cache{ Enabled=").append(iSearchResultsCacheEnabled);
        sb.append(", Name=").append(iSearchResultsCacheName);
        sb.append(", Size=").append(iSearchResultsCacheSize);
        sb.append(", Limit=").append(iSearchResultSizeLmit);
        sb.append(", Timeout=").append(iSearchResultsCacheTimeOut);
        sb.append(" }");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Round a millisecond time stamp to seconds.
     *
     * @param timeInMilliseconds The millisecond time to round to seconds.
     * @return The time stamp rounded to seconds.
     */
    private long roundToSeconds(long timeInMilliseconds) {
        long returnInSeconds = timeInMilliseconds / 1000;
        if (timeInMilliseconds % 1000 > 499) {
            returnInSeconds++;
        }
        return returnInSeconds;
    }

    /**
     * Get the {@link ContextManager} that is managing {@link TimedDirContext} objects for this {@link LdapConnection}.
     *
     * @return The {@link ContextManager}.
     */
    public ContextManager getContextManager() {
        return this.iContextManager;
    }

    /**
     * Pass along the updated KerberosService
     *
     * @param ks
     */
    protected void updateKerberosService(KerberosService ks) {
        kerberosService = ks;
        iContextManager.updateKerberosService(ks, this);
    }
}
