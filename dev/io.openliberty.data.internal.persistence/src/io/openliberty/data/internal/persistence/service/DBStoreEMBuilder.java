/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence.service;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.InMemoryMappingFile;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.data.internal.persistence.EntityInfo;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;

/**
 * This builder is used when a data source JNDI name, id, resource reference,
 * or a dataStore id is configured as the repository dataStore.
 * It creates entity managers from a PersistenceServiceUnit from the persistence service.
 */
public class DBStoreEMBuilder extends EntityManagerBuilder {
    static final String EOLN = String.format("%n");
    private static final long MAX_WAIT_FOR_SERVICE_NS = TimeUnit.SECONDS.toNanos(60);
    private static final TraceComponent tc = Tr.register(DBStoreEMBuilder.class);

    private final ClassDefiner classDefiner = new ClassDefiner();

    /**
     * The id of a databaseStore configuration element.
     */
    private final String databaseStoreId;

    /**
     * DataSourceFactory.target property of the databaseStore configuration element.
     */
    private final String dataSourceFactoryFilter;

    /**
     * A map of generated entity class to the record class for which it was generated.
     */
    private final Map<Class<?>, Class<?>> generatedToRecordClass = new HashMap<>();

    /**
     * The persistence service unit is obtained from the persistence service during the
     * initialization that is performed by the run method.
     */
    private final PersistenceServiceUnit persistenceServiceUnit;

