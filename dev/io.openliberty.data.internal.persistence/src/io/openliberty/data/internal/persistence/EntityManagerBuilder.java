/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;
import static jakarta.data.repository.By.ID;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
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
 */
public abstract class EntityManagerBuilder {
    private static final TraceComponent tc = Tr.register(EntityManagerBuilder.class);

    /**
     * The dataStore value of the Repository annotation,
     * or that value prefixed with java:comp/env,
     * or if unspecified, then java:comp/DefaultDataSource
     */
    protected final String dataStore;

    /**
     * Mapping of entity class (as seen by the user, not a generated record entity class)
     * to entity information.
     */
    protected final ConcurrentHashMap<Class<?>, CompletableFuture<EntityInfo>> entityInfoMap = new ConcurrentHashMap<>();

    /**
     * OSGi service component that provides the CDI extension for Data.
     */
    public final DataProvider provider;

    /**
     * The class loader for repository classes.
     */
    protected final ClassLoader repositoryClassLoader;

    /**
     * The repository interfaces that require the entity manager.
     */
    protected final Set<Class<?>> repositoryInterfaces;

    /**
     * Common constructor for subclasses.
     *
     * @param provider              core OSGi service for our built-in provider
     * @param repositoryClassLoader class loader of repository interfaces
     * @param repositoryInterface   repository interfaces
     * @param dataStore             value derived from the dataStore of the
     *                                  Repository annotation
     */
    @Trivial
    protected EntityManagerBuilder(DataProvider provider,
                                   ClassLoader repositoryClassLoader,
                                   Set<Class<?>> repositoryInterfaces,
                                   String dataStore) {
        this.provider = provider;
        this.repositoryClassLoader = repositoryClassLoader;
        this.repositoryInterfaces = repositoryInterfaces;
        this.dataStore = dataStore;
    }

