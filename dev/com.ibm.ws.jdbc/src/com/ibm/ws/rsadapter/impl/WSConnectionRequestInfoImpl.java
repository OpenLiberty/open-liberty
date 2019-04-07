/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.rsadapter.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.resource.spi.ConnectionRequestInfo;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.jca.adapter.WSConnectionManager;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.rsadapter.AdapterUtil;

/**
 * The class implements the javax.resource.spi.ConnectionRequestInfo interface. The
 * ConnectionRequestInfoImpl enables a resource adapter to pass its own request
 * specific data structure across the connection request flow. A resource adapter
 * extends the empty interface to supports its own data structures for connection request.
 * 
 * <p>A typical use allows a resource adapter to handle application component specified
 * per-connection request properties (example -client ID, language). The application
 * server passes these properties back across to match/createManagedConnection calls
 * on the resource adapter. These properties remain opaque to the application server
 * during the connection request flow.
 * 
 * <p>Once the ConnectionRequestInfo reaches match / createManagedConnection methods on the
 * ManagedConnectionFactoryinstance, resource adapter uses this additional per-request
 * information to do connection creation and matching.
 * 
 * <p> This ConnectionRequestInfoImpl consists the following properties:
 * <ul>
 * <li>Isolation level</li>
 * <li>User name</li>
 * <li>Password</li>
 * <li>Catalog</li>
 * <li>IsReadOnly indicator</li>
 * <li>Sharding Key</li>
 * <li>Super Sharding Key</li>
 * <li>Type Map</li>
 * <li>Cursor Holdability</li>
 * <li>Schema</li>
 * <li>DataSource Configuration ID</li>
 * </ul>
 * 
 * <p>Several of the Connection properties, including isolationLevel, catalog, isReadOnly,
 * and typeMap, may change during normal use of the ManagedConnection. In this case, they
 * will be updated on the ConnectionRequestInfo using the provided setter methods.</p>
 */
public class WSConnectionRequestInfoImpl implements ConnectionRequestInfo, FFDCSelfIntrospectable {
    private static final TraceComponent tc = Tr.register(WSConnectionRequestInfoImpl.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE);

    /**
     * This flag will cause chaning the cri instance to throw an exception
     * unless changed to true. developers will need to make sure to use a newed up version
     * of the CRI and to call markAsChangable on it to change this flag values.
     * Developers will also need to make sure that setters check for this flag and throw exception
     * if flag is not set to true.
     * Finally, developers will need to make sure any changes done here are ok with J2c pooling and
     * reassociation of the mc.
     */
    private boolean changable = false; 

    String ivUserName;
    String ivPassword;
    int ivIsoLevel;
    String ivCatalog;
    Boolean ivReadOnly;
    Map<String, Class<?>> ivTypeMap;
    int ivHoldability; 
    int ivConfigID; 
    String ivSchema;
    Object ivShardingKey;
    Object ivSuperShardingKey;
    int ivNetworkTimeout;

    // If the CRI is aware of the meaning of the default values for unspecified properties,
    // then it can do matching based on the default values.
    String defaultCatalog;
    int defaultHoldability;
    Boolean defaultReadOnly;
    Map<String, Class<?>> defaultTypeMap;
    String defaultSchema;
    int defaultNetworkTimeout;

    GSSName gssName; // used to compare if identities are different
    GSSCredential gssCredential; // used to reset identity on the connection
    boolean kerberosIdentityisSet; // set to true if gssName or gssCredential is set
    boolean kerberosMappingIsUsed;

    // Cache this hashcode as much as possible for performance
    private int hashcode;

    /** Indicates if this CRI has only the isolation level property specified. */
    private boolean hasIsolationLevelOnly;

    boolean supportIsolvlSwitching; 

    /**
     * Create a ConnectionRequestInfo with no properties specified.
     */
    WSConnectionRequestInfoImpl() {
        // hash code shouldn't include connection properties that can be reset later.  hashcode is used only
        // by the j2c to lookup connections from the free pool, i.e. connections can always get reset so
        // no need to update the hash in those cases.

        ivIsoLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED; 
        hasIsolationLevelOnly = true; 
    }

