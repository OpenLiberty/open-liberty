/*
 * Copyright (c) 1998, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 IBM Corporation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
//
//     04/30/2009-2.0 Michael O'Brien
//       - 266912: JPA 2.0 Metamodel API (part of Criteria API)
//         Add Set<RelationalDescriptor> mappedSuperclassDescriptors
//         to support the Metamodel API
//     06/17/2009-2.0 Michael O'Brien
//       - 266912: change mappedSuperclassDescriptors Set to a Map
//          keyed on MetadataClass - avoiding the use of a hashCode/equals
//          override on RelationalDescriptor, but requiring a contains check prior to a put
//     09/23/2009-2.0 Michael O'Brien
//       - 266912: Add metamodelIdClassMap to store IdClass types for exclusive
//         use by the IdentifiableTypeImpl class in the JPA 2.0 Metamodel API
//     06/30/2011-2.3.1 Guy Pelletier
//       - 341940: Add disable/enable allowing native queries
//     09/09/2011-2.3.1 Guy Pelletier
//       - 356197: Add new VPD type to MultitenantType
//     09/14/2011-2.3.1 Guy Pelletier
//       - 357533: Allow DDL queries to execute even when Multitenant entities are part of the PU
//     14/05/2012-2.4 Guy Pelletier
//       - 376603: Provide for table per tenant support for multitenant applications
//     31/05/2012-2.4 Guy Pelletier
//       - 381196: Multitenant persistence units with a dedicated emf should allow for DDL generation.
//     08/11/2012-2.5 Guy Pelletier
//       - 393867: Named queries do not work when using EM level Table Per Tenant Multitenancy
//     04/11/2018 - Will Dazey
//       - 533148 : Add the eclipselink.jpa.sql-call-deferral property
package org.eclipse.persistence.sessions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.persistence.annotations.IdValidation;
import org.eclipse.persistence.config.CacheIsolationType;
import org.eclipse.persistence.core.sessions.CoreProject;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.MultitenantPolicy;
import org.eclipse.persistence.descriptors.partitioning.PartitioningPolicy;
import org.eclipse.persistence.internal.helper.ConcurrentFixedCache;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.NonSynchronizedVector;
import org.eclipse.persistence.internal.identitymaps.AbstractIdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.queries.AttributeGroup;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.QueryResultsCachePolicy;
import org.eclipse.persistence.queries.SQLResultSetMapping;
import org.eclipse.persistence.sessions.server.ConnectionPolicy;
import org.eclipse.persistence.sessions.server.Server;
import org.eclipse.persistence.sessions.server.ServerSession;

/**
 * <b>Purpose</b>: Maintain all of the EclipseLink configuration information for a system.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Project options and defaults
 * <li> Database login information
 * <li> Descriptors
 * <li> Validate Descriptors
 * <li> Maintain sequencing information {@literal &} other project options
 * </ul>
 *
 * @see DatabaseLogin
 */
public class Project extends CoreProject<ClassDescriptor, Login, DatabaseSession> implements Serializable, Cloneable {
    protected String name;
    protected Login datasourceLogin;
    protected Map<Class, ClassDescriptor> descriptors;
    protected List<ClassDescriptor> orderedDescriptors;

    // Currently only one is supported.
    protected MultitenantPolicy multitenantPolicy;

    /** Holds the default set of read-only classes that apply to each UnitOfWork. */
    protected Vector defaultReadOnlyClasses;

    /** Cache the EJBQL descriptor aliases. */
    protected Map aliasDescriptors;

    /** Cache if any descriptor is isolated. (set during initialization) */
    protected boolean hasIsolatedClasses;
    /** Cache if all descriptors are isolated in the unit of work. (set during initialization) */
    protected boolean hasNonIsolatedUOWClasses;
    /** Cache if any descriptor has history. (set during initialization) */
    protected boolean hasGenericHistorySupport;
    /** Cache if any descriptor is using ProxyIndirection. (set during initialization */
    protected boolean hasProxyIndirection;

    /** This a collection of 'maps' that allow users to map custom SQL to query results */
    protected Map<String, SQLResultSetMapping> sqlResultSetMappings;

    /** PERF: Provide an JPQL parse cache to optimize dynamic JPQL. */
    protected transient ConcurrentFixedCache jpqlParseCache;

    /** Define the default setting for configuring if dates and calendars are mutable. */
    protected boolean defaultTemporalMutable = false;

    /** Indicates whether there is at least one descriptor that has at least on mapping that
     *  require a call on deleted objects to update change sets.
     */
    protected transient boolean hasMappingsPostCalculateChangesOnDeleted = false;

    /** Default value for ClassDescriptor.identityMapClass. */
    protected Class defaultIdentityMapClass = AbstractIdentityMap.getDefaultIdentityMapClass();

    /** Default value for ClassDescriptor.identityMapSize. */
    protected int defaultIdentityMapSize = 100;

    /** Default value for ClassDescriptor.isIsolated. */
    protected CacheIsolationType defaultCacheIsolation;

    /** Default value for query caching options for all named queries. */
    protected QueryResultsCachePolicy defaultQueryResultsCachePolicy;

    /** Default value for ClassDescriptor.idValidation. */
    protected IdValidation defaultIdValidation;

    /** List of queries - once Project is initialized, these are copied to the Session. */
    protected List<DatabaseQuery> queries;

    /** List of named AttributeGroups - once Project is initialized, these are copied to the Session. */
    protected Map<String, AttributeGroup> attributeGroups = null;

    /** List of queries from JPA that need special processing before execution. */
    protected List<DatabaseQuery> jpaQueries;

    /** List of queries from JPA that may special processing and handling before execution. */
    protected List<DatabaseQuery> jpaTablePerTenantQueries;

    /** Flag that allows native queries or not */
    protected boolean allowNativeSQLQueries = true;

    /** Flag that allows DDL generation of table per tenant multitenant descriptors */
    protected boolean allowTablePerMultitenantDDLGeneration = false;

    /** Flag that allows call deferral to be disabled */
    protected boolean allowSQLDeferral = true;

