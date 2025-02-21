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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource;

import io.openliberty.data.internal.persistence.QueryInfo.Type;
import io.openliberty.data.internal.persistence.cdi.DataExtension;
import io.openliberty.data.internal.persistence.cdi.FutureEMBuilder;
import io.openliberty.data.internal.persistence.service.DBStoreEMBuilder;
import jakarta.data.exceptions.DataConnectionException;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.transaction.Status;

public class RepositoryImpl<R> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final ThreadLocal<Deque<AutoCloseable>> defaultMethodResources = new ThreadLocal<>();

    private final AtomicBoolean isDisposed = new AtomicBoolean();
    final CompletableFuture<EntityInfo> primaryEntityInfoFuture;
    final DataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    final Class<R> repositoryInterface;
    final EntityValidator validator;

    @FFDCIgnore(CompletionException.class)
    public RepositoryImpl(DataProvider provider,
                          DataExtension extension,
                          FutureEMBuilder futureEMBuilder,
                          Class<R> repositoryInterface,
                          Class<?> primaryEntityClass,
                          Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
        EntityManagerBuilder builder;
        try {
            builder = futureEMBuilder.join();
        } catch (CompletionException x) {
            // The CompletionException does not have the current stack. Replace it.
            Throwable cause = x.getCause();
            if (cause != null)
                x = new CompletionException(cause.getMessage(), cause);
            throw x;
        }

        // EntityManagerBuilder implementations guarantee that the future
        // in the following map will be completed even if an error occurs
        this.primaryEntityInfoFuture = primaryEntityClass == null //
                        ? null //
                        : builder.entityInfoMap.computeIfAbsent(primaryEntityClass,
                                                                EntityInfo::newFuture);
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        Object validation = provider.validationService;
        this.validator = validation == null //
                        ? null //
                        : EntityValidator.newInstance(validation, repositoryInterface);

        // reusable instance for supplying this instance to completion stages:
        CompletableFuture<RepositoryImpl<?>> thisCF = queriesPerEntityClass.isEmpty() //
                        ? null //
                        : CompletableFuture.completedFuture(this);

        List<CompletableFuture<EntityInfo>> entityInfoFutures = new ArrayList<>();
        List<QueryInfo> entitylessQueryInfos = null;

        for (Entry<Class<?>, List<QueryInfo>> entry : queriesPerEntityClass.entrySet()) {
            Class<?> entityClass = entry.getKey();

            if (QueryInfo.ENTITY_TBD.equals(entityClass)) {
                entitylessQueryInfos = entry.getValue();
            } else {
                CompletableFuture<EntityInfo> entityInfoFuture = //
                                builder.entityInfoMap.computeIfAbsent(entityClass,
                                                                      EntityInfo::newFuture);
                entityInfoFutures.add(entityInfoFuture);

                for (QueryInfo queryInfo : entry.getValue()) {
                    if (queryInfo.type == QueryInfo.Type.RESOURCE_ACCESS) {
                        queryInfo.validateParams = validator != null &&
                                                   validator.isValidatable(queryInfo.method)[1];
                        queries.put(queryInfo.method,
                                    CompletableFuture.completedFuture(queryInfo));
                    } else {
                        queries.put(queryInfo.method,
                                    entityInfoFuture.thenCombine(thisCF,
                                                                 queryInfo::init));
                    }
                }
            }
        }

        if (entitylessQueryInfos != null) {
            if (entityInfoFutures.isEmpty()) {
                for (QueryInfo queryInfo : entitylessQueryInfos) {
                    MappingException x = exc(MappingException.class,
                                             "CWWKD1001.no.primary.entity",
                                             queryInfo.method,
                                             repositoryInterface.getName(),
                                             "DataRepository<EntityClass, EntityIdClass>");
                    queries.put(queryInfo.method, CompletableFuture.failedFuture(x));
                }
            }

            CompletableFuture<?>[] futures = entityInfoFutures //
                            .toArray(new CompletableFuture<?>[entityInfoFutures.size()]);
            CompletableFuture<Map<String, CompletableFuture<EntityInfo>>> allEntityInfo = //
                            CompletableFuture.allOf(futures) //
                                            .handle((VOID, x) -> x) //
                                            .thenCombine(CompletableFuture.completedFuture(entityInfoFutures),
                                                         this::allEntityInfoAsMap);
            for (QueryInfo queryInfo : entitylessQueryInfos) {
                queries.put(queryInfo.method, allEntityInfo //
                                .thenCombine(CompletableFuture.completedFuture(this),
                                             queryInfo::init));
            }
        }
    }

    /**
     * Constructs a map of entity name to completed EntityInfo future.
     *
     * @param entityInfoFutures completed futures for entity information.
     * @param x                 failure if any.
     * @return map of entity name to completed EntityInfo future.
     */
    private Map<String, CompletableFuture<EntityInfo>> //
                    allEntityInfoAsMap(Throwable x,
                                       List<CompletableFuture<EntityInfo>> futures) {
        Map<String, CompletableFuture<EntityInfo>> entityInfos = new HashMap<>();
        for (CompletableFuture<EntityInfo> future : futures) {
            if (future.isCompletedExceptionally()) {
                entityInfos.putIfAbsent(EntityInfo.FAILED, future);
            } else if (future.isDone()) {
                EntityInfo entityInfo = future.join();
                CompletableFuture<EntityInfo> conflict = //
                                entityInfos.put(entityInfo.name, future);
                if (entityInfo.recordClass != null && conflict == null) {
                    int end = entityInfo.name.length() -
                              EntityInfo.RECORD_ENTITY_SUFFIX.length();
                    String recordName = entityInfo.name.substring(0, end);
                    conflict = entityInfos.put(recordName, future);
                }
                if (conflict != null) {
                    MappingException conflictX;
                    EntityInfo conflictInfo = conflict.join(); // already completed
                    List<String> classNames = List //
                                    .of(entityInfo.getType().getName(),
                                        conflictInfo.getType().getName());
                    if (entityInfo.recordClass == null &&
                        conflictInfo.recordClass == null) {
                        conflictX = exc(MappingException.class,
                                        "CWWKD1068.entity.name.conflict",
                                        repositoryInterface.getName(),
                                        entityInfo.name,
                                        classNames,
                                        List.of(Entity.class.getName(),
                                                Table.class.getName()));
                    } else { // conflict involving one or more record entity
                        String longerName = entityInfo.name;
                        String shorterName = entityInfo.name;
                        if (conflictInfo.name.length() > longerName.length())
                            longerName = conflictInfo.name;
                        else
                            shorterName = conflictInfo.name;

                        conflictX = exc(MappingException.class,
                                        "CWWKD1069.record.entity.name.conflict",
                                        repositoryInterface.getName(),
                                        shorterName,
                                        classNames,
                                        longerName);
                    }
                    entityInfos.putIfAbsent(EntityInfo.FAILED,
                                            CompletableFuture.failedFuture(conflictX));
                }
            } else {
                entityInfos.putIfAbsent(EntityInfo.FAILED,
                                        CompletableFuture.failedFuture(x));
            }
        }
        return entityInfos;
    }

    /**
     * Invoked when the bean for the repository is disposed.
     */
    public void beanDisposed() {
        isDisposed.set(true);
    }

    /**
     * Replaces an exception with a Jakarta Data specification-defined exception,
     * chaining the original exception as the cause.
     * This method replaces all exceptions that are not RuntimeExceptions.
     * For RuntimeExceptions, it only replaces those that are
     * jakarta.persistence.PersistenceException (and subclasses).
     *
     * @param original exception to possibly replace.
     * @return exception to replace with, if any. Otherwise, the original.
     */
    @FFDCIgnore(Exception.class) // secondary error
    @Trivial
    static RuntimeException failure(Exception original, EntityManagerBuilder emb) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        RuntimeException x = null;
        if (original instanceof PersistenceException) {
            for (Throwable cause = original; x == null && cause != null; cause = cause.getCause()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "checking " + cause.getClass().getName() + " with message " + cause.getMessage());

                if (emb instanceof DBStoreEMBuilder && cause instanceof SQLException) { //attempt to have the JDBC layer determine if this is a connection exception
                    try {
                        WSJdbcDataSource ds = (WSJdbcDataSource) emb.getDataSource(null, null);
                        if (ds != null && ds.getDatabaseHelper().isConnectionError((java.sql.SQLException) cause)) {
                            x = new DataConnectionException(original);
                        }
                    } catch (Exception e) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(tc, "Could not obtain DataSource during Exception checking");
                    }
                }
                if (x == null)
                    if (cause instanceof SQLRecoverableException
                        || cause instanceof SQLNonTransientConnectionException
                        || cause instanceof SQLTransientConnectionException)
                        x = new DataConnectionException(original);
                    else if (cause instanceof SQLSyntaxErrorException)
                        x = new MappingException(original);
                    else if (cause instanceof SQLIntegrityConstraintViolationException)
                        x = new EntityExistsException(original);
            }
            if (x == null) {
                if (original instanceof OptimisticLockException)
                    x = new OptimisticLockingFailureException(original);
                else if (original instanceof jakarta.persistence.EntityExistsException)
                    x = new EntityExistsException(original);
                else if (original instanceof NoResultException)
                    x = new EmptyResultException(original);
                else if (original instanceof jakarta.persistence.NonUniqueResultException)
                    x = new NonUniqueResultException(original);
                else
                    x = new DataException(original);
            }
        } else if (original instanceof CompletionException) {
            Throwable cause = original.getCause();
            if (cause == null)
                x = new MappingException(original);
            else if (DataException.class.equals(cause.getClass()))
                x = new DataException(cause.getMessage(), original);
            else if (DataConnectionException.class.equals(cause.getClass()))
                x = new DataConnectionException(cause.getMessage(), original);
            else if (EmptyResultException.class.equals(cause.getClass()))
                x = new EmptyResultException(cause.getMessage(), original);
            else if (MappingException.class.equals(cause.getClass()))
                x = new MappingException(cause.getMessage(), original);
            else if (NonUniqueResultException.class.equals(cause.getClass()))
                x = new NonUniqueResultException(cause.getMessage(), original);
            else if (UnsupportedOperationException.class.equals(cause.getClass()))
                x = new UnsupportedOperationException(cause.getMessage(), original);
            else
                x = new MappingException(original);
        } else if (original instanceof IllegalArgumentException) {
            if (original.getCause() == null) // raised by Liberty
                x = (IllegalArgumentException) original;
            else // raised by Jakarta Persistence provider
                 // Example: Problem compiling [SELECT o FROM Account o WHERE (o.accountId>?1)]. The
                 // relationship mapping 'o.accountId' cannot be used in conjunction with the > operator
                x = new MappingException(original);
        } else if (original instanceof RuntimeException) {
            // Per EclipseLink, "This exception is used for any problem that is detected with a descriptor or mapping"
            if ("org.eclipse.persistence.exceptions.DescriptorException".equals(original.getClass().getName()))
                x = new MappingException(original);
            else
                x = (RuntimeException) original;
        } else {
            x = new DataException(original);
        }

        if (trace && tc.isDebugEnabled())
            if (x == original)
                Tr.debug(tc, "Failure occurred: " + x.getClass().getName());
            else
                Tr.debug(tc, original.getClass().getName() + " replaced with " + x.getClass().getName());
        return x;
    }

    /**
     * Return a name for the parameter, suitable for display in an NLS message.
     *
     * @param param parameter
     * @param index zero-based method index.
     * @return parameter name.
     */
    @Trivial
    private static final String getName(Parameter param, int index) {
        return param.isNamePresent() //
                        ? param.getName() //
                        : ("(" + (index + 1) + ")");
    }

    /**
     * Request an instance of a resource of the specified type.
     *
     * @param method the repository method.
     * @return instance of the resource. Never null.
     * @throws UnsupportedOperationException if the type of resource is not available.
     */
    private <T> T getResource(Method method) {
        Deque<AutoCloseable> resources = defaultMethodResources.get();
        Object resource = null;
        Class<?> type = method.getReturnType();
        if (EntityManager.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder.createEntityManager();
        else if (DataSource.class.equals(type))
            resource = primaryEntityInfoFuture.join().builder //
                            .getDataSource(method, repositoryInterface);
        else if (Connection.class.equals(type))
            try {
                resource = primaryEntityInfoFuture.join().builder //
                                .getDataSource(method, repositoryInterface) //
                                .getConnection();
            } catch (SQLException x) {
                throw new DataConnectionException(x);
            }

        if (resource == null)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1044.invalid.resource.type",
                      method.getName(),
                      repositoryInterface.getName(),
                      type.getName(),
                      List.of(Connection.class.getName(),
                              DataSource.class.getName(),
                              EntityManager.class.getName()));

        if (resource instanceof AutoCloseable) {
            if (resources == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    String thisClassName = getClass().getName();

                    // skip Thread.getStackTrace and RepositoryImpl in stack,
                    int s = 0;
                    while (++s < stack.length &&
                           stack[s].getClassName().equals(thisClassName));
                    StackTraceElement[] shortened = //
                                    new StackTraceElement[stack.length - s];
                    System.arraycopy(stack, s, shortened, 0, shortened.length);

                    Tr.debug(this, tc,
                             type.getSimpleName() + " accessed outside of repository default method",
                             (Object[]) shortened);
                }
            } else {
                resources.add((AutoCloseable) resource);
            }
        }

        @SuppressWarnings("unchecked")
        T t = (T) resource;
        return t;
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CompletableFuture<QueryInfo> queryInfoFuture = queries.get(method);
        boolean isDefaultMethod = false;
        EntityManager em = null;

        if (queryInfoFuture == null)
            if (method.isDefault()) {
                isDefaultMethod = true;
            } else {
                String methodName = method.getName();
                if (args == null) {
                    if ("hashCode".equals(methodName))
                        return System.identityHashCode(proxy);
                    else if ("toString".equals(methodName))
                        return repositoryInterface.getName() + "(Proxy)@" + Integer.toHexString(System.identityHashCode(proxy));
                } else if (args.length == 1) {
                    if ("equals".equals(methodName))
                        return proxy == args[0];
                }
                throw new UnsupportedOperationException(method.toString());
            }

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(),
                     provider.loggable(repositoryInterface, method, args));

        EntityInfo entityInfo = null;

        try {
            if (isDisposed.get())
                throw exc(IllegalStateException.class,
                          "CWWKD1076.repo.disposed",
                          method.getName(),
                          repositoryInterface.getName(),
                          new StringBuilder("RepositoryImpl@") //
                                          .append(Integer.toHexString(hashCode())) //
                                          .append("/(proxy)@") //
                                          .append(Integer.toHexString(System.identityHashCode(proxy))));

            if (isDefaultMethod) {
                Deque<AutoCloseable> resourceStack = defaultMethodResources.get();
                boolean added;
                if (added = (resourceStack == null))
                    defaultMethodResources.set(resourceStack = new LinkedList<>());
                else
                    resourceStack.add(null); // indicator of nested default method
                try {
                    Object returnValue = InvocationHandler.invokeDefault(proxy, method, args);
                    if (trace && tc.isEntryEnabled())
                        Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), returnValue);
                    return returnValue;
                } finally {
                    for (AutoCloseable resource; (resource = resourceStack.pollLast()) != null;)
                        if (!(resource instanceof EntityManager) ||
                            ((EntityManager) resource).isOpen())
                            try {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "close " + resource);
                                resource.close();
                            } catch (Throwable x) {
                                FFDCFilter.processException(x, getClass().getName(), "1827", this);
                            }
                    if (added)
                        defaultMethodResources.remove();
                }
            }

            LocalTransactionCoordinator suspendedLTC = null;

            Object returnValue;
            boolean failed = true;
            Type queryType = null;
            boolean startedTransaction = false;

            try {
                QueryInfo queryInfo = queryInfoFuture.join();
                entityInfo = queryInfo.entityInfo;

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, queryInfo.toString());

                if (queryInfo.validateParams)
                    validator.validateParameters(proxy, method, args);

                if ((queryType = queryInfo.type).requiresTransaction &&
                    Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus()) {
                    suspendedLTC = provider.localTranCurrent.suspend();
                    provider.tranMgr.begin();
                    startedTransaction = true;
                }

                if (queryType != Type.RESOURCE_ACCESS)
                    em = entityInfo.builder.createEntityManager();

                returnValue = switch (queryType) {
                    case FIND, FIND_AND_DELETE -> queryInfo.find(em, args);
                    case COUNT -> queryInfo.count(em, args);
                    case EXISTS -> queryInfo.exists(em, args);
                    case INSERT -> queryInfo.insert(args[0], em);
                    case SAVE -> queryInfo.save(args[0], em);
                    case QM_UPDATE, QM_DELETE -> queryInfo.execute(em, args);
                    case LC_DELETE -> queryInfo.delete(args[0], em);
                    case LC_UPDATE -> queryInfo.update(args[0], em);
                    case LC_UPDATE_RET_ENTITY -> queryInfo.findAndUpdate(args[0], em);
                    case RESOURCE_ACCESS -> getResource(method);
                };

                if (queryInfo.validateResult)
                    validator.validateReturnValue(proxy, method, returnValue);

                failed = false;
            } finally {
                if (em != null)
                    em.close();

                try {
                    if (startedTransaction) {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            provider.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.commit();
                    } else {
                        if (failed && Status.STATUS_ACTIVE == provider.tranMgr.getStatus())
                            provider.tranMgr.setRollbackOnly();
                    }
                } finally {
                    if (suspendedLTC != null)
                        provider.localTranCurrent.resume(suspendedLTC);
                }
            }

            if (trace && tc.isEntryEnabled()) {
                boolean hideValue = queryType == Type.FIND ||
                                    queryType == Type.FIND_AND_DELETE ||
                                    queryType == Type.INSERT ||
                                    queryType == Type.SAVE ||
                                    queryType == Type.LC_UPDATE_RET_ENTITY;
                Object valueToLog = hideValue //
                                ? provider.loggable(repositoryInterface, method, returnValue) //
                                : returnValue;
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(),
                        valueToLog);
            }
            return returnValue;
        } catch (Throwable x) {
            if (!isDefaultMethod && x instanceof Exception)
                x = failure((Exception) x, entityInfo == null ? null : entityInfo.builder);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), x);
            throw x;
        }
    }
}