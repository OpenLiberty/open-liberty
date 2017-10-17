/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_ENABLED;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_HOST;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_INIT_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_MAX_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_POOL_TIME_OUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_POOL_WAIT_TIME;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PORT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PREF_POOL_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_REFERAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_REFERRAL;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_RETURN_TO_PRIMARY_SERVER;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_COUNT_LIMIT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_PAGE_SIZE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_RESULTS_SIZE_LIMIT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SEARCH_TIME_OUT;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SERVER_TTL_ATTRIBUTE;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_PROP_SSL_ENABLED;
import static com.ibm.websphere.security.wim.ConfigConstants.CONFIG_REUSE_CONNECTION;
import static com.ibm.websphere.security.wim.ConfigConstants.SEARCH_CACHE_CONFIG;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMMessageHelper;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.wim.FactoryManager;
import com.ibm.ws.security.wim.env.ICacheUtil;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.wim.SchemaConstants;
import com.ibm.wsspi.security.wim.exception.EntityAlreadyExistsException;
import com.ibm.wsspi.security.wim.exception.EntityHasDescendantsException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.InvalidInitPropertyException;
import com.ibm.wsspi.security.wim.exception.MissingInitPropertyException;
import com.ibm.wsspi.security.wim.exception.OperationNotSupportedException;
import com.ibm.wsspi.security.wim.exception.WIMApplicationException;
import com.ibm.wsspi.security.wim.exception.WIMException;
import com.ibm.wsspi.security.wim.exception.WIMSystemException;
import com.ibm.wsspi.security.wim.model.IdentifierType;

public class LdapConnection {

    /**  */
    private static final String SERVER2 = "server";

    /**  */
    private static final String FAILOVER_SERVERS = "failoverServers";

    /**
     * Register the class to trace service.
     */
    private final static TraceComponent tc = Tr.register(LdapConnection.class);

    /**
     * The ID for the Ldap Repository.
     */
    private String reposId = null;

    /**
     * The key to extract the id of the configuration. The value returned by this is used as the repository id.
     */
    private static final String KEY_ID = "config.id";

    /**
     * Semaphore for locking.
     */
    private final Object lock = new Object() {};

    /**
     * Flag to determine if the search results cache is enabled.
     */
    private boolean iSearchResultsCacheEnabled = true;

    // Search Results Cache
    /**
     * Name of the search results cache.
     */
    private String iSearchResultsCacheName = "SearchResultsCache";

    /**
     * The search results cache
     */
    private ICacheUtil iSearchResultsCache = null;

    /**
     * Default search results cache size
     */
    private int iSearchResultsCacheSize = 2000;

    /**
     * Default search results cache timeout in milliseconds
     * If you see the history, this was changed from 1200 to 1200000
     * to correctly be milliseconds. WAS used 1200 seconds to setup
     * the cache, but Liberty cache impl is set directly in ms.
     * PI81954
     */
    private long iSearchResultsCacheTimeOut = 1200000;
    /**
     * default search results cache size limit
     */
    private int iSearchResultSizeLmit = 2000;

    /**
     * Flag to determine if the attributes cache is enabled.
     */
    private boolean iAttrsCacheEnabled = true;

    // Attributes Cache properties
    /**
     * Name of the attributes cache.
     */
    private String iAttrsCacheName = "AttributesCache";

    /**
     * The attributes cache
     */
    private ICacheUtil iAttrsCache = null;

    /**
     * Default attributes cache size
     */
    private int iAttrsCacheSize = 4000;

    /**
     * Default attributes cache timeout in milliseconds
     * If you see the history, this was changed from 1200 to 1200000
     * to correctly be milliseconds. WAS used 1200 seconds to setup
     * the cache, but Liberty cache impl is set directly in ms.
     * PI81954
     */
    private long iAttrsCacheTimeOut = 1200000;

    /**
     * default attributes cache size limit
     */
    private int iAttrsSizeLmit = 2000;

    /**
     * default server time to live
     */
    private String iServerTTLAttr = null;

    /**
     * The search result count limit. This is initialized as part of server initialization.
     */
    private int iCountLimit = 1000;

    /**
     * The search result time limit. This is initialized as part of server initialization.
     */
    private int iTimeLimit = 60000;

    /**
     * The default connect time limit. This is initialized as part of server initialization.
     */
    private final long DEFAULT_TIMEOUT = 600000;

    /**
     * Instance of the Ldap Configuration manager attained from the Ldap Adapter.
     */
    private LdapConfigManager iLdapConfigMgr = null;

    /**
     * What is the step size while retrieving range attributes.
     */
    private int iAttrRangeStep;

    /**
     * Is Context Pool enabled?
     */
    private boolean iEnableContextPool = true;

    /**
     * Keyword to look for while retrieving range attributes.
     */
    private final static String ATTR_RANGE_KEYWORD = ";range=";

    /**
     * Constant for Ldap query while retrieving range attributes.
     */
    private final static String ATTR_RANGE_QUERY = ";range={0}-{1}";

    /**
     * Constant for Ldap query while retrieving the last step of range attributes.
     */
    private final static String ATTR_RANGE_LAST_QUERY = ";range={0}-*";

    /**
     * List that acts as a storage for the Pool of Directory contexts.
     */
    private List<TimedDirContext> iContexts = null;

    /**
     * The initial pool size for the DirContext pool. Defaults to 1.
     */
    private int iInitPoolSize = 1;

    /**
     * The number of live context objects in the pool. Defaults to 0
     */
    private int iLiveContexts = 0;

    /**
     * The Maximum pool size for the DirContext pool. Defaults to 0, no max size.
     * If you see the history, this was changed from 1 to 0 to match our published
     * defaults. We were not matching our metatype defaults, resulting in different
     * defaults in different scenarios. PI81923
     */
    private int iMaxPoolSize = 0;

    /**
     * The Preferred pool size. Defaults to 3.
     * If you see the history, this was changed from 1 to 3 to match our published
     * defaults. We were not matching our metatype defaults, resulting in different
     * defaults in different scenarios. PI81923
     */
    private int iPrefPoolSize = 3;

    /**
     * The Pool wait time. Defaults to 3 seconds.
     */
    private long iPoolWaitTime = 3000; // milliseconds

    /**
     * The pool timeout. Defaults to infinite (zero).
     * Sent in from config as milliseconds (see metatype.xml),
     * but we will convert it to seconds when
     * reading in the config. Min time is 1 second. All
     * milliseconds will be rounded to the nearest second.
     * 1499ms == 1s. 1500ms == 2 seconds.
     */
    private long iPoolTimeOut = 0;

    /**
     * Create pool time stamp.
     */
    private long iPoolCreateTimestampSeconds = 0;

    /**
     * Create pool time stamp milliseconds
     */
    private long iPoolCreateTimestampMillisec = 0;

    /**
     * Constant for Single URL
     */
    private static final int URLTYPE_SINGLE = 0;

    /**
     * Constant for URL sequence
     */
    private static final int URLTYPE_SEQUENCE = 1;

    /**
     * Constant for key ENVKEY_ACTIVE_URL
     */
    private static final String ENVKEY_ACTIVE_URL = "_ACTIVE_URL_";

    /**
     * Constant for key ENVKEY_URL_LIST
     */
    private static final String ENVKEY_URL_LIST = "_URL_LIST_";

    /**
     * The name of LDAP Distinguished Name.
     */
    private static final String LDAP_DN = "distinguishedName";

    /**
     * The name of WAS SSL Socket factory class.
     */
    public static final String WAS_SSL_SOCKET_FACTORY = "com.ibm.ws.ssl.protocol.LibertySSLSocketFactory";

    private static final AtomicLong LDAP_STATS_TIMER = new AtomicLong(0);

    private static final AtomicInteger QUICK_LDAP_BIND = new AtomicInteger(0);

    private static int LDAP_CONNECT_TIMEOUT_TRACE = 1000;

    /**
     * The table that stores the Ldap environment.
     */
    private Hashtable<String, Object> iEnvironment = null;

    // Failover servers configuration
    /**
     * Return to primary server configuration
     */
    private boolean iReturnToPrimary = false;

    private long iQueryInterval = 900;

    private long iLastQueryTime = System.currentTimeMillis() / 1000;

    private String iSSLAlias = null;

    private final Control[] iConnCtls = null;

    /**
     * Search Page size. Default to 0
     */
    private int iPageSize = 0;

    /**
     * The name parser
     */
    private NameParser iNameParser = null;

    /**
     * Write to secondary server configuration
     */
    private boolean iWriteToSecondary = false;

    /**
     * Reference to the SSL Socket factory to be used.
     */
    protected String iSSLFactory = null;

    /**
     * Cache offload to disk
     */
    // private boolean iDiskOffLoad = false;