    /** Flag that allows transform named stored procedure parameters into positional/index based */
    protected boolean namingIntoIndexed = false;

    /**
     * Mapped Superclasses (JPA 2) collection of parent non-relational descriptors keyed on MetadataClass
     * without creating a compile time dependency on JPA.
     * The descriptor values of this map must not be replaced by a put() so that the
     * mappings on the initial descriptor are not overwritten.<p>
     * These descriptors are only to be used by Metamodel generation.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    protected Map<String, ClassDescriptor> mappedSuperclassDescriptors;

    /**
     * Store the IdClass Id attributes for exclusive use by the Metamodel API
     * Keyed on the fully qualified accessible object owner class name.
     * Value is a List of the fully qualified id class name or id attribute name.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    protected Map<String, List<String>> metamodelIdClassMap;

    /** Map of named partitioning policies, keyed by their name. */
    protected Map<String, PartitioningPolicy> partitioningPolicies;

    /** Ensures that only one thread at a time can add/remove descriptors */
    protected Object descriptorsLock = new Boolean(true);

    /** VPD connection settings */
    protected String vpdIdentifier;
    protected String vpdLastIdentifierClassName; // Used for validation exception.

    /** used for Caching JPA projects */
    protected Collection<String> classNamesForWeaving;
    protected Collection<String> structConverters;

    protected boolean allowNullResultMaxMin = true;
    protected boolean allowConvertResultToBoolean = true;

    /**
     * PUBLIC:
     * Create a new project.
     */
    public Project() {
        this.name = "";
        this.descriptors = new HashMap();
        this.defaultReadOnlyClasses = NonSynchronizedVector.newInstance();
        this.orderedDescriptors = new ArrayList<ClassDescriptor>();
        this.hasIsolatedClasses = false;
        this.hasGenericHistorySupport = false;
        this.hasProxyIndirection = false;
        this.jpqlParseCache = new ConcurrentFixedCache(200);
        this.queries = new ArrayList<DatabaseQuery>();
        this.mappedSuperclassDescriptors = new HashMap<String, ClassDescriptor>(2);
        this.metamodelIdClassMap = new HashMap<String, List<String>>();
        this.attributeGroups = new HashMap<String, AttributeGroup>();
    }

    /**
     * PUBLIC:
     * Create a new project that will connect through the login information.
     * This method can be used if the project is being create in code instead of being read from a file.
     */
    public Project(Login login) {
        this();
        this.datasourceLogin = login;
    }

    /**
     * PUBLIC:
     * Create a new project that will connect through JDBC using the login information.
     * This method can be used if the project is being create in code instead of being read from a file.
     */
    public Project(DatabaseLogin login) {
        this();
        this.datasourceLogin = login;
    }

    /**
     * PUBLIC:
     * Return the default values for query caching options for all named queries.
     */
    public QueryResultsCachePolicy getDefaultQueryResultsCachePolicy() {
        return defaultQueryResultsCachePolicy;
    }

    /**
     * PUBLIC:
     * Set the default values for query caching options for all named queries.
     * By default no query caching is used.
     */
    public void setDefaultQueryResultsCachePolicy(QueryResultsCachePolicy defaultQueryResultsCachePolicy) {
        this.defaultQueryResultsCachePolicy = defaultQueryResultsCachePolicy;
    }

    /**
     * PUBLIC:
     * Return the default setting for configuring if dates and calendars are mutable.
     * Mutable means that changes to the date's year/month/day are detected.
     * By default they are treated as not mutable.
     */
    public boolean getDefaultTemporalMutable() {
        return defaultTemporalMutable;
    }

    /**
     * PUBLIC:
     * Set the default setting for configuring if dates and calendars are mutable.
     * Mutable means that changes to the date's year/month/day are detected.
     * By default they are treated as not mutable.
     */
    public void setDefaultTemporalMutable(boolean defaultTemporalMutable) {
        this.defaultTemporalMutable = defaultTemporalMutable;
    }

    /**
     * INTERNAL:
     * Return all pre-defined not yet parsed JPQL queries.
     */
    public List<DatabaseQuery> getJPAQueries() {
        // PERF: lazy init, not normally required.
        if (jpaQueries == null) {
            jpaQueries = new ArrayList();
        }

        return jpaQueries;
    }

    /**
     * INTERNAL:
     * Return all pre-defined not yet parsed JPQL queries to table per tenant
     * entities.
     */
    public List<DatabaseQuery> getJPATablePerTenantQueries() {
        // PERF: lazy init, not normally required.
        if (jpaTablePerTenantQueries == null) {
            jpaTablePerTenantQueries = new ArrayList();
        }

        return jpaTablePerTenantQueries;
    }

    /**
     * INTERNAL:
     * Return the JPQL parse cache.
     * This is used to optimize dynamic JPQL.
     */
    public ConcurrentFixedCache getJPQLParseCache() {
        if (jpqlParseCache==null) {
            jpqlParseCache = new ConcurrentFixedCache(200);
        }
        return jpqlParseCache;
    }

    /**
     * ADVANCED:
     * Set the JPQL parse cache max size.
     * This is used to optimize dynamic JPQL.
     */
    public void setJPQLParseCacheMaxSize(int maxSize) {
        setJPQLParseCache(new ConcurrentFixedCache(maxSize));
    }

    /**
     * ADVANCED:
     * Return the JPQL parse cache max size.
     * This is used to optimize dynamic JPQL.
     */
    public int getJPQLParseCacheMaxSize() {
        return getJPQLParseCache().getMaxSize();
    }

    /**
     * INTERNAL:
     * Set the JPQL parse cache.
     * This is used to optimize dynamic JPQL.
     */
    protected void setJPQLParseCache(ConcurrentFixedCache jpqlParseCache) {
        this.jpqlParseCache = jpqlParseCache;
    }