    /**
     * Invoked by subclass constructors to obtain the EntityInfo for each entity type.
     * After this method completes successfully, the entityInfoMap is populated.
     *
     * @param entityTypes entity classes as known by the user, not generated.
     * @throws Exception if an error occurs.
     */
    // FFDC is not needed because exceptions are logged to Tr.error
    // and also re-thrown upon CompletableFuture<EntityInfo>.get()
    @FFDCIgnore(Throwable.class)
    protected void collectEntityInfo(Set<Class<?>> entityTypes) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        EntityManager em = createEntityManager();
        try {
            Set<Class<?>> missingEntityTypes = new HashSet<>(entityTypes);
            Metamodel model = em.getMetamodel();
            for (EntityType<?> entityType : model.getEntities()) {
                Map<String, String> attributeNames = new HashMap<>();
                Map<String, List<Member>> attributeAccessors = new HashMap<>();
                SortedSet<String> attributeNamesForUpdate = new TreeSet<>();
                SortedMap<String, Class<?>> attributeTypes = new TreeMap<>();
                SortedMap<String, Member> idClassAttributeAccessors = null;
                Map<String, Class<?>> collectionElementTypes = new HashMap<>();
                Map<Class<?>, List<String>> relationAttributeNames = new HashMap<>();
                Queue<Attribute<?, ?>> relationships = new LinkedList<>();
                Queue<String> relationPrefixes = new LinkedList<>();
                Queue<List<Member>> relationAccessors = new LinkedList<>();
                Class<?> recordClass = getRecordClass(entityType.getJavaType());
                Class<?> idType = null;
                String versionAttrName = null;

                Class<?> jpaEntityClass = entityType.getJavaType();
                Class<?> userEntityClass = recordClass == null ? jpaEntityClass : recordClass;
                missingEntityTypes.remove(userEntityClass);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "collecting info for " +
                                       userEntityClass.getName());

                try {
                    for (Attribute<?, ?> attr : entityType.getAttributes()) {
                        String attributeName = attr.getName();
                        PersistentAttributeType attributeType = attr.getPersistentAttributeType();
                        switch (attributeType) {
                            case BASIC:
                            case ELEMENT_COLLECTION:
                                if (attributeNamesForUpdate != null)
                                    attributeNamesForUpdate.add(attributeName);
                                break;
                            case ONE_TO_ONE:
                            case MANY_TO_ONE:
                                attributeNamesForUpdate = null; // must use merge instead
                                // continue
                            case EMBEDDED:
                                relationAttributeNames.put(attr.getJavaType(), new ArrayList<>());
                                relationships.add(attr);
                                relationPrefixes.add(attributeName);
                                relationAccessors.add(Collections.singletonList(attr.getJavaMember()));
                                break;
                            case ONE_TO_MANY:
                            case MANY_TO_MANY:
                                attributeNamesForUpdate = null; // must use merge instead
                                break;
                            default:
                                throw new RuntimeException(); // unreachable unless more types are added
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
                                attributeNames.put(ID, attributeName);
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
                            switch (attributeType) {
                                case BASIC:
                                case ELEMENT_COLLECTION:
                                    if (attributeNamesForUpdate != null)
                                        attributeNamesForUpdate.add(fullAttributeName);
                                    break;
                                case ONE_TO_ONE:
                                case MANY_TO_ONE:
                                    attributeNamesForUpdate = null; // must use merge instead
                                    // continue
                                case EMBEDDED:
                                    relationAttributeNames.put(relAttr.getJavaType(), new ArrayList<>());
                                    relationships.add(relAttr);
                                    relationPrefixes.add(fullAttributeName);
                                    relationAccessors.add(relAccessors);
                                    break;
                                case ONE_TO_MANY:
                                case MANY_TO_MANY:
                                    attributeNamesForUpdate = null; // must use merge instead
                                    break;
                                default:
                                    throw new RuntimeException(); // unreachable unless more types are added
                            }

                            // Allow a qualified name such as @OrderBy("address.street.name")
                            // No chance of conflicts because attributes defined on the entity cannot have a period
                            String fullAttributeNameLower = fullAttributeName.toLowerCase();
                            attributeNames.put(fullAttributeNameLower, fullAttributeName);

                            // Allow a qualified name such as findByAddress_Street_Name if it doesn't overlap
                            // Check for conflicts with attributes defined on the entity to avoid ambiguous queries and sorts
                            String relationAttributeName_ = fullAttributeNameLower.replace('.', '_');
                            String conflictingAttribute = attributeNames.putIfAbsent(relationAttributeName_, fullAttributeName);

                            if (conflictingAttribute != null)
                                throw exc(MappingException.class,
                                          "CWWKD1075.entity.attr.conflict",
                                          userEntityClass.getName(),
                                          fullAttributeName,
                                          conflictingAttribute);

                            // Allow a qualified name such as findByAddressStreetName if it doesn't overlap
                            // Check for conflicts with attributes defined on the entity to avoid ambiguous queries and sorts
                            String relationAttributeNameUndelimited = fullAttributeNameLower.replace(".", "");
                            conflictingAttribute = attributeNames.putIfAbsent(relationAttributeNameUndelimited, fullAttributeName);

                            // TODO need to handle the situation where a field name on the base entity conflicts with the column name
                            // of an embeddable.  See issue: https://github.com/OpenLiberty/open-liberty/issues/29643

                            // Precedence for this valid scenario is defined under
                            // 2.9.2. Scenario 2: Customer Repository with
                            // Resolution that requires a Delimiter
                            if (conflictingAttribute != null && trace && tc.isDebugEnabled())
                                Tr.debug(tc, "overlapping undelimited name " +
                                             relationAttributeNameUndelimited +
                                             " will resolve to basic attribute " +
                                             conflictingAttribute + " instead of " +
                                             fullAttributeName);

                            attributeAccessors.put(fullAttributeName, relAccessors);

                            attributeTypes.put(fullAttributeName, relAttr.getJavaType());
                            if (relAttr.isCollection()) {
                                if (relAttr instanceof PluralAttribute)
                                    collectionElementTypes.put(fullAttributeName, ((PluralAttribute<?, ?, ?>) relAttr).getElementType().getJavaType());
                            } else if (relAttr instanceof SingularAttribute) {
                                SingularAttribute<?, ?> singleAttr = ((SingularAttribute<?, ?>) relAttr);
                                if (singleAttr.isId() && attributeNames.putIfAbsent(ID, fullAttributeName) == null) {
                                    idType = singleAttr.getJavaType();
                                } else if (singleAttr.isVersion()) {
                                    versionAttrName = relationAttributeName_; // to be suitable for query-by-method
                                }
                            }
                        }
                    }

                    String idAttrName = attributeNames.get(ID);

                    if (attributeNamesForUpdate != null) {
                        attributeNamesForUpdate.remove(ID);
                        if (idAttrName != null)
                            attributeNamesForUpdate.remove(idAttrName);
                        if (versionAttrName != null)
                            attributeNamesForUpdate.remove(versionAttrName);
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
                                attributeNames.remove(ID);
                                idType = idClassType.getJavaType();
                                idClassAttributeAccessors = getIdClassAccessors(idType, idClassAttributes);
                            }
                        }
                    }