    /**
     * Creates a ConnectionRequestInfo with isolation level as the only property provided.
     * All other properties remain null, allowing the .equals method to be optimized when two
     * such CRIs are compared. 
     * 
     * @param isolationLevel the transaction isolation level.
     * @param isJDBCHandle true if requesting a JDBC handle; false if requesting a CCI handle.
     */
    public WSConnectionRequestInfoImpl(int isolationLevel) {
        // hash code shouldn't include connection properties that can be reset later.  hashcode is used only
        // by the j2c to lookup connections from the free pool, i.e. connections can always get reset so
        // no need to update the hash in those cases.

        ivIsoLevel = isolationLevel; 
        hasIsolationLevelOnly = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ConnectionRequestInfo created", this); 
    }

    /**
     * Creates a ConnectionRequestInfo with isolation level and supportIsolvlSwitching as the
     * properties provided.
     * 
     * @param isolationLevel the transaction isolation level.
     * @param supportIsolationLvlSwitching support isolationlevel switching flag
     */
    public WSConnectionRequestInfoImpl(int isolationLevel, boolean supportIsolationLvlSwitching) {
        // hash code shouldn't include connection properties that can be reset later.  hashcode is used only
        // by the j2c to lookup connections from the free pool, i.e. connections can always get reset so
        // no need to update the hash in those cases.

        ivIsoLevel = isolationLevel;
        supportIsolvlSwitching = supportIsolationLvlSwitching;
        hashcode = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ConnectionRequestInfo created", this); 
    }

    /**
     * Creates a ConnectionRequestInfo object for the parameters provided. When this
     * constructor is used, a JDBC connection handle is always requested.
     * 
     * @param user the user name, or null if none.
     * @param password the password, or null if none.
     * @param isolationLevel the transaction isolation level.
     * @param configID the DataSource configuration ID. 
     * @param shareWithCMPOnly boolean specified if sharing with cmp is requested 
     *            true: share with cmp connections
     *            false: don't share with cmp connections
     */
    public WSConnectionRequestInfoImpl(String user, String password, int isolationLevel, int configID,
                                       boolean supportIsolationLvlSwitching)

    {
        ivUserName = user;
        ivPassword = password;
        ivIsoLevel = isolationLevel;
        ivConfigID = configID;
        supportIsolvlSwitching = supportIsolationLvlSwitching; 

        // hash code shouldn't include connection properties that can be reset later.  hashcode is used only
        // by the j2c to lookup connections from the free pool, i.e. connections can always get reset so
        // no need to update the hash in those cases.

        hashcode = ivConfigID +
                   (ivUserName == null ? 0 : ivUserName.hashCode()) +
                   (ivPassword == null ? 0 : ivPassword.hashCode());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ConnectionRequestInfo created", this); 
    }

    /**
     * Creates a ConnectionRequestInfo object for the parameters provided.
     * 
     * @param user the user name, or null if none.
     * @param password the password, or null if none.
     * @param isolationLevel the transaction isolation level.
     * @param catalog the catalog name.
     * @param isReadOnly indicator of whether the connection is read only.
     * @param shardingKey sharding key
     * @param superShardingKey super sharding key
     * @param typeMap a type mapping for custom SQL structured types and distinct types.
     * @param schema the schema to be used for the connection
     * @param networkTimeout The number of milliseconds the driver will wait for the database request to complete.
     * @param configID the DataSource configuration ID. 
     * @param supportIsolationLvlSwitching support isolationlevel switching flag 
     */
    public WSConnectionRequestInfoImpl(String user, String password, int isolationLevel,
                                       String catalog, Boolean isReadOnly, Object shardingKey, Object superShardingKey,
                                       Map<String, Class<?>> typeMap, int holdability, 
                                       String schema, int networkTimeout, 
                                       int configID,
                                       boolean supportIslSwitching)
    {
        ivUserName = user;
        ivPassword = password;
        ivIsoLevel = isolationLevel;
        ivCatalog = catalog;
        ivReadOnly = isReadOnly;
        ivShardingKey = shardingKey;
        ivSuperShardingKey = superShardingKey;
        ivTypeMap = typeMap;
        ivSchema = schema;
        ivNetworkTimeout = networkTimeout;
        ivHoldability = holdability; 
        ivConfigID = configID; 
        supportIsolvlSwitching = supportIslSwitching; 

        hashcode = ivConfigID +
                   (ivUserName == null ? 0 : ivUserName.hashCode()) +
                   (ivPassword == null ? 0 : ivPassword.hashCode());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ConnectionRequestInfo created", this); 
    }