    /**
     * INTERNAL:
     * List of queries that upon initialization are copied over to the session
     */
    public List<DatabaseQuery> getQueries() {
        return queries;
    }
    /**
     * INTERNAL:
     * @param queries
     */
    public void setQueries(List<DatabaseQuery> queries) {
        this.queries = queries;
    }

    /**
     * INTERNAL:
     * List of named AttributesGroups that will be copied to the session at initialization time.
     */
    public Map<String, AttributeGroup> getAttributeGroups(){
        return this.attributeGroups;
    }

    /**
     * INTERNAL:
     * Set the VPD identifier for this project. This identifier should be
     * populated from a descriptor VPDMultitenantPolicy and should not be
     * set directly.
     */
    public void setVPDIdentifier(String vpdIdentifier) {
        this.vpdIdentifier = vpdIdentifier;
    }

    /**
     * INTERNAL:
     * Set from individual descriptors from the project that set a VPD
     * identifier and used in validation exception.
     */
    public void setVPDLastIdentifierClassName(String vpdLastIdentifierClassName) {
        this.vpdLastIdentifierClassName = vpdLastIdentifierClassName;
    }

    /**
     * PUBLIC:
     * Add the read-only class which apply to each UnitOfWork created by default.
     */
    public void addDefaultReadOnlyClass(Class readOnlyClass) {
        getDefaultReadOnlyClasses().addElement(readOnlyClass);
    }

    /**
     * PUBLIC:
     * Add the descriptor to the project.
     */
    @Override
    public void addDescriptor(ClassDescriptor descriptor) {
        getOrderedDescriptors().add(descriptor);
        String alias = descriptor.getAlias();
        if (alias != null) {
            addAlias(alias, descriptor);
        }

        // Avoid loading class definition at this point if we haven't done so yet.
        if ((descriptors != null) && !descriptors.isEmpty()) {
            getDescriptors().put(descriptor.getJavaClass(), descriptor);
        }
    }

    /**
     * INTERNAL: Used by the BuilderInterface when reading a Project from INI files.
     * @param descriptor The descriptor to be added to the session and the project.
     * @param session     The current database session.
     */
    public void addDescriptor(final ClassDescriptor descriptor, final DatabaseSessionImpl session) {
        synchronized (this.descriptorsLock) {
            if (session.isConnected()) {
                final String alias = descriptor.getAlias();
                // Descriptor aliases may be concurrently accessed by other threads.
                // Make a clone, add new descriptor to the clone, override original with the clone.
                if (alias != null) {
                    final Map aliasDescriptorsClone = getAliasDescriptors() != null
                            ? (Map)((HashMap)getAliasDescriptors()).clone() : new HashMap();
                    aliasDescriptorsClone.put(alias, descriptor);
                    setAliasDescriptors(aliasDescriptorsClone);
                }
                // Descriptors may be concurrently accessed by other threads.
                // Make a clone, add new descriptor to the clone, override original with the clone.
                final Map<Class, ClassDescriptor> descriptorsClone = (Map)((HashMap)getDescriptors()).clone();
                descriptorsClone.put(descriptor.getJavaClass(), descriptor);
                setDescriptors(descriptorsClone);
                session.copyDescriptorsFromProject();
                session.initializeDescriptorIfSessionAlive(descriptor);
                getOrderedDescriptors().add(descriptor);
            } else {
                addDescriptor(descriptor);
            }
        }
    }

    /**
     * INTERNAL:
     * Add the descriptors to the session.
     * All persistent classes must have a descriptor registered for them with the session.
     * This method allows for a batch of descriptors to be added at once so that EclipseLink
     * can resolve the dependencies between the descriptors and perform initialization optimally.
     * @param descriptors The descriptors to be added to the session and the project.
     * @param session     The current database session.
     */
    public void addDescriptors(final Collection descriptors, final DatabaseSessionImpl session) {
        synchronized (this.descriptorsLock) {
            if (session.isConnected()) {
                // Descriptor aliases may be concurrently accessed by other threads.
                // Make a clone, add new descriptors to the clone, override original with the clone.
                final Map aliasDescriptorsClone = getAliasDescriptors() != null
                        ? (Map)((HashMap)getAliasDescriptors()).clone() : new HashMap();
                // Descriptors may be concurrently accessed by other threads.
                // Make a clone, add new descriptors to the clone, override original with the clone.
                final Map<Class, ClassDescriptor> descriptorsClone = (Map)((HashMap)getDescriptors()).clone();
                for (ClassDescriptor descriptor : (Collection<ClassDescriptor>) descriptors) {
                    descriptorsClone.put(descriptor.getJavaClass(), descriptor);
                    final String alias = descriptor.getAlias();
                    if (alias != null) {
                        aliasDescriptorsClone.put(alias, descriptor);
                    }
                }
                if (!aliasDescriptorsClone.isEmpty()) {
                    setAliasDescriptors(aliasDescriptorsClone);
                }
                setDescriptors(descriptorsClone);
                session.copyDescriptorsFromProject();
                session.initializeDescriptors(descriptors);
            } else {
                final Map<Class, ClassDescriptor> projectDescriptors = getDescriptors();
                for (ClassDescriptor descriptor : (Collection<ClassDescriptor>) descriptors) {
                    final String alias = descriptor.getAlias();
                    projectDescriptors.put(descriptor.getJavaClass(), descriptor);
                    if (alias != null) {
                        addAlias(alias, descriptor);
                    }
                }
            }
            getOrderedDescriptors().addAll(descriptors);
        }
    }

    /**
     * PUBLIC:
     * Merge the descriptors from another project into this one.
     * All persistent classes must have a descriptor registered for them with the session.
     * This method allows for a batch of descriptors to be added at once so that EclipseLink
     * can resolve the dependencies between the descriptors and perform initialization optimally.
     */
    public void addDescriptors(Project project, DatabaseSessionImpl session) {
        addDescriptors(project.getDescriptors().values(), session);
    }

