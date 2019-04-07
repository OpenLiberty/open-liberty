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
package com.ibm.ws.jpa.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 *
 */
public class EclipselinkIntrospection extends AbstractIntrospection {
    private final static String FFDCCN = EclipselinkIntrospection.class.getName();

    // Eclipselink Session Objects
    private final Set<Object> sessionSet = new HashSet<Object>();
    private final Map<Object, String> sessionDiagMap = new HashMap<Object, String>();

    // Eclipselink Project Objects
    private final Set<Object> projectSet = new HashSet<Object>();
    private final Map<Object, String> projectDiagMap = new HashMap<Object, String>();

    // Eclipselink ClassDescriptor Objects
    private final Set<Object> classDescriptorSet = new HashSet<Object>();
    private final Map<Object, String> classDescriptorDiagMap = new HashMap<Object, String>();

    // Eclipselink DatabaseTable Objects
    private final Set<Object> databaseTableSet = new HashSet<Object>();
    private final Map<Object, String> databaseTableDiagMap = new HashMap<Object, String>();

    // Eclipselink DatabaseField Objects
    private final Set<Object> databaseFieldSet = new HashSet<Object>();
    private final Map<Object, String> databaseFieldDiagMap = new HashMap<Object, String>();

    // Eclipselink DatabaseMapping Objects
    private final Set<Object> databaseMappingSet = new HashSet<Object>();
    private final Map<Object, String> databaseMappingDiagMap = new HashMap<Object, String>();

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jpa.diagnostics.PlatformIntrospection#dumpJPAEntityManagerFactoryState(java.lang.Object, java.io.PrintWriter)
     */
    @Override
    public void dumpJPAEntityManagerFactoryState(final Object emf, final PrintWriter out) {
        dumpECLJPAEntityManagerFactoryState(emf, out);

        try {
            // Dump Collected Info About Session Objects
            out.println();
            out.println("Session Objects (" + sessionDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : sessionDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

            // Dump Collected Info About Project Objects
            out.println("Project Objects (" + projectDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : projectDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

            // Dump Collected Info About ClassDescriptor Objects
            out.println("ClassDescriptor Objects (" + classDescriptorDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : classDescriptorDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

            // Dump Collected Info About DatabaseTable Objects
            out.println("DatabaseTable Objects (" + databaseTableDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : databaseTableDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

            // Dump Collected Info About DatabaseField Objects
            out.println("DatabaseField Objects (" + databaseFieldDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : databaseFieldDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

            // Dump Collected Info About DatabaseMapping Objects
            out.println("DatabaseMapping Objects (" + databaseMappingDiagMap.size() + "):");

            for (Map.Entry<Object, String> entry : databaseMappingDiagMap.entrySet()) {
                out.println(entry.getValue());
                out.println();
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".dumpJPAEntityManagerFactoryState", "39");
        }
    }

    private void dumpECLJPAEntityManagerFactoryState(final Object emf, final PrintWriter out) {
        // Expecting emf to be of type org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl
        final String emfCN = emf.getClass().getName();
        if (!"org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl".equals(emfCN)) {
            out.println("Eclipselink EMF Class Type " + emfCN + " is not recognized.  Cannot introspect further.");
            return;
        }

        out.println("Eclipselink EntityManagerFactory: " + getInstanceClassAndAddress(emf));

        try {
            final Object emfDelegate = reflectObjValue(emf, "delegate");
            out.println("   delegate = " + emfDelegate);

            if (emfDelegate == null) {
                return; // Cannot continue
            }

            out.println("   .session = " + processSessionInfoObject(reflectObjValue(emfDelegate, "session")));
            out.println("   .myCache = " + reflectObjValue(emfDelegate, "myCache"));
            final Object setupImpl = reflectObjValue(emfDelegate, "setupImpl");
            out.println("   .setupImpl = " + setupImpl);
            dumpEntityManagerSetupImpl(setupImpl, out, "       ");
            out.println("   .flushMode = " + reflectObjValue(emfDelegate, "flushMode"));
            out.println("   .referenceMode = " + reflectObjValue(emfDelegate, "referenceMode"));
            out.println("   .commitOrder = " + reflectObjValue(emfDelegate, "commitOrder")); // uow
            out.println("   .owner = " + reflectObjValue(emfDelegate, "owner"));
            out.println("   .isOpen = " + reflectObjValue(emfDelegate, "isOpen"));
            out.println("   .closeOnCommit = " + reflectObjValue(emfDelegate, "closeOnCommit"));
            out.println("   .persistOnCommit = " + reflectObjValue(emfDelegate, "persistOnCommit"));
            out.println("   .shouldValidateExistence = " + reflectObjValue(emfDelegate, "shouldValidateExistence"));
            out.println("   .commitWithoutPersistRules = " + reflectObjValue(emfDelegate, "commitWithoutPersistRules"));
            out.println("   .flushClearCache = " + reflectObjValue(emfDelegate, "flushClearCache"));
            out.println("   .properties = " + poa(reflectObjValue(emfDelegate, "properties"), "      ", true));
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".dumpECLJPAEntityManagerFactoryState", "77");
        }
    }

    private void dumpEntityManagerSetupImpl(final Object emfsi, final PrintWriter out, final String indent) {
        try {
            out.println(indent + " persistenceUnitUniqueName = " + reflectObjValue(emfsi, "persistenceUnitUniqueName"));
            out.println(indent + " sessionName = " + reflectObjValue(emfsi, "sessionName"));
            out.println(indent + " processor = " + reflectObjValue(emfsi, "processor"));
            out.println(indent + " weaver = " + reflectObjValue(emfsi, "weaver"));
            out.println(indent + " persistenceUnitInfo = " + getInstanceClassAndAddress(reflectObjValue(emfsi, "persistenceUnitInfo")));
            out.println(indent + " factoryCount = " + reflectObjValue(emfsi, "factoryCount"));
            out.println(indent + " session = " + processSessionInfoObject(reflectObjValue(emfsi, "session")));
            out.println(indent + " isInContainerMode = " + reflectObjValue(emfsi, "isInContainerMode"));
            out.println(indent + " isSessionLoadedFromSessionsXML = " + reflectObjValue(emfsi, "isSessionLoadedFromSessionsXML"));
            out.println(indent + " projectCacheAccessor = " + reflectObjValue(emfsi, "projectCacheAccessor"));
            out.println(indent + " shouldBuildProject = " + reflectObjValue(emfsi, "shouldBuildProject"));
            out.println(indent + " enableWeaving = " + reflectObjValue(emfsi, "enableWeaving"));
            out.println(indent + " isWeavingStatic = " + reflectObjValue(emfsi, "isWeavingStatic"));
            out.println(indent + " staticWeaveInfo = " + reflectObjValue(emfsi, "staticWeaveInfo"));
            out.println(indent + " securableObjectHolder = " + reflectObjValue(emfsi, "securableObjectHolder"));
            out.println(indent + " deployLock = " + reflectObjValue(emfsi, "deployLock"));
            out.println(indent + " requiresConnection = " + reflectObjValue(emfsi, "requiresConnection"));
            out.println(indent + " metaModel = " + reflectObjValue(emfsi, "metaModel"));
            out.println(indent + " structConverters = " + poa(reflectObjValue(emfsi, "structConverters"), "         ", true));
            out.println(indent + " state = " + reflectObjValue(emfsi, "state"));
            out.println(indent + " compositeEmSetupImpl = " + reflectObjValue(emfsi, "compositeEmSetupImpl"));
            out.println(indent + " compositeMemberEmSetupImpls = " + poa(reflectObjValue(emfsi, "compositeMemberEmSetupImpls"), "         ", true));
            out.println(indent + " mode = " + reflectObjValue(emfsi, "mode"));
            out.println(indent + " throwExceptionOnFail = " + reflectObjValue(emfsi, "throwExceptionOnFail"));
            out.println(indent + " weaveChangeTracking = " + reflectObjValue(emfsi, "weaveChangeTracking"));
            out.println(indent + " weaveLazy = " + reflectObjValue(emfsi, "weaveLazy"));
            out.println(indent + " weaveEager = " + reflectObjValue(emfsi, "weaveEager"));
            out.println(indent + " weaveFetchGroups = " + reflectObjValue(emfsi, "weaveFetchGroups"));
            out.println(indent + " weaveInternal = " + reflectObjValue(emfsi, "weaveInternal"));
            out.println(indent + " weaveRest = " + reflectObjValue(emfsi, "weaveRest"));
            out.println(indent + " isMetadataExpired = " + reflectObjValue(emfsi, "isMetadataExpired"));
            out.println(indent + " persistenceException = " + reflectObjValue(emfsi, "persistenceException"));
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".dumpEntityManagerSetupImpl", "89");
        }
    }

    private String processSessionInfoObject(final Object session) {
        if (session == null || sessionSet.contains(session)
            || !isCastable("org.eclipse.persistence.internal.sessions.AbstractSession", session.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(session);
        }

        sessionSet.add(session);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(session));
            out.println("      .toString = " + poa(session));
            final Class sessionObjC = session.getClass();
            final String sessionObjCN = sessionObjC.getName();

            // org.eclipse.persistence.internal.sessions.AbstractSession Fields
            out.println("      .exceptionHandler = " + reflectObjValue(session, "exceptionHandler"));
            out.println("      .integrityChecker = " + reflectObjValue(session, "integrityChecker"));
            out.println("      .project = " + processProjectObject(reflectObjValue(session, "project")));
            out.println("      .transactionMutex = " + reflectObjValue(session, "transactionMutex"));
            out.println("      .commitManager = " + reflectObjValue(session, "commitManager"));
            out.println("      .broker = " + reflectObjValue(session, "broker"));
            out.println("      .platform = " + reflectObjValue(session, "platform"));
            out.println("      .externalTransactionController = " + reflectObjValue(session, "externalTransactionController"));
            out.println("      .commandManager = " + reflectObjValue(session, "commandManager"));
            out.println("      .queryBuilder = " + reflectObjValue(session, "queryBuilder"));
            out.println("      .serializer = " + reflectObjValue(session, "serializer"));
            out.println("      .entityListenerInjectionManager = " + reflectObjValue(session, "entityListenerInjectionManager"));
            out.println("      .queries = " + poa(reflectObjValue(session, "queries"), "         ", true));
            final Object descriptors = reflectObjValue(session, "descriptors");
            out.println("      .descriptors = " + poa(descriptors, "         ", true)); // Map<Class, ClassDescriptor>
            try {
                if (descriptors != null && !(((Map<?, ?>) descriptors).isEmpty())) {
                    processClassDescriptorCollection(((Map<?, ?>) descriptors).values());
                }
            } catch (Throwable t) {
            }
            out.println("      .identityMapAccessor = " + reflectObjValue(session, "identityMapAccessor"));
            out.println("      .partitioningPolicy = " + reflectObjValue(session, "partitioningPolicy"));
            out.println("      .attributeGroups = " + poa(reflectObjValue(session, "attributeGroups"), "         ", true)); // Map<String, AttributeGroup>
            out.println("      .properties = " + poa(reflectObjValue(session, "properties"), "         ", true));
            out.println("      .staticMetamodelClasses = " + poa(reflectObjValue(session, "staticMetamodelClasses"), "         ", true));
            out.println("      .deferredEvents = " + poa(reflectObjValue(session, "deferredEvents"), "         ", true));
            out.println("      .wasJTSTransactionInternallyStarted = " + reflectObjValue(session, "wasJTSTransactionInternallyStarted"));
            out.println("      .jpaQueriesProcessed = " + reflectObjValue(session, "jpaQueriesProcessed"));
            out.println("      .isInBroker = " + reflectObjValue(session, "isInBroker"));
            out.println("      .shouldCheckWriteLock = " + reflectObjValue(session, "shouldCheckWriteLock"));
            out.println("      .shouldPropagateChanges = " + reflectObjValue(session, "shouldPropagateChanges"));
            out.println("      .isInProfile = " + reflectObjValue(session, "isInProfile"));
            out.println("      .isLoggingOff = " + reflectObjValue(session, "isLoggingOff"));
            out.println("      .isFinalizersEnabled = " + reflectObjValue(session, "isFinalizersEnabled"));
            out.println("      .isSynchronized = " + reflectObjValue(session, "isSynchronized"));
            out.println("      .isConcurrent = " + reflectObjValue(session, "isConcurrent"));
            out.println("      .isExecutingEvents = " + reflectObjValue(session, "isExecutingEvents"));
            out.println("      .shouldOptimizeResultSetAccess = " + reflectObjValue(session, "shouldOptimizeResultSetAccess"));
            out.println("      .tolerateInvalidJPQL = " + reflectObjValue(session, "tolerateInvalidJPQL"));
            out.println("      .numberOfActiveUnitsOfWork = " + reflectObjValue(session, "numberOfActiveUnitsOfWork"));
            out.println("      .pessimisticLockTimeoutDefault = " + reflectObjValue(session, "pessimisticLockTimeoutDefault"));
            out.println("      .queryTimeoutDefault = " + reflectObjValue(session, "queryTimeoutDefault"));
            out.println("      .queryTimeoutUnitDefault = " + reflectObjValue(session, "queryTimeoutUnitDefault"));
            out.println("      .name = " + reflectObjValue(session, "name"));

            if (isCastable("org.eclipse.persistence.sessions.server.ClientSession", sessionObjC)) {
                out.println("       ----- ClientSession fields");
                out.println("      .parent = " + processSessionInfoObject(reflectObjValue(session, "parent")));
                out.println("      .connectionPolicy = " + reflectObjValue(session, "connectionPolicy"));
                out.println("      .writeConnections = " + poa(reflectObjValue(session, "writeConnections"), "         ", true));
                out.println("      .sequencing = " + reflectObjValue(session, "sequencing"));
                out.println("      .isActive = " + reflectObjValue(session, "isActive"));
            }

            if (isCastable("org.eclipse.persistence.internal.sessions.DatabaseSessionImpl", sessionObjC)) {
                out.println("       ----- DatabaseSessionImpl fields");
                out.println("      .databaseEventListener = " + reflectObjValue(session, "databaseEventListener"));
                out.println("      .sequencingHome = " + reflectObjValue(session, "sequencingHome"));
                out.println("      .serverPlatform = " + reflectObjValue(session, "serverPlatform"));
                out.println("      .tuner = " + reflectObjValue(session, "tuner"));
                out.println("      .connectedTime = " + reflectObjValue(session, "connectedTime"));
                out.println("      .isLoggedIn = " + reflectObjValue(session, "isLoggedIn"));
            }

            if (isCastable("org.eclipse.persistence.sessions.remote.DistributedSession", sessionObjC)) {
                out.println("       ----- DistributedSession fields");
                out.println("      .remoteConnection = " + reflectObjValue(session, "remoteConnection"));
                out.println("      .hasDefaultReadOnlyClasses = " + reflectObjValue(session, "hasDefaultReadOnlyClasses"));
                out.println("      .isMetadataRemote = " + reflectObjValue(session, "isMetadataRemote"));
            }

            if (isCastable("org.eclipse.persistence.sessions.remote.RemoteSession", sessionObjC)) {
                out.println("       ----- RemoteSession fields");
                out.println("      .sequencing = " + reflectObjValue(session, "sequencing"));
                out.println("      .shouldEnableDistributedIndirectionGarbageCollection = " + reflectObjValue(session, "shouldEnableDistributedIndirectionGarbageCollection"));
            }

            if (isCastable("org.eclipse.persistence.sessions.server.ServerSession", sessionObjC)) {
                out.println("       ----- ServerSession fields");
                out.println("      .readConnectionPool = " + reflectObjValue(session, "readConnectionPool"));
                out.println("      .connectionPools = " + poa(reflectObjValue(session, "connectionPools"), "         ", true));
                out.println("      .defaultConnectionPolicy = " + reflectObjValue(session, "defaultConnectionPolicy"));
                out.println("      .numberOfNonPooledConnectionsUsed = " + reflectObjValue(session, "numberOfNonPooledConnectionsUsed"));
                out.println("      .maxNumberOfNonPooledConnections = " + reflectObjValue(session, "maxNumberOfNonPooledConnections"));
            }
            if (isCastable("org.eclipse.persistence.sessions.broker.SessionBroker", sessionObjC)) {
                out.println("       ----- SessionBroker fields");
                out.println("      .parent = " + processSessionInfoObject(reflectObjValue(session, "parent")));
                out.println("      .sessionNamesByClass = " + poa(reflectObjValue(session, "sessionNamesByClass"), "         ", true));
                out.println("      .sessionsByName = " + poa(reflectObjValue(session, "sessionsByName"), "         ", true));
                out.println("      .sequencing = " + reflectObjValue(session, "sequencing"));
                out.println("      .shouldUseDescriptorAliases = " + reflectObjValue(session, "shouldUseDescriptorAliases"));
            }

            if (isCastable("org.eclipse.persistence.internal.sessions.UnitOfWorkImpl", sessionObjC)) {
                out.println("       ----- UnitOfWorkImpl fields");
                out.println("      .unitOfWorkChangeSet = " + reflectObjValue(session, "unitOfWorkChangeSet"));
                // TODO
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processSessionInfoObject", "253");
        } finally {
            sessionDiagMap.put(session, baos.toString());
        }

        return getInstanceClassAndAddress(session);
    }

    /*
     * Project
     */

    private String processProjectObject(final Object project) {
        if (project == null || projectSet.contains(project)
            || !isCastable("org.eclipse.persistence.sessions.Project", project.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(project);
        }

        projectSet.add(project);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(project));
            out.println("      .toString = " + poa(project));

            // org.eclipse.persistence.sessions.Project Fields
            out.println("      .name = " + reflectObjValue(project, "name"));
            out.println("      .datasourceLogin = " + reflectObjValue(project, "datasourceLogin"));
            final Object descriptorsMap = reflectObjValue(project, "descriptors");
            out.println("      .descriptors = " + poa(descriptorsMap, "         ", true)); // Map<Class, ClassDescriptor>
            try {
                if (descriptorsMap != null && (!((Map) descriptorsMap).isEmpty())) {
                    processClassDescriptorCollection(((Map) descriptorsMap).values());
                }
            } catch (Throwable t) {
            }
            final Object orderedDescriptors = reflectObjValue(project, "orderedDescriptors");
            out.println("      .orderedDescriptors = " + poa(orderedDescriptors, "         ", true)); // List<ClassDescriptor>
            try {
                if (orderedDescriptors != null && !(((List) orderedDescriptors).isEmpty())) {
                    processClassDescriptorCollection(orderedDescriptors);
                }
            } catch (Throwable t) {
            }
            out.println("      .multitenantPolicy = " + reflectObjValue(project, "multitenantPolicy"));
            out.println("      .defaultReadOnlyClasses = " + poa(reflectObjValue(project, "defaultReadOnlyClasses"), "         ", true)); // Vector
            out.println("      .aliasDescriptors = " + poa(reflectObjValue(project, "aliasDescriptors"), "         ", true)); // Map
            out.println("      .hasIsolatedClasses = " + reflectObjValue(project, "hasIsolatedClasses"));
            out.println("      .hasNonIsolatedUOWClasses = " + reflectObjValue(project, "hasNonIsolatedUOWClasses"));
            out.println("      .hasGenericHistorySupport = " + reflectObjValue(project, "hasGenericHistorySupport"));
            out.println("      .hasProxyIndirection = " + reflectObjValue(project, "hasProxyIndirection"));
            out.println("      .sqlResultSetMappings = " + poa(reflectObjValue(project, "sqlResultSetMappings"), "         ", true)); // Map<String, SQLResultSetMapping>
            out.println("      .jpqlParseCache = " + reflectObjValue(project, "jpqlParseCache"));
            out.println("      .defaultTemporalMutable = " + reflectObjValue(project, "defaultTemporalMutable"));
            out.println("      .hasMappingsPostCalculateChangesOnDeleted = " + reflectObjValue(project, "hasMappingsPostCalculateChangesOnDeleted"));
            out.println("      .defaultIdentityMapClass = " + reflectObjValue(project, "defaultIdentityMapClass"));
            out.println("      .defaultIdentityMapSize = " + reflectObjValue(project, "defaultIdentityMapSize"));
            out.println("      .defaultCacheIsolation = " + reflectObjValue(project, "defaultCacheIsolation"));
            out.println("      .defaultQueryResultsCachePolicy = " + reflectObjValue(project, "defaultQueryResultsCachePolicy"));
            out.println("      .defaultIdValidation = " + reflectObjValue(project, "defaultIdValidation"));
            out.println("      .queries = " + poa(reflectObjValue(project, "queries"), "         ", true)); // List<DatabaseQuery>
            out.println("      .attributeGroups = " + poa(reflectObjValue(project, "attributeGroups"), "         ", true)); // Map<String, AttributeGroup>
            out.println("      .jpaQueries = " + poa(reflectObjValue(project, "jpaQueries"), "         ", true)); // List
            out.println("      .jpaTablePerTenantQueries = " + poa(reflectObjValue(project, "jpaTablePerTenantQueries"), "         ", true)); // List
            out.println("      .allowNativeSQLQueries = " + reflectObjValue(project, "allowNativeSQLQueries"));
            out.println("      .allowTablePerMultitenantDDLGeneration = " + reflectObjValue(project, "allowTablePerMultitenantDDLGeneration"));
            out.println("      .allowSQLDeferral = " + reflectObjValue(project, "allowSQLDeferral"));
            final Object mappedSuperclassDescriptors = reflectObjValue(project, "mappedSuperclassDescriptors");
            out.println("      .mappedSuperclassDescriptors = " + poa(mappedSuperclassDescriptors, "         ", true)); // Map<String, ClassDescriptor>
            try {
                if (mappedSuperclassDescriptors != null && (!((Map) mappedSuperclassDescriptors).isEmpty())) {
                    processClassDescriptorCollection(((Map) mappedSuperclassDescriptors).values());
                }
            } catch (Throwable t) {
            }
            out.println("      .metamodelIdClassMap = " + poa(reflectObjValue(project, "metamodelIdClassMap"), "         ", true)); // Map
            out.println("      .partitioningPolicies = " + poa(reflectObjValue(project, "partitioningPolicies"), "         ", true)); // Map
            out.println("      .descriptorsLock = " + reflectObjValue(project, "descriptorsLock"));
            out.println("      .vpdIdentifier = " + reflectObjValue(project, "vpdIdentifier"));
            out.println("      .vpdLastIdentifierClassName = " + reflectObjValue(project, "vpdLastIdentifierClassName"));
            out.println("      .classNamesForWeaving = " + poa(reflectObjValue(project, "classNamesForWeaving"), "         ", true)); // Collection
            out.println("      .structConverters = " + poa(reflectObjValue(project, "structConverters"), "         ", true)); // Collection
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processSessionInfoObject", "279");
        } finally {
            projectDiagMap.put(project, baos.toString());
        }

        return getInstanceClassAndAddress(project);
    }

    /*
     * ClassDescriptor
     */

    private void processClassDescriptorCollection(final Object classDescriptorCollection) {
        if (classDescriptorCollection == null || !isCastable("java.util.Collection", classDescriptorCollection.getClass())) {
            return;
        }

        final Collection<?> c = (Collection<?>) classDescriptorCollection;
        for (Object descriptor : c) {
            processClassDescriptorObject(descriptor);
        }
    }

    private String processClassDescriptorObject(final Object classDescriptor) {
        if (classDescriptor == null || classDescriptorSet.contains(classDescriptor)
            || !isCastable("org.eclipse.persistence.descriptors.ClassDescriptor", classDescriptor.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(classDescriptor);
        }

        classDescriptorSet.add(classDescriptor);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(classDescriptor));
            out.println("      .toString = " + poa(classDescriptor));

            // org.eclipse.persistence.descriptors.ClassDescriptor Fields
            out.println("      .javaClass = " + reflectObjValue(classDescriptor, "javaClass"));
            out.println("      .javaClassName = " + reflectObjValue(classDescriptor, "javaClassName"));
            final Object tables = reflectObjValue(classDescriptor, "tables");
            out.println("      .tables = " + poa(tables, "         ", true)); // Vector<DatabaseTable>
            try {
                if (tables != null && !(((Collection) tables).isEmpty())) {
                    processDatabaseTableCollection(tables);
                }
            } catch (Throwable t) {
            }
            out.println("      .defaultTable = " + processDatabaseTableObject(reflectObjValue(classDescriptor, "defaultTable")));
            final Object primaryKeyFields = reflectObjValue(classDescriptor, "primaryKeyFields");
            out.println("      .primaryKeyFields = " + poa(primaryKeyFields, "         ", true)); // List<DatabaseField>
            try {
                if (primaryKeyFields != null && !(((Collection) primaryKeyFields).isEmpty())) {
                    processDatabaseFieldCollection(primaryKeyFields);
                }
            } catch (Throwable t) {
            }
            final Object additionalTablePrimaryKeyFields = reflectObjValue(classDescriptor, "additionalTablePrimaryKeyFields");
            out.println("      .additionalTablePrimaryKeyFields = " + poa(additionalTablePrimaryKeyFields, "         ", true)); // Map<DatabaseTable, Map<DatabaseField, DatabaseField>>
            try {
                if (additionalTablePrimaryKeyFields != null && !(((Map) additionalTablePrimaryKeyFields).isEmpty())) {
                    processDatabaseTableObject(((Map) additionalTablePrimaryKeyFields).keySet());

                    final Collection values = ((Map) additionalTablePrimaryKeyFields).values();
                    for (Object val : values) { // val is a Map<DatabaseField, DatabaseField>
                        processDatabaseFieldObject(((Map) val).keySet());
                        processDatabaseFieldObject(((Map) val).values());
                    }
                }
            } catch (Throwable t) {
            }
            final Object multipleTableInsertOrder = reflectObjValue(classDescriptor, "multipleTableInsertOrder");
            out.println("      .multipleTableInsertOrder = " + poa(multipleTableInsertOrder, "         ", true)); // List<DatabaseTable>
            try {
                if (multipleTableInsertOrder != null && !(((List) multipleTableInsertOrder).isEmpty())) {
                    processDatabaseTableObject(multipleTableInsertOrder);
                }
            } catch (Throwable t) {
            }
            final Object multipleTableForeignKeys = reflectObjValue(classDescriptor, "multipleTableForeignKeys"); // Map<DatabaseTable, Set<DatabaseTable>>
            out.println("      .multipleTableForeignKeys = " + poa(multipleTableForeignKeys, "         ", true));
            try {
                if (multipleTableForeignKeys != null && !(((Map) multipleTableForeignKeys).isEmpty())) {
                    processDatabaseTableObject(((Map) multipleTableForeignKeys).keySet());
                    processDatabaseTableCollection(((Map) multipleTableForeignKeys).values());
                }
            } catch (Throwable t) {
            }
            out.println("      .isCascadeOnDeleteSetOnDatabaseOnSecondaryTables = " + reflectObjValue(classDescriptor, "isCascadeOnDeleteSetOnDatabaseOnSecondaryTables"));
            final Object fields = reflectObjValue(classDescriptor, "fields");
            out.println("      .fields = " + poa(fields, "         ", true)); // Vector<DatabaseField>
            try {
                if (fields != null && !(((Collection<?>) fields).isEmpty())) {
                    processDatabaseFieldCollection(fields);
                }
            } catch (Throwable t) {
            }
            final Object allFields = reflectObjValue(classDescriptor, "allFields");
            out.println("      .allFields = " + poa(allFields, "         ", true)); // Vector<DatabaseField>
            try {
                if (allFields != null && !(((Collection<?>) allFields).isEmpty())) {
                    processDatabaseFieldCollection(allFields);
                }
            } catch (Throwable t) {
            }
            final Object selectionFields = reflectObjValue(classDescriptor, "selectionFields");
            out.println("      .selectionFields = " + poa(selectionFields, "         ", true)); // List<DatabaseField>
            try {
                if (selectionFields != null && !(((Collection<?>) selectionFields).isEmpty())) {
                    processDatabaseFieldCollection(selectionFields);
                }
            } catch (Throwable t) {
            }
            final Object allSelectionFields = reflectObjValue(classDescriptor, "allSelectionFields");
            out.println("      .allSelectionFields = " + poa(allSelectionFields, "         ", true)); // List<DatabaseField>
            try {
                if (allSelectionFields != null && !(((Collection<?>) allSelectionFields).isEmpty())) {
                    processDatabaseFieldCollection(allSelectionFields);
                }
            } catch (Throwable t) {
            }
            final Object mappings = reflectObjValue(classDescriptor, "mappings");
            out.println("      .mappings = " + poa(mappings, "         ", true)); // Vector<DatabaseMapping>
            try {
                if (mappings != null && !(((Collection<?>) mappings).isEmpty())) {
                    processDatabaseMappingCollection(mappings);
                }
            } catch (Throwable t) {
            }
            final Object referencingClasses = reflectObjValue(classDescriptor, "referencingClasses");
            out.println("      .referencingClasses = " + poa(referencingClasses, "         ", true)); // Set<ClassDescriptor>
            try {
                if (referencingClasses != null && !(((Set) referencingClasses).isEmpty())) {
                    processClassDescriptorCollection(referencingClasses);
                }
            } catch (Throwable t) {
            }
            final Object lockableMappings = reflectObjValue(classDescriptor, "lockableMappings");
            out.println("      .lockableMappings = " + poa(lockableMappings, "         ", true)); // List<DatabaseMapping>
            try {
                if (lockableMappings != null && !(((Collection<?>) lockableMappings).isEmpty())) {
                    processDatabaseMappingCollection(lockableMappings);
                }
            } catch (Throwable t) {
            }
            out.println("      .queryKeys = " + poa(reflectObjValue(classDescriptor, "queryKeys"), "         ", true)); // Map<String, QueryKey>
            out.println("      .sequenceNumberName = " + reflectObjValue(classDescriptor, "sequenceNumberName"));
            out.println("      .sequenceNumberField = " + reflectObjValue(classDescriptor, "sequenceNumberField"));
            out.println("      .sessionName = " + reflectObjValue(classDescriptor, "sessionName"));
            out.println("      .constraintDependencies = " + poa(reflectObjValue(classDescriptor, "constraintDependencies"), "         ", true)); // Vector
            out.println("      .amendmentMethodName = " + reflectObjValue(classDescriptor, "amendmentMethodName"));
            out.println("      .amendmentClass = " + reflectObjValue(classDescriptor, "amendmentClass"));
            out.println("      .amendmentClassName = " + reflectObjValue(classDescriptor, "amendmentClassName"));
            out.println("      .alias = " + reflectObjValue(classDescriptor, "alias"));
            out.println("      .shouldBeReadOnly = " + reflectObjValue(classDescriptor, "shouldBeReadOnly"));
            out.println("      .shouldAlwaysConformResultsInUnitOfWork = " + reflectObjValue(classDescriptor, "shouldAlwaysConformResultsInUnitOfWork"));
            out.println("      .shouldRegisterResultsInUnitOfWork = " + reflectObjValue(classDescriptor, "shouldRegisterResultsInUnitOfWork"));
            out.println("      .queryManager = " + reflectObjValue(classDescriptor, "queryManager"));
            out.println("      .copyPolicy = " + reflectObjValue(classDescriptor, "copyPolicy"));
            out.println("      .copyPolicyClassName = " + reflectObjValue(classDescriptor, "copyPolicyClassName"));
            out.println("      .interfacePolicy = " + reflectObjValue(classDescriptor, "interfacePolicy"));
            out.println("      .optimisticLockingPolicy = " + reflectObjValue(classDescriptor, "optimisticLockingPolicy"));
            out.println("      .cascadeLockingPolicies = " + poa(reflectObjValue(classDescriptor, "cascadeLockingPolicies"), "         ", true)); // List<CascadeLockingPolicy>
            out.println("      .wrapperPolicy = " + reflectObjValue(classDescriptor, "wrapperPolicy"));
            out.println("      .changePolicy = " + reflectObjValue(classDescriptor, "changePolicy"));
            out.println("      .returningPolicy = " + reflectObjValue(classDescriptor, "returningPolicy"));
            out.println("      .historyPolicy = " + reflectObjValue(classDescriptor, "historyPolicy"));
            out.println("      .partitioningPolicyName = " + reflectObjValue(classDescriptor, "partitioningPolicyName"));
            out.println("      .partitioningPolicy = " + reflectObjValue(classDescriptor, "partitioningPolicy"));
            out.println("      .cmpPolicy = " + reflectObjValue(classDescriptor, "cmpPolicy"));
            out.println("      .cachePolicy = " + reflectObjValue(classDescriptor, "cachePolicy"));
            out.println("      .multitenantPolicy = " + reflectObjValue(classDescriptor, "multitenantPolicy"));
            out.println("      .serializedObjectPolicy = " + reflectObjValue(classDescriptor, "serializedObjectPolicy"));
            out.println("      .fetchGroupManager = " + reflectObjValue(classDescriptor, "fetchGroupManager"));
            out.println("      .properties = " + poa(reflectObjValue(classDescriptor, "properties"), "         ", true));
            out.println("      .unconvertedProperties = " + poa(reflectObjValue(classDescriptor, "unconvertedProperties"), "         ", true));
            out.println("      .initializationStage = " + reflectObjValue(classDescriptor, "initializationStage"));
            out.println("      .interfaceInitializationStage = " + reflectObjValue(classDescriptor, "interfaceInitializationStage"));
            out.println("      .descriptorType = " + reflectObjValue(classDescriptor, "descriptorType"));
            out.println("      .shouldOrderMappings = " + reflectObjValue(classDescriptor, "shouldOrderMappings"));
            out.println("      .cacheInvalidationPolicy = " + reflectObjValue(classDescriptor, "cacheInvalidationPolicy"));
            out.println("      .shouldAcquireCascadedLocks = " + reflectObjValue(classDescriptor, "shouldAcquireCascadedLocks"));
            out.println("      .cascadedLockingInitialized = " + reflectObjValue(classDescriptor, "cascadedLockingInitialized"));
            out.println("      .hasSimplePrimaryKey = " + reflectObjValue(classDescriptor, "hasSimplePrimaryKey"));
            out.println("      .hasMultipleTableConstraintDependecy = " + reflectObjValue(classDescriptor, "hasMultipleTableConstraintDependecy"));
            out.println("      .shouldUseFullChangeSetsForNewObjects = " + reflectObjValue(classDescriptor, "shouldUseFullChangeSetsForNewObjects"));
            out.println("      .isNativeConnectionRequired = " + reflectObjValue(classDescriptor, "isNativeConnectionRequired"));
            out.println("      .idValidation = " + reflectObjValue(classDescriptor, "idValidation"));
            out.println("      .primaryKeyIdValidations = " + poa(reflectObjValue(classDescriptor, "primaryKeyIdValidations"), "         ", true));
            final Object derivesIdMappings = reflectObjValue(classDescriptor, "derivesIdMappings");
            out.println("      .derivesIdMappings = " + poa(derivesIdMappings, "         ", true)); // Map<String, DatabaseMapping>
            try {
                if (derivesIdMappings != null && !(((Map<?, ?>) derivesIdMappings).isEmpty())) {
                    processDatabaseMappingCollection(((Map<?, ?>) derivesIdMappings).values());
                }
            } catch (Throwable t) {
            }
            out.println("      .defaultQueryRedirector = " + reflectObjValue(classDescriptor, "defaultQueryRedirector"));
            out.println("      .defaultReadAllQueryRedirector = " + reflectObjValue(classDescriptor, "defaultReadAllQueryRedirector"));
            out.println("      .defaultReadObjectQueryRedirector = " + reflectObjValue(classDescriptor, "defaultReadObjectQueryRedirector"));
            out.println("      .defaultReportQueryRedirector = " + reflectObjValue(classDescriptor, "defaultReportQueryRedirector"));
            out.println("      .defaultUpdateObjectQueryRedirector = " + reflectObjValue(classDescriptor, "defaultUpdateObjectQueryRedirector"));
            out.println("      .defaultInsertObjectQueryRedirector = " + reflectObjValue(classDescriptor, "defaultInsertObjectQueryRedirector"));
            out.println("      .defaultDeleteObjectQueryRedirector = " + reflectObjValue(classDescriptor, "defaultDeleteObjectQueryRedirector"));
            out.println("      .defaultQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultQueryRedirectorClassName"));
            out.println("      .defaultReadAllQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultReadAllQueryRedirectorClassName"));
            out.println("      .defaultReadObjectQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultReadObjectQueryRedirectorClassName"));
            out.println("      .defaultReportQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultReportQueryRedirectorClassName"));
            out.println("      .defaultUpdateObjectQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultUpdateObjectQueryRedirectorClassName"));
            out.println("      .defaultInsertObjectQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultInsertObjectQueryRedirectorClassName"));
            out.println("      .defaultDeleteObjectQueryRedirectorClassName = " + reflectObjValue(classDescriptor, "defaultDeleteObjectQueryRedirectorClassName"));
            out.println("      .sequence = " + reflectObjValue(classDescriptor, "sequence"));
            final Object mappingsPostCalculateChanges = reflectObjValue(classDescriptor, "mappingsPostCalculateChanges");
            out.println("      .mappingsPostCalculateChanges = " + poa(mappingsPostCalculateChanges, "         ", true)); // List<DatabaseMapping>
            try {
                if (mappingsPostCalculateChanges != null && !(((Collection<?>) mappingsPostCalculateChanges).isEmpty())) {
                    processDatabaseMappingCollection(mappingsPostCalculateChanges);
                }
            } catch (Throwable t) {
            }
            final Object mappingsPostCalculateChangesOnDeleted = reflectObjValue(classDescriptor, "mappingsPostCalculateChangesOnDeleted");
            out.println("      .mappingsPostCalculateChangesOnDeleted = " + poa(mappingsPostCalculateChangesOnDeleted, "         ", true)); // List<DatabaseMapping>
            try {
                if (mappingsPostCalculateChangesOnDeleted != null && !(((Collection<?>) mappingsPostCalculateChangesOnDeleted).isEmpty())) {
                    processDatabaseMappingCollection(mappingsPostCalculateChangesOnDeleted);
                }
            } catch (Throwable t) {
            }
            final Object additionalAggregateCollectionKeyFields = reflectObjValue(classDescriptor, "additionalAggregateCollectionKeyFields");
            out.println("      .additionalAggregateCollectionKeyFields = " + poa(additionalAggregateCollectionKeyFields, "         ", true)); // List<DatabaseField>
            try {
                if (additionalAggregateCollectionKeyFields != null && !(((Collection<?>) additionalAggregateCollectionKeyFields).isEmpty())) {
                    processDatabaseFieldCollection(additionalAggregateCollectionKeyFields);
                }
            } catch (Throwable t) {
            }
            final Object preDeleteMappings = reflectObjValue(classDescriptor, "preDeleteMappings");
            out.println("      .preDeleteMappings = " + poa(preDeleteMappings, "         ", true)); // List<DatabaseMapping>
            try {
                if (preDeleteMappings != null && !(((Collection<?>) preDeleteMappings).isEmpty())) {
                    processDatabaseMappingCollection(preDeleteMappings);
                }
            } catch (Throwable t) {
            }
            final Object additionalWritableMapKeyFields = reflectObjValue(classDescriptor, "additionalWritableMapKeyFields");
            out.println("      .additionalWritableMapKeyFields = " + poa(additionalWritableMapKeyFields, "         ", true)); // List<DatabaseField>
            try {
                if (additionalWritableMapKeyFields != null && !(((Collection<?>) additionalWritableMapKeyFields).isEmpty())) {
                    processDatabaseFieldCollection(additionalWritableMapKeyFields);
                }
            } catch (Throwable t) {
            }
            out.println("      .hasRelationships = " + reflectObjValue(classDescriptor, "hasRelationships"));
            final Object foreignKeyValuesForCaching = reflectObjValue(classDescriptor, "foreignKeyValuesForCaching");
            out.println("      .foreignKeyValuesForCaching = " + poa(foreignKeyValuesForCaching, "         ", true)); // Set<DatabaseField>
            try {
                if (foreignKeyValuesForCaching != null && !(((Collection<?>) foreignKeyValuesForCaching).isEmpty())) {
                    processDatabaseFieldCollection(foreignKeyValuesForCaching);
                }
            } catch (Throwable t) {
            }
            out.println("      .hasNoncacheableMappings = " + reflectObjValue(classDescriptor, "hasNoncacheableMappings"));
            out.println("      .hasNoncacheableMappings = " + reflectObjValue(classDescriptor, "hasNoncacheableMappings"));
            out.println("      .virtualAttributeMethods = " + poa(reflectObjValue(classDescriptor, "virtualAttributeMethods"), "         ", true)); // List<VirtualAttributeMethodInfo>
            out.println("      .virtualAttributeMethods = " + poa(reflectObjValue(classDescriptor, "virtualAttributeMethods"), "         ", true)); // List<VirtualAttributeMethodInfo>
            out.println("      .accessorTree = " + poa(reflectObjValue(classDescriptor, "accessorTree"), "         ", true)); // List<AttributeAccessor>
            out.println("      .descriptorCustomizerClassName = " + reflectObjValue(classDescriptor, "descriptorCustomizerClassName"));
            out.println("      .shouldLockForClone = " + reflectObjValue(classDescriptor, "shouldLockForClone"));

        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processClassDescriptorObject", "383");
        } finally {
            classDescriptorDiagMap.put(classDescriptor, baos.toString());
        }

        return getInstanceClassAndAddress(classDescriptor);
    }

    /*
     * DatabaseTable
     */

    private void processDatabaseTableCollection(final Object databaseTableCollection) {
        if (databaseTableCollection == null || !isCastable("java.util.Collection", databaseTableCollection.getClass())) {
            return;
        }

        final Collection<?> c = (Collection<?>) databaseTableCollection;
        for (Object descriptor : c) {
            processDatabaseTableObject(descriptor);
        }
    }

    private String processDatabaseTableObject(final Object databaseTable) {
        if (databaseTable == null || databaseTableSet.contains(databaseTable)
            || !isCastable("org.eclipse.persistence.internal.helper.DatabaseTable", databaseTable.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(databaseTable);
        }

        databaseTableSet.add(databaseTable);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(databaseTable));
            out.println("      .toString = " + poa(databaseTable));

            // org.eclipse.persistence.internal.helper.DatabaseTable Fields
            out.println("      .name = " + reflectObjValue(databaseTable, "name"));
            out.println("      .tableQualifier = " + reflectObjValue(databaseTable, "tableQualifier"));
            out.println("      .qualifiedName = " + reflectObjValue(databaseTable, "qualifiedName"));
            out.println("      .foreignKeyConstraints = " + poa(reflectObjValue(databaseTable, "foreignKeyConstraints"), "         ", true)); // Map<String, ForeignKeyConstraint>
            out.println("      .uniqueConstraints = " + poa(reflectObjValue(databaseTable, "uniqueConstraints"), "         ", true)); // Map<String, List<List<String>>>
            out.println("      .indexes = " + poa(reflectObjValue(databaseTable, "indexes"), "         ", true)); // List<IndexDefinition>
            out.println("      .useDelimiters = " + reflectObjValue(databaseTable, "useDelimiters"));
            out.println("      .creationSuffix = " + reflectObjValue(databaseTable, "creationSuffix"));
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processDatabaseTableObject", "383");
        } finally {
            databaseTableDiagMap.put(databaseTable, baos.toString());
        }

        return getInstanceClassAndAddress(databaseTable);
    }

    /*
     * DatabaseField
     */

    private void processDatabaseFieldCollection(final Object databaseFieldCollection) {
        if (databaseFieldCollection == null || !isCastable("java.util.Collection", databaseFieldCollection.getClass())) {
            return;
        }

        final Collection<?> c = (Collection<?>) databaseFieldCollection;
        for (Object descriptor : c) {
            processDatabaseFieldObject(descriptor);
        }
    }

    private String processDatabaseFieldObject(final Object databaseField) {
        if (databaseField == null || databaseFieldSet.contains(databaseField)
            || !isCastable("org.eclipse.persistence.internal.helper.DatabaseField", databaseField.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(databaseField);
        }

        databaseFieldSet.add(databaseField);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(databaseField));
            out.println("      .toString = " + poa(databaseField));

            // org.eclipse.persistence.internal.helper.DatabaseField Fields
            out.println("      .scale = " + reflectObjValue(databaseField, "scale"));
            out.println("      .length = " + reflectObjValue(databaseField, "length"));
            out.println("      .precision = " + reflectObjValue(databaseField, "precision"));
            out.println("      .isUnique = " + reflectObjValue(databaseField, "isUnique"));
            out.println("      .isNullable = " + reflectObjValue(databaseField, "isNullable"));
            out.println("      .isUpdatable = " + reflectObjValue(databaseField, "isUpdatable"));
            out.println("      .isInsertable = " + reflectObjValue(databaseField, "isInsertable"));
            out.println("      .isCreatable = " + reflectObjValue(databaseField, "isCreatable"));
            out.println("      .isPrimaryKey = " + reflectObjValue(databaseField, "isPrimaryKey"));
            out.println("      .columnDefinition = " + reflectObjValue(databaseField, "columnDefinition"));
            out.println("      .name = " + reflectObjValue(databaseField, "name"));
            out.println("      .qualifiedName = " + reflectObjValue(databaseField, "qualifiedName"));
            out.println("      .table = " + processDatabaseTableObject(reflectObjValue(databaseField, "table")));
            out.println("      .type = " + reflectObjValue(databaseField, "type"));
            out.println("      .typeName = " + reflectObjValue(databaseField, "typeName"));
            out.println("      .sqlType = " + reflectObjValue(databaseField, "sqlType"));
            out.println("      .index = " + reflectObjValue(databaseField, "index"));
            out.println("      .useDelimiters = " + reflectObjValue(databaseField, "useDelimiters"));
            out.println("      .nameForComparisons = " + reflectObjValue(databaseField, "nameForComparisons"));
            out.println("      .useUpperCaseForComparisons = " + reflectObjValue(databaseField, "useUpperCaseForComparisons"));
            out.println("      .isTranslated = " + reflectObjValue(databaseField, "isTranslated"));
            out.println("      .keepInRow = " + reflectObjValue(databaseField, "keepInRow"));

            if (isCastable("org.eclipse.persistence.mappings.structures.ObjectRelationalDatabaseField", databaseField.getClass())) {
                out.println("       ----- ObjectRelationalDatabaseField fields");
                out.println("      .sqlTypeName = " + reflectObjValue(databaseField, "sqlTypeName"));
                out.println("      .nestedTypeField = " + processDatabaseFieldObject(reflectObjValue(databaseField, "nestedTypeField")));
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processDatabaseFieldObject", "383");
        } finally {
            databaseFieldDiagMap.put(databaseField, baos.toString());
        }

        return getInstanceClassAndAddress(databaseField);
    }

    /*
     * DatabaseMapping
     */
    private void processDatabaseMappingCollection(final Object databaseMappingCollection) {
        if (databaseMappingCollection == null || !isCastable("java.util.Collection", databaseMappingCollection.getClass())) {
            return;
        }

        final Collection<?> c = (Collection<?>) databaseMappingCollection;
        for (Object descriptor : c) {
            processDatabaseMappingObject(descriptor);
        }
    }

    private String processDatabaseMappingObject(final Object databaseMapping) {
        if (databaseMapping == null || databaseMappingSet.contains(databaseMapping)
            || !isCastable("org.eclipse.persistence.mappings.DatabaseMapping", databaseMapping.getClass())) {
            // Null or Already processed, so skip.
            return getInstanceClassAndAddress(databaseMapping);
        }

        databaseMappingSet.add(databaseMapping);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos, true);

        try {
            out.println("   " + getInstanceClassAndAddress(databaseMapping));
            out.println("      .toString = " + poa(databaseMapping));

            // org.eclipse.persistence.mappings.DatabaseMapping Fields
            out.println("      .descriptor = " + processClassDescriptorObject(reflectObjValue(databaseMapping, "descriptor")));
            out.println("      .attributeAccessor = " + reflectObjValue(databaseMapping, "attributeAccessor"));
            out.println("      .isReadOnly = " + reflectObjValue(databaseMapping, "isReadOnly"));
            out.println("      .isOptional = " + reflectObjValue(databaseMapping, "isOptional"));
            out.println("      .isLazy = " + reflectObjValue(databaseMapping, "isLazy"));
            final Object fields = reflectObjValue(databaseMapping, "fields");
            out.println("      .fields = " + poa(fields, "         ", true)); // Vector<DatabaseField>
            try {
                if (fields != null && !(((Collection<?>) fields).isEmpty())) {
                    processDatabaseFieldCollection(fields);
                }
            } catch (Throwable t) {
            }
            out.println("      .isRemotelyInitialized = " + reflectObjValue(databaseMapping, "isRemotelyInitialized"));
            out.println("      .weight = " + reflectObjValue(databaseMapping, "weight"));
            out.println("      .properties = " + poa(reflectObjValue(databaseMapping, "properties"), "         ", true));
            out.println("      .unconvertedProperties = " + poa(reflectObjValue(databaseMapping, "unconvertedProperties"), "         ", true));
            out.println("      .derivesId = " + reflectObjValue(databaseMapping, "derivesId"));
            out.println("      .isJPAId = " + reflectObjValue(databaseMapping, "isJPAId"));
            out.println("      .mapsIdValue = " + reflectObjValue(databaseMapping, "mapsIdValue"));
            out.println("      .derivedIdMapping = " + processDatabaseMappingObject(reflectObjValue(databaseMapping, "derivedIdMapping")));
            out.println("      .isPrimaryKeyMapping = " + reflectObjValue(databaseMapping, "isPrimaryKeyMapping"));
            out.println("      .attributeName = " + reflectObjValue(databaseMapping, "attributeName"));
            out.println("      .isMapKeyMapping = " + reflectObjValue(databaseMapping, "isMapKeyMapping"));
            out.println("      .isCacheable = " + reflectObjValue(databaseMapping, "isCacheable"));
            out.println("      .isInSopObject = " + reflectObjValue(databaseMapping, "isInSopObject"));

            if (isCastable("org.eclipse.persistence.mappings.foundation.AbstractColumnMapping", databaseMapping.getClass())) {
                out.println("       ----- AbstractColumnMapping fields");
                out.println("      .field = " + processDatabaseFieldObject(reflectObjValue(databaseMapping, "field")));
                out.println("      .converter = " + reflectObjValue(databaseMapping, "converter"));
                out.println("      .converterClassName = " + reflectObjValue(databaseMapping, "converterClassName"));
                out.println("      .isInsertable = " + reflectObjValue(databaseMapping, "isInsertable"));
                out.println("      .isUpdatable = " + reflectObjValue(databaseMapping, "isUpdatable"));
            }

            if (isCastable("org.eclipse.persistence.mappings.foundation.AbstractDirectMapping", databaseMapping.getClass())) {
                out.println("       ----- AbstractDirectMapping fields");
                out.println("      .attributeClassification = " + reflectObjValue(databaseMapping, "attributeClassification"));
                out.println("      .attributeClassificationName = " + reflectObjValue(databaseMapping, "attributeClassificationName"));
                out.println("      .attributeObjectClassification = " + reflectObjValue(databaseMapping, "attributeObjectClassification"));
                out.println("      .nullValue = " + poa(reflectObjValue(databaseMapping, "nullValue")));
                out.println("      .keyTableForMapKey = " + processDatabaseTableObject(reflectObjValue(databaseMapping, "keyTableForMapKey")));
                out.println("      .fieldClassificationClassName = " + reflectObjValue(databaseMapping, "fieldClassificationClassName"));
                out.println("      .bypassDefaultNullValueCheck = " + reflectObjValue(databaseMapping, "bypassDefaultNullValueCheck"));
                out.println("      .isMutable = " + reflectObjValue(databaseMapping, "isMutable"));
            }

            if (isCastable("org.eclipse.persistence.mappings.foundation.AbstractCompositeDirectCollectionMapping", databaseMapping.getClass())) {
                out.println("       ----- AbstractCompositeDirectCollectionMapping fields");
                out.println("      .field = " + processDatabaseFieldObject(reflectObjValue(databaseMapping, "field")));
                out.println("      .elementDataTypeName = " + reflectObjValue(databaseMapping, "elementDataTypeName"));
                out.println("      .valueConverter = " + reflectObjValue(databaseMapping, "valueConverter"));
                out.println("      .containerPolicy = " + reflectObjValue(databaseMapping, "containerPolicy"));
            }

            if (isCastable("org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping", databaseMapping.getClass())) {
                out.println("       ----- AbstractTransformationMapping fields");
                out.println("      .attributeTransformerClassName = " + reflectObjValue(databaseMapping, "attributeTransformerClassName"));
                out.println("      .attributeTransformer = " + reflectObjValue(databaseMapping, "attributeTransformer"));
                out.println("      .fieldTransformations = " + poa(reflectObjValue(databaseMapping, "fieldTransformations"), "         ", true));
                out.println("      .fieldToTransformers = " + poa(reflectObjValue(databaseMapping, "fieldToTransformers"), "         ", true));
                out.println("      .isMutable = " + reflectObjValue(databaseMapping, "isMutable"));
                out.println("      .indirectionPolicy = " + reflectObjValue(databaseMapping, "indirectionPolicy"));
            }

            if (isCastable("org.eclipse.persistence.mappings.AggregateMapping", databaseMapping.getClass())) {
                out.println("       ----- AggregateMapping fields");
                out.println("      .referenceClass = " + reflectObjValue(databaseMapping, "referenceClass"));
                out.println("      .referenceClassName = " + reflectObjValue(databaseMapping, "referenceClassName"));
                out.println("      .referenceDescriptor = " + processClassDescriptorObject(reflectObjValue(databaseMapping, "referenceDescriptor")));
                out.println("      .hasNestedIdentityReference = " + reflectObjValue(databaseMapping, "hasNestedIdentityReference"));
            }

            if (isCastable("org.eclipse.persistence.mappings.ForeignReferenceMapping", databaseMapping.getClass())) {
                out.println("       ----- ForeignReferenceMapping fields");
                out.println("      .referenceClass = " + reflectObjValue(databaseMapping, "referenceClass"));
                out.println("      .referenceClassName = " + reflectObjValue(databaseMapping, "referenceClassName"));
                out.println("      .tempInitSession = " + reflectObjValue(databaseMapping, "tempInitSession"));
                out.println("      .referenceDescriptor = " + processClassDescriptorObject(reflectObjValue(databaseMapping, "referenceDescriptor")));
                out.println("      .selectionQuery = " + reflectObjValue(databaseMapping, "selectionQuery"));
                out.println("      .isPrivateOwned = " + reflectObjValue(databaseMapping, "isPrivateOwned"));
                out.println("      .batchFetchType = " + reflectObjValue(databaseMapping, "batchFetchType"));
                out.println("      .indirectionPolicy = " + reflectObjValue(databaseMapping, "indirectionPolicy"));
                out.println("      .hasCustomSelectionQuery = " + reflectObjValue(databaseMapping, "hasCustomSelectionQuery"));
                out.println("      .relationshipPartner = " + processDatabaseMappingObject(reflectObjValue(databaseMapping, "relationshipPartner")));
                out.println("      .relationshipPartnerAttributeName = " + reflectObjValue(databaseMapping, "relationshipPartnerAttributeName"));
                out.println("      .cascadePersist = " + reflectObjValue(databaseMapping, "cascadePersist"));
                out.println("      .cascadeMerge = " + reflectObjValue(databaseMapping, "cascadeMerge"));
                out.println("      .cascadeRefresh = " + reflectObjValue(databaseMapping, "cascadeRefresh"));
                out.println("      .cascadeRemove = " + reflectObjValue(databaseMapping, "cascadeRemove"));
                out.println("      .cascadeDetach = " + reflectObjValue(databaseMapping, "cascadeDetach"));
                out.println("      .requiresTransientWeavedFields = " + reflectObjValue(databaseMapping, "requiresTransientWeavedFields"));
                out.println("      .joinFetch = " + reflectObjValue(databaseMapping, "joinFetch"));
                out.println("      .forceInitializationOfSelectionCriteria = " + reflectObjValue(databaseMapping, "forceInitializationOfSelectionCriteria"));
                out.println("      .extendPessimisticLockScope = " + reflectObjValue(databaseMapping, "extendPessimisticLockScope"));
                out.println("      .isCascadeOnDeleteSetOnDatabase = " + reflectObjValue(databaseMapping, "isCascadeOnDeleteSetOnDatabase"));
                out.println("      .partitioningPolicy = " + reflectObjValue(databaseMapping, "partitioningPolicy"));
                out.println("      .partitioningPolicyName = " + reflectObjValue(databaseMapping, "partitioningPolicyName"));
                out.println("      .mappedBy = " + reflectObjValue(databaseMapping, "mappedBy"));
            }

//            out.println("      .XXX = " + reflectObjValue(databaseMapping, "XXX"));
//            out.println("      .XXX = " + reflectObjValue(databaseMapping, "XXX"));

        } catch (Throwable t) {
            FFDCFilter.processException(t, FFDCCN + ".processDatabaseMappingObject", "792");
        } finally {
            databaseMappingDiagMap.put(databaseMapping, baos.toString());
        }

        return getInstanceClassAndAddress(databaseMapping);
    }

}