    private Hashtable<String, String[]> iEnityAttrIds = null;

    private String[] iAttrIds = null;

    /**
     * Ignore case for DN in attribute cache.
     */
    private boolean ignoreDNCase = true;

    /**
     * Helper method to get the current active URL
     *
     * @return
     */
    @Trivial
    private String getActiveURL() {
        return (String) iEnvironment.get(ENVKEY_ACTIVE_URL);
    }

    /**
     * Helper method to get the configured Primary URL
     *
     * @return
     */
    @Trivial
    private String getPrimaryURL() {
        return getEnvURLList().get(0);
    }

    /**
     * Helper method to get the configured list of URLs.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Trivial
    private List<String> getEnvURLList() {
        return (List<String>) iEnvironment.get(ENVKEY_URL_LIST);
    }

    /**
     * Helper function to set the given URL as the active URL
     *
     * @param activeURL
     */
    @Trivial
    private void setActiveURL(String activeURL) {
        synchronized (lock) {
            iEnvironment.put(ENVKEY_ACTIVE_URL, activeURL);
        }
    }

    /**
     * Returns the next URL after the specified URL.
     *
     * @param currentURL Current URL
     * @return Next URL
     */
    @Trivial
    private String getNextURL(String currentURL) {
        List<String> urlList = getEnvURLList();
        int urlIndex = getURLIndex(currentURL, urlList);
        return urlList.get((urlIndex + 1) % urlList.size());
    }

    /**
     * Returns a hash key for the provider|name|filter tuple used in the search
     * query-results cache.
     *
     * @param name
     *            The name of the object from which to retrieve attributes.
     * @param attrIds
     *            The identifiers of the attributes to retrieve.
     * @throws NamingException
     *             If a naming exception is encountered.
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

    public NameParser getNameParser() throws WIMException {
        final String METHODNAME = "getNameParser()";
        if (iNameParser == null) {
            TimedDirContext ctx = getDirContext();
            try {
                try {
                    iNameParser = ctx.getNameParser("");
                } catch (NamingException e) {
                    if (!isConnectionException(e, METHODNAME)) {
                        throw e;
                    }
                    ctx = reCreateDirContext(ctx, e.toString());
                    iNameParser = ctx.getNameParser("");
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)), e);
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            } finally {
                releaseDirContext(ctx);
            }
        }
        return iNameParser;
    }

    /**
     * Constructor.
     *
     * @param ldapConfigMgr
     */
    public LdapConnection(LdapConfigManager ldapConfigMgr) {
        iLdapConfigMgr = ldapConfigMgr;
    }

    public void initialize(Map<String, Object> configProps) throws WIMException {
        final String METHODNAME = "initialize(DataObject)";

        reposId = (String) configProps.get(KEY_ID);

        // Initialize LDAP server related settings
        initializeServers(configProps);

        // Initialize the DirContext Pool settings
        initializeContextPool(configProps);

        // initialize the cache pool names
        iAttrsCacheName = reposId + "/" + iAttrsCacheName;
        iSearchResultsCacheName = reposId + "/" + iSearchResultsCacheName;

        // Create Context Pool
        try {
            createContextPool(iInitPoolSize, null);
        } catch (NamingException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Can not create context pool: " + e.toString(true));
            }
        }

        // Initialize Caches setting and create caches.
        initializeCaches(configProps);