    /**
     * Constructor to be used by connection builder.
     *
     * @param mcf the managed connection factory of the data source that is used to make the connection request
     * @param cm the connection manager that managed connections to the data source that is used to make the connection request
     * @param user the user name specified on the connection builder, or null if none.
     * @param password the password, or null if none.
     * @param shardingKey the sharding key, or null if none.
     * @param superShardingKey the super sharding key, or null if none.
     */
    public WSConnectionRequestInfoImpl(WSManagedConnectionFactoryImpl mcf, WSConnectionManager cm,
                                       String user, String password, Object shardingKey, Object superShardingKey) {
        ivUserName = user;
        ivPassword = password;
        ivShardingKey = shardingKey;
        ivSuperShardingKey = superShardingKey;
        ivConfigID = mcf.instanceID;
        supportIsolvlSwitching = mcf.getHelper().isIsolationLevelSwitchingSupport();

        // Get the isolation level from the resource reference, or if that is not specified, use the
        // configured isolationLevel value, otherwise use a default that we choose for the database.
        ResourceRefInfo resRefInfo = cm.getResourceRefInfo();
        ivIsoLevel = resRefInfo == null ? Connection.TRANSACTION_NONE : resRefInfo.getIsolationLevel();
        if (ivIsoLevel == Connection.TRANSACTION_NONE)
            ivIsoLevel = mcf.dsConfig.get().isolationLevel;
        if (ivIsoLevel == -1)
            ivIsoLevel = mcf.getHelper().getDefaultIsolationLevel();

        hashcode = ivConfigID +
                   (ivUserName == null ? 0 : ivUserName.hashCode()) +
                   (ivPassword == null ? 0 : ivPassword.hashCode());

        markAsChangable();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "ConnectionRequestInfo created", this);
    }

    /**
     * Returns the DataSource configuration ID. [Method added in ]
     * 
     * @return the DataSource configuration ID.
     */
    public final int getConfigID() {
        return ivConfigID;
    }

    /**
     * Return a user name
     * 
     * @return a user name
     */
    public final String getUserName() {
        return ivUserName;
    }

    /**
     * Return a password
     * 
     * @return a user password
     */
    public final String getPassword() {
        return ivPassword;
    }

    /**
     * Return an Isolation Level that is used for a connection. The return isolation level
     * has one of the following values:
     * <ul>
     * <li>READ_UNCOMMITTED; </li>
     * <li>READ_COMMITTED; </li>
     * <li>REPEATABLE_READ; </li>
     * <li>SERIALABLE</li>
     * </ul>
     * 
     * @return an Isolation Level
     */
    public final int getIsolationLevel() {
        return ivIsoLevel;
    }

    /**
     * @return the catalog name.
     */
    public final String getCatalog() {
        return ivCatalog;
    }

    /**
     * @return the sharding key.
     */
    public final Object getShardingKey() {
        return ivShardingKey;
    }

    /**
     * @return the super sharding key.
     */
    public final Object getSuperShardingKey() {
        return ivSuperShardingKey;
    }

    /**
     * @return the type map used for the custom mapping of SQL structured types and distinct
     *         types.
     */
    public final Map<String, Class<?>> getTypeMap() {
        return ivTypeMap;
    }

    /**
     * @return the cursor holdability
     */
    public final int getHoldability() {
        return ivHoldability;
    }
    
    /**
     * @return the database schema
     */
    public final String getSchema(){
        return ivSchema;
    }
    
    /**
     * @return the number of milliseconds the driver will wait for a database request to complete.
     */
    public final int getNetworkTimeout(){
        return ivNetworkTimeout;
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public String[] introspectSelf() {
        com.ibm.ws.rsadapter.FFDCLogger info = new com.ibm.ws.rsadapter.FFDCLogger(this);

        info.append("changable CRI = ", changable); 
        info.append("User Name:", ivUserName);
        info.append("Password:", ivPassword == null ? null : "******");
        info.append("Isolation Level:", AdapterUtil.getIsolationLevelString(ivIsoLevel));
        info.append("Catalog:", ivCatalog);
        info.append("Schema:", ivSchema);
        info.append("Sharding key:", ivShardingKey);
        info.append("Super Sharding key:", ivSuperShardingKey);
        info.append("Is Read Only?", ivReadOnly);
        info.append("Type Map:", ivTypeMap);
        info.append("Cursor Holdability:", AdapterUtil.getCursorHoldabilityString(ivHoldability)); 
        info.append("Config ID: " + ivConfigID); 
        info.append("Hash Code:", Integer.toHexString(hashcode));
        info.append("Support isolation switching on connection:", supportIsolvlSwitching); 
        info.append("gssName = ").append(gssName == null ? null : gssName.toString()); 
        info.append("gssCredential = ").append(gssCredential == null ? null : gssCredential.toString()); 

        return info.toStringArray();
    }

    /**
     * @return true if the connection is read only; otherwise false. Value will be null if the
     *         readOnly property was never set, which means the database default will be used.
     */
    public final Boolean isReadOnly() {
        return ivReadOnly;
    }

    /**
     * Indicates whether a Connection with this CRI may be reconfigured to the specific CRI.
     * 
     * @param cri The CRI to test against.
     * 
     * @return true if a connection with the CRI represented by this class can be reconfigured
     *         to the specified CRI. 
     */
    public final boolean isReconfigurable(WSConnectionRequestInfoImpl cri, boolean reauth) 
    {
        // The CRI is only reconfigurable if all fields which cannot be changed already match.
        // Although sharding keys can sometimes be changed via connection.setShardingKey,
        // the spec does not guarantee that this method will allow all sharding keys
        // (even ones that are known to be valid) to be set on any connection.  It leaves open
        // the possibility that the JDBC driver implementation can decide not to allow switching
        // between certain sharding keys.  Given that we don't have any way of knowing in
        // advance that a switching will be accepted (without preemptively trying to change it),
        // we must consider sharding keys to be non-reconfigurable for the purposes of
        // selecting a connection from the pool.

        if (reauth) 
        {
            return ivConfigID == cri.ivConfigID;
        } else {
            return match(ivUserName, cri.ivUserName) &&
                   match(ivPassword, cri.ivPassword) &&
                   match(ivShardingKey, cri.ivShardingKey) &&
                   match(ivSuperShardingKey, cri.ivSuperShardingKey) &&
                   ivConfigID == cri.ivConfigID;
        }

    }

    /**
     * Checks whether this instance is equal to another.
     * <p>Overrides: equals in class java.lang.Object
     * 
     * <P><B>Note</b> We do not check the connection type as part of the equals
     * don't want to do this because we want to make sure that both BMP and CMP can
     * match to the same managed connection as per the WAB's request.
     * 
     * <P>We cannot just compare the hashcode because it is not guaranteed to be
     * unique.
     * 
     * @param arg0 WSCRI object to compare
     * @return boolean
     */
    @Override
    public final boolean equals(Object arg0) {
        boolean result;

        if (arg0 == this)
            result = true;
        else if (arg0 == null)
            result = false;
        else
            try {
                WSConnectionRequestInfoImpl wscri = (WSConnectionRequestInfoImpl) arg0;

                if (supportIsolvlSwitching) {
                    // skip the isolation level comparison
                    result = (hasIsolationLevelOnly && wscri.hasIsolationLevelOnly) ||
                                                    (match(ivUserName, wscri.ivUserName) &&
                                                     match(ivPassword, wscri.ivPassword) &&
                                                     match(ivShardingKey, wscri.ivShardingKey) &&
                                                     match(ivSuperShardingKey, wscri.ivSuperShardingKey) &&
                                                     matchKerberosIdentities(wscri) && 
                                                     matchHoldability(wscri) && 
                                                     matchCatalog(wscri) && 
                                                     matchReadOnly(wscri) && 
                                                     matchTypeMap(wscri) && 
                                                     matchSchema(wscri) && 
                                                     matchNetworkTimeout(wscri) &&
                                                    ivConfigID == wscri.ivConfigID); 
                } else {
                    result = ivIsoLevel == wscri.ivIsoLevel &&
                             ((hasIsolationLevelOnly && wscri.hasIsolationLevelOnly) ||

                             (hashcode == wscri.hashcode && // shortcut for detecting inequality 
                              match(ivUserName, wscri.ivUserName) &&
                              match(ivPassword, wscri.ivPassword) &&
                              match(ivShardingKey, wscri.ivShardingKey) &&
                              match(ivSuperShardingKey, wscri.ivSuperShardingKey) &&
                              matchKerberosIdentities(wscri) && 
                              matchHoldability(wscri) && 
                              matchCatalog(wscri) && 
                              matchReadOnly(wscri) && 
                              matchTypeMap(wscri) && 
                              matchSchema(wscri) && 
                              matchNetworkTimeout(wscri) && 
                             ivConfigID == wscri.ivConfigID)); 
                }
            } catch (RuntimeException runtimeX) {
                // No FFDC code needed. Doesn't match.
                result = false;
            }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "equals?", new Object[] 
                     {
                      AdapterUtil.toString(this),
                      AdapterUtil.toString(arg0),
                      result
            });

        return result;
    }

    /**
     * Per JAVA Docs,
     * method returns true if the two names contain at least one primitive element in common.
     * If either of the names represents an anonymous entity, the method will return false.
     */
    private boolean matchKerberosIdentities(WSConnectionRequestInfoImpl cri) {
        boolean flag = false;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(this, tc, "matchKerberosIdentities", this, cri);

        if (kerberosIdentityisSet && cri.kerberosIdentityisSet) {
            flag = AdapterUtil.matchGSSName(cri.gssName, gssName);
        } else if (kerberosIdentityisSet || cri.kerberosIdentityisSet) {
            // one of them is true, so no match
            flag = false;
            if (isTraceOn && tc.isDebugEnabled()) 
                Tr.debug(this, tc, "only one has kerberos identity attributes set so no match");
        } else 
        {// else: both are are false thus, match
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(this, tc, "both have kerberos identity attributes not set so match");
            flag = true; 
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(this, tc, "matchKerberosIdentities", flag);

        return flag;
    }

    // Copied from AdapterUtil.
    /**
     * Determine if two objects, either of which may be null, are equal.
     * 
     * @param obj1
     *            one object.
     * @param obj2
     *            another object.
     * 
     * @return true if the objects are equal or are both null, otherwise false.
     */
    private static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * Determine if the catalog property matches. It is considered to match if
     * - Both catalog values are unspecified.
     * - Both catalog values are the same value.
     * - One of the catalog values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the catalogs match, otherwise false.
     */
    private final boolean matchCatalog(WSConnectionRequestInfoImpl cri) {
        // At least one of the CRIs should know the default value.
        String defaultValue = defaultCatalog == null ? cri.defaultCatalog : defaultCatalog;

        return match(ivCatalog, cri.ivCatalog)
               || ivCatalog == null && match(defaultValue, cri.ivCatalog)
               || cri.ivCatalog == null && match(ivCatalog, defaultValue);
    }
    
    /**
     * Determine if the schema property matches. It is considered to match if
     * - Both schema values are unspecified.
     * - Both schema values are the same value.
     * - One of the schema values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the schema match, otherwise false.
     */
    private final boolean matchSchema(WSConnectionRequestInfoImpl cri){
        // At least one of the CRIs should know the default value.
        String defaultValue = defaultSchema == null ? cri.defaultSchema : defaultSchema;

        return match(ivSchema, cri.ivSchema)
               || ivSchema == null && match(defaultValue, cri.ivSchema)
               || cri.ivSchema == null && match(ivSchema, defaultValue);
    }
    
    /**
     * Determine if the networkTimeout property matches. It is considered to match if
     * - Both networkTimeout values are unspecified.
     * - Both networkTimeout values are the same value.
     * - One of the networkTimeout values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the networkTimeouts match, otherwise false.
     */
    private final boolean matchNetworkTimeout(WSConnectionRequestInfoImpl cri){
        // At least one of the CRIs should know the default value.
        int defaultValue = defaultNetworkTimeout == 0 ? cri.defaultNetworkTimeout : defaultNetworkTimeout;

        return ivNetworkTimeout == cri.ivNetworkTimeout
               || ivNetworkTimeout == 0 && (defaultValue == cri.ivNetworkTimeout)
               || cri.ivNetworkTimeout == 0 && ivNetworkTimeout == defaultValue;
    }

    /**
     * Determine if the result set holdability property matches. It is considered to match if
     * - Both holdability values are unspecified.
     * - Both holdability values are the same value.
     * - One of the holdability values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the result set holdabilities match, otherwise false.
     */
    private final boolean matchHoldability(WSConnectionRequestInfoImpl cri) {
        // At least one of the CRIs should know the default value.
        int defaultValue = defaultHoldability == 0 ? cri.defaultHoldability : defaultHoldability;

        return ivHoldability == cri.ivHoldability
               || ivHoldability == 0 && match(defaultValue, cri.ivHoldability)
               || cri.ivHoldability == 0 && match(ivHoldability, defaultValue);
    }

    /**
     * Determine if the read-only property matches. It is considered to match if
     * - Both read-only values are unspecified.
     * - Both read-only values are the same value.
     * - One of the read-only values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the read-only values match, otherwise false.
     */
    private final boolean matchReadOnly(WSConnectionRequestInfoImpl cri) {
        // At least one of the CRIs should know the default value.
        Boolean defaultValue = defaultReadOnly == null ? cri.defaultReadOnly : defaultReadOnly;

        return match(ivReadOnly, cri.ivReadOnly)
               || ivReadOnly == null && match(defaultValue, cri.ivReadOnly)
               || cri.ivReadOnly == null && match(ivReadOnly, defaultValue);
    }

    /**
     * Determine if the type map property matches. It is considered to match if
     * - Both type map values are unspecified.
     * - Both type map values are the same value.
     * - One of the type map values is unspecified and the other CRI requested the default value.
     * 
     * @return true if the type map values match, otherwise false.
     */
    private final boolean matchTypeMap(WSConnectionRequestInfoImpl cri) {
        // At least one of the CRIs should know the default value.
        Map<String, Class<?>> defaultValue = defaultTypeMap == null ? cri.defaultTypeMap : defaultTypeMap;

        return matchTypeMap(ivTypeMap, cri.ivTypeMap)
               || ivTypeMap == null && matchTypeMap(defaultValue, cri.ivTypeMap)
               || cri.ivTypeMap == null && matchTypeMap(ivTypeMap, defaultValue);
    }

    /**
     * determines if two typeMaps match. Note that this method takes under account
     * an Oracle 11g change with TypeMap
     * 
     * @param m1
     * @param m2
     * @return
     */
    public static final boolean matchTypeMap(Map<String, Class<?>> m1, Map<String, Class<?>> m2) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "matchTypeMap", new Object[] { m1, m2 });

        boolean match = false;

        if (m1 == m2)
            match = true;
        else if (m1 != null && m1.equals(m2))
            match = true;
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "matchTypeMap", match);
        return match;
    }

    /**
     * Initialize default values that are used for unspecified properties.
     * This allows us to match unspecified values with specified values in another CRI.
     * 
     * @param catalog the default catalog value, or NULL if not supported.
     * @param holdability the default holdability value, or 0 if not supported.
     * @param readOnly the default read-only value, or NULL if not supported.
     * @param typeMap the default type map value, or NULL if not supported.
     * @param schema the default schema value, or NULL if not supported.
     * @param networkTimeout the default network timeout.
     */
    public void setDefaultValues(String catalog, int holdability, Boolean readOnly, Map<String, Class<?>> typeMap,
                                 String schema, int networkTimeout) {
        defaultCatalog = catalog;
        defaultHoldability = holdability;
        defaultReadOnly = readOnly;
        defaultTypeMap = typeMap;
        defaultSchema = schema;
        defaultNetworkTimeout = networkTimeout;
    }

    /**
     * write out the contents of this object
     * 
     * @return a String containing the contents of this object
     */
    @Override
    public String toString() {
        String lineSeparator = AdapterUtil.EOLN;
        StringBuilder sb = new StringBuilder(500)
                        .append(AdapterUtil.toString(this))
                        .append(lineSeparator).append("  changable CRI         = ").append(changable) 
                        .append(lineSeparator).append("  UserName              = ").append(ivUserName)
                        .append(lineSeparator).append("  Password              = ").append(ivPassword == null ? "null" : "******")
                        .append(lineSeparator).append("  Catalog/default       = ").append(ivCatalog);

        if (defaultCatalog != null)
            sb.append(" / ").append(defaultCatalog);
        
        sb.append(lineSeparator).append("  Schema/default        = ").append(ivSchema);
        if (defaultSchema != null)
            sb.append(" / ").append(defaultSchema);

        sb.append(lineSeparator).append("  ShardingKey           = ").append(ivShardingKey);

        sb.append(lineSeparator).append("  SuperShardingKey      = ").append(ivSuperShardingKey);

        sb.append(lineSeparator).append("  NetworkTimeout/default= ").append(ivNetworkTimeout);
        sb.append(" / ").append(defaultNetworkTimeout);

        sb.append(lineSeparator).append("  IsReadOnly/default    = ").append(ivReadOnly);

        if (defaultReadOnly != null)
            sb.append(" / ").append(defaultReadOnly);

        sb.append(lineSeparator).append("  TypeMap/default       = ").append(ivTypeMap);

        if (defaultTypeMap != null)
            sb.append(" / ").append(defaultTypeMap);

        sb.append(lineSeparator).append("  gssName               = ").append(gssName == null ? null : gssName.toString()) 
          .append(lineSeparator).append("  gssCredential         = ").append(gssCredential == null ? null : gssCredential.toString()) 
          .append(lineSeparator).append("  Holdability/default   = ").append(AdapterUtil.getCursorHoldabilityString(ivHoldability)); 

        if (defaultHoldability != 0)
            sb.append(" / ").append(defaultHoldability);

        sb.append(lineSeparator).append("  ConfigID              = ").append(ivConfigID) 
          .append(lineSeparator).append("  Isolation             = ")
          .append(AdapterUtil.getIsolationLevelString(ivIsoLevel))
          .append(lineSeparator).append("  Isolation switching?    ").append(supportIsolvlSwitching);
        return new String(sb);
    }

    /**
     * Returns the hashCode of the ConnectionRequestInfo.
     * If two objects are equal according to the equals(Object) method,
     * then calling the hashCode method on each of the two objects must
     * produce the same integer result.
     * 
     * <p>We cannot just return super.hashCode() because CM is using this
     * method to uniquely identify connection handles that are stored into
     * a hash table. So the hashCode() is being used in the sense of getPrimaryKey()
     * 
     * <p>Overrides: hashCode in class java.lang.Object
     * 
     * @return a hash code of this instance
     * @see http://java.sun.com/products/jdk/1.2/docs/api/java/lang/Object.html
     */
    @Override
    public final int hashCode() {
        return hashcode;
    }

    public void setGssName(GSSName gn) throws SQLException {
        if (!changable) 
        {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[] { 
                                                             "ConnectionRequestInfo cannot be modified, doing so may result in corruption: GSSName, cri", gn, this }));

        }
        gssName = gn;
        kerberosIdentityisSet = true;
    }

    public void setGssCredential(GSSCredential gc) throws SQLException {
        if (!changable) 
        {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[] { 
                                                             "ConnectionRequestInfo cannot be modified, doing so may result in corruption: GSSCredential, cri", gc, this }));

        }
        gssCredential = gc;
        kerberosIdentityisSet = true;
    }

    /**
     * 
     * @return true if kerberos login module is used.
     */
    public boolean isKerberosMappingUsed() {
        return kerberosMappingIsUsed;
    }


    public void markAsChangable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
            Tr.debug(this, tc, "This CRI is marked as changable: ", this);
        changable = true;
    }


    /**
     * returns boolean indicating if cri is changable or not.
     */
    public boolean isCRIChangable() 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isCRIChangable :", changable);
        return changable;

    }


    /**
     * Change the value of the catalog property in the connection request information.
     * 
     * @param catalog The new value.
     * @throws IllegalArgumentException if the key is incorrect.
     * @throws SQLException if the connection request information is not editable.
     */
    public void setCatalog(String catalog) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[]
            { "ConnectionRequestInfo cannot be modified, doing so may result in corruption: catalog, cri", catalog, this }));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting catalog on the CRI to: " + catalog);

        ivCatalog = catalog;
    }

    /**
     * Change the value of the result set holdability property in the connection request information.
     * 
     * @param holdability The new value.
     * @throws IllegalArgumentException if the key is incorrect.
     * @throws SQLException if the connection request information is not editable.
     */
    public void setHoldability(int holdability) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[]
            { "ConnectionRequestInfo cannot be modified, doing so may result in corruption: holdability, cri", holdability, this }));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting holdability on the CRI to: " + holdability);

        ivHoldability = holdability;
    }

    /**
     * sets the isolation level for this CRI. The J2C team are aware of the change and they are
     * ok with setting the isolation level as the values are not part of the hashCode and hence any changes
     * there will not affect the bucket in which the mc will reside in the connection pool
     * We are in need for setting the cri in case the application sets the value on the connection.
     */
    public void setTransactionIsolationLevel(int iso) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", 
                                                             "ConnectionRequestInfo cannot be modified, doing so may result in corruption: iso , cri", iso, this));

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting isolation level on the CRI to:", iso);
        ivIsoLevel = iso;
    }

    /**
     * sets the readonly for this CRI. The J2C team are aware of the change and they are
     * ok with setting the readonly as the values are not part of the hashCode and hence any changes
     * there will not affect the bucket in which the mc will reside in the connection pool.
     * We are in need for setting the cri in case the application sets the value on the connection.
     */
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR",
                                                             new Object[] {
                                                                           "ConnectionRequestInfo cannot be modified, doing so may result in corruption: readOnly , cri",
                                                                           readOnly, this }));

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting read only on the CRI to:", readOnly);

        ivReadOnly = readOnly;
    }

    /**
     * Change the value of the sharding key property in the connection request information.
     * 
     * @param shardingKey the new value.
     * @throws SQLException if the connection request information is not editable.
     */
    public void setShardingKey(Object shardingKey) throws SQLException {
        if (!changable)
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR",
                "ConnectionRequestInfo cannot be modified, doing so may result in corruption: shardingKey, cri", shardingKey, this));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting sharding key on the CRI to: " + shardingKey);

        ivShardingKey = shardingKey;
    }

    /**
     * Change the value of the super sharding key property in the connection request information.
     * 
     * @param superShardingKey the new value.
     * @throws SQLException if the connection request information is not editable.
     */
    public void setSuperShardingKey(Object superShardingKey) throws SQLException {
        if (!changable)
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR",
                "ConnectionRequestInfo cannot be modified, doing so may result in corruption: superShardingKey, cri", superShardingKey, this));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting super sharding key on the CRI to: " + superShardingKey);

        ivSuperShardingKey = superShardingKey;
    }

    /**
     * Change the value of the type map property in the connection request information.
     * 
     * @param map The new value.
     * @throws IllegalArgumentException if the key is incorrect.
     * @throws SQLException if the connection request information is not editable.
     */
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[]
            { "ConnectionRequestInfo cannot be modified, doing so may result in corruption: type map, cri", map, this }));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting the type map on the CRI to: " + map);

        ivTypeMap = map;
    }
    
    public void setSchema(String schema) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[]
            { "ConnectionRequestInfo cannot be modified, doing so may result in corruption: schema, cri", schema, this }));
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting the schema on the CRI to: " + schema);

        ivSchema = schema;
    }
    
    public void setNetworkTimeout(int networkTimeout) throws SQLException {
        if (!changable) {
            throw new SQLException(AdapterUtil.getNLSMessage("WS_INTERNAL_ERROR", new Object[]
            { "ConnectionRequestInfo cannot be modified, doing so may result in corruption: networkTimeout, cri", networkTimeout, this }));
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "Setting the networkTimeout on the CRI to: " + networkTimeout);

        ivNetworkTimeout = networkTimeout;
    }

    public final boolean getSupportIsolvlSwitchingValue() {
        return supportIsolvlSwitching;
    }

    /**
     * utility to create changable CRI from non changable one.
     * 
     * @param oldCRI
     * @return
     */
    public static WSConnectionRequestInfoImpl createChangableCRIFromNon(WSConnectionRequestInfoImpl oldCRI) {
        WSConnectionRequestInfoImpl connInfo = new WSConnectionRequestInfoImpl(
                        oldCRI.getUserName(),
                        oldCRI.getPassword(),
                        oldCRI.getIsolationLevel(),
                        oldCRI.getCatalog(),
                        oldCRI.isReadOnly(),
                        oldCRI.getShardingKey(),
                        oldCRI.getSuperShardingKey(),
                        oldCRI.getTypeMap(),
                        oldCRI.getHoldability(),
                        oldCRI.getSchema(),
                        oldCRI.getNetworkTimeout(),
                        oldCRI.getConfigID(),
                        oldCRI.getSupportIsolvlSwitchingValue());

        connInfo.setDefaultValues(oldCRI.defaultCatalog, oldCRI.defaultHoldability, 
                                  oldCRI.defaultReadOnly, oldCRI.defaultTypeMap, oldCRI.defaultSchema,
                                  oldCRI.defaultNetworkTimeout);
        connInfo.markAsChangable();

        return connInfo;
    }
}