    /**
     * Locates an existing databaseStore or creates a new one corresponding to the
     * dataStore name that is specified on the Repository annotation.
     *
     * @param provider              OSGi service that provides the CDI extension.
     * @param repositoryClassLoader class loader of the repository interface.
     * @param dataStore             dataStore name specified on the Repository annotation, or
     *                                  if obtained from CDI then the config.displayId of a dataSource.
     * @param isJNDIName            indicates if the dataStore name is a JNDI name (begins with java: or is inferred to be java:comp/env/...)
     * @param metaDataIdentifier    metadata identifier for the class loader of the repository interface.
     * @param appModComp            application/module/component in which the repository interface is defined.
     *                                  Module and component might be null or absent.
     * @param entityTypes           entity classes as known by the user, not generated.
     */
    public DBStoreEMBuilder(DataExtensionProvider provider, ClassLoader repositoryClassLoader,
                            String dataStore, boolean isJNDIName,
                            String metadataIdentifier, String[] appModComp,
                            Set<Class<?>> entityTypes) {
        super(provider, repositoryClassLoader);
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        try {
            String qualifiedName = null;
            boolean javaApp = false, javaModule = false, javaComp = false;
            String application = appModComp.length < 1 ? null : appModComp[0];
            String module = appModComp.length < 2 ? null : appModComp[1];
            String component = appModComp.length < 3 ? null : appModComp[2];

            // Qualify resource reference and DataSourceDefinition JNDI names with the application/module/component name to make them unique
            if (isJNDIName) {
                javaApp = dataStore.regionMatches(5, "app", 0, 3);
                javaModule = !javaApp && dataStore.regionMatches(5, "module", 0, 6);
                // TODO detect web module metadata to avoid including component
                javaComp = !javaApp && !javaModule && dataStore.regionMatches(5, "comp", 0, 4);
                StringBuilder s = new StringBuilder(dataStore.length() + 80);
                if (application != null && (javaApp || javaModule || javaComp)) {
                    s.append("application[").append(application).append(']').append('/');
                    if (module != null && (javaModule || javaComp)) {
                        s.append("module[").append(module).append(']').append('/');
                        if (component != null && javaComp)
                            s.append("component[").append(component).append(']').append('/');
                    }
                }
                qualifiedName = s.append("databaseStore[").append(dataStore).append(']').toString();

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "computed qualified dataStore name from JNDI name as " + qualifiedName);
            }

            Map<String, Configuration> dbStoreConfigurations = provider.dbStoreConfigAllApps.get(application);
            Configuration dbStoreConfig = dbStoreConfigurations == null ? null : dbStoreConfigurations.get(isJNDIName ? qualifiedName : dataStore);
            Dictionary<String, Object> dbStoreConfigProps = dbStoreConfig == null ? null : dbStoreConfig.getProperties();
            String dbStoreId = dbStoreConfigProps == null ? null : (String) dbStoreConfigProps.get("id");
            if (dbStoreId == null) {
                String dsFactoryFilter = null;
                BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();
                ServiceReference<ResourceFactory> dsRef = null;
                if (isJNDIName) {
                    // Look for DataSourceDefinition with jndiName and application/module/component matching
                    StringBuilder filter = new StringBuilder(200) //
                                    .append("(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)");
                    if (application != null && (javaApp || javaModule || javaComp))
                        filter.append(FilterUtils.createPropertyFilter("application", application));
                    if (module != null && javaModule || javaComp)
                        filter.append(FilterUtils.createPropertyFilter("module", module));
                    if (component != null && javaComp)
                        filter.append(FilterUtils.createPropertyFilter("jndiName", dataStore));
                    filter.append(FilterUtils.createPropertyFilter("jndiName", dataStore)) //
                                    .append(')');

                    Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class,
                                                                                                   filter.toString());
                    if (!dsRefs.isEmpty()) {
                        dbStoreId = qualifiedName;
                        dsRef = dsRefs.iterator().next();
                    }
                } else {
                    // Look for databaseStore with id matching
                    String filter = FilterUtils.createPropertyFilter("id", dataStore);
                    Collection<ServiceReference<DatabaseStore>> dbStoreRefs = bc.getServiceReferences(DatabaseStore.class, filter);
                    if (!dbStoreRefs.isEmpty()) {
                        dbStoreId = dataStore;
                        dsFactoryFilter = (String) dbStoreRefs.iterator().next().getProperty("DataSourceFactory.target");
                    } else {
                        // Look for dataSource with id matching
                        filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("id", dataStore) + ')';
                        Collection<ServiceReference<ResourceFactory>> dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                        if (!dsRefs.isEmpty()) {
                            dbStoreId = "application[" + application + "]/databaseStore[" + dataStore + ']';
                            dsRef = dsRefs.iterator().next();
                        } else {
                            // Look for dataSource with jndiName matching
                            filter = "(&(service.factoryPid=com.ibm.ws.jdbc.dataSource)" + FilterUtils.createPropertyFilter("jndiName", dataStore) + ')';
                            dsRefs = bc.getServiceReferences(ResourceFactory.class, filter);
                            if (!dsRefs.isEmpty()) {
                                dbStoreId = "application[" + application + "]/databaseStore[" + dataStore + ']';
                                dsRef = dsRefs.iterator().next();
                            } // else no databaseStore or dataSource is found
                        }
                    }
                }
                if (dbStoreId == null) {
                    // Create a ResourceFactory that can delegate back to a resource reference lookup
                    ResourceFactory delegator = new DelegatingResourceFactory(dataStore, metadataIdentifier, provider);
                    Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                    dbStoreId = isJNDIName ? qualifiedName : ("application[" + application + "]/databaseStore[" + dataStore + ']');
                    String id = dbStoreId + "/ResourceFactory";
                    svcProps.put("id", id);
                    svcProps.put("config.displayId", id);
                    if (application != null)
                        svcProps.put("application", application);
                    ServiceRegistration<ResourceFactory> reg = bc.registerService(ResourceFactory.class, delegator, svcProps);
                    dsRef = reg.getReference();//

                    Queue<ServiceRegistration<ResourceFactory>> registrations = provider.delegatorsAllApps.get(application);
                    if (registrations == null) {
                        Queue<ServiceRegistration<ResourceFactory>> empty = new ConcurrentLinkedQueue<>();
                        if ((registrations = provider.delegatorsAllApps.putIfAbsent(application, empty)) == null)
                            registrations = empty;
                    }
                    registrations.add(reg);
                }

                // If we generated a databaseStore id, then create the configuration for it,
                if (dbStoreId != dataStore) {
                    if (dbStoreConfigurations == null) {
                        Map<String, Configuration> empty = new ConcurrentHashMap<>();
                        if ((dbStoreConfigurations = provider.dbStoreConfigAllApps.putIfAbsent(application, empty)) == null)
                            dbStoreConfigurations = empty;
                    }

                    String dataSourceId = (String) dsRef.getProperty("id");
                    boolean nonJTA = Boolean.FALSE.equals(dsRef.getProperty("transactional"));

                    Hashtable<String, Object> svcProps = new Hashtable<String, Object>();
                    svcProps.put("id", dbStoreId);
                    svcProps.put("config.displayId", dbStoreId);

                    if (dataSourceId == null)
                        dsFactoryFilter = "(jndiName=" + dsRef.getProperty("jndiName") + ')';
                    else
                        dsFactoryFilter = "(id=" + dataSourceId + ')';

                    svcProps.put("DataSourceFactory.target", dsFactoryFilter);

                    svcProps.put("AuthData.target", "(service.pid=${authDataRef})");
                    svcProps.put("AuthData.cardinality.minimum", 0);

                    svcProps.put("NonJTADataSourceFactory.cardinality.minimum", nonJTA ? 1 : 0);
                    if (nonJTA)
                        svcProps.put("NonJTADataSourceFactory.target", svcProps.get("DataSourceFactory.target"));
                    else
                        svcProps.put("NonJTADataSourceFactory.target", "(&(service.pid=${nonTransactionalDataSourceRef})(transactional=false))");

                    svcProps.put("createTables", provider.createTables);
                    svcProps.put("dropTables", provider.dropTables);
                    svcProps.put("tablePrefix", "");
                    svcProps.put("keyGenerationStrategy", "AUTO");

                    dbStoreConfig = provider.configAdmin.createFactoryConfiguration("com.ibm.ws.persistence.databaseStore", bc.getBundle().getLocation());
                    dbStoreConfig.update(svcProps);
                    dbStoreConfigurations.put(isJNDIName ? qualifiedName : dataStore, dbStoreConfig);
                } else if (dsRef != null) {
                    dsFactoryFilter = "(config.displayId=" + dsRef.getProperty("config.displayId") + ')';
                }
                dataSourceFactoryFilter = dsFactoryFilter;
            } else {
                dataSourceFactoryFilter = (String) dbStoreConfigProps.get("DataSourceFactory.target");
            }