        // Ignore case.
        ignoreDNCase = (Boolean) configProps.get(CONFIG_IGNORE_CASE);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Set ignoreDNCase as [" + ignoreDNCase + "]");
    }

    private void initializeCaches(Map<String, Object> configProps) {
        final String METHODNAME = "initializeCaches(DataObject)";
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
            // TODO:: Do we need this configuration?
            // iDiskOffLoad  = Boolean.parseBoolean((String)cacheConfig.get(ConfigConstants.CONFIG_PROP_CACHES_DISK_OFF_LOAD));

            if (attrCacheConfig != null) {
                iAttrsCacheEnabled = (Boolean) attrCacheConfig.get(CONFIG_PROP_ENABLED);
                if (iAttrsCacheEnabled) {
                    // Initialize the Attributes Cache size
                    iAttrsCacheSize = (Integer) attrCacheConfig.get(CONFIG_PROP_CACHE_SIZE);
                    iAttrsCacheTimeOut = (Long) attrCacheConfig.get(CONFIG_PROP_CACHE_TIME_OUT);
                    iAttrsSizeLmit = (Integer) attrCacheConfig.get(CONFIG_PROP_ATTRIBUTE_SIZE_LIMIT);
                    iServerTTLAttr = (String) attrCacheConfig.get(CONFIG_PROP_SERVER_TTL_ATTRIBUTE);

/*
 * TODO:: Cache Distribution is not yet needed.
 * String cacheDistPolicy = attrCacheConfig.getString(CONFIG_PROP_CACHE_DIST_POLICY);
 * if (cacheDistPolicy != null) {
 * iAttrsCacheDistPolicy = LdapHelper.getCacheDistPolicyInt(cacheDistPolicy);
 * }
 */
                    initializeRetrieveAttrIds();
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

    public void initializeRetrieveAttrIds() {
        iEnityAttrIds = new Hashtable<String, String[]>();
        List<LdapEntity> ldapEntities = iLdapConfigMgr.getLdapEntities();
        Set<String> allAttrIds = new HashSet<String>();
        String[] temp = new String[0];
        for (int i = 0; i < ldapEntities.size(); i++) {
            Set<String> attrIds = ldapEntities.get(i).getAttributes();
            attrIds.add(LdapConstants.LDAP_ATTR_OBJECTCLASS);
            // Add external id
            String extId = ldapEntities.get(i).getExtId();
            if (!LDAP_DN.equalsIgnoreCase(extId)) {
                attrIds.add(extId);
            }
            // No need to retrieve userPassword.
            attrIds.remove(LdapConstants.LDAP_ATTR_USER_PASSWORD);
            attrIds.remove(LdapConstants.LDAP_ATTR_UNICODEPWD);
            //If ttl attribute is supported
            if (iServerTTLAttr != null) {
                attrIds.add(iServerTTLAttr);
            }
            // Need to retrieve memberOf attribute
            String membershipAttr = iLdapConfigMgr.getMembershipAttribute();
            if (membershipAttr != null) {
                attrIds.add(membershipAttr);
            }
            iEnityAttrIds.put(ldapEntities.get(i).getName(), attrIds.toArray(temp));
            allAttrIds.addAll(attrIds);
        }
        // Need to retrieve object classes
        allAttrIds.add(LdapConstants.LDAP_ATTR_OBJECTCLASS);

        String[] mbrAttrs = iLdapConfigMgr.getMemberAttributes();
        Set<String> allAttrIdsWithGrpMbrs = new HashSet<String>(allAttrIds);
        for (int i = 0; i < mbrAttrs.length; i++) {
            // Group attribute include member attribute
            allAttrIdsWithGrpMbrs.add(mbrAttrs[i]);
        }

        iAttrIds = allAttrIds.toArray(temp);
    }

    private void initializeContextPool(Map<String, Object> configProps) throws InvalidInitPropertyException {
        final String METHODNAME = "initializeContextPool(DataObject)";
        iEnableContextPool = true;

        List<Map<String, Object>> poolConfigs = Nester.nest(CONFIG_CONTEXT_POOL, configProps);

        Map<String, Object> poolConfig = null;
        if (!poolConfigs.isEmpty()) {
            poolConfig = poolConfigs.get(0);
        }

        // Reuse Connection.
        boolean reuseConn = (Boolean) configProps.get(CONFIG_REUSE_CONNECTION);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Set reuseConnection as [" + reuseConn + "]");

        if (poolConfig != null) {
            iEnableContextPool = (Boolean) poolConfig.get(CONFIG_PROP_ENABLED);
        }

        if (!reuseConn)
            iEnableContextPool = false;

        long providedPoolTimeOut = iPoolTimeOut;

        if (iEnableContextPool) {
            if (poolConfig != null) {
                if (poolConfig.get(CONFIG_PROP_INIT_POOL_SIZE) != null) {
                    iInitPoolSize = (Integer) poolConfig.get((CONFIG_PROP_INIT_POOL_SIZE));
                }
                if (poolConfig.get(CONFIG_PROP_MAX_POOL_SIZE) != null) {
                    iMaxPoolSize = (Integer) poolConfig.get((CONFIG_PROP_MAX_POOL_SIZE));
                }
                if (poolConfig.get(CONFIG_PROP_PREF_POOL_SIZE) != null) {
                    iPrefPoolSize = (Integer) poolConfig.get((CONFIG_PROP_PREF_POOL_SIZE));
                }
                if (iMaxPoolSize != 0 && iMaxPoolSize < iInitPoolSize) {
                    String msg = Tr.formatMessage(tc, WIMMessageKey.INIT_POOL_SIZE_TOO_BIG,
                                                  WIMMessageHelper.generateMsgParms(Integer.valueOf(iInitPoolSize), Integer.valueOf(iMaxPoolSize)));
                    throw new InvalidInitPropertyException(WIMMessageKey.INIT_POOL_SIZE_TOO_BIG, msg);
                }
                if (iMaxPoolSize != 0 && iPrefPoolSize != 0 && iMaxPoolSize < iPrefPoolSize) {
                    String msg = Tr.formatMessage(tc, WIMMessageKey.PREF_POOL_SIZE_TOO_BIG,
                                                  WIMMessageHelper.generateMsgParms(Integer.valueOf(iInitPoolSize), Integer.valueOf(iMaxPoolSize)));
                    throw new InvalidInitPropertyException(WIMMessageKey.PREF_POOL_SIZE_TOO_BIG, msg);
                }
                if (poolConfig.get(CONFIG_PROP_POOL_TIME_OUT) != null) {
                    providedPoolTimeOut = (Long) poolConfig.get((CONFIG_PROP_POOL_TIME_OUT));

                    /**
                     * The metatype is set to long for this property and all
                     * values will be passed as milliseconds.
                     * A value of 0 means no timeout, leave at 0
                     * Between 0 and 1000ms, round up to 1s
                     * Otherwise round to nearest second
                     */
                    if (providedPoolTimeOut > 0 && providedPoolTimeOut <= 1000) {
                        iPoolTimeOut = 1; // override to 1 second
                    } else {
                        iPoolTimeOut = roundToSeconds(providedPoolTimeOut);
                    }
                }
                if (poolConfig.get(CONFIG_PROP_POOL_WAIT_TIME) != null) {
                    iPoolWaitTime = (Long) poolConfig.get((CONFIG_PROP_POOL_WAIT_TIME));
                }
            }
            if (tc.isDebugEnabled()) {
                StringBuffer strBuf = new StringBuffer();
                strBuf.append("\nContext Pool is enabled: ").append("\n");
                strBuf.append("\tInitPoolSize: ").append(iInitPoolSize).append("\n");
                strBuf.append("\tMaxPoolSize: ").append(iMaxPoolSize).append("\n");
                strBuf.append("\tPrefPoolSize: ").append(iPrefPoolSize).append("\n");
                strBuf.append("\tPoolTimeOut: ").append(providedPoolTimeOut).append(" rounded to ").append(iPoolTimeOut).append("\n");
                strBuf.append("\tPoolWaitTime: ").append(iPoolWaitTime);
                Tr.debug(tc, METHODNAME + strBuf.toString());
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + "\nContext Pool is disabled.");
        }
    }

    private void initializeServers(Map<String, Object> configProps) throws WIMException {
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
            if (iLdapConfigMgr.getLdapType().startsWith("MICROSOFT ACTIVE DIRECTORY")) {
                iPageSize = 1000;
            }
        }

        /*
         * Set attribute range step
         */
        if (configProps.containsKey(CONFIG_PROP_ATTRIBUTE_RANGE_STEP)) {
            iAttrRangeStep = Integer.parseInt((String) configProps.get(CONFIG_PROP_ATTRIBUTE_RANGE_STEP));
        } else {
            if (iLdapConfigMgr.getLdapType().equals("AD2000") || iLdapConfigMgr.getLdapType().equals("ADAM")) {
                iAttrRangeStep = 1000;
            } else if (iLdapConfigMgr.getLdapType().startsWith("AD2003")) {
                iAttrRangeStep = 1500;
            } else if (iLdapConfigMgr.getLdapType().startsWith("MICROSOFT ACTIVE DIRECTORY")) {
                iAttrRangeStep = 1000;
            }
        }

        iWriteToSecondary = Boolean.getBoolean((String) configProps.get(CONFIG_PROP_ALLOW_WRITE_TO_SECONDARY_SERVERS));
        iReturnToPrimary = (Boolean) configProps.get(CONFIG_PROP_RETURN_TO_PRIMARY_SERVER);
        iQueryInterval = (Integer) configProps.get(CONFIG_PROP_PRIMARY_SERVER_QUERY_TIME_INTERVAL) * 60;

        // Initialize SSL settings
        initializeSSL(configProps);

        // Initialize servers
        List<Map<String, Object>> serversConfig = Nester.nest(FAILOVER_SERVERS, configProps);

        boolean sslEnabled = (Boolean) configProps.get(CONFIG_PROP_SSL_ENABLED);
        iEnvironment = initializeEnvironmentProperties(sslEnabled, serversConfig, configProps);
    }

    private Hashtable<String, Object> initializeEnvironmentProperties(boolean sslEnabled, List<Map<String, Object>> serversConfig,
                                                                      Map<String, Object> configProps) throws WIMApplicationException {
        final String METHODNAME = "initializeEnvironmentProperties";
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LdapConstants.LDAP_SUN_SPI);

        String urlPrefix = null;
        if (sslEnabled) {
            env.put(LdapConstants.LDAP_ENV_PROP_FACTORY_SOCKET, WAS_SSL_SOCKET_FACTORY);
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            urlPrefix = LdapConstants.LDAP_URL_SSL_PREFIX;
        } else {
            urlPrefix = LdapConstants.LDAP_URL_PREFIX;
        }

        List<String> urlList = new ArrayList<String>();
        // Extract the primary server.
        String mainHost = (String) configProps.get(CONFIG_PROP_HOST);
        int mainPort = (Integer) configProps.get(CONFIG_PROP_PORT);

        urlList.add(urlPrefix + mainHost.trim() + ":" + mainPort);

        // Iterate over the fail-over servers

        for (Map<String, Object> serverConfig : serversConfig) {
            List<Map<String, Object>> servers = Nester.nest(SERVER2, serverConfig);
            for (Map<String, Object> server : servers) {
                String ldapHost = (String) server.get(CONFIG_PROP_HOST);

                if (!(ldapHost.startsWith("[") && ldapHost.endsWith("]"))) {
                    if (LdapHelper.isIPv6Addr(ldapHost)) {
                        ldapHost = LdapHelper.formatIPv6Addr(ldapHost);
                    }
                }

                if (server.get(CONFIG_PROP_PORT) != null) {
                    int ldapPort = (Integer) server.get(CONFIG_PROP_PORT);
                    urlList.add(urlPrefix + ldapHost.trim() + ":" + ldapPort);
                }
            }
        }

        if (urlList != null && urlList.size() > 0) {
            String url = urlList.get(0);
            env.put(ENVKEY_URL_LIST, urlList);
            env.put(ENVKEY_ACTIVE_URL, url);
            env.put(Context.PROVIDER_URL, url);
        }

        String bindDN = (String) configProps.get(CONFIG_PROP_BIND_DN);
        // if ldapAdminDN is null or empty, not throw exception, instead, ignore it to allow anonymous users.
        if (bindDN != null && bindDN.length() > 0) {
            env.put(Context.SECURITY_PRINCIPAL, bindDN);
            SerializableProtectedString sps = (SerializableProtectedString) configProps.get(CONFIG_PROP_BIND_PASSWORD);
            String password = sps == null ? "" : new String(sps.getChars());
            String decodedPassword = PasswordUtil.passwordDecode(password.trim());

            // bindPwd is mandatory
            if (decodedPassword == null || decodedPassword.length() == 0) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.MISSING_INI_PROPERTY, WIMMessageHelper.generateMsgParms(CONFIG_PROP_BIND_PASSWORD));
                throw new MissingInitPropertyException(WIMMessageKey.MISSING_INI_PROPERTY, msg);
            }
            env.put(Context.SECURITY_CREDENTIALS, new ProtectedString(decodedPassword.toCharArray()));
        }

        // Set the ldap connection time out
        Long cTimeout = (Long) configProps.get(CONFIG_PROP_CONNECT_TIMEOUT);
        if (cTimeout != null)
            env.put("com.sun.jndi.ldap.connect.timeout", cTimeout.toString());
        else
            env.put("com.sun.jndi.ldap.connect.timeout", DEFAULT_TIMEOUT);

        // TODO::
/*
 * String authen = (String) configProps.get(ConfigConstants.CONFIG_PROP_AUTHENTICATION);
 * env.put(Context.SECURITY_AUTHENTICATION, authen);
 */

        /*
         * Determine referral handling behavior. Initially the attribute was spelled missing an 'r' so
         * for backwards compatibility, support customers who might still be using it. The "referal"
         * attribute has no default so unless it is set we won't use it.
         */
        String referal = (String) configProps.get(CONFIG_PROP_REFERAL);
        String referral = (String) configProps.get(CONFIG_PROP_REFERRAL);
        referral = referal != null ? referal : referral;
        env.put(Context.REFERRAL, referral.toLowerCase());

        // TODO::