    /**
     * PUBLIC:
     * Add a named SQLResultSetMapping to this project.  These SQLResultSetMappings
     * can be later used by ResultSetMappingQueries to map Custom sql results to
     * results as defined by the SQLResultSetMappings.
     */
    public void addSQLResultSetMapping(SQLResultSetMapping sqlResultSetMapping){
        if (sqlResultSetMapping == null || sqlResultSetMapping.getName() == null){
            return;
        }
        if (this.sqlResultSetMappings == null){
            this.sqlResultSetMappings = new HashMap();
        }
        this.sqlResultSetMappings.put(sqlResultSetMapping.getName(), sqlResultSetMapping);
    }

    /**
     * PUBLIC:
     * Set all this project's descriptors to conform all read queries within the context of the unit of work.
     */
    public void conformAllDescriptors() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.setShouldAlwaysConformResultsInUnitOfWork(true);
        }
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this project to actual class-based settings.
     * This will also reset any class references to the version of the class from the class loader.
     */
    @Override
    public void convertClassNamesToClasses(ClassLoader classLoader){
        Iterator ordered = orderedDescriptors.iterator();
        while (ordered.hasNext()){
            ClassDescriptor descriptor = (ClassDescriptor)ordered.next();
            descriptor.convertClassNamesToClasses(classLoader);
        }
        for (AttributeGroup group : this.getAttributeGroups().values()){
            group.convertClassNamesToClasses(classLoader);
        }
        // Clear old descriptors to allow rehash on new classes.
        this.descriptors = new HashMap();
        // convert class names to classes for each SQLResultSetMapping
        if (this.sqlResultSetMappings != null) {
            for (SQLResultSetMapping mapping : this.sqlResultSetMappings.values()) {
                mapping.convertClassNamesToClasses(classLoader);
            }
        }
        if (this.partitioningPolicies != null) {
            for (PartitioningPolicy policy : this.partitioningPolicies.values()) {
                policy.convertClassNamesToClasses(classLoader);
            }
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to assume existence for non-null primary keys.
     */
    public void assumeExistenceForDoesExist() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.getQueryManager().assumeExistenceForDoesExist();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to check the cache for existence.
     */
    public void checkCacheForDoesExist() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.getQueryManager().checkCacheForDoesExist();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to check the database for existence.
     */
    public void checkDatabaseForDoesExist() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.getQueryManager().checkDatabaseForDoesExist();
        }
    }

    /**
     * INTERNAL:
     * Clones the descriptor
     */
    @Override
    public Project clone() {
        try {
            return (Project) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new InternalError(exception.toString());
        }
    }

    /**
     * PUBLIC:
     * Factory method to create session.
     * This returns an implementor of the DatabaseSession interface, which can be used to login
     * and add descriptors from other projects.  The Session interface however should be used for
     * reading and writing once connected for complete portability.
     */
    @Override
    public DatabaseSession createDatabaseSession() {
        return new DatabaseSessionImpl(this);
    }

    /**
     * PUBLIC:
     * Factory method to create a server session.
     * This returns an implementor of the Server interface, which can be used to login
     * and add descriptors from other projects, configure connection pooling and acquire client sessions.
     * <br>
     * By default the ServerSession has a single shared read/write connection pool
     * with 32 min/max connections and an initial of 1 connection.
     */
    public Server createServerSession() {
        return new ServerSession(this);
    }

    /**
     * PUBLIC:
     * Factory method to create a server session.
     * This returns an implementor of the Server interface, which can be used to login
     * and add descriptors from other projects, configure connection pooling and acquire client sessions.
     * Configure the min and max number of connections for the default shared read/write pool.
     */
    public Server createServerSession(int min, int max) {
        return new ServerSession(this, min, max);
    }

    /**
     * PUBLIC:
     * Factory method to create a server session.
     * This returns an implementor of the Server interface, which can be used to login
     * and add descriptors from other projects, configure connection pooling and acquire client sessions.
     * Configure the min and max number of connections for the default shared read/write pool.
     */
    public Server createServerSession(int initial, int min, int max) {
        return new ServerSession(this, initial, min, max);
    }

    /**
     * PUBLIC:
     * Factory method to create a server session.
     * This returns an implementor of the Server interface, which can be used to login
     * and add descriptors from other projects, configure connection pooling and acquire client sessions.
     * Configure the default connection policy to be used.
     * This policy is used on the "acquireClientSession()" protocol.
     * <br>
     * By default the ServerSession has a single shared read/write connection pool
     * with 32 min/max connections and an initial of 1 connection.
     */
    public Server createServerSession(ConnectionPolicy defaultConnectionPolicy) {
        return new ServerSession(this, defaultConnectionPolicy);
    }

    /**
     * PUBLIC:
     * Returns the default set of read-only classes.
     */
    public Vector getDefaultReadOnlyClasses() {
        return defaultReadOnlyClasses;
    }

    /**
     * PUBLIC:
     * Return default value for descriptor cache type.
     */
    public Class getDefaultIdentityMapClass() {
        return this.defaultIdentityMapClass;
    }

    /**
     * PUBLIC:
     * Return default value descriptor cache size.
     */
    public int getDefaultIdentityMapSize() {
        return this.defaultIdentityMapSize;
    }

    /**
     * PUBLIC:
     * Return default value for whether descriptor should use isolated cache.
     * @deprecated see getDefaultCacheIsolation()
     */
    @Deprecated
    public boolean getDefaultIsIsolated() {
        return this.defaultCacheIsolation.equals(CacheIsolationType.ISOLATED);
    }

    /**
     * PUBLIC:
     * Return the project level default for class cache isolation;
     */
    public CacheIsolationType getDefaultCacheIsolation(){
        return this.defaultCacheIsolation;
    }

    /**
     * PUBLIC:
     * Return default value for descriptor primary key validation.
     */
    public IdValidation getDefaultIdValidation() {
        return this.defaultIdValidation;
    }

    /**
     * PUBLIC:
     * Return the descriptor specified for the class.
     * If the passed Class parameter is null, null will be returned.
     */
    public ClassDescriptor getClassDescriptor(Class theClass) {
        return getDescriptor(theClass);
    }

    /**
     * PUBLIC:
     * Return the descriptor specified for the class.
     */
    @Override
    public ClassDescriptor getDescriptor(Class theClass) {
        if (theClass == null) {
            return null;
        }
        return getDescriptors().get(theClass);
    }

    /**
     * PUBLIC:
     * Return the descriptors in a ClassDescriptors Map keyed on the Java class.
     */
    public Map<Class, ClassDescriptor> getDescriptors() {
        // Lazy initialize class references from orderedDescriptors when reading from XML.
        if (descriptors.isEmpty() && (!orderedDescriptors.isEmpty())) {
            for (Iterator iterator = orderedDescriptors.iterator(); iterator.hasNext();) {
                ClassDescriptor descriptor = (ClassDescriptor)iterator.next();
                descriptors.put(descriptor.getJavaClass(), descriptor);
            }
        }
        return descriptors;
    }

    /**
     * INTERNAL:
     * Return the descriptors in the order added.
     * Used to maintain consistent order in XML.
     */
    @Override
    public List<ClassDescriptor> getOrderedDescriptors() {
        return orderedDescriptors;
    }

    /**
     * INTERNAL:
     * Set the descriptors order.
     * Used to maintain consistent order in XML.
     */
    public void setOrderedDescriptors(List<ClassDescriptor> orderedDescriptors) {
        this.orderedDescriptors = orderedDescriptors;
        for (ClassDescriptor descriptor : orderedDescriptors) {
            String alias = descriptor.getAlias();
            if (alias != null) {
                addAlias(alias, descriptor);
            }
        }
    }

    /**
     * INTERNAL:
     * Returns all classes in this project that are needed for weaving.
     * This list currently includes entity, embeddables and mappedSuperClasses.
     */
    public Collection<String> getClassNamesForWeaving() {
        return classNamesForWeaving;
    }

    /**
     * INTERNAL:
     * Returns all classes in this project that are needed for weaving.
     * This list currently includes entity, embeddables and mappedSuperClasses.
     */
    public void setClassNamesForWeaving(Collection<String> classNamesForWeaving) {
        this.classNamesForWeaving = classNamesForWeaving;
    }

    /**
     * OBSOLETE:
     * Return the login, the login holds any database connection information given.
     * This has been replaced by getDatasourceLogin to make use of the Login interface
     * to support non-relational datasources,
     * if DatabaseLogin API is required it will need to be cast.
     */
    public DatabaseLogin getLogin() {
        return (DatabaseLogin)datasourceLogin;
    }

    /**
     * PUBLIC:
     * Return the login, the login holds any database connection information given.
     * This return the Login interface and may need to be cast to the datasource specific implementation.
     */
    @Override
    public Login getDatasourceLogin() {
        return datasourceLogin;
    }

    /**
     * PUBLIC:
     * get the name of the project.
     */
    public String getName() {
        return name;
    }

    /**
     * PUBLIC:
     * Get a named SQLResultSetMapping from this project.  These SQLResultSetMappings
     * can be used by ResultSetMappingQueries to map Custom sql results to
     * results as defined by the SQLResultSetMappings.
     */
    public SQLResultSetMapping getSQLResultSetMapping(String sqlResultSetMapping){
        if (sqlResultSetMapping == null || this.sqlResultSetMappings == null){
            return null;
        }
        return this.sqlResultSetMappings.get(sqlResultSetMapping);
    }

    /**
     * INTERNAL:
     * Returns structure converter class names that would be set on the databasePlatform instance
     * This is used to avoid the platform instance changing at login.
     */
    public Collection<String> getStructConverters() {
        return structConverters;
    }

    /**
     * INTERNAL:
     * Returns structure converter class names that would be set on the databasePlatform instance
     * This is used to avoid the platform instance changing at login.
     */
    public void setStructConverters(Collection<String> structConverters) {
        this.structConverters = structConverters;
    }

    /**
     * INTERNAL:
     * Return the name of the last class to set a VPD identifiers.
     */
    public String getVPDLastIdentifierClassName() {
        return vpdLastIdentifierClassName;
    }

    /**
     * INTERNAL:
     * Return the VPD identifier for this project.
     */
    public String getVPDIdentifier() {
        return vpdIdentifier;
    }

    /**
     * INTERNAL:
     */
    public MultitenantPolicy getMultitenantPolicy() {
        return multitenantPolicy;
    }

    /**
     * INTERNAL:
     * Answers if at least one Descriptor or Mapping had a HistoryPolicy at initialize time.
     */
    public boolean hasGenericHistorySupport() {
        return hasGenericHistorySupport;
    }

    /**
     * PUBLIC:
     * Set the read-only classes which apply to each UnitOfWork create by default.
     */
    public void setDefaultReadOnlyClasses(Collection newValue) {
        this.defaultReadOnlyClasses = new Vector(newValue);
    }

    /**
     * PUBLIC:
     * Set default value for descriptor cache type.
     */
    public void setDefaultIdentityMapClass(Class defaultIdentityMapClass) {
        this.defaultIdentityMapClass = defaultIdentityMapClass;
    }

    /**
     * PUBLIC:
     * Set default value descriptor cache size.
     */
    public void setDefaultIdentityMapSize(int defaultIdentityMapSize) {
        this.defaultIdentityMapSize = defaultIdentityMapSize;
    }

    /**
     * PUBLIC:
     * Set default value for whether descriptor should use isolated cache.
     * @deprecated see setDefaultCacheIsolation(CacheIsolationType)
     */
    @Deprecated
    public void setDefaultIsIsolated(boolean defaultIsIsolated) {
        this.defaultCacheIsolation = defaultIsIsolated ? CacheIsolationType.ISOLATED : CacheIsolationType.SHARED;
    }

    /**
     * PUBLIC:
     * Set project level default value for class cache isolation.
     */
    public void setDefaultCacheIsolation(CacheIsolationType isolationType) {
        this.defaultCacheIsolation = isolationType;
    }
    /**
     * PUBLIC:
     * Set default value for descriptor primary key validation.
     */
    public void setDefaultIdValidation(IdValidation defaultIdValidation) {
        this.defaultIdValidation = defaultIdValidation;
    }

    /**
     * INTERNAL:
     * Set the descriptors registered with this session.
     */
    public void setDescriptors(Map descriptors) {
        this.descriptors = descriptors;
        for (Iterator iterator = descriptors.values().iterator(); iterator.hasNext();) {
            ClassDescriptor descriptor = (ClassDescriptor)iterator.next();
            String alias = descriptor.getAlias();
            if (alias != null) {
                addAlias(alias, descriptor);
            }
        }
    }

    /**
     * ADVANCED:
     * This method is a 'helper' method for updating all of the descriptors
     * within this project to have a particular deferral level.  The levels are
     * as follows
     *     ClassDescriptor.ALL_MODIFICATIONS - this is the default and recommended.
     *        The writing of all changes will be deferred until the end of the
     *       transaction
     *    ClassDescriptor.UPDATE_MODIFICATIONS - this will cause the update changes to
     *        be deferred and all other changes to be written immediately.
     *    ClassDescriptor.NONE - this will cause all changes to be written on each
     *        container call.
     */
    public void setDeferModificationsUntilCommit(int deferralLevel) {
        for (Iterator iterator = descriptors.values().iterator(); iterator.hasNext();) {
            ClassDescriptor descriptor = (ClassDescriptor)iterator.next();
            if (descriptor.getCMPPolicy() != null) {
                descriptor.getCMPPolicy().setDeferModificationsUntilCommit(deferralLevel);
            }
        }
    }


    /**
     * INTERNAL:
     * Set to true during descriptor initialize if any descriptor has history.
     */
    public void setHasGenericHistorySupport(boolean hasGenericHistorySupport) {
        this.hasGenericHistorySupport = hasGenericHistorySupport;
    }

    /**
     * INTERNAL:
     * Return whether this project has a descriptor that is both Isolated and
     * has a cache isolation level other than ISOLATE_CACHE_ALWAYS
     * @return
     */
    public boolean hasIsolatedCacheClassWithoutUOWIsolation(){
        // checked cached boolean to avoid iteration
        if (!hasIsolatedClasses){
            return false;
        }
        Iterator<ClassDescriptor> i = orderedDescriptors.iterator();
        while (i.hasNext()){
            ClassDescriptor descriptor = i.next();
            if (descriptor.getCachePolicy().isIsolated() && !descriptor.getCachePolicy().shouldIsolateObjectsInUnitOfWork()) {
                return true;
            }
        }
        return false;
    }

    /**
     * INTERNAL:
     * Return if any descriptors are isolated.
     * Set to true during descriptor initialize if any descriptor is isolated.
     * Determines if an isolated client session is required.
     */
    public boolean hasIsolatedClasses() {
        return hasIsolatedClasses;
    }

    /**
     * INTERNAL:
     * Set to true during descriptor initialize if any descriptor is isolated.
     * Determines if an isolated client session is required.
     */
    public void setHasIsolatedClasses(boolean hasIsolatedClasses) {
        this.hasIsolatedClasses = hasIsolatedClasses;
    }

    /**
     * INTERNAL:
     * Return if any descriptors are not isolated to the unit of work.
     * Set to true during descriptor initialize if any descriptor is not isolated.
     * Allows uow merge to be bypassed.
     */
    public boolean hasNonIsolatedUOWClasses() {
        return hasNonIsolatedUOWClasses;
    }

    /**
     * INTERNAL:
     * Set if any descriptors are not isolated to the unit of work.
     * Set to true during descriptor initialize if any descriptor is not isolated.
     * Allows uow merge to be bypassed.
     */
    public void setHasNonIsolatedUOWClasses(boolean hasNonIsolatedUOWClasses) {
        this.hasNonIsolatedUOWClasses = hasNonIsolatedUOWClasses;
    }

    /**
     * INTERNAL:
     * Return if any descriptors use ProxyIndirection.
     * Set to true during descriptor initialize if any descriptor uses ProxyIndirection
     * Determines if ProxyIndirectionPolicy.getValueFromProxy should be called.
     */
    public boolean hasProxyIndirection() {
        return this.hasProxyIndirection;
    }

    /**
     * PUBLIC:
     * Return true if the sql result set mapping name exists.
     */
    public boolean hasSQLResultSetMapping(String sqlResultSetMapping) {
        return sqlResultSetMappings.containsKey(sqlResultSetMapping);
    }

    /**
     * PUBLIC:
     * Return true if there is a VPD identifier for this project. Will not be
     * set till after descriptor initialization.
     */
    public boolean hasVPDIdentifier(AbstractSession session) {
        return (vpdIdentifier != null && session.getProperty(vpdIdentifier) != null);
    }

    /**
     * INTERNAL:
     * Set to true during descriptor initialize if any descriptor uses ProxyIndirection
     * Determines if ProxyIndirectionPolicy.getValueFromProxy should be called.
     */
    public void setHasProxyIndirection(boolean hasProxyIndirection) {
        this.hasProxyIndirection = hasProxyIndirection;
    }
    /**
     * PUBLIC:
     * Set the login to be used to connect to the database for this project.
     */
    public void setLogin(DatabaseLogin datasourceLogin) {
        this.datasourceLogin = datasourceLogin;
    }

    /**
     * INTERNAL:
     * Set the multitenant policy.
     */
    public void setMultitenantPolicy(MultitenantPolicy policy) {
        multitenantPolicy = policy;
    }

    /**
     * PUBLIC:
     * Set the login to be used to connect to the database for this project.
     */
    @Override
    public void setLogin(Login datasourceLogin) {
        this.datasourceLogin = datasourceLogin;
    }

    /**
     * PUBLIC:
     * Set the login to be used to connect to the database for this project.
     */
    public void setDatasourceLogin(Login datasourceLogin) {
        this.datasourceLogin = datasourceLogin;
    }

    /**
     * PUBLIC:
     * Set the name of the project.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * INTERNAL:
     */
    @Override
    public String toString() {
        return Helper.getShortClassName(getClass()) + "(" + getName() + ")";
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the cache identity map.
     */
    public void useCacheIdentityMap() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useCacheIdentityMap();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the cache identity map the size.
     */
    public void useCacheIdentityMap(int cacheSize) {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useCacheIdentityMap();
            descriptor.setIdentityMapSize(cacheSize);
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the full identity map.
     */
    public void useFullIdentityMap() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useFullIdentityMap();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the full identity map with initial cache size.
     */
    public void useFullIdentityMap(int initialCacheSize) {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useFullIdentityMap();
            descriptor.setIdentityMapSize(initialCacheSize);
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use no identity map.
     */
    public void useNoIdentityMap() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useNoIdentityMap();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the soft cache weak identity map.
     */
    public void useSoftCacheWeakIdentityMap() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useSoftCacheWeakIdentityMap();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the soft cache weak identity map with soft cache size.
     */
    public void useSoftCacheWeakIdentityMap(int cacheSize) {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useSoftCacheWeakIdentityMap();
            descriptor.setIdentityMapSize(cacheSize);
        }
    }

    /**
     * INTERNAL:
     * Asks each descriptor if is uses optimistic locking.
     */
    public boolean usesOptimisticLocking() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            if (descriptor.usesOptimisticLocking()) {
                return true;
            }
        }
        return false;
    }

    /**
     * INTERNAL:
     * Asks each descriptor if is uses sequencing.
     */
    public boolean usesSequencing() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            if (descriptor.usesSequenceNumbers()) {
                return true;
            }
        }
        return false;
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the weak identity map.
     */
    public void useWeakIdentityMap() {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useWeakIdentityMap();
        }
    }

    /**
     * PUBLIC:
     * Switch all descriptors to use the weak identity map.
     */
    public void useWeakIdentityMap(int initialCacheSize) {
        Iterator descriptors = getDescriptors().values().iterator();
        while (descriptors.hasNext()) {
            ClassDescriptor descriptor = (ClassDescriptor)descriptors.next();
            descriptor.useWeakIdentityMap();
            descriptor.setIdentityMapSize(initialCacheSize);
        }
    }

    /**
     * INTERNAL:
     * Default apply  login implementation.
     * Defined for generated subclasses that may not have a login.
     * BUG#2669342
     */
    public void applyLogin() {
        // Do nothing by default.
    }

    /**
     * INTERNAL:
     * Returns the alias descriptors hashtable.
     */
    public Map getAliasDescriptors() {
        return aliasDescriptors;
    }

    /**
     * PUBLIC:
     * Add an alias for the descriptor.
     */
    public void addAlias(String alias, ClassDescriptor descriptor) {
        if (aliasDescriptors == null) {
            aliasDescriptors = new HashMap(10);
        }
        aliasDescriptors.put(alias, descriptor);
    }

    /**
     * INTERNAL:
     * Return true if native sql is allowed on this project.
     */
    public boolean allowTablePerMultitenantDDLGeneration() {
        return this.allowTablePerMultitenantDDLGeneration;
    }

    /**
     * INTERNAL:
     * Return true if native sql is allowed on this project.
     */
    public boolean allowNativeSQLQueries() {
        return this.allowNativeSQLQueries;
    }

    /**
     * INTERNAL:
     * Return true if Max/Min functions should return Null for this project.
     */
    public boolean allowNullResultMaxMin() {
        return this.allowNullResultMaxMin;
    }

    /**
     * INTERNAL:
     * Return true if ResultSet values should be converted for this project.
     */
    public boolean allowConvertResultToBoolean() {
        return this.allowConvertResultToBoolean;
    }

    /**
     * INTERNAL:
     * Return true if SQL calls can defer to EOT on this project.
     */
    public boolean allowSQLDeferral() {
        return this.allowSQLDeferral;
    }

    /**
     * INTERNAL:
     * Return true is allowed to transform named stored procedure parameters into positional/index based.
     */
    public boolean namingIntoIndexed() {
        return this.namingIntoIndexed;
    }

    /**
     * PUBLIC:
     * Return the descriptor for  the alias
     */
    public ClassDescriptor getDescriptorForAlias(String alias) {
        ClassDescriptor descriptor = null;
        if (aliasDescriptors != null) {
            descriptor = (ClassDescriptor)aliasDescriptors.get(alias);
        }
        return descriptor;
    }

    /**
     * INTERNAL:
     * Set the alias descriptors hashtable.
     */
    public void setAliasDescriptors(Map aHashtable) {
        aliasDescriptors = aHashtable;
    }

    /**
     * INTERNAL:
     * Set whether ddl generation should allowed for table per tenant
     * multitenant descriptors. This will only be true when a non shared emf
     * is used and all the tenant context properties are provided at deploy
     * time.
     */
    public void setAllowTablePerMultitenantDDLGeneration(boolean allowTablePerMultitenantDDLGeneration) {
        this.allowTablePerMultitenantDDLGeneration = allowTablePerMultitenantDDLGeneration;
    }

    /**
     * INTERNAL:
     * Set whether native sql is allowed on this project.
     */
    public void setAllowNativeSQLQueries(boolean allowNativeSQLQueries) {
        this.allowNativeSQLQueries = allowNativeSQLQueries;
    }

    /**
     * INTERNAL:
     * Set whether Max/Min functions should return Null for this project.
     */
    public void setAllowNullResultMaxMin(boolean allowNullResultMaxMin) {
        this.allowNullResultMaxMin = allowNullResultMaxMin;
    }

    /**
     * INTERNAL:
     * Set whether ResultSet values should be converted for this project.
     */
    public void setAllowConvertResultToBoolean(boolean allowConvertResultToBoolean) {
        this.allowConvertResultToBoolean = allowConvertResultToBoolean;
    }

    /**
     * INTERNAL:
     * Set whether sql deferral is allowed on this project
     */
    public void setAllowSQLDeferral(boolean allowSQLDeferral) {
        this.allowSQLDeferral = allowSQLDeferral;
    }

    /**
     * INTERNAL:
     * Set whether named stored procedure parameters is allowed to transform into positional/index based.
     */
    public void setNamingIntoIndexed(boolean namingIntoIndexed) {
        this.namingIntoIndexed = namingIntoIndexed;
    }

    /**
     * INTERNAL:
     * Indicates whether there is at least one descriptor that has at least on mapping that
     * require a call on deleted objects to update change sets.
     */
    public boolean hasMappingsPostCalculateChangesOnDeleted() {
        return hasMappingsPostCalculateChangesOnDeleted;
    }

    /**
     * INTERNAL:
     * Indicates whether there is at least one descriptor that has at least on mapping that
     * require a call on deleted objects to update change sets.
     */
    public void setHasMappingsPostCalculateChangesOnDeleted(boolean hasMappingsPostCalculateChangesOnDeleted) {
        this.hasMappingsPostCalculateChangesOnDeleted = hasMappingsPostCalculateChangesOnDeleted;
    }

    /**
     * INTERNAL:
     * Return whether there any mappings that are mapped superclasses.
     * @return
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public boolean hasMappedSuperclasses() {
        return (null != this.mappedSuperclassDescriptors && !this.mappedSuperclassDescriptors.isEmpty());
    }

    /**
     * INTERNAL:
     * Return whether the given class is mapped as superclass.
     * @param className
     * @return
     * @since EclipseLink 2.3 for the JPA 2.0 Reference Implementation
     */
    public boolean hasMappedSuperclass(String className) {
        if (!hasMappedSuperclasses()) {
            return false;
        }

        return this.mappedSuperclassDescriptors.containsKey(className);
    }

    /**
     * INTERNAL:
     * Return all pre-defined not yet parsed EJBQL queries.
     */
    public void addJPAQuery(DatabaseQuery query) {
        getJPAQueries().add(query);
    }

    /**
     * INTERNAL:
     * Return all pre-defined not yet parsed EJBQL queries to table per tenant entities.
     */
    public void addJPATablePerTenantQuery(DatabaseQuery query) {
        getJPATablePerTenantQueries().add(query);
    }

    /**
     * INTERNAL:
     * 266912: Add a descriptor to the Map of mappedSuperclass descriptors
     * @param key (Metadata class)
     * @param value (RelationalDescriptor)
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public void addMappedSuperclass(String key, ClassDescriptor value, boolean replace) {
        // Lazy initialization of the mappedSuperclassDescriptors field.
        if(null == this.mappedSuperclassDescriptors) {
            this.mappedSuperclassDescriptors = new HashMap<String, ClassDescriptor>(2);
        }
        // Avoid replacing the current RelationalDescriptor that may have mappings set
        if(replace || !this.mappedSuperclassDescriptors.containsKey(key)) {
            this.mappedSuperclassDescriptors.put(key, value);
        }
    }

    /**
     * INTERNAL:
     * Use the Metadata key parameter to lookup the
     * Descriptor from the Map of mappedSuperclass descriptors
     * @param key - theMetadata class
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public ClassDescriptor getMappedSuperclass(String key) {
        // TODO: this implementation may have side effects when we have the same class
        // in different class loaders - however currently there is only one classLoader per project
        // Lazy initialization of the mappedSuperclassDescriptors field.
        if(null == this.mappedSuperclassDescriptors) {
            this.mappedSuperclassDescriptors = new HashMap<String, ClassDescriptor>(2);
            return null;
        }
        return this.mappedSuperclassDescriptors.get(key);
    }

    /**
     * INTERNAL:
     * Return the Map of RelationalDescriptor objects representing mapped superclass parents
     * keyed by className of the metadata class.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public Map<String, ClassDescriptor> getMappedSuperclassDescriptors() {
        // Lazy initialization of the mappedSuperclassDescriptors field.
        if(null == this.mappedSuperclassDescriptors) {
            this.mappedSuperclassDescriptors = new HashMap<String, ClassDescriptor>(2);
        }
        return this.mappedSuperclassDescriptors;
    }

    /**
     * INTERNAL:
     * Add an IdClass entry to the map of ids for a particular owner
     * This function is used exclusively by the Metamodel API.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public void addMetamodelIdClassMapEntry(String ownerName, String name) {
        // Add a possible composite key to the owner - this function will handle duplicates by overwriting the entry
        if(this.metamodelIdClassMap.containsKey(ownerName)) {
            // If we have a key entry then the list will always exist
            this.metamodelIdClassMap.get(ownerName).add(name);
        } else {
            List<String> ownerList = new ArrayList<String>();
            ownerList.add(name);
            this.metamodelIdClassMap.put(ownerName, ownerList);
        }
    }

    /**
     * INTERNAL:
     * Return the Map of IdClass attribute lists keyed on owner class name.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public Map<String, List<String>> getMetamodelIdClassMap() {
        return metamodelIdClassMap;
    }

    /**
     * PUBLIC:
     * Return the map of partitioning policies, keyed by name.
     */
    public Map<String, PartitioningPolicy> getPartitioningPolicies() {
        if (this.partitioningPolicies == null) {
            this.partitioningPolicies = new HashMap<String, PartitioningPolicy>();
        }
        return partitioningPolicies;
    }

    /**
     * PUBLIC:
     * Set the map of partitioning policies, keyed by name.
     */
    public void setPartitioningPolicies(Map<String, PartitioningPolicy> partitioningPolicies) {
        this.partitioningPolicies = partitioningPolicies;
    }

    /**
     * PUBLIC:
     * Set the map of partitioning policies, keyed by name.
     */
    public void addPartitioningPolicy(PartitioningPolicy partitioningPolicy) {
        getPartitioningPolicies().put(partitioningPolicy.getName(), partitioningPolicy);
    }

    /**
     * PUBLIC:
     * Return the partitioning policies for the name.
     */
    public PartitioningPolicy getPartitioningPolicy(String name) {
        if (this.partitioningPolicies == null) {
            return null;
        }
        return this.partitioningPolicies.get(name);
    }
}