            databaseStoreId = dbStoreId;

            BundleContext bc = FrameworkUtil.getBundle(DatabaseStore.class).getBundleContext();

            ServiceReference<DatabaseStore> ref = null;
            for (long start = System.nanoTime(), poll_ms = 125L; ref == null; poll_ms = poll_ms < 1000L ? poll_ms * 2 : 1000L) {
                Collection<ServiceReference<DatabaseStore>> refs = bc.getServiceReferences(DatabaseStore.class,
                                                                                           FilterUtils.createPropertyFilter("id", databaseStoreId));
                if (refs.isEmpty()) {
                    if (System.nanoTime() - start < MAX_WAIT_FOR_SERVICE_NS) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "Wait " + poll_ms + " ms for service reference to become available...");
                        TimeUnit.MILLISECONDS.sleep(poll_ms);
                    } else {
                        throw new IllegalStateException("The " + databaseStoreId + " service component did not become available within " +
                                                        TimeUnit.NANOSECONDS.toSeconds(MAX_WAIT_FOR_SERVICE_NS) + " seconds.");
                    }
                } else {
                    ref = refs.iterator().next();
                }
            }

            String tablePrefix = (String) ref.getProperty("tablePrefix");

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, databaseStoreId + " databaseStore reference", ref);

            // Classes explicitly annotated with JPA @Entity:
            Set<String> entityClassNames = new LinkedHashSet<>(entityTypes.size() * 2);
            Set<String> entityTableNames = new LinkedHashSet<>(entityClassNames.size());

            ArrayList<InMemoryMappingFile> generatedEntities = new ArrayList<InMemoryMappingFile>();

            // List of classes to inspect for the above
            Queue<Class<?>> annotatedEntityClassQueue = new LinkedList<>();

            // XML to make all other classes into JPA entities:
            ArrayList<String> entityClassInfo = new ArrayList<>(entityTypes.size());

            Queue<Class<?>> embeddableTypesQueue = new LinkedList<>();

            Set<Class<?>> converterTypes = new HashSet<>(); // TODO why do we need to write converters to orm.xml at all?

            for (Class<?> c : entityTypes) {
                if (c.isAnnotationPresent(Entity.class)) {
                    annotatedEntityClassQueue.add(c);

                    for (Field field : c.getFields()) {
                        Convert convert = field.getAnnotation(Convert.class);
                        if (convert != null)
                            converterTypes.add(convert.converter());
                    }

                    for (Method method : c.getMethods()) {
                        Convert convert = method.getAnnotation(Convert.class);
                        if (convert != null)
                            converterTypes.add(convert.converter());
                    }
                } else {
                    if (c.isRecord()) {
                        String entityClassName = c.getName() + EntityInfo.RECORD_ENTITY_SUFFIX; // an entity class is generated for the record
                        byte[] generatedEntityBytes = generateEntityClassBytes(c, entityClassName);
                        generatedEntities.add(new InMemoryMappingFile(generatedEntityBytes, entityClassName.replace('.', '/') + ".class"));
                        Class<?> generatedEntity = classDefiner.findLoadedOrDefineClass(getRepositoryClassLoader(), entityClassName, generatedEntityBytes);
                        generatedToRecordClass.put(generatedEntity, c);
                        c = generatedEntity;
                    }

                    StringBuilder xml = new StringBuilder(500).append(" <entity class=\"").append(c.getName()).append("\">").append(EOLN);

                    xml.append("  <table name=\"").append(tablePrefix).append(c.getSimpleName()).append("\"/>").append(EOLN);

                    writeAttributes(xml, c, false, embeddableTypesQueue);

                    xml.append(" </entity>").append(EOLN);

                    entityClassInfo.add(xml.toString());
                }
            }

            Set<Class<?>> embeddableTypes = new HashSet<>();
            for (Class<?> type; (type = embeddableTypesQueue.poll()) != null;)
                // TODO what if the embeddable type is a record?
                if (embeddableTypes.add(type)) { // only write each type once
                    StringBuilder xml = new StringBuilder(500).append(" <embeddable class=\"").append(type.getName()).append("\">").append(EOLN);
                    writeAttributes(xml, type, true, embeddableTypesQueue);
                    xml.append(" </embeddable>").append(EOLN);
                    entityClassInfo.add(xml.toString());
                }

            for (Class<?> type : converterTypes) {
                StringBuilder xml = new StringBuilder(500).append(" <converter class=\"").append(type.getName()).append("\"></converter>").append(EOLN);
                entityClassInfo.add(xml.toString());
            }

            // Discover entities that are indirectly referenced via OneToOne, ManyToMany, and so forth
            for (Class<?> c; (c = annotatedEntityClassQueue.poll()) != null;)
                if (entityClassNames.add(c.getName())) {
                    Table table = c.getAnnotation(Table.class);
                    entityTableNames.add(table == null || table.name().length() == 0 ? c.getSimpleName() : table.name());
                    Class<?> e;
                    for (Field f : c.getFields())
                        if (f.getType().isAnnotationPresent(Entity.class))
                            annotatedEntityClassQueue.add(f.getType());
                        else if ((e = getEntityClass(f.getGenericType())) != null)
                            annotatedEntityClassQueue.add(e);
                    for (Method m : c.getMethods())
                        if (m.getReturnType().isAnnotationPresent(Entity.class))
                            annotatedEntityClassQueue.add(m.getReturnType());
                        else if ((e = getEntityClass(m.getGenericReturnType())) != null)
                            annotatedEntityClassQueue.add(e);
                }

            Map<String, Object> properties = new HashMap<>();

            properties.put("io.openliberty.persistence.internal.entityClassInfo",
                           entityClassInfo.toArray(new String[entityClassInfo.size()]));

            properties.put("io.openliberty.persistence.internal.tableNames", entityTableNames);

            if (!generatedEntities.isEmpty())
                properties.put("io.openliberty.persistence.internal.generatedEntities", generatedEntities);

            DatabaseStore dbstore = bc.getService(ref);
            persistenceServiceUnit = dbstore.createPersistenceServiceUnit(getRepositoryClassLoader(),
                                                                          properties,
                                                                          entityClassNames.toArray(new String[entityClassNames.size()]));

            collectEntityInfo(entityTypes);

        } catch (RuntimeException x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw new CompletionException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entityTypes)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            throw x;
        }
    }

    /**
     * Initially copied from @nmittles pull #25248
     *
     * Adds the default (no arg) constructor. <p>
     *
     * @param cw     ASM ClassWriter to add the constructor to.
     * @param parent fully qualified name of the parent class
     *                   with '/' as the separator character
     *                   (i.e. internal name).
     */
    private static void addDefaultCtor(ClassWriter cw, String parent) {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "    " + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>()
        // {
        // -----------------------------------------------------------------------
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();

        // -----------------------------------------------------------------------
        //    super();
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Initially copied from @nmittles pull #25248
     *
     * @param recordClass
     * @param internalEntityClassName
     * @param cw
     * @param parent
     */
    private static void addRecordCtor(Class<?> recordClass, String internalEntityClassName, ClassWriter cw, String parent) {
        org.objectweb.asm.commons.Method constructor = org.objectweb.asm.commons.Method.getMethod("void <init> (" + recordClass.getTypeName() + ")");
        GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, constructor, null, null, cw);
        mg.loadThis();
        mg.invokeConstructor(org.objectweb.asm.Type.getType(Object.class), org.objectweb.asm.commons.Method.getMethod("void <init> ()"));

        for (RecordComponent component : recordClass.getRecordComponents()) {
            String componentName = component.getName();
            String typeDesc = component.getType().getTypeName();
            String methodName = "set" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            int componentValue = mg.newLocal(org.objectweb.asm.Type.getType(component.getType()));

            mg.loadArg(0);
            mg.invokeVirtual(org.objectweb.asm.Type.getType(recordClass),
                             org.objectweb.asm.commons.Method.getMethod(typeDesc + " " + componentName + " ()"));

            mg.storeLocal(componentValue);

            mg.loadThis();
            mg.loadLocal(componentValue);
            mg.invokeVirtual(org.objectweb.asm.Type.getObjectType(internalEntityClassName),
                             org.objectweb.asm.commons.Method.getMethod("void " + methodName + " (" + typeDesc + ")"));
        }
        mg.returnValue();
        mg.endMethod();
    }

    @Override
    public EntityManager createEntityManager() {
        return persistenceServiceUnit.createEntityManager();
    }

    @Override
    @Trivial
    public boolean equals(Object o) {
        DBStoreEMBuilder b;
        return this == o || o instanceof DBStoreEMBuilder
                            && databaseStoreId.equals((b = (DBStoreEMBuilder) o).databaseStoreId)
                            && getRepositoryClassLoader().equals(b.getRepositoryClassLoader());
    }

    /**
     * Initially copied from @nmittles pull #25248
     *
     * @param recordClass
     * @param entityClassName
     * @return
     */
    private byte[] generateEntityClassBytes(Class<?> recordClass, String entityClassName) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String internal_entityClassName = entityClassName.replace('.', '/');

        // Define the Entity Class object
        cw.visit(V17, ACC_PUBLIC + ACC_SUPER,
                 internal_entityClassName,
                 null,
                 "java/lang/Object",
                 null);

        // Define the source code file and debug settings
        String sourceFileName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add default constructor
        addDefaultCtor(cw, null);

        FieldVisitor fv;
        for (RecordComponent component : recordClass.getRecordComponents()) {
            String componentName = component.getName();
            Class<?> componentClass = component.getType();
            Type componentType = component.getGenericType();
            String typeDesc = org.objectweb.asm.Type.getDescriptor(componentClass);
            String fieldSig = null, setterSig = null, getterSig = null;
            if (componentType instanceof ParameterizedType) {
                SignatureWriter sigwriter = new SignatureWriter();
                SignatureVisitor componentClassVisitor = sigwriter.visitParameterType();
                componentClassVisitor.visitClassType(componentClass.getName());
                for (Type typeVarType : ((ParameterizedType) componentType).getActualTypeArguments()) {
                    SignatureVisitor typeVarVisitor = componentClassVisitor.visitTypeArgument('=');
                    typeVarVisitor.visitClassType(typeVarType.getTypeName());
                    typeVarVisitor.visitEnd();
                }
                sigwriter.visitEnd();
                sigwriter.visitReturnType().visitBaseType('V');
                setterSig = sigwriter.toString().replace('.', '/');
                fieldSig = setterSig.substring(1, setterSig.length() - 2); // omit beginning ( and ending )V
                getterSig = "()" + fieldSig;
            }

            // --------------------------------------------------------------------
            // public <FieldType> <FieldName>;
            // --------------------------------------------------------------------
            if (trace && tc.isEntryEnabled())
                Tr.debug(tc, "     " + "adding field : " +
                             componentName + " " +
                             typeDesc,
                         fieldSig);

            fv = cw.visitField(ACC_PUBLIC, componentName,
                               typeDesc,
                               fieldSig,
                               null);

            fv.visitEnd();

            // TODO include signature for setter and getter

            // --------------------------------------------------------------------
            // public setter...
            // --------------------------------------------------------------------
            if (trace && tc.isEntryEnabled())
                Tr.debug(tc, "     " + "adding setter : " +
                             component.getName() + " " +
                             component.getType().descriptorString(),
                         setterSig);
            String methodName = "set" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName, "(" + typeDesc + ")V", setterSig, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(org.objectweb.asm.Type.getType(component.getType()).getOpcode(ILOAD), 1);
            mv.visitFieldInsn(PUTFIELD, internal_entityClassName, componentName, typeDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();

            // --------------------------------------------------------------------
            // public getter...
            // --------------------------------------------------------------------
            methodName = "get" + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
            mv = cw.visitMethod(ACC_PUBLIC, methodName, "()" + typeDesc, getterSig, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internal_entityClassName, componentName, typeDesc);
            mv.visitInsn(org.objectweb.asm.Type.getType(component.getType()).getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        // Add constructor that will invoke all setters with data from a Record
        addRecordCtor(recordClass, internal_entityClassName, cw, null);

        // Mark the end of the generated wrapper class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();

        if (trace && tc.isEntryEnabled()) {

            // Get class bytes as a string for debugging
            String classByteString = "";
            for (int i = 0; i < classBytes.length; i++) {
                classByteString += i % 8 == 0 ? "\t" : ""; // Separate 8 bytes by tab
                classByteString += i % 32 == 0 ? System.lineSeparator() : ""; // Separate 16 bytes by line
                classByteString += String.format("%02X", classBytes[i]) + " "; // Separate each byte by space
            }

            Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes" +
                        classByteString);
        }

        return classBytes;
    }

    /**
     * Obtains the DataSource that is used by the EntityManager.
     *
     * @return the DataSource that is used by the EntityManager.
     */
    @Override
    public DataSource getDataSource() {
        BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
        Collection<ServiceReference<ResourceFactory>> dsFactoryRefs;
        try {
            dsFactoryRefs = bc.getServiceReferences(ResourceFactory.class, dataSourceFactoryFilter);
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never happen
        }
        if (dsFactoryRefs.isEmpty())
            throw new IllegalStateException("The " + dataSourceFactoryFilter +
                                            " DataSource that is used by the repository is not available."); // TODO NLS

        ResourceFactory dsFactory = bc.getService(dsFactoryRefs.iterator().next());
        try {
            ResourceConfig resRef = null;
            if (!(dsFactory instanceof DelegatingResourceFactory)) {
                // Use a resource reference that includes the authDataRef of the databaseStore.
                resRef = provider.resourceConfigFactory.createResourceConfig(DataSource.class.getName());
                resRef.setSharingScope(ResourceConfig.SHARING_SCOPE_SHAREABLE);
                resRef.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
                resRef.setResAuthType(ResourceConfig.AUTH_CONTAINER);

                String dbStoreFilter = FilterUtils.createPropertyFilter("id", databaseStoreId);
                Collection<ServiceReference<DatabaseStore>> dbStoreRefs = bc.getServiceReferences(DatabaseStore.class, dbStoreFilter);
                if (dbStoreRefs.isEmpty())
                    throw new IllegalStateException("The " + dbStoreFilter + " resource that is used by the repository is not available."); // TODO NLS

                ServiceReference<DatabaseStore> ref = dbStoreRefs.iterator().next();
                if (ref.getProperty("authDataRef") != null) {
                    String authDataFilter = (String) ref.getProperty("AuthData.target");
                    ServiceReference<?>[] authDataRefs = bc.getServiceReferences("com.ibm.websphere.security.auth.data.AuthData",
                                                                                 authDataFilter);
                    if (authDataRefs == null)
                        throw new IllegalStateException("The " + authDataFilter + " resource that is used by the repository is not available."); // TODO NLS

                    // The following pattern is copied from DatabaseStoreImpl,
                    String authDataId = (String) authDataRefs[0].getProperty("id");
                    resRef.addLoginProperty("DefaultPrincipalMapping",
                                            authDataId.matches(".*(\\]/).*(\\[default-\\d*\\])") //
                                                            ? (String) authDataRefs[0].getProperty("config.displayId") //
                                                            : authDataId);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "using resource reference", resRef);
            }

            return (DataSource) dsFactory.createResource(resRef);
        } catch (Exception x) {
            throw new DataException(x); // TODO NLS
        }
    }

    /**
     * Obtains the entity class (if any) for a type that might be parameterized.
     *
     * @param type a type that might be parameterized.
     * @return entity class or null.
     */
    @Trivial
    private Class<?> getEntityClass(java.lang.reflect.Type type) {
        Class<?> c = null;
        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            type = typeParams.length == 1 ? typeParams[0] : null;
            if (type instanceof Class && ((Class<?>) type).isAnnotationPresent(Entity.class))
                c = (Class<?>) type;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "getEntityClass from parameterized " + type + ": " + c);
        }
        return c;
    }

    @Override
    @Trivial
    protected Class<?> getRecordClass(Class<?> generatedEntityClass) {
        return generatedToRecordClass.get(generatedEntityClass);
    }

    @Override
    @Trivial
    public int hashCode() {
        return databaseStoreId.hashCode();

    }

    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(26 + databaseStoreId.length()) //
                        .append("DBStoreEMBuilder@") //
                        .append(Integer.toHexString(hashCode())) //
                        .append(":").append(databaseStoreId) //
                        .toString();
    }

    /**
     * Write attributes for the specified entity or embeddable to XML.
     *
     * @param xml                  XML for defining the entity attributes
     * @param c                    entity class
     * @param isEmbeddable         indicates if the class is an embeddable type rather than an entity.
     * @param embeddableTypesQueue queue of embeddable types. This method adds to the queue when an embeddable type is found.
     */
    private void writeAttributes(StringBuilder xml, Class<?> c, boolean isEmbeddable, Queue<Class<?>> embeddableTypesQueue) {

        // Identify attributes
        SortedMap<String, Class<?>> attributes = new TreeMap<>();

        // TODO cover records once compiling against Java 17

        for (Field f : c.getFields())
            attributes.putIfAbsent(f.getName(), f.getType());

        try {
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(c).getPropertyDescriptors();
            if (propertyDescriptors != null)
                for (PropertyDescriptor p : propertyDescriptors) {
                    Method setter = p.getWriteMethod();
                    if (setter != null)
                        attributes.putIfAbsent(p.getName(), p.getPropertyType());
                }
        } catch (IntrospectionException x) {
            throw new MappingException(x);
        }

        String keyAttributeName = null;
        if (!isEmbeddable) {
            // Determine which attribute is the id.
            // Precedence is:
            // (1) has name of Id, or ID, or id.
            // (2) name ends with Id.
            // (3) name ends with ID.
            // (4) type is UUID.
            // (5) name ends with id.
            int precedence = 10;
            for (Map.Entry<String, Class<?>> attribute : attributes.entrySet()) {
                String name = attribute.getKey();
                Class<?> type = attribute.getValue(); // TODO compare type against the repository key type if defined
                if (name.length() > 2) {
                    if (precedence > 2) {
                        char i = name.charAt(name.length() - 2);
                        if (i == 'I') {
                            char d = name.charAt(name.length() - 1);
                            if (d == 'd') {
                                keyAttributeName = name;
                                precedence = 2;
                            } else if (d == 'D' && precedence > 3) {
                                keyAttributeName = name;
                                precedence = 3;
                            }
                        } else if (i == 'i' && precedence > 5 && name.charAt(name.length() - 1) == 'd') {
                            keyAttributeName = name;
                            precedence = 5;
                        }
                    }
                } else if (name.equalsIgnoreCase("ID")) {
                    keyAttributeName = name;
                    precedence = 1;
                    break;
                } else if (precedence > 4 && UUID.class.equals(type)) {
                    keyAttributeName = name;
                    precedence = 4;
                }
            }

            if (keyAttributeName == null)
                throw new MappingException("Entity class " + c.getName() + " lacks a public field of the form *ID or public method of the form get*ID."); // TODO NLS
        }

        // Write the attributes to XML:

        xml.append("  <attributes>").append(EOLN);

        for (Map.Entry<String, Class<?>> attributeInfo : attributes.entrySet()) {
            String attributeName = attributeInfo.getKey();
            Class<?> attributeType = attributeInfo.getValue();
            boolean isCollection = Collection.class.isAssignableFrom(attributeType);
            boolean isPrimitive = attributeType.isPrimitive();

            String columnType;
            if (isPrimitive || attributeType.isInterface() || Serializable.class.isAssignableFrom(attributeType)) {
                columnType = keyAttributeName != null && keyAttributeName.equalsIgnoreCase(attributeName) ? "id" : //
                                "version".equalsIgnoreCase(attributeName) ? "version" : //
                                                isCollection ? "element-collection" : // TODO add fetch-type eager
                                                                "basic";
            } else {
                columnType = "embedded";
                embeddableTypesQueue.add(attributeType);
            }

            xml.append("   <").append(columnType).append(" name=\"").append(attributeName).append('"');
            if (isCollection)
                xml.append(" fetch=\"EAGER\"");
            xml.append('>').append(EOLN);

            if (isEmbeddable) {
                if (!"embedded".equals(columnType))
                    xml.append("    <column name=\"").append(c.getSimpleName().toUpperCase()).append(attributeName.toUpperCase()).append("\"/>").append(EOLN);
            } else {
                if (isPrimitive)
                    xml.append("    <column nullable=\"false\"/>").append(EOLN);
            }

            xml.append("   </" + columnType + ">").append(EOLN);
        }

        xml.append("  </attributes>").append(EOLN);
    }
}