                    EntityInfo entityInfo = new EntityInfo( //
                                    entityType.getName(), //
                                    jpaEntityClass, //
                                    recordClass, //
                                    attributeAccessors, //
                                    attributeNames, //
                                    attributeNamesForUpdate, //
                                    attributeTypes, //
                                    collectionElementTypes, //
                                    relationAttributeNames, //
                                    idType, //
                                    idClassAttributeAccessors, //
                                    versionAttrName, //
                                    this);

                    entityInfoMap.computeIfAbsent(userEntityClass, EntityInfo::newFuture).complete(entityInfo);
                } catch (Throwable x) { // Ignored FFDC
                    if (!(x instanceof DataException))
                        x = exc(CompletionException.class,
                                "CWWKD1081.entity.general.err",
                                userEntityClass.getName(),
                                getClassNames(repositoryInterfaces),
                                dataStore,
                                x.getMessage()).initCause(x);
                    entityInfoMap.computeIfAbsent(userEntityClass, EntityInfo::newFuture) //
                                    .completeExceptionally(x);
                }
            }

            if (!missingEntityTypes.isEmpty()) {
                DataException x = exc(DataException.class,
                                      "CWWKD1082.entity.classes.missing",
                                      dataStore,
                                      getClassNames(missingEntityTypes),
                                      getClassNames(repositoryInterfaces));
                for (Class<?> ec : missingEntityTypes)
                    entityInfoMap.computeIfAbsent(ec, EntityInfo::newFuture) //
                                    .completeExceptionally(x);
            }
        } finally {
            if (em != null)
                em.close();
        }
    }

    /**
     * Creates a new EntityManager instance.
     *
     * @return a new EntityManager instance.
     */
    public abstract EntityManager createEntityManager();

    /**
     * Returns an alphabetized comma-delimited list of the class names as text
     * that can be used in NLS messages. For example:
     * org.example.MyEntity1, org.example.MyEntity2, org.example.MyEntity3
     *
     * @param classes the list of classes.
     * @return an alphabetized comma-delimited list of the class names as text.
     */
    @Trivial
    public static final String getClassNames(Iterable<Class<?>> classes) {
        TreeSet<String> names = new TreeSet<>();
        for (Class<?> c : classes)
            names.add(c.getName());
        StringBuilder s = new StringBuilder();
        boolean first = true;
        for (String name : names)
            if (first) {
                s.append(name);
                first = false;
            } else {
                s.append(", ").append(name);
            }
        return s.toString();
    }

    /**
     * Obtains the DataSource that is used by the EntityManager.
     * This method is used by resource accessor methods of a repository.
     *
     * @param repoMethod    repository resource accessor method.
     * @param repoInterface repository interface.
     * @return the DataSource that is used by the EntityManager.
     * @throws UnsupportedOperationException if the DataSource cannot be obtained
     *                                           from the EntityManager.
     */
    public abstract DataSource getDataSource(Method repoMethod,
                                             Class<?> repoInterface);

    @FFDCIgnore(NoSuchFieldException.class)
    private static final SortedMap<String, Member> getIdClassAccessors(Class<?> idType,
                                                                       Set<SingularAttribute<?, ?>> idClassAttributes) //
                    throws IntrospectionException, NoSuchFieldException, NoSuchMethodException, SecurityException {
        SortedMap<String, Member> accessors = new TreeMap<>();
        boolean isIdClassRecord = idType.isRecord();
        for (SingularAttribute<?, ?> attr : idClassAttributes) {
            String entityAttrName = attr.getName();
            Member idClassMember = null;;
            if (isIdClassRecord)
                idClassMember = idType.getMethod(entityAttrName);
            else
                try {
                    idClassMember = idType.getField(entityAttrName);
                } catch (NoSuchFieldException x) {
                    for (PropertyDescriptor pd : Introspector.getBeanInfo(idType).getPropertyDescriptors())
                        if (entityAttrName.equals(pd.getName())) {
                            idClassMember = pd.getReadMethod();
                            break;
                        }
                    if (idClassMember == null)
                        throw x;
                }

            accessors.put(entityAttrName.toLowerCase(), idClassMember);
        }
        return accessors;
    }

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
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     */
    public void introspect(PrintWriter writer, String indent) {
        writer.println(indent + toString());
        writer.println(indent + "  dataStore: " + dataStore);
        writer.println(indent + "  repository class loader: " + repositoryClassLoader);
        writer.println(indent + "  repositories:");
        for (Class<?> r : repositoryInterfaces)
            writer.println(indent + "    " + r.getName());
    }
}