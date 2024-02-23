/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.model.Model;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Creates EntityManager instances from an EntityManagerFactory (from a persistence unit reference)
 * or from a PersistenceServiceUnit of a databaseStore.
 * Implementations must provide an equals method to identify when repositories have matching dataStore configuration.
 */
public abstract class EntityManagerBuilder implements Runnable {
    private static final TraceComponent tc = Tr.register(EntityManagerBuilder.class);

    protected final Set<Class<?>> entities = new HashSet<>();

    // Mapping of JPA entity class (not record class) to entity information.
    final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();

    /**
     * The class loader for repository classes.
     */
    private final ClassLoader repositoryClassLoader;

    @Trivial
    protected EntityManagerBuilder(ClassLoader repositoryClassLoader) {
        this.repositoryClassLoader = repositoryClassLoader;
    }

    /**
     * Adds an entity class to be handled.
     *
     * @param entityClass entity class.
     */
    @Trivial
    public void add(Class<?> entityClass) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "add: " + entityClass.getName());

        entities.add(entityClass);
    }

    /**
     * Creates a new EntityManager instance.
     *
     * @return a new EntityManager instance.
     */
    public abstract EntityManager createEntityManager();

    /**
     * Returns the record class that corresponds to the specified generated entity class.
     * If the specified class is not a generated entity class for a record, then this method returns null.
     *
     * @param generatedEntityClass entity class that might be generated from a record.
     * @return corresponding record class for a generated entity class. Otherwise null.
     */
    @Trivial
    protected Class<?> getRecordClass(Class<?> generatedEntityClass) {
        return null;
    }

    /**
     * Request the Id type, allowing for an EclipseLink extension that lets the
     * Id to be located on an attribute of an Embeddable of the entity.
     *
     * @param entityType
     * @return the Id type. Null if the type of Id cannot be determined.
     */
    @FFDCIgnore(RuntimeException.class)
    @Trivial
    private static Type<?> getIdType(EntityType<?> entityType) {
        Type<?> idType;
        try {
            idType = entityType.getIdType();
        } catch (RuntimeException x) {
            // occurs with EclipseLink extension to JPA that allows @Id on an embeddable attribute
            if ("ConversionException".equals(x.getClass().getSimpleName()))
                idType = null;
            else
                throw x;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, entityType.getName() + " getIdType: " + idType);
        return idType;
    }

    /**
     * Returns the class loader for repository classes.
     *
     * @return the class loader for repository classes.
     */
    @Trivial
    protected ClassLoader getRepositoryClassLoader() {
        return repositoryClassLoader;
    }

    /**
     * Initializes the builder before using it.
     *
     * @throws Exception if an error occurs.
     */
    @Trivial
    protected abstract void initialize() throws Exception;

    /**
     * Assigns the public static volatile fields of @StaticMetamodel classes
     * to be the corresponding entity attribute name from the metamodel.
     *
     * @param staticMetamodels static metamodel class(es) per entity class.
     */
    public void populateStaticMetamodelClasses(Map<Class<?>, List<Class<?>>> staticMetamodels) {
        for (Class<?> entityClass : entities) {
            List<Class<?>> metamodelClasses = staticMetamodels.get(entityClass);
            if (metamodelClasses != null) {
                CompletableFuture<EntityInfo> entityInfoFuture = entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);
                EntityInfo entityInfo = entityInfoFuture.join();
                for (Class<?> metamodelClass : metamodelClasses)
                    Model.initialize(metamodelClass, entityInfo.attributeNames);
            }
        }
    }

    /**
     * Initializes the builder once before using it.
     */
    @Override
    @Trivial
    public void run() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "run: define entities", entities);

        EntityManager em = null;
        try {
            initialize();

            em = createEntityManager();

            Metamodel model = em.getMetamodel();
            for (EntityType<?> entityType : model.getEntities()) {
                Map<String, String> attributeNames = new HashMap<>();
                Map<String, List<Member>> attributeAccessors = new HashMap<>();
                SortedMap<String, Class<?>> attributeTypes = new TreeMap<>();
                Map<String, Class<?>> collectionElementTypes = new HashMap<>();
                Map<Class<?>, List<String>> relationAttributeNames = new HashMap<>();
                Queue<Attribute<?, ?>> relationships = new LinkedList<>();
                Queue<String> relationPrefixes = new LinkedList<>();
                Queue<List<Member>> relationAccessors = new LinkedList<>();
                Class<?> recordClass = getRecordClass(entityType.getJavaType());
                Class<?> idType = null;
                SortedMap<String, Member> idClassAttributeAccessors = null;
                String versionAttrName = null;

                for (Attribute<?, ?> attr : entityType.getAttributes()) {
                    String attributeName = attr.getName();
                    PersistentAttributeType attributeType = attr.getPersistentAttributeType();
                    if (PersistentAttributeType.EMBEDDED.equals(attributeType) ||
                        PersistentAttributeType.ONE_TO_ONE.equals(attributeType) ||
                        PersistentAttributeType.MANY_TO_ONE.equals(attributeType)) {
                        relationAttributeNames.put(attr.getJavaType(), new ArrayList<>());
                        relationships.add(attr);
                        relationPrefixes.add(attributeName);
                        relationAccessors.add(Collections.singletonList(attr.getJavaMember()));
                    }

                    Member accessor = recordClass == null ? attr.getJavaMember() : recordClass.getMethod(attributeName);

                    attributeNames.put(attributeName.toLowerCase(), attributeName);
                    attributeAccessors.put(attributeName, Collections.singletonList(accessor));
                    attributeTypes.put(attributeName, attr.getJavaType());
                    if (attr.isCollection()) {
                        if (attr instanceof PluralAttribute)
                            collectionElementTypes.put(attributeName, ((PluralAttribute<?, ?, ?>) attr).getElementType().getJavaType());
                    } else {
                        SingularAttribute<?, ?> singleAttr = attr instanceof SingularAttribute ? (SingularAttribute<?, ?>) attr : null;
                        if (singleAttr != null && singleAttr.isId()) {
                            attributeNames.put("id", attributeName);
                            idType = singleAttr.getJavaType();
                        } else if (singleAttr != null && singleAttr.isVersion()) {
                            versionAttrName = attributeName;
                        } else if (Collection.class.isAssignableFrom(attr.getJavaType())) {
                            // collection attribute that is not annotated with ElementCollection
                            collectionElementTypes.put(attributeName, Object.class);
                        }
                    }
                }

                // Guard against recursive processing of OneToOne (and similar) relationships
                // by tracking whether we have already processed each entity class involved.
                Set<Class<?>> entityTypeClasses = new HashSet<>();
                entityTypeClasses.add(entityType.getJavaType());

                for (Attribute<?, ?> attr; (attr = relationships.poll()) != null;) {
                    String prefix = relationPrefixes.poll();
                    List<Member> accessors = relationAccessors.poll();
                    ManagedType<?> relation = model.managedType(attr.getJavaType());
                    if (relation instanceof EntityType && !entityTypeClasses.add(attr.getJavaType()))
                        break;
                    List<String> relAttributeList = relationAttributeNames.get(attr.getJavaType());
                    for (Attribute<?, ?> relAttr : relation.getAttributes()) {
                        String relationAttributeName = relAttr.getName();
                        String fullAttributeName = prefix + '.' + relationAttributeName;
                        List<Member> relAccessors = new LinkedList<>(accessors);
                        relAccessors.add(relAttr.getJavaMember());
                        relAttributeList.add(fullAttributeName);

                        PersistentAttributeType attributeType = relAttr.getPersistentAttributeType();
                        if (PersistentAttributeType.EMBEDDED.equals(attributeType) ||
                            PersistentAttributeType.ONE_TO_ONE.equals(attributeType) ||
                            PersistentAttributeType.MANY_TO_ONE.equals(attributeType)) {
                            relationAttributeNames.put(relAttr.getJavaType(), new ArrayList<>());
                            relationships.add(relAttr);
                            relationPrefixes.add(fullAttributeName);
                            relationAccessors.add(relAccessors);
                        }

                        // Allow the simple attribute name if it doesn't overlap
                        relationAttributeName = relationAttributeName.toLowerCase();
                        attributeNames.putIfAbsent(relationAttributeName, fullAttributeName);

                        // Allow a qualified name such as @OrderBy("address.street.name")
                        relationAttributeName = fullAttributeName.toLowerCase();
                        attributeNames.put(relationAttributeName, fullAttributeName);

                        // Allow a qualified name such as findByAddress_Street_Name if it doesn't overlap
                        String relationAttributeName_ = relationAttributeName.replace('.', '_');
                        attributeNames.putIfAbsent(relationAttributeName_, fullAttributeName);

                        // Allow a qualified name such as findByAddressStreetName if it doesn't overlap
                        String relationAttributeNameUndelimited = relationAttributeName.replace(".", "");
                        attributeNames.putIfAbsent(relationAttributeNameUndelimited, fullAttributeName);

                        attributeAccessors.put(fullAttributeName, relAccessors);

                        attributeTypes.put(fullAttributeName, relAttr.getJavaType());
                        if (relAttr.isCollection()) {
                            if (relAttr instanceof PluralAttribute)
                                collectionElementTypes.put(fullAttributeName, ((PluralAttribute<?, ?, ?>) relAttr).getElementType().getJavaType());
                        } else if (relAttr instanceof SingularAttribute) {
                            SingularAttribute<?, ?> singleAttr = ((SingularAttribute<?, ?>) relAttr);
                            if (singleAttr.isId()) {
                                attributeNames.put("id", fullAttributeName);
                                idType = singleAttr.getJavaType();
                            } else if (singleAttr.isVersion()) {
                                versionAttrName = relationAttributeName_; // to be suitable for query-by-method
                            }
                        }
                    }
                }

                if (!entityType.hasSingleIdAttribute()) {
                    // Per JavaDoc, the above means there is an IdClass.
                    // An EclipseLink extension that allows an Id on an embeddable of an entity
                    // is an exception to this, which we indicate with idClassType null.
                    Type<?> idClassType = getIdType(entityType);
                    if (idClassType != null) {
                        @SuppressWarnings("unchecked")
                        Set<SingularAttribute<?, ?>> idClassAttributes = (Set<SingularAttribute<?, ?>>) (Set<?>) entityType.getIdClassAttributes();
                        if (idClassAttributes != null) {
                            attributeNames.remove("id");
                            idType = idClassType.getJavaType();
                            idClassAttributeAccessors = new TreeMap<>();
                            for (SingularAttribute<?, ?> attr : idClassAttributes) {
                                Member entityMember = attr.getJavaMember();
                                Member idClassMember = entityMember instanceof Field //
                                                ? idType.getField(entityMember.getName()) //
                                                : idType.getMethod(entityMember.getName());
                                idClassAttributeAccessors.put(attr.getName().toLowerCase(), idClassMember);
                            }
                        }
                    }
                }

                Class<?> entityClass = entityType.getJavaType();

                EntityInfo entityInfo = new EntityInfo( //
                                entityType.getName(), //
                                entityClass, //
                                getRecordClass(entityClass), //
                                attributeAccessors, //
                                attributeNames, //
                                attributeTypes, //
                                collectionElementTypes, //
                                relationAttributeNames, //
                                idType, //
                                idClassAttributeAccessors, //
                                versionAttrName, //
                                this);

                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).complete(entityInfo);
            }
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities");
        } catch (RuntimeException x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } catch (Exception x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw new RuntimeException(x);
        } catch (Error x) {
            for (Class<?> entityClass : entities)
                entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture).completeExceptionally(x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "run: define entities", x);
            throw x;
        } finally {
            if (em != null)
                em.close();
        }
    }
}