/*
 * String derefAliases = (String) configProps.get(ConfigConstants.CONFIG_PROP_DEREFALIASES);
 * if (!"always".equalsIgnoreCase(derefAliases)) {
 * env.put(LdapConstants.LDAP_ENV_PROP_DEREF_ALIASES, derefAliases);
 * }
 */
        // Add binary attribute names
        String binAttrNames = getBinaryAttributes();
        if (binAttrNames != null && binAttrNames.length() > 0) {
            env.put(LdapConstants.LDAP_ENV_PROP_ATTRIBUTES_BINARY, binAttrNames);
        }

        //TODO::
/*
 * // Add binary attribute names
 * String binAttrNames = getBinaryAttributes();
 * if (binAttrNames != null && binAttrNames.length() > 0) {
 * env.put(LDAP_ENV_PROP_ATTRIBUTES_BINARY, binAttrNames);
 * }
 * // Initialize addtional environment properties. These environ props will overwrite the above settings.
 * List envProps = server.getList(CONFIG_PROP_ENVIRONMENT_PROPERTIES);
 * for (int i = 0; i < envProps.size(); i++) {
 * DataObject envProp = (DataObject) envProps.get(i);
 * String name = envProp.getString(CONFIG_PROP_NAME);
 * String value = envProp.getString(CONFIG_PROP_VALUE);
 * env.put(name, value);
 * }
 */
        if (tc.isDebugEnabled()) {
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("\nLDAP Server(s): ").append(urlList).append("\n");
            strBuf.append("\tBind DN: ").append(bindDN).append("\n");
            // strBuf.append("\tAhthenticate: ").append(authen).append("\n");
            strBuf.append("\tReferral: ").append(referral).append("\n");
            // strBuf.append("\tEnable Connection Pool: ").append(connPool).append("\n");
            // strBuf.append("\tBinary Attributes: ").append(binAttrNames).append("\n");
            // strBuf.append("\tAdditional Evn Props: ").append(envProps);
            Tr.debug(tc, METHODNAME + strBuf.toString());
        }
        return env;
    }

    private void initializeSSL(Map<String, Object> configProps) {
        final String METHODNAME = "initializeSSL(DataObject)";
        // New:: According to the new configuration
        // String sslAlias = (String)configProps.get(ConfigConstants.CONFIG_PROP_SSL_CONFIGURATION);
        String sslAlias = (String) configProps.get("sslRef");

        // Use WAS SSL
        if (sslAlias != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Use WAS SSL Configuration. " + sslAlias);
            }
            iSSLAlias = sslAlias;
        }
    }

    /**
     * Method to create the search results cache, if configured.
     */
    private void createSearchResultsCache() {
        final String METHODNAME = "createSearchResultsCache";

        if (iSearchResultsCacheEnabled) {
            if (FactoryManager.getCacheUtil().isCacheAvailable()) {
                iSearchResultsCache = FactoryManager.getCacheUtil().initialize(iSearchResultsCacheSize, iSearchResultsCacheSize, iSearchResultsCacheTimeOut);
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
                iAttrsCache = FactoryManager.getCacheUtil().initialize(iAttrsCacheSize, iAttrsCacheSize, iAttrsCacheTimeOut);
                if (iAttrsCache != null) {
                    if (tc.isDebugEnabled()) {
                        StringBuilder strBuf = new StringBuilder(METHODNAME);
                        strBuf.append(" \nAttributes Cache: ").append(iAttrsCacheName).append(" is enabled:\n");
                        strBuf.append("\tCacheSize: ").append(iAttrsCacheSize).append("\n");
                        strBuf.append("\tCacheTimeOut: ").append(iAttrsCacheTimeOut).append("\n");
                        strBuf.append("\tCacheSizeLimit: ").append(iAttrsSizeLmit).append("\n");
                        strBuf.append("\tCacheTTLAttr: ").append(iServerTTLAttr).append("\n");
                        Tr.debug(tc, strBuf.toString());
                    }
                }
            }
        }
    }

    /**
     * Method to return the instance of the search results cache.
     *
     * @return
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
     * @return
     */
    private ICacheUtil getAttributesCache() {
        if (iAttrsCache == null) {
            createAttributesCache();
        }
        return iAttrsCache;
    }

    /**
     * Method to invalidate the specified entry from the attributes cache.
     *
     * @param DN
     * @param extId
     * @param uniqueName
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
    public void invalidateNamesCache() {
        if (getSearchResultsCache() != null)
            getSearchResultsCache().clear();
    }

    /**
     * Remove all entries from the attributes cache.
     */
    public void invalidateAttributeCache() {
        if (getAttributesCache() != null)
            getAttributesCache().clear();
    }

    @Trivial
    private String toKey(String name) {
        if (ignoreDNCase)
            return name.toLowerCase();
        else
            return name;
    }

    @Trivial
    public boolean isIgnoreCase() {
        return ignoreDNCase;
    }

    public LdapEntry getEntityByIdentifier(IdentifierType id, List<String> inEntityTypes, List<String> propNames, boolean getMbrshipAttr,
                                           boolean getMbrAttr) throws WIMException {
        return getEntityByIdentifier(id.getExternalName(), id.getExternalId(), id.getUniqueName(),
                                     inEntityTypes, propNames, getMbrshipAttr, getMbrAttr);
    }

    public LdapEntry getEntityByIdentifier(String dn, String extId, String uniqueName, List<String> inEntityTypes,
                                           List<String> propNames, boolean getMbrshipAttr, boolean getMbrAttr) throws WIMException {
        String[] attrIds = iLdapConfigMgr.getAttributeNames(inEntityTypes, propNames, getMbrshipAttr, getMbrAttr);
        Attributes attrs = null;
        if (dn == null && !iLdapConfigMgr.needTranslateRDN()) {
            dn = iLdapConfigMgr.switchToLdapNode(uniqueName);
        }
        // New:: Changed the order of the if-else ladder. Please check against tWAS code for the original order
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
                            boolean isRDN = true;
                            for (int j = 0; j < rdnAttr.length; j++) {
                                if (!rdnAttr[j].equalsIgnoreCase(rdnName[j])) {
                                    isRDN = false;
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

    @FFDCIgnore({ NamingException.class, NameNotFoundException.class })
    private Attributes getAttributes(String name, String[] attrIds) throws WIMException {
        final String METHODNAME = "getAttributes";
        Attributes attributes = null;

        if (iLdapConfigMgr.getUseEncodingInSearchExpression() != null)
            name = LdapHelper.encodeAttribute(name, iLdapConfigMgr.getUseEncodingInSearchExpression());

        if (iAttrRangeStep > 0) {
            attributes = getRangeAttributes(name, attrIds);
        } else {
            TimedDirContext ctx = getDirContext();
            try {
                try {
                    if (attrIds.length > 0) {
                        attributes = ctx.getAttributes(new LdapName(name), attrIds);
                    } else {
                        attributes = new BasicAttributes();
                    }
                } catch (NamingException e) {
                    if (!isConnectionException(e, METHODNAME)) {
                        throw e;
                    }
                    ctx = reCreateDirContext(ctx, e.toString());
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
                releaseDirContext(ctx);
            }
        }
        return attributes;
    }

    @FFDCIgnore({ NameNotFoundException.class, NamingException.class })
    private Attributes getRangeAttributes(String name, String[] attrIds) throws WIMException {
        final String METHODNAME = "getRangeAttributes";

        Attributes attributes = null;
        TimedDirContext ctx = getDirContext();
        try {
            try {
                attributes = ctx.getAttributes(new LdapName(name), attrIds);
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
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
            releaseDirContext(ctx);
        }
        return attributes;
    }

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
            if (missAttrIds.length > 0) {
                if (cachedAttrs != null) {
                    cachedAttrs = (Attributes) cachedAttrs.clone();
                } else {
                    cachedAttrs = new BasicAttributes(true);
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
                getAttributesCache().put(key, cachedAttrs, 1, iAttrsCacheTimeOut, 0, null);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Update " + iAttrsCacheName + "(size: " + getAttributesCache().size() + ")\n" + key
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
            if (cachedAttrs != null) {
                cachedAttrs = (Attributes) cachedAttrs.clone();
            } else {
                cachedAttrs = new BasicAttributes(true);
            }

            //Set extIdAttrs = iLdapConfigMgr.getExtIds();
            for (NamingEnumeration<?> neu = missAttrs.getAll(); neu.hasMoreElements();) {
                Attribute attr = (Attribute) neu.nextElement();
                // If the size of the attributes is larger than the limit, don not cache it.
                if (!(iAttrsSizeLmit > 0 && attr.size() > iAttrsSizeLmit)) {
                    cachedAttrs.put(attr);
                }
            }
            getAttributesCache().put(key, cachedAttrs, 1, iAttrsCacheTimeOut, 0, null);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Update " + iAttrsCacheName + "(size: " + getAttributesCache().size() + ")\n" + key + ": " + cachedAttrs);
            }
        }
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, int scope, String[] attrIds) throws WIMException {
        SearchControls controls = new SearchControls(scope, iCountLimit, iTimeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, null, controls);
    }

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
     * Clone the given SearchResult Enumeration.
     *
     * @param results
     * @param clone1
     * @param clone2
     * @return
     * @throws WIMSystemException
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

    private NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs,
                                                   SearchControls cons, Control requestControls[]) throws WIMException {
        final String METHODNAME = "search(String, String, Object[], SearchControls)";

        if (iPageSize == 0) {
            TimedDirContext ctx = getDirContext();
            NamingEnumeration<SearchResult> neu = null;
            try {
                try {
                    if (filterArgs == null) {
                        neu = ctx.search(new LdapName(name), filterExpr, cons);
                    } else {
                        neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                    }
                } catch (NamingException e) {
                    if (!isConnectionException(e, METHODNAME)) {
                        throw e;
                    }
                    ctx = reCreateDirContext(ctx, e.toString());
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
                releaseDirContext(ctx);
            }

            return neu;
        } else {
            TimedDirContext ctx = getDirContext();
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
                do {
                    NamingEnumeration<SearchResult> neu = null;
                    try {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Search page: " + pageCount);
                        }
                        if (filterArgs == null) {
                            neu = ctx.search(new LdapName(name), filterExpr, cons);
                        } else {
                            neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                        }
                    } catch (NamingException e) {
                        if ((e instanceof CommunicationException) || (e instanceof ServiceUnavailableException)) {
                            ctx = reCreateDirContext(ctx, e.toString());

                            /*
                             * If the cookie is not null, we are recovering from a failure after receiving
                             * results, so reuse the cookie to begin where we left off.
                             */
                            PagedResultsControl pagedResultsControl = null;
                            if (cookie != null && cookie.length > 0) {
                                pagedResultsControl = new PagedResultsControl(iPageSize, cookie, false);
                            } else {
                                pagedResultsControl = new PagedResultsControl(iPageSize, false);
                            }

                            if (requestControls != null) {
                                ctx.setRequestControls(new Control[] { requestControls[0], pagedResultsControl });
                            } else {
                                ctx.setRequestControls(new Control[] { pagedResultsControl });
                            }
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, METHODNAME + " Search page (retry): " + pageCount);
                            }
                            if (filterArgs == null) {
                                neu = ctx.search(new LdapName(name), filterExpr, cons);
                            } else {
                                neu = ctx.search(new LdapName(name), filterExpr, filterArgs, cons);
                            }
                        } else {
                            throw e;
                        }
                    }
                    if (neu != null) {
                        pageCount++;
                        while (neu.hasMoreElements()) {
                            allNeu.add(neu.nextElement());
                            count++;
                        }
                        Control[] resCtrls = ctx.getResponseControls();
                        if (resCtrls != null && resCtrls.length > 0) {
                            PagedResultsResponseControl resCtrl = (PagedResultsResponseControl) resCtrls[0];
                            cookie = resCtrl.getCookie();
                            if (cookie != null && cookie.length > 0) {
                                ctx.setRequestControls(new Control[] { new PagedResultsControl(iPageSize, cookie, false) });
                            }
                        }
                    }
                } while (cookie != null && cookie.length > 0 && count < cons.getCountLimit());
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
                releaseDirContext(ctx);
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
        final String METHODNAME = "getAttributesByUniqueName";

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
        String filter = iLdapConfigMgr.getLdapRDNFilter(null, LdapHelper.getRDN(uniqueName));
        String parent = LdapHelper.getParentDN(uniqueName);
        SearchControls controls = new SearchControls();
        controls.setTimeLimit(iTimeLimit);
        controls.setCountLimit(iCountLimit);
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setReturningAttributes(attrIds); // Only retrieve requested attributes
        controls.setReturningObjFlag(false);

        TimedDirContext ctx = getDirContext();
        NamingEnumeration<SearchResult> neu = null;
        try {
            try {
                neu = ctx.search(parent, filter, controls);
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                neu = ctx.search(parent, filter, controls);
            }
            if (neu.hasMoreElements()) {
                SearchResult thisEntry = neu.nextElement();
                if (thisEntry != null) {
                    DN = LdapHelper.prepareDN(thisEntry.getName(), parent);
                    attributes = thisEntry.getAttributes();
                    if (iAttrRangeStep > 0) {
                        supportRangeAttributes(attributes, DN, ctx);
                    }
                }
            }

        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_NOT_FOUND, WIMMessageHelper.generateMsgParms(parent));
            throw new EntityNotFoundException(WIMMessageKey.ENTITY_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
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

    public Set<LdapEntry> searchEntities(String base, String filter, Object[] filterArgs, int scope, List<String> inEntityTypes,
                                         List<String> propNames, boolean getMbrshipAttr, boolean getMbrAttr) throws WIMException {
        return searchEntities(base, filter, filterArgs, scope, inEntityTypes, propNames, getMbrshipAttr, getMbrAttr,
                              iCountLimit, iTimeLimit);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, int scope, String[] attrIds, int countLimit,
                                                  int timeLimit) throws WIMException {
        SearchControls controls = new SearchControls(scope, countLimit, timeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, null, controls);
    }

    public NamingEnumeration<SearchResult> search(String name, String filter, Object[] filterArgs, int scope, String[] attrIds,
                                                  int countLimit, int timeLimit) throws WIMException {
        SearchControls controls = new SearchControls(scope, countLimit, timeLimit, attrIds, false, false);
        return checkSearchCache(name, filter, filterArgs, controls);
    }

    public Set<LdapEntry> searchEntities(String base, String filter, Object[] filterArgs, int scope, List<String> inEntityTypes,
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
            neu = search(base, filter, scope, attrIds, countLimit, timeLimit);
        } else {
            neu = search(base, filter, filterArgs, scope, attrIds, countLimit, timeLimit);
        }

        Set<LdapEntry> entities = this.populateResultSet(neu, base, scope, inEntityTypes, attrIds);

        return entities;
    }

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
                if (iLdapConfigMgr.isRacf() && !inEntityTypes.contains(entityType))
                    continue;
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
                    Tr.debug(tc, METHODNAME + " Range attriute retrieved: " + attrName);
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
                        if ((e instanceof CommunicationException) || (e instanceof ServiceUnavailableException)) {
                            ctx = reCreateDirContext(ctx, e.toString());
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

    @FFDCIgnore({ InterruptedException.class, NamingException.class })
    private TimedDirContext getDirContext() throws WIMSystemException {
        final String METHODNAME = "getDirContext";
        TimedDirContext ctx = null;
        long currentTime = roundToSeconds(System.currentTimeMillis());
        // String sDomainName = DomainManagerUtils.getDomainName();

        if (iEnableContextPool) {
            do {
                //Get the lock for the current domain
                synchronized (lock) {
                    if (iContexts == null) {
                        try {
                            createContextPool(iInitPoolSize, null);
                        } catch (NamingException e) {
                            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                        }
                    }

                    if (iContexts.size() > 0) {
                        ctx = iContexts.remove(iContexts.size() - 1);
                    } else if (iLiveContexts < iMaxPoolSize || iMaxPoolSize == 0) {
                        //Will create later outside of the synchronized code.
                        iLiveContexts++;
                    } else {
                        try {
                            lock.wait(iPoolWaitTime);
                        } catch (InterruptedException e) {
                            // This is ok...if exception occurs, then continue...
                        }
                        continue;
                    }
                }
                TimedDirContext oldCtx = null;
                if (ctx != null) {
                    // If iPoolTimeOut > 0, check if the DirContex expires or not.
                    // If iPoolTimeOut = 0, the DirContext will be used forever until it is staled.
                    if (iPoolTimeOut > 0 && (currentTime - ctx.getPoolTimestamp()) > iPoolTimeOut) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " ContextPool: context is time out. currentTime=" + currentTime + ", createTime="
                                         + ctx.getPoolTimestamp() + ", iPoolTimeOut=" + iPoolTimeOut);
                        }
                        oldCtx = ctx;
                        ctx = null;
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " ContextPool: no free context, create a new one...");
                    }
                }
                if (ctx == null) {
                    try {
                        ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getActiveURL()));
                    } catch (NamingException e) {
                        iLiveContexts--;
                        String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                        throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                    }
                } else {
                    // Check
                    if (iReturnToPrimary && (currentTime - iLastQueryTime) > iQueryInterval) {
                        try {
                            String currentURL = getProviderURL(ctx);
                            String primaryURL = getPrimaryURL();
                            if (!primaryURL.equalsIgnoreCase(currentURL)) {
                                // Test if primaryURL is available
                                Hashtable<?, ?> env = getEnvironment(URLTYPE_SINGLE, primaryURL);
                                boolean primaryOK = false;
                                try {
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "'...");
                                    }
                                    TimedDirContext testCtx = createDirContext(env);
                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': success");
                                    }

                                    // Log the URL being used.
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));

                                    primaryOK = true;
                                    TimedDirContext tempCtx = ctx;
                                    try {
                                        tempCtx.close();
                                    } catch (NamingException e) {
                                        if (tc.isDebugEnabled())
                                            Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                                    }
                                    ctx = testCtx;
                                } catch (NamingException e) {
                                    if (tc.isInfoEnabled())
                                        Tr.info(tc, WIMMessageKey.CANNOT_CONNECT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(primaryURL));

                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': fail");
                                    }
                                }

                                // Refresh context pool if another thread has not already done so
                                if (primaryOK) {
                                    synchronized (lock) {
                                        if (!getActiveURL().equalsIgnoreCase(primaryURL)) {
                                            createContextPool(iLiveContexts - 1, primaryURL);
                                            ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                                        }
                                    }
                                }
                            }
                            iLastQueryTime = currentTime;
                        } catch (NamingException e) {
                            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                        }
                    }
                }
                if (oldCtx != null) {
                    try {
                        oldCtx.close();
                    } catch (NamingException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                    }
                }
            } while (ctx == null);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size() + ", currentTime=" + currentTime + ", createTime="
                             + ctx.getPoolTimestamp());
            }
        } else {
            try {
                // Test if primaryURL is available
                if (iReturnToPrimary && (currentTime - iLastQueryTime) > iQueryInterval) {
                    String primaryURL = getPrimaryURL();
                    if (!primaryURL.equalsIgnoreCase(getActiveURL())) {
                        Hashtable<?, ?> env = getEnvironment(URLTYPE_SINGLE, primaryURL);
                        try {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "'...");
                            ctx = createDirContext(env);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': success");

                            if (tc.isDebugEnabled())
                                Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
                        } catch (NamingException e) {
                            if (tc.isInfoEnabled())
                                Tr.info(tc, WIMMessageKey.CANNOT_CONNECT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(primaryURL));
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, METHODNAME + " Ping primary server '" + primaryURL + "': fail");
                        }
                    }
                    iLastQueryTime = currentTime;
                }

                // create the connection
                if (ctx == null) {
                    ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getActiveURL()));

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
                }
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
        return ctx;
    }

    /**
     * Recreate a Directory context, where the oldContext failed with the given error message.
     *
     * @param oldCtx
     * @param errorMessage
     * @return
     * @throws WIMSystemException
     */
    public TimedDirContext reCreateDirContext(TimedDirContext oldCtx, String errorMessage) throws WIMSystemException {
        final String METHODNAME = "DirContext reCreateDirContext(String errorMessage)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " Communication exception occurs: " + errorMessage + " Creating a new connection.");
        }

        try {
            // PM95697, check if we should get or create a DirContext
            Long oldCreateTimeStamp = oldCtx.getCreateTimestamp();
            TimedDirContext ctx;
            if (oldCreateTimeStamp < iPoolCreateTimestampMillisec) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Pool refreshed, skip to getDirContext. oldCreateTimeStamp: " + oldCreateTimeStamp + " iPoolCreateTimestampMillisec:"
                                 + iPoolCreateTimestampMillisec);
                }
                ctx = getDirContext();
            } else {
                String oldURL = getProviderURL(oldCtx);
                ctx = createDirContext(getEnvironment(URLTYPE_SEQUENCE, getNextURL(oldURL)));
                String newURL = getProviderURL(ctx);

                synchronized (lock) {
                    // Refresh context pool if another thread hasn't already done so
                    if (oldCtx.getCreateTimestamp() >= iPoolCreateTimestampSeconds) {
                        createContextPool(iLiveContexts - 1, newURL);
                        ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                    }
                }
            }

            oldCtx.close();

            if (tc.isDebugEnabled())
                Tr.debug(tc, WIMMessageKey.CURRENT_LDAP_SERVER, WIMMessageHelper.generateMsgParms(getActiveURL()));
            return ctx;
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        }
    }

    /**
     * Create a directory context
     *
     * @param principal
     * @param credential
     * @return
     * @throws javax.naming.NamingException
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(NamingException.class)
    public TimedDirContext createDirContext(String principal, byte[] credential) throws javax.naming.NamingException {
        final String METHODNAME = "createDirContext(String, byte[])";

        String activeURL = getActiveURL();
        Hashtable<Object, Object> environment = (Hashtable<Object, Object>) getEnvironment(URLTYPE_SINGLE, activeURL);
        environment.put(Context.SECURITY_PRINCIPAL, principal);
        environment.put(Context.SECURITY_CREDENTIALS, credential);

        Properties currentSSLProps = FactoryManager.getSSLUtil().getSSLPropertiesOnThread();
        try {
            if (iSSLAlias != null) {
                try {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Use WAS SSL Configuration.");
                    FactoryManager.getSSLUtil().setSSLAlias(iSSLAlias, environment);
                } catch (Exception ssle) {
                    throw new NamingException(ssle.getMessage());
                }
            }

            // New:: Set the classloader so that a class in the package can be loaded by JNDI
            Thread thread = Thread.currentThread();
            ClassLoader origCL = thread.getContextClassLoader();
            thread.setContextClassLoader(getClass().getClassLoader());
            try {
                TimedDirContext ctx = null;
                try {
                    ctx = new TimedDirContext(environment, getConnectionRequestControls(), roundToSeconds(System.currentTimeMillis()));
                } catch (NamingException e) {
                    if (!isConnectionException(e, METHODNAME)) {
                        throw e;
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Encountered an exception while creating a context : " + e.getMessage());
                    }

                    // Get the Next URL
                    environment = (Hashtable<Object, Object>) getEnvironment(URLTYPE_SEQUENCE, getNextURL(activeURL));
                    // Reset the security credentials on the environment.
                    environment.put(Context.SECURITY_PRINCIPAL, principal);
                    environment.put(Context.SECURITY_CREDENTIALS, credential);

                    ctx = new TimedDirContext(environment, getConnectionRequestControls(), roundToSeconds(System.currentTimeMillis()));
                    String newURL = getProviderURL(ctx);
                    long creationTimeMillisec = System.currentTimeMillis();

                    synchronized (lock) {
                        // Refresh context pool if another thread hasnt already done so
                        if (creationTimeMillisec > iPoolCreateTimestampMillisec) {
                            createContextPool(iLiveContexts, newURL);
                            ctx.setCreateTimestamp(iPoolCreateTimestampSeconds);
                        }
                    }
                }
                return ctx;
            } finally {
                thread.setContextClassLoader(origCL);
            }
        } finally {
            FactoryManager.getSSLUtil().setSSLPropertiesOnThread(currentSSLProps);
        }
    }

    /**
     * Create a directory context
     *
     * @param env
     * @return
     * @throws NamingException
     */
    public TimedDirContext createDirContext(Hashtable<?, ?> env) throws NamingException {
        return createDirContext(env, roundToSeconds(System.currentTimeMillis()));
    }

    /**
     * Create a directory context
     *
     * @param env
     * @return
     * @throws NamingException
     */
    public TimedDirContext createDirContext(Hashtable<?, ?> env, long createTimestamp) throws NamingException {
        Object o = env.get(Context.SECURITY_CREDENTIALS);
        // Check if the credential is a protected string. It will be unprotected if this is an
        // anonymous bind
        if (o instanceof ProtectedString) {
            // Reset the bindPassword to simple string.
            ProtectedString sps = (ProtectedString) env.get(Context.SECURITY_CREDENTIALS);
            String password = sps == null ? "" : new String(sps.getChars());
            String decodedPassword = PasswordUtil.passwordDecode(password.trim());
            ((Hashtable<Object, Object>) env).put(Context.SECURITY_CREDENTIALS, decodedPassword);
        }

        Properties currentSSLProps = FactoryManager.getSSLUtil().getSSLPropertiesOnThread();
        try {
            if (iSSLAlias != null) {
                try {
                    FactoryManager.getSSLUtil().setSSLAlias(iSSLAlias, env);
                } catch (Exception ssle) {
                    throw new NamingException(ssle.getMessage());
                }
            }

            // New:: Set the classloader so that a class in the package can be loaded by JNDI
            Thread thread = Thread.currentThread();
            ClassLoader origCL = thread.getContextClassLoader();
            thread.setContextClassLoader(getClass().getClassLoader());
            try {
                TimedDirContext ctx = new TimedDirContext(env, getConnectionRequestControls(), createTimestamp);
                String newURL = getProviderURL(ctx);
                // Set the active URL if context pool is disabled,
                // otherwise active URL will be set when pool is refreshed
                if (!iEnableContextPool)
                    if (!newURL.equalsIgnoreCase(getActiveURL()))
                        setActiveURL(newURL);

                return ctx;
            } finally {
                thread.setContextClassLoader(origCL);
            }
        } finally {
            FactoryManager.getSSLUtil().setSSLPropertiesOnThread(currentSSLProps);
        }
    }

    /**
     * Returns LDAP environment containing specified URL sequence.
     *
     * @param type Single or sequence
     * @param startingURL Starting URL
     * @return Environment containing specified URL sequence
     */
    @SuppressWarnings("unchecked")
    private Hashtable<?, ?> getEnvironment(int type, String startingURL) {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>(iEnvironment);
        List<String> urlList = (List<String>) env.remove(ENVKEY_URL_LIST);
        int numURLs = urlList.size();

        // get active URL index
        int startingURLIndex = getURLIndex(startingURL, urlList);

        // generate the sequence
        String ldapUrl = null;
        for (int i = startingURLIndex; i < startingURLIndex + numURLs; i++) {
            if (i > startingURLIndex)
                ldapUrl = ldapUrl + " " + urlList.get(i % numURLs);
            else
                ldapUrl = urlList.get(i % numURLs);

            if (type == URLTYPE_SINGLE)
                break;
        }

        env.put(Context.PROVIDER_URL, ldapUrl);
        env.remove(ENVKEY_ACTIVE_URL);

        return env;
    }

    /**
     * Returns URL index in the URL list.
     *
     * @param url URL
     * @param urlList List of URLs
     * @return URL index
     */
    private int getURLIndex(String url, List<String> urlList) {
        int urlIndex = 0;
        int numURLs = urlList.size();

        // get URL index
        if (url != null)
            for (int i = 0; i < numURLs; i++)
                if ((urlList.get(i)).equalsIgnoreCase(url)) {
                    urlIndex = i;
                    break;
                }

        return urlIndex;
    }

    /**
     * Get the provider URL from the given directory context.
     *
     * @param ctx
     * @return
     */
    @Trivial
    @FFDCIgnore(NamingException.class)
    private String getProviderURL(TimedDirContext ctx) {
        try {
            return (String) ctx.getEnvironment().get(Context.PROVIDER_URL);
        } catch (NamingException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getProviderURL", e.toString(true));
            }
            return "(null)";
        }
    }

    private Control[] getConnectionRequestControls() {
        return iConnCtls;
    }

    /**
     * Create a directory context pool of the specified size.
     *
     * @param poolSize
     * @param providerURL
     * @throws NamingException
     */
    private void createContextPool(int poolSize, String providerURL) throws NamingException {
        final String METHODNAME = "createContextPool";

        // Validate provider URL
        if (providerURL == null)
            providerURL = getPrimaryURL();

        if (iEnableContextPool) {
            long currentTimeMillisec = System.currentTimeMillis();
            long currentTimeSeconds = roundToSeconds(currentTimeMillisec);

            // Don't purge the pool more than once per second
            // This prevents multiple threads from purging the pool
            if (currentTimeMillisec - iPoolCreateTimestampMillisec > 1000) {
                List<TimedDirContext> contexts = new Vector<TimedDirContext>(poolSize);
                Hashtable<?, ?> env = getEnvironment(URLTYPE_SEQUENCE, providerURL);

                String currentURL = null;
                try {
                    for (int i = 0; i < poolSize; i++) {
                        TimedDirContext ctx = createDirContext(env, currentTimeSeconds);
                        currentURL = getProviderURL(ctx);
                        if (!providerURL.equalsIgnoreCase(currentURL)) {
                            env = getEnvironment(URLTYPE_SEQUENCE, currentURL);
                            providerURL = currentURL;
                        }
                        contexts.add(ctx);
                        //iLiveContexts++;
                    }
                } catch (NamingException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context Pool creation FAILED for " + Thread.currentThread() + ", iLiveContext=" + iLiveContexts, e);
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Cleanup contexts in temp pool: " + contexts.size());
                    }
                    for (int j = 0; j < contexts.size(); j++) {
                        try {
                            TimedDirContext ctx = contexts.get(j);
                            ctx.close();
                        } catch (Exception ee) {
                        }
                    }
                    throw e;
                }
                iLiveContexts += poolSize;

                // set active URL
                setActiveURL(providerURL);

                List<TimedDirContext> oldCtxs = iContexts;
                iContexts = contexts;
                iPoolCreateTimestampSeconds = currentTimeSeconds;
                iPoolCreateTimestampMillisec = currentTimeMillisec;
                closeContextPool(oldCtxs);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, METHODNAME + " Active Provider URL: " + getActiveURL());
                    Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size(),
                             ", iPoolCreateTimestampSeconds=" + iPoolCreateTimestampSeconds);
                }
            } else if (tc.isDebugEnabled())
                Tr.debug(tc, METHODNAME + " Pool has already been purged within past second... skipping purge");
        } else
            setActiveURL(providerURL);
    }

    @FFDCIgnore(NamingException.class)
    private void closeContextPool(List<TimedDirContext> contexts) {
        final String METHODNAME = "closeContextPool";
        if (contexts != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " Context pool being closed by " + Thread.currentThread() + ", Context pool size=" + contexts.size());
            }
            for (int i = 0; i < contexts.size(); i++) {
                TimedDirContext context = contexts.get(i);
                try {
                    context.close();
                    iLiveContexts--;
                } catch (NamingException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " Can not close LDAP connection: " + e.toString(true));
                }
            }
        }
    }

    /**
     * Most LDAPs throw CommunicationException when LDAP server is down, but
     * z/OS sometime throws ServiceUnavailableException when ldap server is down.
     */
    private boolean isConnectionException(NamingException e, String METHODNAME) {
        if (e instanceof CommunicationException) {
            return true;
        } else if (e instanceof ServiceUnavailableException) {
            return true;
        }
        return false;
    }

    /**
     * Release the given directory context.
     *
     * @param ctx
     * @throws WIMException
     */
    @FFDCIgnore(NamingException.class)
    public void releaseDirContext(TimedDirContext ctx) throws WIMException {
        final String METHODNAME = "releaseDirContext";
        if (iEnableContextPool) {

            //Get the lock for the current domain
            synchronized (lock) {
                // If the DirContextTTL is 0, no need to put it back to pool
                // If the size of the pool is larger than minimum size or total dirContextes larger than max size
                // If context URL no longer matches active URL, then discard
                if (iContexts.size() >= iPrefPoolSize || (iMaxPoolSize != 0 && iLiveContexts > iMaxPoolSize)
                    || ctx.getCreateTimestamp() < iPoolCreateTimestampSeconds
                    || !getProviderURL(ctx).equalsIgnoreCase(getActiveURL())) {
                    try {
                        iLiveContexts--; //PM95697
                        ctx.close();
                    } catch (NamingException e) {
                        String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                        throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
                    }

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context is discarded.");
                    }
                } else {
                    if (iContexts != null && iContexts.size() > 0 && iContexts.contains(ctx)) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Context already present in Context pool. No need to add it again to context pool.  ContextPool: total=" + iLiveContexts
                                         + ", poolSize=" + iContexts.size());
                        }
                    } else {
                        if (iContexts != null)
                            iContexts.add(ctx);
                        if (iPoolTimeOut > 0) {
                            ctx.setPoolTimeStamp(roundToSeconds(System.currentTimeMillis()));
                        }

                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, METHODNAME + " Before Notifying the waiting threads and Context is back to pool.  ContextPool: total=" + iLiveContexts
                                         + ", poolSize=" + iContexts.size());
                        }
                    }
                    lock.notifyAll();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, METHODNAME + " Context is back to pool.");
                    }

                }
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, METHODNAME + " ContextPool: total=" + iLiveContexts + ", poolSize=" + iContexts.size());
            }
        } else {
            try {
                ctx.close();
            } catch (NamingException e) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
                throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
            }
        }
    }

    /**
     * Search using operational attribute specified in the parameter.
     *
     * @param DN
     * @param filter
     * @param oprAttribute
     * @param grpTypes
     * @param supportedProps
     * @return
     */
    public SearchResult searchByOperationalAttribute(String DN, String filter, List<String> inEntityTypes, List<String> propNames, String oprAttribute) throws WIMException {
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
        neu = search(DN, filter, SearchControls.OBJECT_SCOPE, attrIds, iCountLimit, iTimeLimit);

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

    private String getBinaryAttributes() {
        // TODO:: This is currently hardcoded for objectguid.

        // Add binary settings for all octet string attributes.
        /*
         * StringBuffer binaryAttrNamesBuffer = new StringBuffer(LdapConstants.LDAP_ATTR_OBJECTGUID);
         * return binaryAttrNamesBuffer.toString().trim();
         */

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
     * Delete the given name from the Ldap tree.
     *
     * @param name
     * @throws WIMException
     */
    public void destroySubcontext(String name) throws WIMException {
        final String METHODNAME = "destroySubcontext";
        TimedDirContext ctx = getDirContext();
        // checkWritePermission(ctx);
        try {
            try {
                ctx.destroySubcontext(new LdapName(name));
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                ctx.destroySubcontext(new LdapName(name));
            }
        } catch (ContextNotEmptyException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_HAS_DESCENDENTS, WIMMessageHelper.generateMsgParms(name));
            throw new EntityHasDescendantsException(WIMMessageKey.ENTITY_HAS_DESCENDENTS, msg, e);
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.LDAP_ENTRY_NOT_FOUND, WIMMessageHelper.generateMsgParms(name, e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.LDAP_ENTRY_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
        }
    }

    /**
     * Modify the given ldap name according to the specified modification items.
     *
     * @param name
     * @param mods
     * @throws NamingException
     * @throws WIMException
     */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException, WIMException {
        final String METHODNAME = "modifyAttributes(Name name, ModificationItem[] mods)";
        TimedDirContext ctx = getDirContext();
        // checkWritePermission(ctx);
        try {
            try {
                ctx.modifyAttributes(new LdapName(name), mods);
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                ctx.modifyAttributes(new LdapName(name), mods);
            }
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } catch (NamingException e) {
            throw e;
        } finally {
            releaseDirContext(ctx);
        }
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException, WIMException {
        final String METHODNAME = "modifyAttributes(Name, int, Attributes)";

        TimedDirContext ctx = getDirContext();
        checkWritePermission(ctx);
        try {
            try {
                ctx.modifyAttributes(new LdapName(name), mod_op, attrs);
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                ctx.modifyAttributes(new LdapName(name), mod_op, attrs);
            }
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } catch (NamingException e) {
            throw e;
        }

        finally {
            releaseDirContext(ctx);
        }
    }

    public DirContext createSubcontext(String name, Attributes attrs) throws WIMException {
        final String METHODNAME = "createSubcontext";
        DirContext dirContext = null;
        TimedDirContext ctx = getDirContext();
        checkWritePermission(ctx);
        try {
            try {
                long startTime = System.currentTimeMillis();
                dirContext = ctx.createSubcontext(new LdapName(name), attrs);
                long endTime = System.currentTimeMillis();
                if ((endTime - startTime) > LDAP_CONNECT_TIMEOUT_TRACE) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " **LDAPConnect time: " + (endTime - startTime) + " ms, lock held " + Thread.holdsLock(lock)
                                     + ", principal=" + name);
                } else {
                    handleBindStat(endTime - startTime);
                }
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                long startTime = System.currentTimeMillis();
                dirContext = ctx.createSubcontext(new LdapName(name), attrs);
                long endTime = System.currentTimeMillis();
                if ((endTime - startTime) > LDAP_CONNECT_TIMEOUT_TRACE) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, METHODNAME + " **LDAPConnect time: " + (endTime - startTime) + " ms, lock held " + Thread.holdsLock(lock)
                                     + ", principal=" + name);
                } else {
                    handleBindStat(endTime - startTime);
                }
            }
        } catch (NameAlreadyBoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.ENTITY_ALREADY_EXIST, WIMMessageHelper.generateMsgParms(name));
            throw new EntityAlreadyExistsException(WIMMessageKey.ENTITY_ALREADY_EXIST, msg, e);
        } catch (NameNotFoundException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.PARENT_NOT_FOUND, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new EntityNotFoundException(WIMMessageKey.PARENT_NOT_FOUND, msg, e);
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
        }
        return dirContext;
    }

    /**
     * Track the count of "quick" binds. Dump the updated statistic to the log at most once every 30 seconds.
     *
     * @param elapsedTime The time in milliseconds that the bind took
     */
    private void handleBindStat(long elapsedTime) {
        String METHODNAME = "handleBindStat(long)";
        if (elapsedTime < LDAP_CONNECT_TIMEOUT_TRACE) {
            QUICK_LDAP_BIND.getAndIncrement();
        }

        long now = System.currentTimeMillis();

        // Print out at most every 30 minutes the latest number of "quick" binds
        if (now - LDAP_STATS_TIMER.get() > 1800000) {
            //Update the last update time, then make certain no one beat us to it
            long lastUpdated = LDAP_STATS_TIMER.getAndSet(now);
            if (now - lastUpdated > 1800000) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " **LDAPBindStat: " + QUICK_LDAP_BIND.get() + " binds took less then " + LDAP_CONNECT_TIMEOUT_TRACE + " ms");
            }
        }
    }

    private void checkWritePermission(TimedDirContext ctx) throws OperationNotSupportedException {
        if (!iWriteToSecondary) {
            String providerURL = getProviderURL(ctx);
            if (!getPrimaryURL().equalsIgnoreCase(providerURL)) {
                String msg = Tr.formatMessage(tc, WIMMessageKey.WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED, WIMMessageHelper.generateMsgParms(providerURL));
                throw new OperationNotSupportedException(WIMMessageKey.WRITE_TO_SECONDARY_SERVERS_NOT_ALLOWED, msg);
            }
        }
    }

    /**
     * Rename an entity
     *
     * @param dn
     * @param newDn
     */
    public void rename(String dn, String newDn) throws WIMException {
        final String METHODNAME = "rename";
        TimedDirContext ctx = getDirContext();
        checkWritePermission(ctx);
        try {
            try {
                ctx.rename(dn, newDn);
            } catch (NamingException e) {
                if (!isConnectionException(e, METHODNAME)) {
                    throw e;
                }
                ctx = reCreateDirContext(ctx, e.toString());
                ctx.rename(dn, newDn);
            }
        } catch (NamingException e) {
            String msg = Tr.formatMessage(tc, WIMMessageKey.NAMING_EXCEPTION, WIMMessageHelper.generateMsgParms(e.toString(true)));
            throw new WIMSystemException(WIMMessageKey.NAMING_EXCEPTION, msg, e);
        } finally {
            releaseDirContext(ctx);
        }
    }

    /**
     * Return the searchTimeout
     *
     */
    public int getSearchTimeout() {
        return iTimeLimit;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName()).append(":{");
        sb.append("Repositoriy ID=").append(reposId).append("\n");
        sb.append(", SSL Alias=").append(iSSLAlias).append("\n");
        sb.append(", SSL Factory=").append(iSSLFactory).append("\n");
        sb.append(", Server TTL Attribute=").append(iServerTTLAttr).append("\n");
        sb.append(", Page Size=").append(iPageSize).append("\n");
        sb.append(", Attribute Range Step=").append(iAttrRangeStep).append("\n");
        sb.append(", Return to primary=").append(iReturnToPrimary).append("\n");
        sb.append(", Last Query Time=").append(iLastQueryTime).append("\n");
        sb.append(", Query Interval=").append(iQueryInterval).append("\n");
        sb.append(", Write To Secondary=").append(iWriteToSecondary).append("\n");
        sb.append(", Ignore DN Case=").append(ignoreDNCase).append("\n");
        sb.append(", JNDI Environment=").append(iEnvironment).append("\n");
        sb.append(", Search Result Count Limit=").append(iCountLimit).append("\n");
        sb.append(", Search Result Time Limit=").append(iTimeLimit).append("\n");

        /* Context Pool */
        sb.append(", Context Pool{ Enabled=").append(iEnableContextPool);
        sb.append(", Initial Size=").append(iInitPoolSize);
        sb.append(", Max Size=").append(iMaxPoolSize);
        sb.append(", Preferred Size=").append(iPrefPoolSize);
        sb.append(", Wait Time=").append(iPoolWaitTime);
        sb.append(", Timeout=").append(iPoolTimeOut);
        sb.append(", Create Timestamp=").append(iPoolCreateTimestampSeconds);
        sb.append(" }");

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

    private long roundToSeconds(long timeInMilliseconds) {
        long returnInSeconds = timeInMilliseconds / 1000;
        if (timeInMilliseconds % 1000 > 499) {
            returnInSeconds++;
        }
        return returnInSeconds;
    }
}
