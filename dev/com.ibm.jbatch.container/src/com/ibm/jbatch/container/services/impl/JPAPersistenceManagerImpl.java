/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.services.impl;

import java.io.Writer;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.jbatch.container.RASConstants;
import com.ibm.jbatch.container.exception.BatchIllegalJobStatusTransitionException;
import com.ibm.jbatch.container.exception.ExecutionAssignedToServerException;
import com.ibm.jbatch.container.exception.JobStoppedException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV2;
import com.ibm.jbatch.container.persistence.jpa.JobExecutionEntityV3;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV2;
import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntityV3;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionEntity;
import com.ibm.jbatch.container.persistence.jpa.RemotablePartitionKey;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadExecutionEntityV2;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.StepThreadInstanceKey;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepExecutionEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceEntity;
import com.ibm.jbatch.container.persistence.jpa.TopLevelStepInstanceKey;
import com.ibm.jbatch.container.services.IJPAQueryHelper;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.util.WSPartitionStepAggregateImpl;
import com.ibm.jbatch.container.util.WSStepThreadExecutionAggregateImpl;
import com.ibm.jbatch.container.validation.IdentifierValidator;
import com.ibm.jbatch.container.ws.BatchLocationService;
import com.ibm.jbatch.container.ws.InstanceState;
import com.ibm.jbatch.container.ws.JobInstanceNotQueuedException;
import com.ibm.jbatch.container.ws.WSPartitionStepAggregate;
import com.ibm.jbatch.container.ws.WSPartitionStepThreadExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionExecution;
import com.ibm.jbatch.container.ws.WSRemotablePartitionState;
//import com.ibm.jbatch.container.ws.WSSearchObject;
import com.ibm.jbatch.container.ws.WSStepThreadExecutionAggregate;
import com.ibm.jbatch.container.ws.impl.WSStartupRecoveryServiceImpl;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.persistence.DDLGenerationParticipant;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

/**
 * Database-backed (via JPA) batch persistence impl.
 *
 * Note that we use global transactions for database access, even reads which don't really need to be coordinated with anything else
 * in any "atomic" manner. The reason for this is that we have execution flows in which multiple
 * database reads/writes are performed in a sequence on a single thread, so a mix of global trans
 * for the insert/updates and non-global trans for the reads would not allow these accesses to all
 * be handled with a single database connection (per WebSphere connection pooling). This substantially
 * complicates the question of "how many connections do I need?" and the related pooling settings needed
 * to avoid deadlock, etc. Since a 2pc transaction involving only a single resource is optimized in WebSphere,
 * there should be only a minimal performance hit to using a global tran in a single resource case such as
 * one in which we just want to read from a single DB.
 *
 * Note: JPAPersistenceManagerImpl is ranked higher than MemoryPeristenceManagerImpl
 * so if they're both activated, JPA should take precedence. Note that all @Reference
 * injectors of IPersistenceManagerService should set the GREEDY option so that they
 * always get injected with JPA over Memory if it's available.
 */
@Component(configurationPid = "com.ibm.ws.jbatch.container.persistence", service = { IPersistenceManagerService.class,
                                                                                     DDLGenerationParticipant.class,
}, configurationPolicy = ConfigurationPolicy.REQUIRE, property = { "service.vendor=IBM",
                                                                   "service.ranking:Integer=20",
                                                                   "persistenceType=JPA" })
public class JPAPersistenceManagerImpl extends AbstractPersistenceManager implements IPersistenceManagerService, DDLGenerationParticipant {

    private final static Logger logger = Logger.getLogger(JPAPersistenceManagerImpl.class.getName(),
                                                          RASConstants.BATCH_MSG_BUNDLE);

    /**
     * For wrapping jpa calls in trans
     */
    private EmbeddableWebSphereTransactionManager tranMgr;

    /**
     * For controlling local transactions
     */
    private LocalTransactionCurrent localTranCurrent;

    /**
     * Persistent store for batch runtime DB.
     */
    private DatabaseStore databaseStore;

    /**
     * config.displayId for the database store configuration element.
     */
    private String databaseStoreDisplayId;

    /**
     * For resolving the batch REST url and serverId of this server.
     */
    private BatchLocationService batchLocationService;

    /**
     * For async operations, such as the initial setup of the JPA datastore
     */
    private ExecutorService executorService;

    /**
     * Persistence service unit. Gets initiated lazily upon first access.
     * For the details on why we chose lazy activation for this, see defect 166203.
     *
     * Note: marked 'volatile' to avoid problems with double-checked locking algorithms
     * (see http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html)
     */
    private volatile PersistenceServiceUnit psu;

    /**
     * Used to cache the result of our job execution table V2 check.
     */
    private Integer executionVersion = null;

    /**
     * Used to cache the result of our job instance table V2 check.
     */
    private Integer instanceVersion = null;

    /**
     * Used to cache the result of our remotable partition table check.
     */
    private Integer partitionVersion = null;

    /**
     * Most current versions of entities.
     */
    private static final int MAX_EXECUTION_VERSION = 3;
    private static final int MAX_INSTANCE_VERSION = 3;
    private static final int MAX_PARTITION_VERSION = 2;

    /**
     * Declarative Services method for setting the Liberty executor.
     *
     * @param svc the service
     */
    @Reference(target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * DS inject
     */
    @Reference(name = "jobStore", target = "(id=unbound)")
    protected void setDatabaseStore(DatabaseStore databaseStore, Map<String, Object> props) {
        this.databaseStore = databaseStore;
        this.databaseStoreDisplayId = (String) props.get("config.displayId");
    }

    /**
     * DS inject
     */
    @Reference
    protected void setTransactionManager(EmbeddableWebSphereTransactionManager svc) {
        tranMgr = svc;
    }

    /**
     * DS inject
     */
    @Reference
    protected void setLocalTransactionCurrent(LocalTransactionCurrent ltc) {
        localTranCurrent = ltc;
    }

    /**
     * DS injection
     */
    @Reference
    protected void setBatchLocationService(BatchLocationService batchLocationService) {
        this.batchLocationService = batchLocationService;
    }

    /**
     * DS activate
     */
    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {
        logger.log(Level.INFO, "persistence.service.status", new Object[] { "JPA", "activated" });
    }

    /**
     * DS deactivate
     */
    @Deactivate
    protected void deactivate() {

        if (psu != null) {
            try {
                psu.close();
            } catch (Exception e) {
                // FFDC.
            }
        }

        logger.log(Level.INFO, "persistence.service.status", new Object[] { "JPA", "deactivated" });
    }

    /* Interface methods */

    @Override
    public void init(IBatchConfig batchConfig) {
    }

    @Override
    public void shutdown() {
    }

    //private static boolean writtenDDL = false;

    /**
     * @return the PersistenceServiceUnit.
     */
    private PersistenceServiceUnit getPsu() {
        if (psu == null) {
            try {
                psu = createPsu();
            } catch (Exception e) {
                throw new BatchRuntimeException("Failed to load JPA PersistenceServiceUnit", e);
            }
        }
        return psu;
    }

    /**
     * Creates a PersistenceServiceUnit using the most recent entities.
     */
    private PersistenceServiceUnit createLatestPsu() throws Exception {
        return createPsu(MAX_INSTANCE_VERSION, MAX_EXECUTION_VERSION, MAX_PARTITION_VERSION);
    }

    /**
     * Creates a PersistenceServiceUnit using the specified entity versions.
     */
    private PersistenceServiceUnit createPsu(int jobInstanceVersion, int jobExecutionVersion, int partitionVersion) throws Exception {
        List<String> entityClasses = new ArrayList<String>(Arrays.asList(
                                                                         StepThreadInstanceEntity.class.getName(),
                                                                         TopLevelStepExecutionEntity.class.getName(),
                                                                         TopLevelStepInstanceEntity.class.getName()));
        if (jobExecutionVersion <= 1) {
            entityClasses.add(JobExecutionEntity.class.getName());
        } else if (jobExecutionVersion == 2) {
            entityClasses.add(JobExecutionEntityV2.class.getName());
        } else if (jobExecutionVersion >= 3) {
            entityClasses.add(JobExecutionEntityV3.class.getName());
        }

        if (jobInstanceVersion <= 1) {
            JobInstanceEntity.class.getName();
        } else if (jobInstanceVersion == 2) {
            entityClasses.add(JobInstanceEntityV2.class.getName());
        } else if (jobInstanceVersion >= 3) {
            entityClasses.add(JobInstanceEntityV3.class.getName());
        }

        if (partitionVersion <= 1) {
            entityClasses.add(StepThreadExecutionEntity.class.getName());
        } else if (partitionVersion >= 2) {
            entityClasses.add(StepThreadExecutionEntityV2.class.getName());
            entityClasses.add(RemotablePartitionEntity.class.getName());
        }

        return databaseStore.createPersistenceServiceUnit(JobInstanceEntity.class.getClassLoader(),
                                                          entityClasses.toArray(new String[0]));
    }

    /**
     * @return create and return the PSU.
     *
     * @throws Exception
     */
    private synchronized PersistenceServiceUnit createPsu() throws Exception {
        if (psu != null) {
            return psu;
        }

        // Load the PSU including the most recent entities.
        PersistenceServiceUnit retMe = createLatestPsu();
        instanceVersion = MAX_INSTANCE_VERSION;
        executionVersion = MAX_EXECUTION_VERSION;
        partitionVersion = MAX_PARTITION_VERSION;

        // If any tables are not up to the current code level, re-load the PSU with backleveled entities.
        setPartitionEntityVersion(retMe);
        if (partitionVersion < 2) {
            logger.fine("The REMOTABLEPARTITION table could not be found. The persistence service unit will exclude the remotable partition entity.");
            retMe.close();
            retMe = createPsu(instanceVersion, executionVersion, partitionVersion);
        }

        setJobInstanceEntityVersion(retMe);
        if (instanceVersion < 3) {
            logger.fine("The GROUPNAMES column could not be found. The persistence service unit will exclude the V3 instance entity.");
            retMe.close();
            retMe = createPsu(instanceVersion, executionVersion, partitionVersion);
        }

        setJobExecutionEntityVersion(retMe);
        if (executionVersion < 2) {
            logger.fine("The JOBPARAMETERS table could not be found. The persistence service unit will exclude the V2 execution entity.");
            retMe.close();
            retMe = createPsu(instanceVersion, executionVersion, partitionVersion);
        }

        // Perform recovery immediately, before returning from this method, so that
        // other callers won't be able to access the PSU (via getPsu()) until recovery is complete.
        WSStartupRecoveryServiceImpl startupRecovery = new WSStartupRecoveryServiceImpl().setIPersistenceManagerService(JPAPersistenceManagerImpl.this).setPersistenceServiceUnit(retMe).recoverLocalJobsInInflightStates();

        // Only do this if the RemotablePartition table exists
        if (partitionVersion >= 2) {
            startupRecovery.recoverLocalPartitionsInInflightStates();
        }

        // Make sure we assign psu before leaving the synchronized block.
        psu = retMe;

        return psu;
    }

    //
    // BEGINNING OF INTERFACE IMPL
    //

    @Override
    public String getDisplayId() {
        // display id will be formatted like: databaseStore[BatchDatabaseStore]
        // get the actual ref name out of there, in this case: BatchDatabaseStore
        Pattern pattern = Pattern.compile(".*\\[(.*)\\]");
        Matcher matcher = pattern.matcher(databaseStoreDisplayId);
        matcher.find();
        return matcher.group(1);
    }

    @Override
    public String getPersistenceType() {
        return "JPA";
    }

    @Override
    public JobInstanceEntity createJobInstance(final String appName, final String jobXMLName, final String submitter, final Date createTime) {

        return this.createJobInstance(appName, jobXMLName, null, submitter, createTime);
    }

    @Override
    public JobInstanceEntity createJobInstance(final String appName, final String jobXMLName, final String jsl, final String submitter, final Date createTime) {

        final EntityManager em = getPsu().createEntityManager();
        try {

            JobInstanceEntity instance = new TranRequest<JobInstanceEntity>(em) {
                @Override
                public JobInstanceEntity call() {
                    JobInstanceEntity jobInstance;
                    if (instanceVersion == 2) {
                        jobInstance = new JobInstanceEntityV2();
                    } else if (instanceVersion >= 3) {
                        jobInstance = new JobInstanceEntityV3();
                    } else {
                        jobInstance = new JobInstanceEntity();
                    }
                    jobInstance.setAmcName(appName);
                    jobInstance.setJobXmlName(jobXMLName);
                    jobInstance.setJobXml(jsl);
                    jobInstance.setSubmitter(submitter);
                    jobInstance.setCreateTime(createTime);
                    jobInstance.setLastUpdatedTime(createTime);
                    jobInstance.setInstanceState(InstanceState.SUBMITTED);
                    jobInstance.setBatchStatus(BatchStatus.STARTING); // Not sure how important the batch status is, the instance state is more important.  I guess we'll set it.
                    entityMgr.persist(jobInstance);
                    return jobInstance;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedJobInstanceIds(instance);
            return instance;

        } finally {
            em.close();
        }
    }

    @Override
    public JobInstanceEntity getJobInstance(final long jobInstanceId) throws NoSuchJobInstanceException {
        final EntityManager em = getPsu().createEntityManager();
        try {
            JobInstanceEntity instance = new TranRequest<JobInstanceEntity>(em) {
                @Override
                public JobInstanceEntity call() {
                    return em.find(JobInstanceEntity.class, jobInstanceId);
                }
            }.runInNewOrExistingGlobalTran();

            if (instance == null) {
                throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
            }
            return instance;
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstanceEntity getJobInstanceFromExecutionId(final long jobExecutionId) throws NoSuchJobExecutionException {
        final EntityManager em = getPsu().createEntityManager();
        try {
            JobExecutionEntity exec = new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() {
                    return em.find(JobExecutionEntity.class, jobExecutionId);
                }
            }.runInNewOrExistingGlobalTran();

            if (exec == null) {
                throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
            }
            return exec.getJobInstance();
        } finally {
            em.close();
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(final String jobName, final int start, final int count) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            List<JobInstanceEntity> exec = new TranRequest<List<JobInstanceEntity>>(em) {

                @Override
                public List<JobInstanceEntity> call() throws Exception {
                    TypedQuery<JobInstanceEntity> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_QUERY,
                                                                              JobInstanceEntity.class);
                    query.setParameter("name", jobName);
                    List<JobInstanceEntity> ids = query.setFirstResult(start).setMaxResults(count).getResultList();
                    if (ids == null) {
                        return new ArrayList<JobInstanceEntity>();
                    }
                    return ids;
                }

            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(final String jobName, final String submitter, final int start, final int count) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            List<JobInstanceEntity> exec = new TranRequest<List<JobInstanceEntity>>(em) {
                @Override
                public List<JobInstanceEntity> call() throws Exception {

                    TypedQuery<JobInstanceEntity> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCES_SORT_CREATETIME_BY_JOBNAME_AND_SUBMITTER_QUERY,
                                                                              JobInstanceEntity.class);
                    query.setParameter("name", jobName);
                    query.setParameter("submitter", submitter);
                    List<JobInstanceEntity> ids = query.setFirstResult(start).setMaxResults(count).getResultList();
                    if (ids == null) {
                        return new ArrayList<JobInstanceEntity>();
                    }
                    return ids;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(final int page, final int pageSize) {
        final ArrayList<JobInstanceEntity> result = new ArrayList<JobInstanceEntity>();
        final EntityManager em = getPsu().createEntityManager();
        try {
            List<JobInstanceEntity> exec = new TranRequest<List<JobInstanceEntity>>(em) {

                @Override
                public List<JobInstanceEntity> call() throws Exception {
                    TypedQuery<JobInstanceEntity> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_ALL_QUERY,
                                                                              JobInstanceEntity.class);
                    final List<JobInstanceEntity> jobList = query.setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList();

                    if (jobList != null) {
                        for (JobInstanceEntity instance : jobList) {
                            result.add(instance);
                        }
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(final IJPAQueryHelper queryHelper, final int page, final int pageSize) {
        final ArrayList<JobInstanceEntity> result = new ArrayList<JobInstanceEntity>();

        final EntityManager em = getPsu().createEntityManager();
        try {
            List<JobInstanceEntity> exec = new TranRequest<List<JobInstanceEntity>>(em) {
                @Override
                public List<JobInstanceEntity> call() throws Exception {
                    // Obtain the JPA query from the Helper
                    String jpaQueryString = queryHelper.getQuery();

                    // Build and populate the parameters of the JPA query
                    TypedQuery<JobInstanceEntity> query = em.createQuery(jpaQueryString, JobInstanceEntity.class);

                    queryHelper.setQueryParameters(query);

                    final List<JobInstanceEntity> jobList = query.setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList();

                    if (jobList != null) {
                        for (JobInstanceEntity instance : jobList) {
                            result.add(instance);
                        }
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public List<JobInstanceEntity> getJobInstances(final int page, final int pageSize, final String submitter) {

        final ArrayList<JobInstanceEntity> result = new ArrayList<JobInstanceEntity>();

        final EntityManager em = getPsu().createEntityManager();
        try {
            List<JobInstanceEntity> exec = new TranRequest<List<JobInstanceEntity>>(em) {
                @Override
                public List<JobInstanceEntity> call() throws Exception {
                    TypedQuery<JobInstanceEntity> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCES_SORT_BY_CREATETIME_FIND_BY_SUBMITTER_QUERY,
                                                                              JobInstanceEntity.class);
                    query.setParameter("submitter", submitter);
                    final List<JobInstanceEntity> jobList = query.setFirstResult(page * pageSize).setMaxResults(pageSize).getResultList();

                    if (jobList != null) {
                        for (JobInstanceEntity instance : jobList) {
                            result.add(instance);
                        }
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public Set<String> getJobNamesSet() {
        final EntityManager em = getPsu().createEntityManager();
        try {
            Set<String> exec = new TranRequest<Set<String>>(em) {
                @Override
                public Set<String> call() throws Exception {
                    TypedQuery<String> query = em.createNamedQuery(JobInstanceEntity.GET_JOB_NAMES_SET_QUERY,
                                                                   String.class);
                    List<String> result = query.getResultList();
                    if (result == null) {
                        return new HashSet<String>();
                    }
                    return cleanUpResult(new HashSet<String>(result));
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public Set<String> getJobNamesSet(final String submitter) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            Set<String> exec = new TranRequest<Set<String>>(em) {
                @Override
                public Set<String> call() throws Exception {
                    TypedQuery<String> query = em.createNamedQuery(JobInstanceEntity.GET_JOB_NAMES_SET_BY_SUBMITTER_QUERY,
                                                                   String.class);
                    query.setParameter("submitter", submitter);
                    List<String> result = query.getResultList();
                    if (result == null) {
                        return new HashSet<String>();
                    }
                    return cleanUpResult(new HashSet<String>(result));
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

    /*
     * Remove any null values.
     */
    private Set<String> cleanUpResult(Set<String> s) {
        s.remove(null);
        return s;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            Integer exec = new TranRequest<Integer>(em) {
                @Override
                public Integer call() throws Exception {
                    TypedQuery<Long> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCE_COUNT_BY_JOBNAME_QUERY,
                                                                 Long.class);
                    query.setParameter("name", jobName);
                    Long result = query.getSingleResult();
                    if (result > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("More than MAX_INTEGER results found.");
                    } else {
                        return result.intValue();
                    }
                }
            }.runInNewOrExistingGlobalTran();

            return exec;

        } finally {
            em.close();
        }
    }

    @Override
    public int getJobInstanceCount(final String jobName, final String submitter) {
        final EntityManager em = getPsu().createEntityManager();
        try {

            Integer exec = new TranRequest<Integer>(em) {

                @Override
                public Integer call() throws Exception {
                    TypedQuery<Long> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCE_COUNT_BY_JOBNAME_AND_SUBMITTER_QUERY,
                                                                 Long.class);
                    query.setParameter("name", jobName);
                    query.setParameter("submitter", submitter);
                    Long result = query.getSingleResult();
                    if (result > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("More than MAX_INTEGER results found.");
                    } else {
                        return result.intValue();
                    }
                }
            }.runInNewOrExistingGlobalTran();

            return exec;

        } finally {
            em.close();
        }
    }

    @Override
    public JobInstanceEntity updateJobInstanceWithInstanceState(final long jobInstanceId, final InstanceState state, final Date lastUpdated) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobInstanceEntity>(em) {
                @Override
                public JobInstanceEntity call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }

                    try {
                        verifyStateTransitionIsValid(instance, state);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    instance.setInstanceState(state);
                    instance.setLastUpdatedTime(lastUpdated);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstance updateJobInstanceOnRestart(final long jobInstanceId, final Date lastUpdated) {
        EntityManager em = getPsu().createEntityManager();
        String BASE_UPDATE = "UPDATE JobInstanceEntity x SET x.instanceState = :instanceState,x.batchStatus = :batchStatus";
        if (instanceVersion >= 2)
            BASE_UPDATE = BASE_UPDATE.replace("JobInstanceEntity", "JobInstanceEntityV2").concat(",x.lastUpdatedTime = :lastUpdatedTime");
        StringBuilder query = new StringBuilder().append(BASE_UPDATE);
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("x.instanceId = :instanceId");
        whereClause.append(" AND x.instanceState IN (com.ibm.jbatch.container.ws.InstanceState.STOPPED,");
        whereClause.append(" com.ibm.jbatch.container.ws.InstanceState.FAILED)");

        query.append(" WHERE " + whereClause);
        final String FINAL_UPDATE = query.toString();

        try {
            return new TranRequest<JobInstance>(em) {
                @Override
                public JobInstance call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }

                    try {
                        verifyStateTransitionIsValid(instance, InstanceState.SUBMITTED);
                        verifyStatusTransitionIsValid(instance, BatchStatus.STARTING);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    Query jpaQuery = entityMgr.createQuery(FINAL_UPDATE);
                    jpaQuery.setParameter("instanceState", InstanceState.SUBMITTED);
                    jpaQuery.setParameter("instanceId", jobInstanceId);
                    if (instanceVersion >= 2)
                        jpaQuery.setParameter("lastUpdatedTime", lastUpdated);
                    jpaQuery.setParameter("batchStatus", BatchStatus.STARTING);

                    int count = jpaQuery.executeUpdate();
                    if (count > 0) {
                        // Need to refresh to pick up changes made to the database
                        entityMgr.refresh(instance);
                    } else {
                        String msg = "The job instance " + jobInstanceId + " cannot be restarted because it is still in a non-final state.";
                        throw new JobRestartException(msg);
                    }
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstanceEntity updateJobInstanceStateOnConsumed(final long jobInstanceId) throws BatchIllegalJobStatusTransitionException, JobInstanceNotQueuedException {
        EntityManager em = getPsu().createEntityManager();
        String BASE_UPDATE = "UPDATE JobInstanceEntity x SET x.instanceState = com.ibm.jbatch.container.ws.InstanceState.JMS_CONSUMED";
        if (instanceVersion >= 2) {
            BASE_UPDATE = BASE_UPDATE.replace("JobInstanceEntity", "JobInstanceEntityV2").concat(",x.lastUpdatedTime = :lastUpdatedTime");
        }
        StringBuilder query = new StringBuilder().append(BASE_UPDATE);
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("x.instanceId = :instanceId");
        whereClause.append(" AND x.instanceState = com.ibm.jbatch.container.ws.InstanceState.JMS_QUEUED");

        query.append(" WHERE " + whereClause);
        final String FINAL_UPDATE = query.toString();

        try {
            return new TranRequest<JobInstanceEntity>(em) {
                @Override
                public JobInstanceEntity call() throws JobInstanceNotQueuedException {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);/* , LockModeType.PESSIMISTIC_WRITE); */
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }

                    Query jpaQuery = entityMgr.createQuery(FINAL_UPDATE);
                    jpaQuery.setParameter("instanceId", jobInstanceId);
                    if (instanceVersion >= 2)
                        jpaQuery.setParameter("lastUpdatedTime", new Date());

                    int count = jpaQuery.executeUpdate();
                    if (count > 0) {
                        logger.finer("Match on updateJobInstanceStateOnConsumed query for instance =  " + jobInstanceId);
                        // Need to refresh to pick up changes made to the database
                        entityMgr.refresh(instance);
                    } else {
                        logger.finer("No match on updateJobInstanceStateOnConsumed query for instance =  " + jobInstanceId);
                        throw new JobInstanceNotQueuedException();
                    }
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstanceEntity updateJobInstanceStateOnQueued(final long jobInstanceId) {
        EntityManager em = getPsu().createEntityManager();
        String BASE_UPDATE = "UPDATE JobInstanceEntity x SET x.instanceState = com.ibm.jbatch.container.ws.InstanceState.JMS_QUEUED";
        if (instanceVersion >= 2) {
            BASE_UPDATE = BASE_UPDATE.replace("JobInstanceEntity", "JobInstanceEntityV2").concat(",x.lastUpdatedTime = :lastUpdatedTime");
        }
        StringBuilder query = new StringBuilder().append(BASE_UPDATE);
        StringBuilder whereClause = new StringBuilder();

        whereClause.append("x.instanceId = :instanceId");
        whereClause.append(" AND x.instanceState = com.ibm.jbatch.container.ws.InstanceState.SUBMITTED");

        query.append(" WHERE " + whereClause);
        final String FINAL_UPDATE = query.toString();

        try {
            return new TranRequest<JobInstanceEntity>(em) {
                @Override
                public JobInstanceEntity call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }

                    try {
                        verifyStateTransitionIsValid(instance, InstanceState.JMS_QUEUED);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    Query jpaQuery = entityMgr.createQuery(FINAL_UPDATE);
                    jpaQuery.setParameter("instanceId", jobInstanceId);
                    if (instanceVersion >= 2)
                        jpaQuery.setParameter("lastUpdatedTime", new Date());

                    int count = jpaQuery.executeUpdate();
                    if (count > 0) {
                        logger.finer("Match on updateJobInstanceStateOnQueued query for instance =  " + jobInstanceId);
                        // Need to refresh to pick up changes made to the database
                        entityMgr.refresh(instance);
                    } else {
                        logger.finer("No match on updateJobInstanceStateOnQueued query for instance =  " + jobInstanceId);
                    }
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstance updateJobInstanceNullOutRestartOn(final long jobInstanceId) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobInstance>(em) {
                @Override
                public JobInstance call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }
                    instance.setRestartOn(null);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstance updateJobInstanceWithRestartOn(final long jobInstanceId, final String restartOn) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobInstance>(em) {
                @Override
                public JobInstance call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }
                    instance.setRestartOn(restartOn);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstance updateJobInstanceWithJobNameAndJSL(final long jobInstanceId, final String jobName, final String jobXml) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobInstance>(em) {
                @Override
                public JobInstance call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }
                    instance.setJobName(jobName);
                    instance.setJobXml(jobXml);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobInstance updateJobInstanceWithInstanceStateAndBatchStatus(final long jobInstanceId, final InstanceState state, final BatchStatus batchStatus,
                                                                        final Date lastUpdated) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobInstance>(em) {
                @Override
                public JobInstance call() {
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }

                    //Thinking a state check will be enough in this case.
                    try {
                        verifyStateTransitionIsValid(instance, state);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    instance.setInstanceState(state);
                    instance.setBatchStatus(batchStatus);
                    instance.setLastUpdatedTime(lastUpdated);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStarted(final long jobExecutionId, final Date startedTime) throws NoSuchJobExecutionException {
        EntityManager em = getPsu().createEntityManager();
        try {
            JobExecution exec = new TranRequest<JobExecution>(em) {
                @Override
                public JobExecution call() {
                    JobExecutionEntity exec = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (exec == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }

                    try {
                        verifyStatusTransitionIsValid(exec, BatchStatus.STARTED);
                        verifyStateTransitionIsValid(exec.getJobInstance(), InstanceState.DISPATCHED);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    exec.setBatchStatus(BatchStatus.STARTED);
                    exec.getJobInstance().setInstanceState(InstanceState.DISPATCHED);
                    exec.getJobInstance().setBatchStatus(BatchStatus.STARTED);
                    exec.getJobInstance().setLastUpdatedTime(startedTime);
                    exec.setStartTime(startedTime);
                    exec.setLastUpdatedTime(startedTime);
                    return exec;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;

        } finally {
            em.close();
        }
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStatusChange(final long jobExecutionId, final BatchStatus newBatchStatus,
                                                                    final Date updateTime) throws NoSuchJobExecutionException {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobExecution>(em) {
                @Override
                public JobExecution call() {
                    JobExecutionEntity exec = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (exec == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }

                    try {
                        verifyStatusTransitionIsValid(exec, newBatchStatus);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    exec.setBatchStatus(newBatchStatus);
                    exec.getJobInstance().setBatchStatus(newBatchStatus);
                    exec.getJobInstance().setLastUpdatedTime(updateTime);
                    exec.setLastUpdatedTime(updateTime);
                    return exec;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnStopBeforeServerAssigned(final long jobExecutionId,
                                                                                final Date updateTime) throws NoSuchJobExecutionException, ExecutionAssignedToServerException {
        final EntityManager em = getPsu().createEntityManager();

        try {
            return new TranRequest<JobExecution>(em) {
                @Override
                public JobExecution call() throws ExecutionAssignedToServerException {
                    final TypedQuery<JobExecutionEntity> query = em.createNamedQuery(JobExecutionEntity.UPDATE_JOB_EXECUTION_AND_INSTANCE_SERVER_NOT_SET,
                                                                                     JobExecutionEntity.class);
                    query.setParameter("batchStatus", BatchStatus.STOPPED);
                    query.setParameter("jobExecId", jobExecutionId);
                    query.setParameter("lastUpdatedTime", updateTime);
                    JobExecutionEntity execution = entityMgr.find(JobExecutionEntity.class, jobExecutionId, LockModeType.PESSIMISTIC_WRITE);
                    if (execution == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }
                    JobInstanceEntity instance = entityMgr.find(JobInstanceEntity.class, execution.getInstanceId());
                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + execution.getInstanceId());
                    }

                    try {
                        verifyStatusTransitionIsValid(execution, BatchStatus.STOPPED);
                        verifyStateTransitionIsValid(instance, InstanceState.STOPPED);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    // Don't want to update the last updated
                    if (instance.getInstanceState() == InstanceState.STOPPED) {
                        logger.finer("Returning since instance = " + instance.getInstanceId() + " is already STOPPED.");
                        return execution;
                    }

                    instance.setBatchStatus(BatchStatus.STOPPED);
                    instance.setInstanceState(InstanceState.STOPPED);
                    instance.setLastUpdatedTime(updateTime);

                    int count = query.executeUpdate();
                    if (count > 0) {
                        // Need to refresh to pick up changes made to the database
                        entityMgr.refresh(execution);
                    } else {
                        String msg = "Job execution " + jobExecutionId + " is in an invalid state";
                        throw new ExecutionAssignedToServerException(msg);
                    }
                    return execution;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecution updateJobExecutionAndInstanceOnEnd(final long jobExecutionId, final BatchStatus finalBatchStatus, final String finalExitStatus,
                                                           final Date endTime) throws NoSuchJobExecutionException {
        return updateJobExecutionAndInstanceFinalStatus(getPsu(), jobExecutionId, finalBatchStatus, finalExitStatus, endTime);
    }

    /**
     * This method is called during recovery, as well as during normal operation.
     *
     * Note this is public but not part of the IPersistenceManagerService interface,
     * since there's no equivalent for in-mem persistence.
     *
     * Set the final batchStatus, exitStatus, and endTime for the given jobExecutionId.
     *
     */
    public JobExecution updateJobExecutionAndInstanceFinalStatus(PersistenceServiceUnit psu,
                                                                 final long jobExecutionId,
                                                                 final BatchStatus finalBatchStatus,
                                                                 final String finalExitStatus,
                                                                 final Date endTime) throws NoSuchJobExecutionException {
        EntityManager em = psu.createEntityManager();
        try {
            return new TranRequest<JobExecution>(em) {
                @Override
                public JobExecution call() {
                    JobExecutionEntity exec = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (exec == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }

                    try {
                        verifyStatusTransitionIsValid(exec, finalBatchStatus);

                        exec.setBatchStatus(finalBatchStatus);
                        exec.getJobInstance().setBatchStatus(finalBatchStatus);
                        exec.setExitStatus(finalExitStatus);
                        exec.getJobInstance().setExitStatus(finalExitStatus);
                        exec.getJobInstance().setLastUpdatedTime(endTime);
                        // set the state to be the same value as the batchstatus
                        // Note: we only want to do this is if the batchStatus is one of the "done" statuses.
                        if (isFinalBatchStatus(finalBatchStatus)) {
                            InstanceState newInstanceState = InstanceState.valueOf(finalBatchStatus.toString());
                            verifyStateTransitionIsValid(exec.getJobInstance(), newInstanceState);
                            exec.getJobInstance().setInstanceState(newInstanceState);
                        }
                        exec.setLastUpdatedTime(endTime);
                        exec.setEndTime(endTime);
                        return exec;
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecutionEntity createJobExecution(final long jobInstanceId, Properties jobParameters, Date createTime) {
        final JobExecutionEntity jobExecution;

        if (executionVersion >= 3) {
            jobExecution = new JobExecutionEntityV3();
        } else if (executionVersion == 2) {
            jobExecution = new JobExecutionEntityV2();
        } else {
            jobExecution = new JobExecutionEntity();
        }

        jobExecution.setCreateTime(createTime);
        jobExecution.setLastUpdatedTime(createTime);
        jobExecution.setBatchStatus(BatchStatus.STARTING);
        jobExecution.setJobParameters(jobParameters);
        jobExecution.setRestUrl(batchLocationService.getBatchRestUrl());

        EntityManager em = getPsu().createEntityManager();
        try {
            new TranRequest<Void>(em) {
                @Override
                public Void call() {
                    JobInstanceEntity jobInstance = entityMgr.find(JobInstanceEntity.class, jobInstanceId);
                    if (jobInstance == null) {
                        throw new IllegalStateException("Didn't find JobInstanceEntity associated with value: " + jobInstanceId);
                    }
                    // The number of executions previously will also conveniently be the index of this, the next execution
                    // (given that numbering starts at 0).
                    int currentNumExecutionsPreviously = jobInstance.getNumberOfExecutions();
                    jobExecution.setExecutionNumberForThisInstance(currentNumExecutionsPreviously);
                    jobInstance.setNumberOfExecutions(currentNumExecutionsPreviously + 1);

                    // Link in each direction
                    jobInstance.getJobExecutions().add(0, jobExecution);
                    jobExecution.setJobInstance(jobInstance);

                    entityMgr.persist(jobExecution);
                    return null;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedJobExecution(jobExecution);

        } finally {
            em.close();
        }
        return jobExecution;
    }

    @Override
    public JobExecutionEntity getJobExecution(final long jobExecutionId) throws NoSuchJobExecutionException {
        final EntityManager em = getPsu().createEntityManager();
        try {
            JobExecutionEntity exec = new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() throws Exception {
                    JobExecutionEntity execution = em.find(JobExecutionEntity.class, jobExecutionId);
                    if (execution == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }
                    return execution;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    /**
     * @param jobInstanceId
     * @return The executions are ordered in sequence, from most-recent to least-recent.
     *         The container keeps its own order and does not depend on execution id or creation time to order these.
     */
    @Override
    public List<JobExecutionEntity> getJobExecutionsFromJobInstanceId(final long jobInstanceId) throws NoSuchJobInstanceException {
        final EntityManager em = getPsu().createEntityManager();
        try {

            List<JobExecutionEntity> exec = new TranRequest<List<JobExecutionEntity>>(em) {

                @Override
                public List<JobExecutionEntity> call() throws Exception {

                    TypedQuery<JobExecutionEntity> query = em.createNamedQuery(JobExecutionEntity.GET_JOB_EXECUTIONS_MOST_TO_LEAST_RECENT_BY_INSTANCE,
                                                                               JobExecutionEntity.class);
                    query.setParameter("instanceId", jobInstanceId);
                    List<JobExecutionEntity> result = query.getResultList();
                    if (result == null || result.size() == 0) {
                        // call this to trigger NoSuchJobInstanceException if instance is completely unknown (as opposed to there being no executions
                        getJobInstance(jobInstanceId);
                        if (result == null) {
                            return new ArrayList<JobExecutionEntity>();
                        }
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    protected List<Long> getJobInstancesRunning(final String jobName) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            List<Long> exec = new TranRequest<List<Long>>(em) {

                @Override
                public List<Long> call() throws Exception {
                    TypedQuery<Long> query = em.createNamedQuery(JobInstanceEntity.GET_JOBINSTANCEIDS_BY_NAME_AND_STATUSES_QUERY,
                                                                 Long.class);
                    query.setParameter("name", jobName);
                    query.setParameter("status", RUNNING_STATUSES);
                    List<Long> result = query.getResultList();
                    if (result == null) {
                        return new ArrayList<Long>();
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;

        } finally {
            em.close();
        }
    }

    @Override
    public List<Long> getJobExecutionsRunning(final String jobName) {

        final List<Long> runningInstances = getJobInstancesRunning(jobName);

        final EntityManager em = getPsu().createEntityManager();
        try {
            List<Long> exec = new TranRequest<List<Long>>(em) {

                @Override
                public List<Long> call() throws Exception {

                    List<Long> result = null;

                    if (runningInstances.size() > 0) {
                        TypedQuery<Long> query = em.createNamedQuery(JobExecutionEntity.GET_JOB_EXECUTIONIDS_BY_JOB_INST_ID,
                                                                     Long.class);
                        query.setParameter("instanceList", runningInstances);

                        result = query.getResultList();
                    }

                    if (result == null) {
                        return new ArrayList<Long>();
                    }

                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;

        } finally {
            em.close();
        }
    }

    /**
     * This method is called during recovery processing.
     *
     * Note: This is not a method on {@link IPersistenceManagerService}, only on the JPA persistence impl
     *
     * @return List<JobExecutionEntity> of jobexecutions with a "running" status and this server's serverId
     */
    public List<JobExecutionEntity> getJobExecutionsRunningLocalToServer(PersistenceServiceUnit psu) {

        final EntityManager em = psu.createEntityManager();
        try {
            List<JobExecutionEntity> exec = new TranRequest<List<JobExecutionEntity>>(em) {
                @Override
                public List<JobExecutionEntity> call() throws Exception {
                    TypedQuery<JobExecutionEntity> query = em.createNamedQuery(JobExecutionEntity.GET_JOB_EXECUTIONS_BY_SERVERID_AND_STATUSES_QUERY,
                                                                               JobExecutionEntity.class);
                    query.setParameter("serverid", batchLocationService.getServerId());
                    query.setParameter("status", RUNNING_STATUSES);
                    List<JobExecutionEntity> result = query.getResultList();
                    if (result == null) {
                        return new ArrayList<JobExecutionEntity>();
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Integer> getRemotablePartitionsRecoveredForStepExecution(final long topLevelStepExecutionId) {
        final EntityManager em = psu.createEntityManager();

        // Just ignore if we don't have the remotable partition table
        if (partitionVersion < 2) {
            return new ArrayList<Integer>();
        }

        try {
            List<Integer> exec = new TranRequest<List<Integer>>(em) {
                @Override
                public List<Integer> call() throws Exception {
                    TypedQuery<Integer> query = em.createNamedQuery(RemotablePartitionEntity.GET_RECOVERED_REMOTABLE_PARTITIONS,
                                                                    Integer.class);
                    query.setParameter("topLevelStepExecutionId", topLevelStepExecutionId);
                    List<Integer> result = query.getResultList();
                    if (result == null) {
                        return new ArrayList<Integer>();
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    /**
     * This method is called during recovery processing.
     *
     * Note: This is not a method on the persistence service, only on the JPA persistence impl
     *
     * @return List<RemotablePartitionEntity> of partitions with a "running" status and this server's serverId
     */
    public List<RemotablePartitionEntity> getRemotablePartitionsRunningLocalToServer(PersistenceServiceUnit psu) {

        final EntityManager em = psu.createEntityManager();

        try {
            List<RemotablePartitionEntity> exec = new TranRequest<List<RemotablePartitionEntity>>(em) {
                @Override
                public List<RemotablePartitionEntity> call() throws Exception {
                    TypedQuery<RemotablePartitionEntity> query = em.createNamedQuery(RemotablePartitionEntity.GET_PARTITION_STEP_THREAD_EXECUTIONIDS_BY_SERVERID_AND_STATUSES_QUERY,
                                                                                     RemotablePartitionEntity.class);
                    query.setParameter("serverid", batchLocationService.getServerId());
                    query.setParameter("status", RUNNING_STATUSES);
                    List<RemotablePartitionEntity> result = query.getResultList();
                    if (result == null) {
                        return new ArrayList<RemotablePartitionEntity>();
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecutionEntity getJobExecutionFromJobExecNum(final long jobInstanceId, final int jobExecNum) throws NoSuchJobInstanceException, IllegalArgumentException {

        final EntityManager em = getPsu().createEntityManager();
        try {
            JobExecutionEntity exec = new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() {
                    final TypedQuery<JobExecutionEntity> query = em.createNamedQuery(
                                                                                     JobExecutionEntity.GET_JOB_EXECUTIONS_BY_JOB_INST_ID_AND_JOB_EXEC_NUM,
                                                                                     JobExecutionEntity.class);
                    query.setParameter("instanceId", jobInstanceId);
                    query.setParameter("jobExecNum", jobExecNum);

                    List<JobExecutionEntity> jobExec = query.getResultList();

                    if (jobExec.size() > 1) {
                        throw new IllegalStateException("Found more than one result for jobInstanceId: " + jobInstanceId + ", jobExecNum: " + jobExecNum);
                    }

                    if (jobExec == null || jobExec.size() == 0) {
                        // call this to trigger NoSuchJobInstanceException if instance is completely unknown (as opposed to there being no executions
                        getJobInstance(jobInstanceId);

                        throw new IllegalArgumentException("Didn't find any job execution entries at job instance id: "
                                                           + jobInstanceId + ", job execution number: "
                                                           + jobExecNum);
                    }

                    return (jobExec.get(0));
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }

    }

    @Override
    public JobExecutionEntity updateJobExecutionLogDir(final long jobExecutionId, final String logDirPath) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() {
                    JobExecutionEntity exec = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (exec == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }
                    exec.setLogpath(logDirPath);
                    return exec;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public JobExecutionEntity updateJobExecutionServerIdAndRestUrlForStartingJob(final long jobExecutionId) throws NoSuchJobExecutionException, JobStoppedException {
        final EntityManager em = getPsu().createEntityManager();

        try {
            return new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() throws JobStoppedException {
                    final TypedQuery<JobExecutionEntity> query = em.createNamedQuery(JobExecutionEntity.UPDATE_JOB_EXECUTION_SERVERID_AND_RESTURL_FOR_STARTING_JOB,
                                                                                     JobExecutionEntity.class);

                    query.setParameter("serverId", batchLocationService.getServerId());
                    query.setParameter("restUrl", batchLocationService.getBatchRestUrl());
                    query.setParameter("jobExecId", jobExecutionId);

                    JobExecutionEntity execution = entityMgr.find(JobExecutionEntity.class, jobExecutionId, LockModeType.PESSIMISTIC_WRITE);
                    if (execution == null) {
                        throw new NoSuchJobExecutionException("No job execution found for id = " + jobExecutionId);
                    }

                    int count = query.executeUpdate();
                    if (count > 0) {
                        // Need to refresh to pick up changes made to the database
                        entityMgr.refresh(execution);
                    } else {
                        // We're guarding here for the case that the execution has been stopped
                        // by the time we reach this query
                        String msg = "No job execution found for id = " + jobExecutionId + " and status = STARTING";
                        throw new JobStoppedException(msg);
                    }
                    return execution;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    /**
     *
     * @param instanceKey
     * @return new step thread execution id
     */
    @Override
    public TopLevelStepExecutionEntity createTopLevelStepExecutionAndNewThreadInstance(final long jobExecutionId, final StepThreadInstanceKey instanceKey,
                                                                                       final boolean isPartitioned) {
        //TODO - should we move this inside the tran?
        EntityManager em = getPsu().createEntityManager();
        try {
            TopLevelStepExecutionEntity stepExecution = new TranRequest<TopLevelStepExecutionEntity>(em) {
                @Override
                public TopLevelStepExecutionEntity call() {

                    // 1. Find related objects
                    final JobInstanceEntity jobInstance = entityMgr.find(JobInstanceEntity.class, instanceKey.getJobInstance());
                    if (jobInstance == null) {
                        throw new IllegalStateException("Didn't find JobInstanceEntity associated with step thread key value: " + instanceKey.getJobInstance());
                    }
                    final JobExecutionEntity jobExecution = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (jobExecution == null) {
                        throw new IllegalStateException("Didn't find JobExecutionEntity associated with value: " + jobExecutionId);
                    }

                    // 2. Construct and initialize new entity instances
                    //   Note some important initialization (e.g. batch status = STARTING and startcount = 1), is done in the constructors
                    final TopLevelStepInstanceEntity stepInstance = new TopLevelStepInstanceEntity(jobInstance, instanceKey.getStepName(), isPartitioned);
                    final TopLevelStepExecutionEntity stepExecution = new TopLevelStepExecutionEntity(jobExecution, instanceKey.getStepName(), isPartitioned);

                    // 3. Update the relationships that didn't get updated in constructors

                    // 3a. Reverting to the safety of known-working, trying to do this drags in some
                    // extra considerations, which we'll have to come back to later (and we can since it
                    // shouldn't affect the table structure).
                    stepInstance.setLatestStepThreadExecution(stepExecution);
//                                      jobInstance.getStepThreadInstances().add(stepInstance);
//                                      jobExecution.getStepThreadExecutions().add(stepExecution);

                    // 4. Persist
                    entityMgr.persist(stepExecution);
                    entityMgr.persist(stepInstance);
                    return stepExecution;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedStepExecution(stepExecution);

            return stepExecution;

        } finally {
            em.close();
        }
    }

    @Override
    public RemotablePartitionEntity createRemotablePartition(final RemotablePartitionKey remotablePartitionKey) {

        // Simply ignore if we don't have the remotable partition table
        if (partitionVersion < 2) {
            return null;
        }

        //TODO - should we move this inside the tran?
        EntityManager em = getPsu().createEntityManager();

        try {
            RemotablePartitionEntity remotablePartition = new TranRequest<RemotablePartitionEntity>(em) {
                @Override
                public RemotablePartitionEntity call() {

                    final JobExecutionEntity jobExecution = entityMgr.find(JobExecutionEntity.class, remotablePartitionKey.getJobExec());
                    if (jobExecution == null) {
                        throw new IllegalArgumentException("Didn't find JobExecutionEntity associated with value: " + remotablePartitionKey.getJobExec());
                    }

                    // 2. Construct and initialize new entity instances
                    final RemotablePartitionEntity remotablePartition = new RemotablePartitionEntity(jobExecution, remotablePartitionKey);
                    remotablePartition.setInternalStatus(WSRemotablePartitionState.QUEUED);
                    remotablePartition.setLastUpdated(new Date());
                    entityMgr.persist(remotablePartition);
                    return remotablePartition;
                }
            }.runInNewOrExistingGlobalTran();

            return remotablePartition;

        } finally {
            em.close();
        }
    }

    @Override
    public StepThreadExecutionEntity createPartitionStepExecutionAndNewThreadInstance(final long jobExecutionId, final StepThreadInstanceKey instanceKey,
                                                                                      final boolean isRemoteDispatch) {
        //TODO - should we move this inside the tran?
        EntityManager em = getPsu().createEntityManager();

        try {
            StepThreadExecutionEntity stepExecution = new TranRequest<StepThreadExecutionEntity>(em) {
                @Override
                public StepThreadExecutionEntity call() {

                    // 1. Find related objects
                    final JobInstanceEntity jobInstance = entityMgr.find(JobInstanceEntity.class, instanceKey.getJobInstance());
                    if (jobInstance == null) {
                        throw new IllegalStateException("Didn't find JobInstanceEntity associated with step thread key value: " + instanceKey.getJobInstance());
                    }
                    final JobExecutionEntity jobExecution = entityMgr.find(JobExecutionEntity.class, jobExecutionId);
                    if (jobExecution == null) {
                        throw new IllegalStateException("Didn't find JobExecutionEntity associated with value: " + jobExecutionId);
                    }
                    TypedQuery<TopLevelStepExecutionEntity> query = entityMgr.createNamedQuery(TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_EXEC_AND_STEP_NAME,
                                                                                               TopLevelStepExecutionEntity.class);
                    query.setParameter("jobExecId", jobExecutionId);
                    query.setParameter("stepName", instanceKey.getStepName());
                    // getSingleResult() validates that there is only one match
                    final TopLevelStepExecutionEntity topLevelStepExecution = query.getSingleResult();

                    // 2. Construct and initalize new entity instances
                    //   Note some important initialization (e.g. batch status = STARTING and startcount = 1), is done in the constructors
                    final StepThreadInstanceEntity stepInstance = new StepThreadInstanceEntity(jobInstance, instanceKey.getStepName(), instanceKey.getPartitionNumber());

                    final StepThreadExecutionEntity stepExecution;
                    if (partitionVersion >= 2) {
                        stepExecution = new StepThreadExecutionEntityV2(jobExecution, instanceKey.getStepName(), instanceKey.getPartitionNumber());
                    } else {
                        stepExecution = new StepThreadExecutionEntity(jobExecution, instanceKey.getStepName(), instanceKey.getPartitionNumber());
                    }

                    // 3. Update the relationships that didn't get updated in constructors

                    // 3a. Reverting to the safety of known-working, trying to do this drags in some
                    // extra considerations, which we'll have to come back to later (and we can since it
                    // shouldn't affect the table structure).
                    stepInstance.setLatestStepThreadExecution(stepExecution);

//                                      jobInstance.getStepThreadInstances().add(stepInstance);
//                                      jobExecution.getStepThreadExecutions().add(stepExecution);
                    stepExecution.setTopLevelStepExecution(topLevelStepExecution);
                    //topLevelStepExecution.getTopLevelAndPartitionStepExecutions().add(stepExecution);

                    RemotablePartitionEntity remotablePartition = null;
                    if (isRemoteDispatch && partitionVersion >= 2) {
                        RemotablePartitionKey remotablePartitionKey = new RemotablePartitionKey(jobExecution.getExecutionId(), instanceKey.getStepName(), instanceKey.getPartitionNumber());
                        remotablePartition = entityMgr.find(RemotablePartitionEntity.class, remotablePartitionKey);

                        //It can be null because if the partition dispatcher is older version, there won't be any remotable partition
                        if (remotablePartition != null) {
                            remotablePartition.setStepExecution(stepExecution);
                            remotablePartition.setRestUrl(batchLocationService.getBatchRestUrl());
                            remotablePartition.setServerId(batchLocationService.getServerId());
                            remotablePartition.setInternalStatus(WSRemotablePartitionState.CONSUMED);
                            remotablePartition.setLastUpdated(new Date());
                        }
                    }

                    // 4. Persist
                    entityMgr.persist(stepInstance);
                    entityMgr.persist(stepExecution);

                    if (isRemoteDispatch && remotablePartition != null) {
                        entityMgr.persist(remotablePartition);
                    }

                    return stepExecution;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedStepExecution(stepExecution);

            return stepExecution;

        } finally {
            em.close();
        }
    }

    /**
     *
     * Needs to:
     *
     * 1. create new stepthreadexec id, in STARTING state 2. increment step
     * instance start count (for top-level only) 3. copy over persistent
     * userdata to new step exec 4. point step thread instance to latest
     * execution
     *
     *
     * @param instanceKey
     * @return new step thread execution
     */
    @Override
    public TopLevelStepExecutionEntity createTopLevelStepExecutionOnRestartFromPreviousStepInstance(final long jobExecutionId,
                                                                                                    final TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException {
        EntityManager em = getPsu().createEntityManager();
        try {
            TopLevelStepExecutionEntity stepExecution = new TranRequest<TopLevelStepExecutionEntity>(em) {
                @Override
                public TopLevelStepExecutionEntity call() {

                    // 1. Find related objects
                    JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);
                    StepThreadExecutionEntity lastStepExecution = stepInstance.getLatestStepThreadExecution();

                    // 2. Construct and initalize new entity instances
                    TopLevelStepExecutionEntity newStepExecution = new TopLevelStepExecutionEntity(newJobExecution, stepInstance.getStepName(), stepInstance.isPartitionedStep());
                    newStepExecution.setPersistentUserDataBytes(lastStepExecution.getPersistentUserDataBytes());
                    stepInstance.incrementStartCount();

                    // 3. Update the relationships that didn't get updated in constructors

                    // 3a. Reverting to the safety of known-working, trying to do this drags in some
                    // extra considerations, which we'll have to come back to later (and we can since it
                    // shouldn't affect the table structure).
                    stepInstance.setLatestStepThreadExecution(newStepExecution);
//                                      newJobExecution.getStepThreadExecutions().add(newStepExecution);

                    // 4. Persist (The order seems to matter unless I did something else wrong)
                    entityMgr.persist(newStepExecution);
                    entityMgr.merge(stepInstance);
                    return newStepExecution;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedStepExecution(stepExecution);

            return stepExecution;

        } finally {
            em.close();
        }
    }

    /**
     *
     * Needs to:
     *
     * 1. create new stepthreadexec id, in STARTING state 2. copy over persistent
     * userdata to new step exec 3. point step thread instance to latest
     * execution
     *
     *
     * @param instanceKey
     * @return new step thread execution
     */
    @Override
    public StepThreadExecutionEntity createPartitionStepExecutionOnRestartFromPreviousStepInstance(final long jobExecutionId, final StepThreadInstanceEntity stepThreadInstance,
                                                                                                   final boolean isRemoteDispatch) throws NoSuchJobExecutionException {
        EntityManager em = getPsu().createEntityManager();
        try {
            StepThreadExecutionEntity stepExecution = new TranRequest<StepThreadExecutionEntity>(em) {
                @Override
                public StepThreadExecutionEntity call() {

                    // 1. Find related objects
                    JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);
                    StepThreadExecutionEntity lastStepExecution = stepThreadInstance.getLatestStepThreadExecution();

                    TypedQuery<TopLevelStepExecutionEntity> query = entityMgr.createNamedQuery(TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_EXEC_AND_STEP_NAME,
                                                                                               TopLevelStepExecutionEntity.class);
                    query.setParameter("jobExecId", jobExecutionId);
                    query.setParameter("stepName", stepThreadInstance.getStepName());
                    // getSingleResult() validates that there is only one match
                    final TopLevelStepExecutionEntity topLevelStepExecution = query.getSingleResult();

                    // 2. Construct and initalize new entity instances
                    StepThreadExecutionEntity newStepExecution;
                    if (partitionVersion >= 2) {
                        newStepExecution = new StepThreadExecutionEntityV2(newJobExecution, stepThreadInstance.getStepName(), stepThreadInstance.getPartitionNumber());
                    } else {
                        newStepExecution = new StepThreadExecutionEntity(newJobExecution, stepThreadInstance.getStepName(), stepThreadInstance.getPartitionNumber());
                    }
                    newStepExecution.setPersistentUserDataBytes(lastStepExecution.getPersistentUserDataBytes());

                    // 3. Update the relationships that didn't get updated in constructors

                    // 3a. Reverting to the safety of known-working, trying to do this drags in some
                    // extra considerations, which we'll have to come back to later (and we can since it
                    // shouldn't affect the table structure).
                    stepThreadInstance.setLatestStepThreadExecution(newStepExecution);
//                                      newJobExecution.getStepThreadExecutions().add(newStepExecution);
                    //topLevelStepExecution.getTopLevelAndPartitionStepExecutions().add(newStepExecution);
                    newStepExecution.setTopLevelStepExecution(topLevelStepExecution);

                    RemotablePartitionEntity remotablePartition = null;
                    if (isRemoteDispatch) {
                        RemotablePartitionKey remotablePartitionKey = new RemotablePartitionKey(newJobExecution.getExecutionId(), stepThreadInstance.getStepName(), stepThreadInstance.getPartitionNumber());
                        remotablePartition = entityMgr.find(RemotablePartitionEntity.class, remotablePartitionKey);

                        //It can be null because if the partition dispatcher is older version, there won't be any remotable partition
                        if (remotablePartition != null) {
                            remotablePartition.setStepExecution(newStepExecution);
                            remotablePartition.setRestUrl(batchLocationService.getBatchRestUrl());
                            remotablePartition.setServerId(batchLocationService.getServerId());
                            remotablePartition.setInternalStatus(WSRemotablePartitionState.CONSUMED);
                            remotablePartition.setLastUpdated(new Date());
                        }
                    }

                    entityMgr.persist(newStepExecution);
                    entityMgr.merge(stepThreadInstance);

                    if (isRemoteDispatch && remotablePartition != null) {
                        entityMgr.persist(remotablePartition);
                    }

                    return newStepExecution;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedStepExecution(stepExecution);

            return stepExecution;

        } finally {
            em.close();
        }
    }

    /**
     *
     * Needs to:
     *
     * 1. create new stepthreadexec id, in STARTING state 2. increment step
     * instance start count (for top-level only, note this isn't the only imaginable spec interpreation..it's our own). You might consider refreshing the count
     * to 0 as well. * 3. don't copy persistent * userdata 4. delete checkpoint data 5. point step thread instance to latest
     * execution
     *
     *
     * @param instanceKey
     * @return new step thread execution id
     */
    @Override
    public TopLevelStepExecutionEntity createTopLevelStepExecutionOnRestartAndCleanStepInstance(final long jobExecutionId,
                                                                                                final TopLevelStepInstanceEntity stepInstance) throws NoSuchJobExecutionException {
        EntityManager em = getPsu().createEntityManager();
        try {
            TopLevelStepExecutionEntity stepExecution = new TranRequest<TopLevelStepExecutionEntity>(em) {
                @Override
                public TopLevelStepExecutionEntity call() {

                    // 1. Find related objects
                    final JobExecutionEntity newJobExecution = getJobExecution(jobExecutionId);

                    // 2. Construct and initalize new entity instances
                    TopLevelStepExecutionEntity newStepExecution = new TopLevelStepExecutionEntity(newJobExecution, stepInstance.getStepName(), stepInstance.isPartitionedStep());
                    stepInstance.incrementStartCount(); // Non-obvious interpretation of the spec
                    stepInstance.deleteCheckpointData();

                    // 3. Update the relationships that didn't get updated in constructors

                    // 3a. Reverting to the safety of known-working, trying to do this drags in some
                    // extra considerations, which we'll have to come back to later (and we can since it
                    // shouldn't affect the table structure).
                    stepInstance.setLatestStepThreadExecution(newStepExecution);
//                                      newJobExecution.getStepThreadExecutions().add(newStepExecution);

                    // 4. Persist (The order seems to matter unless I did something else wrong)
                    entityMgr.persist(newStepExecution);
                    entityMgr.merge(stepInstance);
                    return newStepExecution;
                }
            }.runInNewOrExistingGlobalTran();

            IdentifierValidator.validatePersistedStepExecution(stepExecution);

            return stepExecution;

        } finally {
            em.close();
        }
    }

    /**
     * @return null if not found (don't throw exception)
     */
    @Override
    public StepThreadInstanceEntity getStepThreadInstance(final StepThreadInstanceKey stepInstanceKey) {
        final EntityManager em = getPsu().createEntityManager();
        try {

            StepThreadInstanceEntity exec = new TranRequest<StepThreadInstanceEntity>(em) {
                @Override
                public StepThreadInstanceEntity call() throws Exception {
                    return em.find(StepThreadInstanceEntity.class, stepInstanceKey);
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

    /**
     * TODO - should we validate that this really is a top-level key?
     *
     * @return list of partition numbers related to this top-level step instance
     *         which are in COMPLETED state, in order of increasing partition number.
     */
    @Override
    public List<Integer> getStepThreadInstancePartitionNumbersOfRelatedCompletedPartitions(
                                                                                           final StepThreadInstanceKey topLevelKey) {

        final EntityManager em = getPsu().createEntityManager();

        try {
            List<Integer> exec = new TranRequest<List<Integer>>(em) {
                @Override
                public List<Integer> call() throws Exception {
                    TypedQuery<Integer> query = em.createNamedQuery(TopLevelStepInstanceEntity.GET_RELATED_PARTITION_LEVEL_COMPLETED_PARTITION_NUMBERS,
                                                                    Integer.class);
                    query.setParameter("instanceId", topLevelKey.getJobInstance());
                    query.setParameter("stepName", topLevelKey.getStepName());

                    return query.getResultList();
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }

    }

    @Override
    public StepThreadInstanceEntity updateStepThreadInstanceWithCheckpointData(final StepThreadInstanceEntity stepThreadInstance) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<StepThreadInstanceEntity>(em) {
                @Override
                public StepThreadInstanceEntity call() {
                    entityMgr.merge(stepThreadInstance);
                    return stepThreadInstance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public TopLevelStepInstanceEntity updateStepThreadInstanceWithPartitionPlanSize(final StepThreadInstanceKey stepInstanceKey, final int numCurrentPartitions) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<TopLevelStepInstanceEntity>(em) {
                @Override
                public TopLevelStepInstanceEntity call() {
                    final TopLevelStepInstanceEntity stepInstance = em.find(TopLevelStepInstanceEntity.class, stepInstanceKey);
                    if (stepInstance == null) {
                        throw new IllegalStateException("No step thread instance found for key = " + stepInstanceKey);
                    }
                    stepInstance.setPartitionPlanSize(numCurrentPartitions);
                    return stepInstance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public void deleteStepThreadInstanceOfRelatedPartitions(final TopLevelStepInstanceKey instanceKey) {
        EntityManager em = getPsu().createEntityManager();
        try {
            new TranRequest<Void>(em) {
                @Override
                public Void call() {
                    TypedQuery<StepThreadInstanceEntity> query = entityMgr.createNamedQuery(TopLevelStepInstanceEntity.GET_RELATED_PARTITION_LEVEL_STEP_THREAD_INSTANCES,
                                                                                            StepThreadInstanceEntity.class);
                    query.setParameter("instanceId", instanceKey.getJobInstance());
                    query.setParameter("stepName", instanceKey.getStepName());
                    final List<StepThreadInstanceEntity> relatedPartitionInstances = query.getResultList();

                    for (StepThreadInstanceEntity partitionInstance : relatedPartitionInstances) {
                        entityMgr.remove(partitionInstance);
                    }
                    return null;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public StepThreadExecutionEntity getStepThreadExecution(final long stepExecutionId) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            StepThreadExecutionEntity exec = new TranRequest<StepThreadExecutionEntity>(em) {
                @Override
                public StepThreadExecutionEntity call() throws Exception {
                    return em.find(StepThreadExecutionEntity.class, stepExecutionId);
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }
    }

    /**
     * order by start time, ascending
     */
    @Override
    public List<StepExecution> getStepExecutionsTopLevelFromJobExecutionId(final long jobExecutionId) throws NoSuchJobExecutionException {
        final EntityManager em = getPsu().createEntityManager();
        try {
            List<StepExecution> exec = new TranRequest<List<StepExecution>>(em) {
                @Override
                public List<StepExecution> call() throws Exception {
                    TypedQuery<StepExecution> query = em.createNamedQuery(TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTIONS_BY_JOB_EXEC_SORT_BY_START_TIME_ASC,
                                                                          StepExecution.class);
                    query.setParameter("jobExecId", jobExecutionId);
                    List<StepExecution> result = query.getResultList();
                    if (result == null) {
                        result = new ArrayList<StepExecution>();
                    }
                    // If empty, try to get job execution to generate NoSuchJobExecutionException if unknown id
                    if (result.isEmpty()) {
                        getJobExecution(jobExecutionId);
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionNumberAndStepName(final long jobInstanceId, final int jobExecNum,
                                                                                                     final String stepName) throws NoSuchJobInstanceException, IllegalArgumentException {

        final WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        final EntityManager em = getPsu().createEntityManager();

        try {
            WSStepThreadExecutionAggregateImpl exec = new TranRequest<WSStepThreadExecutionAggregateImpl>(em) {
                @Override
                public WSStepThreadExecutionAggregateImpl call() throws Exception {

                    if (partitionVersion >= 2) {
                        // Remotable partition table exists, so include those in the aggregate
                        Query query = em.createQuery("SELECT s,r FROM StepThreadExecutionEntity s LEFT JOIN RemotablePartitionEntity r ON r.stepExecutionEntity = s "
                                                     + "WHERE (s.jobExec.jobInstance.instanceId = :jobInstanceId AND s.jobExec.executionNumberForThisInstance = :jobExecNum AND s.stepName = :stepName) ORDER BY s.partitionNumber ASC ");
                        query.setParameter("jobInstanceId", jobInstanceId);
                        query.setParameter("jobExecNum", jobExecNum);
                        query.setParameter("stepName", stepName);
                        List<Object[]> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            // Trigger NoSuchJobInstanceException
                            getJobInstance(jobInstanceId);

                            throw new IllegalArgumentException("Didn't find any step thread exec entries at job instance id: " + jobInstanceId + ", job execution number: "
                                                               + jobExecNum
                                                               + ", and stepName: " + stepName);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0)[0];
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at job instance id: " + jobInstanceId + ", job execution number: "
                                                               + jobExecNum
                                                               + ", and stepName: " + stepName);
                        }

                        // Go through the list and store the entities properly
                        List<WSPartitionStepAggregate> partitionSteps = new ArrayList<WSPartitionStepAggregate>(stepExecs.size());
                        List<WSPartitionStepThreadExecution> partitionLevelStepExecutions = new ArrayList<WSPartitionStepThreadExecution>(stepExecs.size());
                        for (int i = 1; i < stepExecs.size(); i++) {
                            WSPartitionStepAggregateImpl partitionStepAggregate = new WSPartitionStepAggregateImpl(stepExecs.get(i));
                            partitionSteps.add(partitionStepAggregate);
                            partitionLevelStepExecutions.add(partitionStepAggregate.getPartitionStepThread());
                        }

                        retVal.setPartitionAggregate(partitionSteps);
                        retVal.setPartitionLevelStepExecutions(partitionLevelStepExecutions);
                    } else {
                        // No remotable partition table, so just get the step thread execs
                        Query query = em.createNamedQuery(TopLevelStepExecutionEntity.GET_TOP_LEVEL_STEP_EXECUTION_BY_JOB_INSTANCE_JOB_EXEC_NUM_AND_STEP_NAME);

                        query.setParameter("jobInstanceId", jobInstanceId);
                        query.setParameter("jobExecNum", jobExecNum);
                        query.setParameter("stepName", stepName);
                        List<StepThreadExecutionEntity> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            // Trigger NoSuchJobInstanceException
                            getJobInstance(jobInstanceId);

                            throw new IllegalArgumentException("Didn't find any step thread exec entries at job instance id: " + jobInstanceId + ", job execution number: "
                                                               + jobExecNum
                                                               + ", and stepName: " + stepName);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0);
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at job instance id: " + jobInstanceId + ", job execution number: "
                                                               + jobExecNum
                                                               + ", and stepName: " + stepName);
                        }

                        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(stepExecs.subList(1, stepExecs.size())));
                    }

                    return retVal;
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

    @Override
    public StepThreadExecutionEntity updateStepExecution(final RuntimeStepExecution runtimeStepExecution) {
        EntityManager em = getPsu().createEntityManager();
        //Create a synchronization object
        TranSynchronization tranSynch = new TranSynchronization(runtimeStepExecution);
        try {
            Transaction tran = tranMgr.getTransaction();
            if (tran != null) {
                UOWCurrent uowCurrent = (UOWCurrent) tranMgr;
                tranMgr.registerSynchronization(uowCurrent.getUOWCoord(), tranSynch, EmbeddableWebSphereTransactionManager.SYNC_TIER_NORMAL);
            }
        } catch (Throwable t) {
            //TODO: nlsprops transform after verify working
            throw new IllegalStateException("TranSync messed up! Sync = " + tranSynch + " Exception: " + t.toString());
        }
        try {
            return new TranRequest<StepThreadExecutionEntity>(em) {
                @Override
                public StepThreadExecutionEntity call() {

                    StepThreadExecutionEntity stepExec = entityMgr.find(StepThreadExecutionEntity.class, runtimeStepExecution.getInternalStepThreadExecutionId());
                    if (stepExec == null) {
                        throw new IllegalStateException("StepThreadExecEntity with id =" + runtimeStepExecution.getInternalStepThreadExecutionId()
                                                        + " should be persisted at this point, but didn't find.");
                    }

                    updateStepExecutionStatusTimeStampsUserDataAndMetrics(stepExec, runtimeStepExecution);
                    return stepExec;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    /**
     * This method is called during recovery
     *
     * Set the lastUpdated for the given RemotablePartitionEntity
     *
     * There should be no need for locking since this happens as the PSU is initialized on component activation
     * (which is already locked within the JVM), and since the target partition is already known to be
     * associated with this very server, (from the query we performed to get the list of partitions to recover).
     */
    public RemotablePartitionEntity updateRemotablePartitionOnRecovery(PersistenceServiceUnit psu,
                                                                       final RemotablePartitionEntity partition) {
        EntityManager em = psu.createEntityManager();
        try {
            return new TranRequest<RemotablePartitionEntity>(em) {
                @Override
                public RemotablePartitionEntity call() {
                    RemotablePartitionKey key = new RemotablePartitionKey(partition);
                    RemotablePartitionEntity remotablePartition = entityMgr.find(RemotablePartitionEntity.class, key);
                    remotablePartition.setInternalStatus(WSRemotablePartitionState.RECOVERED);
                    remotablePartition.setLastUpdated(new Date());
                    return remotablePartition;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    /**
     * This method is called during recovery.
     *
     * Set the batchStatus, exitStatus, and endTime for the given stepExecution.
     */
    public StepThreadExecutionEntity updateStepExecutionOnRecovery(PersistenceServiceUnit psu,
                                                                   final long stepExecutionId,
                                                                   final BatchStatus newStepBatchStatus,
                                                                   final String newStepExitStatus,
                                                                   final Date endTime) throws IllegalArgumentException {

        EntityManager em = psu.createEntityManager();
        try {
            return new TranRequest<StepThreadExecutionEntity>(em) {
                @Override
                public StepThreadExecutionEntity call() {

                    StepThreadExecutionEntity stepExec = entityMgr.find(StepThreadExecutionEntity.class, stepExecutionId);
                    if (stepExec == null) {
                        throw new IllegalArgumentException("StepThreadExecEntity with id =" + stepExecutionId + " should be persisted at this point, but didn't find it.");
                    }

                    try {
                        verifyThreadStatusTransitionIsValid(stepExec, newStepBatchStatus);
                    } catch (BatchIllegalJobStatusTransitionException e) {
                        throw new PersistenceException(e);
                    }

                    stepExec.setBatchStatus(newStepBatchStatus);
                    stepExec.setExitStatus(newStepExitStatus);
                    stepExec.setEndTime(endTime);
                    return stepExec;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public TopLevelStepExecutionEntity updateStepExecutionWithPartitionAggregate(final RuntimeStepExecution runtimeStepExecution) {
        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<TopLevelStepExecutionEntity>(em) {
                @Override
                public TopLevelStepExecutionEntity call() {
                    TopLevelStepExecutionEntity stepExec = entityMgr.find(TopLevelStepExecutionEntity.class, runtimeStepExecution.getInternalStepThreadExecutionId());
                    if (stepExec == null) {
                        throw new IllegalArgumentException("StepThreadExecEntity with id =" + runtimeStepExecution.getInternalStepThreadExecutionId()
                                                           + " should be persisted at this point, but didn't find.");
                    }
                    updateStepExecutionStatusTimeStampsUserDataAndMetrics(stepExec, runtimeStepExecution);
                    for (StepThreadExecutionEntity stepThreadExec : stepExec.getTopLevelAndPartitionStepExecutions()) {
                        // Exclude the one top-level entry, which shows up in this list, based on its type.
                        if (!(stepThreadExec instanceof TopLevelStepExecutionEntity)) {
                            stepExec.addMetrics(stepThreadExec);
                        }
                    }
                    return stepExec;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregateFromJobExecutionId(
                                                                                      final long jobExecutionId, final String stepName) throws NoSuchJobExecutionException {
        final WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        final EntityManager em = getPsu().createEntityManager();

        try {
            WSStepThreadExecutionAggregate exec = new TranRequest<WSStepThreadExecutionAggregate>(em) {
                @Override
                public WSStepThreadExecutionAggregate call() throws Exception {

                    if (partitionVersion >= 2) {
                        // Remotable partition table exists, so include those in the aggregate
                        Query query = em.createQuery("SELECT s,r FROM StepThreadExecutionEntity s LEFT JOIN RemotablePartitionEntity r ON r.stepExecutionEntity = s "
                                                     + "WHERE s.jobExec.jobExecId = :jobExecId AND s.stepName = :stepName  ORDER BY s.partitionNumber ASC");
                        query.setParameter("jobExecId", jobExecutionId);
                        query.setParameter("stepName", stepName);
                        List<Object[]> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            throw new IllegalArgumentException("Didn't find any step thread exec entries at job execution id: " + jobExecutionId + ", and stepName: " + stepName);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0)[0];
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at job execution id: " + jobExecutionId + ", and stepName: "
                                                               + stepName);
                        }

                        // Go through the list and store the entities properly
                        List<WSPartitionStepAggregate> partitionSteps = new ArrayList<WSPartitionStepAggregate>();
                        List<WSPartitionStepThreadExecution> partitionLevelStepExecutions = new ArrayList<WSPartitionStepThreadExecution>();
                        for (int i = 1; i < stepExecs.size(); i++) {
                            WSPartitionStepAggregateImpl partitionStepAggregate = new WSPartitionStepAggregateImpl(stepExecs.get(i));
                            partitionSteps.add(partitionStepAggregate);
                            partitionLevelStepExecutions.add(partitionStepAggregate.getPartitionStepThread());
                        }

                        retVal.setPartitionAggregate(partitionSteps);
                        retVal.setPartitionLevelStepExecutions(partitionLevelStepExecutions);
                    } else {
                        // No remotable partition table, so just get the step thread execs
                        Query query = em.createNamedQuery(TopLevelStepExecutionEntity.GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_BY_JOB_EXEC_AND_STEP_NAME_SORT_BY_PART_NUM_ASC);

                        query.setParameter("jobExecId", jobExecutionId);
                        query.setParameter("stepName", stepName);
                        List<StepThreadExecutionEntity> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            throw new IllegalArgumentException("Didn't find any step thread exec entries at job execution id: " + jobExecutionId + ", and stepName: " + stepName);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0);
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at job execution id: " + jobExecutionId + ", and stepName: "
                                                               + stepName);
                        }

                        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(stepExecs.subList(1, stepExecs.size())));
                    }

                    return retVal;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }

    }

    @Override
    public WSStepThreadExecutionAggregate getStepExecutionAggregate(final long topLevelStepExecutionId) throws IllegalArgumentException {

        final WSStepThreadExecutionAggregateImpl retVal = new WSStepThreadExecutionAggregateImpl();

        final EntityManager em = getPsu().createEntityManager();

        try {
            WSStepThreadExecutionAggregate exec = new TranRequest<WSStepThreadExecutionAggregate>(em) {
                @Override
                public WSStepThreadExecutionAggregate call() throws Exception {

                    if (partitionVersion >= 2) {
                        // Remotable partition table exists, so include those in the aggregate
                        Query query = em.createQuery("SELECT s,r FROM StepThreadExecutionEntity s LEFT JOIN RemotablePartitionEntity r ON r.stepExecutionEntity = s "
                                                     + "WHERE s.topLevelStepExecution.stepExecutionId = :topLevelStepExecutionId ORDER BY s.partitionNumber ASC");
                        query.setParameter("topLevelStepExecutionId", topLevelStepExecutionId);
                        List<Object[]> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            throw new IllegalArgumentException("Didn't find any step thread exec entries at id: " + topLevelStepExecutionId);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0)[0];
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at id: " + topLevelStepExecutionId, e);
                        }

                        // Go through the list and store the entities properly
                        List<WSPartitionStepAggregate> partitionSteps = new ArrayList<WSPartitionStepAggregate>(stepExecs.size());
                        List<WSPartitionStepThreadExecution> partitionLevelStepExecutions = new ArrayList<WSPartitionStepThreadExecution>(stepExecs.size());
                        for (int i = 1; i < stepExecs.size(); i++) {
                            WSPartitionStepAggregateImpl partitionStepAggregate = new WSPartitionStepAggregateImpl(stepExecs.get(i));
                            partitionSteps.add(partitionStepAggregate);
                            partitionLevelStepExecutions.add(partitionStepAggregate.getPartitionStepThread());
                        }

                        retVal.setPartitionAggregate(partitionSteps);
                        retVal.setPartitionLevelStepExecutions(partitionLevelStepExecutions);

                    } else {
                        // No remotable partition table, so just get the step thread execs
                        Query query = em.createNamedQuery(TopLevelStepExecutionEntity.GET_ALL_RELATED_STEP_THREAD_EXECUTIONS_SORT_BY_PART_NUM_ASC);
                        query.setParameter("topLevelStepExecutionId", topLevelStepExecutionId);
                        List<StepThreadExecutionEntity> stepExecs = query.getResultList();

                        if (stepExecs == null || stepExecs.size() == 0) {
                            throw new IllegalArgumentException("Didn't find any step thread exec entries at id: " + topLevelStepExecutionId);
                        }

                        // Verify the first is the top-level.
                        try {
                            TopLevelStepExecutionEntity topLevelStepExecution = (TopLevelStepExecutionEntity) stepExecs.get(0);
                            retVal.setTopLevelStepExecution(topLevelStepExecution);
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("Didn't find top-level step thread exec entry at id: " + topLevelStepExecutionId, e);
                        }

                        retVal.setPartitionLevelStepExecutions(new ArrayList<WSPartitionStepThreadExecution>(stepExecs.subList(1, stepExecs.size())));
                    }

                    return retVal;
                }
            }.runInNewOrExistingGlobalTran();

            return exec;
        } finally {
            em.close();
        }

    }

    /**
     * This method is called during recovery.
     *
     * Note this method is not on the persistence interface, it is particular to the JPA persistence impl.
     *
     * Unlike other methods involving a list of the spec-defined instance and execution ids, this returns
     * a list sorted from low-to-high stepexecution id (rather than using a timestamp based ordering which
     * preserves order across a wraparound of ids when using a SEQUENCE).
     *
     * @return The list of StepExecutions with "running" statuses for the given jobExecutionId.
     */
    public List<StepExecution> getStepThreadExecutionsRunning(PersistenceServiceUnit psu, final long jobExecutionId) {

        final EntityManager em = psu.createEntityManager();
        try {

            List<StepExecution> exec = new TranRequest<List<StepExecution>>(em) {
                @Override
                public List<StepExecution> call() throws Exception {

                    TypedQuery<StepExecution> query = em.createNamedQuery(StepThreadExecutionEntity.GET_STEP_THREAD_EXECUTIONIDS_BY_JOB_EXEC_AND_STATUSES_QUERY,
                                                                          StepExecution.class);
                    query.setParameter("jobExecutionId", jobExecutionId);
                    query.setParameter("status", RUNNING_STATUSES);
                    List<StepExecution> result = query.getResultList();
                    if (result == null) {
                        return new ArrayList<StepExecution>();
                    }
                    return result;
                }
            }.runInNewOrExistingGlobalTran();
            return exec;
        } finally {
            em.close();
        }
    }

////////////////////////////////////////////////////////////////////
// Disable the split-flow and partition execution methods for now.
////////////////////////////////////////////////////////////////////

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createSplitFlowExecution(com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey)
     */
//      @Override
//      public RemotableSplitFlowEntity createSplitFlowExecution(final RemotableSplitFlowKey splitFlowKey, final Date createTime) {
//
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      return new TranRequest<RemotableSplitFlowEntity>(em){
//                              public RemotableSplitFlowEntity call() {
//                                      final RemotableSplitFlowEntity splitFlow = new RemotableSplitFlowEntity();
//                                      splitFlow.setInternalStatus(BatchStatus.STARTING.ordinal());
//                                      splitFlow.setFlowName(splitFlowKey.getFlowName());
//                                      splitFlow.setCreateTime(createTime);
//                                      splitFlow.setServerId(batchLocationService.getServerId());
//                                      splitFlow.setRestUrl(batchLocationService.getBatchRestUrl());
//                                      JobExecutionEntity jobExec = entityMgr.find(JobExecutionEntity.class, splitFlowKey.getJobExec());
//                                      if (jobExec == null) {
//                                              throw new IllegalStateException("Didn't find JobExecutionEntity associated with value: " + splitFlowKey.getJobExec());
//                                      }
//                                      splitFlow.setJobExecution(jobExec);
//                                      jobExec.getSplitFlowExecutions().add(splitFlow);
//                                      entityMgr.persist(splitFlow);
//                                      return splitFlow;
//                              }
//                      }.runInNewOrExistingGlobalTran();
//              } finally {
//                      em.close();
//              }
//      }
//
//      @Override
//      public RemotableSplitFlowEntity updateSplitFlowExecution(final RuntimeSplitFlowExecution runtimeSplitFlowExecution, final BatchStatus newBatchStatus, final Date date)
//              throws IllegalArgumentException {
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      return new TranRequest<RemotableSplitFlowEntity>(em){
//                              public RemotableSplitFlowEntity call() {
//                                      RemotableSplitFlowKey splitFlowKey = new RemotableSplitFlowKey(runtimeSplitFlowExecution.getTopLevelExecutionId(), runtimeSplitFlowExecution.getFlowName());
//                                      RemotableSplitFlowEntity splitFlowEntity = entityMgr.find(RemotableSplitFlowEntity.class, splitFlowKey);
//                                      if (splitFlowEntity == null) {
//                                              throw new IllegalArgumentException("No split flow execution found for key = " + splitFlowKey);
//                                      }
//                                      splitFlowEntity.setBatchStatus(newBatchStatus);
//                                      splitFlowEntity.setExitStatus(runtimeSplitFlowExecution.getExitStatus());
//                                      ExecutionStatus executionStatus = runtimeSplitFlowExecution.getFlowStatus();
//                                      if (executionStatus != null) {
//                                              splitFlowEntity.setInternalStatus(executionStatus.getExtendedBatchStatus().ordinal());
//                                      }
//                                      if (newBatchStatus.equals(BatchStatus.STARTED)) {
//                                              splitFlowEntity.setStartTime(date);
//                                      } else if (FINAL_STATUS_SET.contains(newBatchStatus)) {
//                                              splitFlowEntity.setEndTime(date);
//                                      }
//                                      return splitFlowEntity;
//                              }
//                      }.runInNewOrExistingGlobalTran();
//              } finally {
//                      em.close();
//              }
//      }
//
//      @Override
//      public RemotableSplitFlowEntity updateSplitFlowExecutionLogDir(final RemotableSplitFlowKey key, final String logDirPath) {
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      return new TranRequest<RemotableSplitFlowEntity>(em){
//                              public RemotableSplitFlowEntity call() {
//                                      RemotableSplitFlowEntity splitFlowEntity = entityMgr.find(RemotableSplitFlowEntity.class, key);
//                                      if (splitFlowEntity == null) {
//                                              throw new IllegalArgumentException("No split flow execution found for key = " + key);
//                                      }
//                                      splitFlowEntity.setLogpath(logDirPath);
//                                      return splitFlowEntity;
//                              }
//                      }.runInNewOrExistingGlobalTran();
//              } finally {
//                      em.close();
//              }
//      }
//
//      @Override
//      public RemotablePartitionEntity createPartitionExecution(final RemotablePartitionKey partitionKey, final Date createTime) {
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      return new TranRequest<RemotablePartitionEntity>(em){
//                              public RemotablePartitionEntity call() {
//                                      final RemotablePartitionEntity partition = new RemotablePartitionEntity();
//                                      partition.setStepName(partitionKey.getStepName());
//                                      partition.setPartitionNumber(partitionKey.getPartitionNumber());
//                                      partition.setInternalStatus(BatchStatus.STARTING.ordinal());
//                                      partition.setCreateTime(createTime);
//                                      partition.setServerId(batchLocationService.getServerId());
//                                      partition.setRestUrl(batchLocationService.getBatchRestUrl());
//                                      JobExecutionEntity jobExec = entityMgr.find(JobExecutionEntity.class, partitionKey.getJobExec());
//                                      if (jobExec == null) {
//                                              throw new IllegalStateException("Didn't find JobExecutionEntity associated with value: " + partitionKey.getJobExec());
//                                      }
//                                      partition.setJobExec(jobExec);
//                                      jobExec.getPartitionExecutions().add(partition);
//                                      entityMgr.persist(partition);
//                                      return partition;
//                              }
//                      }.runInNewOrExistingGlobalTran();
//              } finally {
//                      em.close();
//              }
//      }
//
//      @Override
//      public RemotablePartitionEntity updatePartitionExecution(final RuntimePartitionExecution runtimePartitionExecution, final BatchStatus newBatchStatus, final Date date) {
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      return new TranRequest<RemotablePartitionEntity>(em){
//                              public RemotablePartitionEntity call() {
//                                      RemotablePartitionKey partitionKey = new RemotablePartitionKey(runtimePartitionExecution.getTopLevelExecutionId(),
//                                                      runtimePartitionExecution.getStepName(), runtimePartitionExecution.getPartitionNumber());
//                                      RemotablePartitionEntity partitionEntity = entityMgr.find(RemotablePartitionEntity.class, partitionKey);
//                                      if (partitionEntity == null) {
//                                              throw new IllegalArgumentException("No partition execution found for key = " + partitionKey);
//                                      }
//                                      partitionEntity.setBatchStatus(newBatchStatus);
//                                      partitionEntity.setExitStatus(runtimePartitionExecution.getExitStatus());
//                                      partitionEntity.setInternalStatus(runtimePartitionExecution.getBatchStatus().ordinal());
//                                      if (newBatchStatus.equals(BatchStatus.STARTED)) {
//                                              partitionEntity.setStartTime(date);
//                                      } else if (FINAL_STATUS_SET.contains(newBatchStatus)) {
//                                              partitionEntity.setEndTime(date);
//                                      }
//                                      return partitionEntity;
//                              }
//                      }.runInNewOrExistingGlobalTran();
//              } finally {
//                      em.close();
//              }
//
//      }
//
    @Override
    public RemotablePartitionEntity updateRemotablePartitionLogDir(final RemotablePartitionKey key, final String logDirPath) {

        // Simply ignore if we don't have the remotable partition table
        if (partitionVersion < 2) {
            return null;
        }

        EntityManager em = getPsu().createEntityManager();
        try {
            return new TranRequest<RemotablePartitionEntity>(em) {
                @Override
                public RemotablePartitionEntity call() {
                    RemotablePartitionEntity partitionEntity = entityMgr.find(RemotablePartitionEntity.class, key);
                    if (partitionEntity == null) {
                        return null;
                        //throw new IllegalArgumentException("No partition execution found for key = " + key);
                    }
                    partitionEntity.setLogpath(logDirPath);
                    return partitionEntity;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }

    }

    @Override
    public void purgeInGlassfish(String submitter) {
        // TODO unused
    }

    @Override
    public boolean purgeJobInstanceAndRelatedData(final long jobInstanceId) {
        final EntityManager em = getPsu().createEntityManager();
        try {
            new TranRequest<Void>(em) {
                @Override
                public Void call() {
                    final JobInstanceEntity instance;

                    instance = em.find(JobInstanceEntity.class, jobInstanceId);

                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceId);
                    }
                    entityMgr.remove(instance);
                    return null;
                }
            }.runInNewOrExistingGlobalTran();

            return true;
        } finally {
            em.close();
        }
    }

//
//
//
//
//      @Override
//      public List<StepThreadExecutionEntity> getStepThreadExecutionsForJobExecutionUnsorted(long execid) throws NoSuchJobExecutionException {
//              ArrayList<StepThreadExecutionEntity> resultSteps = new ArrayList<StepThreadExecutionEntity>();
//              EntityManager em = getPsu().createEntityManager();
//              try {
//                      JobExecutionEntity exec = em.find(JobExecutionEntity.class, execid);
//                      if (exec == null) {
//                              throw new NoSuchJobExecutionException("No job execution found for id = " + execid);
//                      }
//                      for (StepThreadExecutionEntity stepExec : exec.getTopLevelAndPartitionStepExecutions()) {
//                              resultSteps.add(stepExec);
//                      }
//                      return resultSteps;
//              } finally {
//                      em.close();
//              }
//      }

// probably can do this with one query now
//    @Override
//    public List<StepExecution> getStepExecutionsPartitionsForJobExecution(long execid, String stepName)
//            throws NoSuchJobExecutionException {
//
//        EntityManager em = getPsu().createEntityManager();
//        ArrayList<StepExecution> resultSteps = new ArrayList<StepExecution>();
//
//        try {
//
//            long jobInstanceId = getJobInstanceIdFromExecutionId(execid);
//
//            TypedQuery<JobInstanceEntity> query = em
//                    .createQuery(
//                            "SELECT i FROM JobInstanceEntity i WHERE i.jobName LIKE :names ORDER BY i.jobInstanceId",
//                            JobInstanceEntity.class);
//            //query.setParameter("names", buildPartitionLevelQuery(jobInstanceId, stepName));
//
//            List<JobInstanceEntity> instances = query.getResultList();
//
//            if (instances != null) {
//                for (JobInstanceEntity instance : instances) {
//                    if (instance.getJobExecutions().size() > 1)
//                        throw new IllegalStateException("Subjob has more than one execution.");
//                    for (JobExecutionEntity exec : instance.getJobExecutions()) {
//                        for (StepThreadExecutionEntity step : exec.getStepThreadExecutions()) {
//                            resultSteps.add(step);
//                        }
//                    }
//                }
//            }
//
//            return resultSteps;
//        } finally {
//            em.close();
//        }
//
//    }

    /**
     * Inner class for wrapping EntityManager persistence functions with transactions, if needed. Used
     * with handleRetry to manage rollbacks.
     */
    private abstract class TranRequest<T> {

        EntityManager entityMgr;
        boolean newTran = false;
        private LocalTransactionCoordinator suspendedLTC;

        public TranRequest(EntityManager em) {
            entityMgr = em;
        }

        public T runInNewOrExistingGlobalTran() {

            T retVal = null;

            try {
                beginOrJoinTran();

                /*
                 * Here is the part where we call to the individual method, just
                 * wanted to make this stand out a bit more visually with all the tran & exc handling.
                 */
                retVal = call();

            } catch (Throwable t) {
                rollbackIfNewTranWasStarted(t);
            }

            commitIfNewTranWasStarted();

            return retVal;
        }

        public abstract T call() throws Exception;

        /**
         * Begin a new transaction, if one isn't currently active (nested transactions not supported).
         */
        protected void beginOrJoinTran() throws SystemException, NotSupportedException {

            int tranStatus = tranMgr.getStatus();

            if (tranStatus == Status.STATUS_NO_TRANSACTION) {
                logger.fine("Suspending current LTC and beginning new transaction");
                suspendedLTC = localTranCurrent.suspend();
                tranMgr.begin();
                newTran = true;
            } else {
                if (tranMgr.getTransaction() == null) {
                    throw new IllegalStateException("Didn't find active transaction but tranStatus = " + tranStatus);
                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Joining existing tran: " + tranMgr.getTransaction());
                    }
                }
            }

            entityMgr.joinTransaction();
        }

        protected void commitIfNewTranWasStarted() {
            if (newTran) {
                logger.fine("Committing new transaction we started.");
                try {
                    tranMgr.commit();
                } catch (Throwable t) {
                    throw new PersistenceException(t);
                } finally {
                    try {
                        resumeAnyExistingLTC();
                    } catch (Throwable t) {
                        throw new PersistenceException("Caught throwable on resume of previous LTC.  Might mask earlier throwable, so check logs.", t);
                    }
                }
            } else {
                logger.fine("Exiting without committing previously-active transaction.");
            }
        }

        protected void rollbackIfNewTranWasStarted(Throwable caughtThrowable) throws PersistenceException {
            if (newTran) {
                logger.fine("Rollback new transaction we started.");
                try {
                    tranMgr.rollback();
                } catch (Throwable t1) {
                    logger.fine("Tried to rollback, caught a new exception, throwing new PersistenceException with this new exception chained");
                    throw new PersistenceException("Caught throwable on rollback after previous throwable: " + caughtThrowable, t1);
                } finally {
                    try {
                        resumeAnyExistingLTC();
                    } catch (Throwable t2) {
                        logger.fine("Tried to resume LTC, caught a new exception, throwing new PersistenceException with this new exception chained");
                        throw new PersistenceException("Caught throwable on resume of previous LTC.  Original throwable: " + caughtThrowable, t2);
                    }
                }
            } else {
                logger.fine("We didn't start a new transaction so simply let the exception get thrown back.");
            }

            // If we haven't gotten a new exception to chain, throw back the original one passed in as a parameter
            logger.fine("No exception encountered, throwing new PersistenceException with original exception chained");
            throw new PersistenceException(caughtThrowable);
        }

        @Trivial
        protected void resumeAnyExistingLTC() {
            logger.fine("Will resume any LTC");
            if (suspendedLTC != null) {
                localTranCurrent.resume(suspendedLTC);
                logger.fine("LTC resumed");
            }
        }

    };

    @Override
    public void generate(Writer out) throws Exception {
        PersistenceServiceUnit ddlGen = createLatestPsu();
        ddlGen.generateDDL(out);
        ddlGen.close();
    }

    @Override
    public String getDDLFileName() {
        /*
         * Because the batchContainer configuration is a singleton we
         * can just use the config displayID as the DDL Filename. The file extension
         * will be added automatically for us.
         */
        return databaseStoreDisplayId + "_batchPersistence";
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     **/
    @Override
    public Integer getJobExecutionEntityVersionField() {
        return executionVersion;
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     **/
    @Override
    public int getJobExecutionEntityVersion() throws Exception {
        if (executionVersion != null) {
            setJobExecutionEntityVersion(getPsu());
        }
        return executionVersion;
    }

    @FFDCIgnore(PersistenceException.class)
    private void setJobExecutionEntityVersion(PersistenceServiceUnit psu) throws Exception {

        // If we have the RemotablePartition table, we're up to date and need the V3 execution entity
        if (partitionVersion == 2) {
            executionVersion = 3;
            return;
        }

        final EntityManager em = psu.createEntityManager();
        try {

            Integer exec = new TranRequest<Integer>(em) {
                @Override
                public Integer call() throws Exception {
                    // Verify that JOBPARAMETER exists by running a query against it.
                    String queryString = "SELECT COUNT(e.jobParameterElements) FROM JobExecutionEntityV2 e";
                    TypedQuery<Long> query = em.createQuery(queryString, Long.class);
                    query.getSingleResult();
                    logger.fine("The JOBPARAMETER table exists, job execution entity version = 2");
                    executionVersion = 2;
                    return executionVersion;
                }
            }.runInNewOrExistingGlobalTran();

        } catch (PersistenceException e) {
            logger.fine("Looking for JobExecutionEntityV2 table support, caught a persistence exception");
            Throwable cause = e.getCause();
            while (cause != null) {
                final String causeMsg = cause.getMessage();
                final String causeClassName = cause.getClass().getCanonicalName();
                logger.fine("Next chained JobExecutionEntityV2 persistence exception: exc class = " + causeClassName + "; causeMsg = " + causeMsg);
                if ((cause instanceof SQLSyntaxErrorException || causeClassName.contains("SqlSyntaxErrorException")) &&
                    causeMsg != null &&
                    (causeMsg.contains("JOBPARAMETER") || causeMsg.contains("ORA-00942"))) {
                    // The table isn't there.
                    logger.fine("The JOBPARAMETER table does not exist, job execution entity version = 1");
                    executionVersion = 1;
                    return;
                }
                cause = cause.getCause();
            }
            logger.fine("Unexpected exception while checking for JobExecutionEntityV2 entity version, re-throwing");
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     **/
    @Override
    public Integer getJobInstanceEntityVersionField() {
        return instanceVersion;
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     **/
    @Override
    public int getJobInstanceEntityVersion() throws Exception {
        if (instanceVersion == null) {
            setJobInstanceEntityVersion(getPsu());
        }
        return instanceVersion;
    }

    @FFDCIgnore(PersistenceException.class)
    private void setJobInstanceEntityVersion(PersistenceServiceUnit psu) throws Exception {

        final EntityManager em = psu.createEntityManager();

        try {

            // Check for the presence of v2 table support (presence of UPDATETIME column)
            try {
                Integer exec = new TranRequest<Integer>(em) {
                    @Override
                    public Integer call() throws Exception {
                        String queryString = "SELECT COUNT(x.lastUpdatedTime) FROM JobInstanceEntityV2 x";
                        TypedQuery<Long> query = em.createQuery(queryString, Long.class);
                        query.getSingleResult();
                        logger.fine("The UPDATETIME column exists, job instance entity version = 2");
                        instanceVersion = 2;
                        return instanceVersion;
                    }
                }.runInNewOrExistingGlobalTran();
            } catch (PersistenceException pe) {
                logger.fine("Looking for JobInstanceEntityV2 table support, caught a persistence exception");
                Throwable cause = pe.getCause();
                while (cause != null) {
                    final String causeMsg = cause.getMessage();
                    final String causeClassName = cause.getClass().getCanonicalName();
                    logger.fine("Next chained JobInstanceEntityV2 persistence exception: exc class = " + causeClassName + "; causeMsg = " + causeMsg);
                    if ((cause instanceof SQLSyntaxErrorException || causeClassName.contains("SqlSyntaxErrorException")) &&
                        causeMsg != null &&
                        (causeMsg.contains("UPDATETIME") || causeMsg.contains("ORA-00942"))) {
                        // The UPDATETIME column isn't there.
                        logger.fine("The UPDATETIME column does not exist, job instance entity version = 1");
                        instanceVersion = 1;
                        return;
                    }
                    cause = cause.getCause();
                }

                if (instanceVersion == null) {
                    // We did not determine an instance version
                    logger.fine("Unexpected exception while checking for JobInstanceEntityV2 entity version, re-throwing");
                    throw pe;
                }
            }

            // Now try for V3
            try {
                Integer exec = new TranRequest<Integer>(em) {
                    @Override
                    public Integer call() throws Exception {
                        // Verify that groupNames column exists by running a query against it.
                        String queryString = "SELECT COUNT(x.groupNames) FROM JobInstanceEntityV3 x";
                        TypedQuery<Long> query = em.createQuery(queryString, Long.class);
                        query.getSingleResult();
                        logger.fine("The GROUPASSOCIATION table exists, job instance entity version = 3");
                        instanceVersion = 3;
                        return instanceVersion;
                    }
                }.runInNewOrExistingGlobalTran();

            } catch (PersistenceException pe) {
                logger.fine("Looking for JobInstanceEntityV3 table support, caught a persistence exception");
                Throwable cause = pe.getCause();
                while (cause != null) {
                    final String causeMsg = cause.getMessage();
                    final String causeClassName = cause.getClass().getCanonicalName();
                    logger.fine("Next chained JobInstanceEntityV3 persistence exception: exc class = " + causeClassName + "; causeMsg = " + causeMsg);
                    if ((cause instanceof SQLSyntaxErrorException || causeClassName.contains("SqlSyntaxErrorException")) &&
                        causeMsg != null &&
                        (causeMsg.contains("GROUPASSOCIATION") || causeMsg.contains("GROUPNAMES") || causeMsg.contains("ORA-00942"))) {
                        // The GROUPASSOCIATION support isn't there.
                        logger.fine("The GROUPASSOCIATION table does not exist, job instance entity version = 2");
                        instanceVersion = 2;
                        return;
                    }
                    cause = cause.getCause();
                }

                logger.fine("Unexpected exception while checking for JobInstanceEntityV3 entity version, re-throwing");
                throw pe;
            }
        } finally {
            logger.fine("determined the job instance entity version: " + instanceVersion);
            em.close();
        }
    }

    @Override
    public JobInstanceEntity updateJobInstanceWithGroupNames(final long jobInstanceID, final Set<String> groupNames) {

        EntityManager em = getPsu().createEntityManager();

        try {
            return new TranRequest<JobInstanceEntityV3>(em) {
                @Override
                public JobInstanceEntityV3 call() {

                    JobInstanceEntityV3 instance = entityMgr.find(JobInstanceEntityV3.class, jobInstanceID);

                    if (instance == null) {
                        throw new NoSuchJobInstanceException("No job instance found for id = " + jobInstanceID);
                    }

                    instance.setGroupNames(groupNames);

                    entityMgr.merge(instance);
                    return instance;
                }
            }.runInNewOrExistingGlobalTran();
        } finally {
            em.close();
        }
    }

    @FFDCIgnore(PersistenceException.class)
    private void setPartitionEntityVersion(PersistenceServiceUnit psu) throws Exception {

        final EntityManager em = psu.createEntityManager();
        try {

            Integer exec = new TranRequest<Integer>(em) {
                @Override
                public Integer call() throws Exception {
                    // Verify that REMOTABLEPARTITION exists by running a query against it.
                    String queryString = "SELECT COUNT(e) FROM RemotablePartitionEntity e";
                    TypedQuery<Long> query = em.createQuery(queryString, Long.class);
                    query.getSingleResult();
                    logger.fine("The REMOTABLEPARTITION table exists, partition entity version = 2");
                    partitionVersion = 2;
                    return partitionVersion;
                }
            }.runInNewOrExistingGlobalTran();

        } catch (PersistenceException e) {
            logger.fine("Looking for RemotablePartition table support, caught a persistence exception");
            Throwable cause = e.getCause();
            while (cause != null) {
                final String causeMsg = cause.getMessage();
                final String causeClassName = cause.getClass().getCanonicalName();
                logger.fine("Next chained RemotablePartition persistence exception: exc class = " + causeClassName + "; causeMsg = " + causeMsg);
                if ((cause instanceof SQLSyntaxErrorException || causeClassName.contains("SqlSyntaxErrorException")
			|| causeClassName.contains("SQLServerException")) &&
                    causeMsg != null &&
                    (causeMsg.contains("REMOTABLEPARTITION") || causeMsg.contains("ORA-00942"))) {
                    // The table isn't there.
                    logger.fine("The REMOTABLEPARTITION table does not exist, partition entity version = 1");
                    partitionVersion = 1;
                    // We need to set this here otherwise we'll try to load execution V3 without remotable partition and blow up
                    executionVersion = 2;
                    return;
                }
                cause = cause.getCause();
            }
            logger.fine("Unexpected exception while checking for RemotablePartition entity version, re-throwing");
            throw e;
        } finally {
            em.close();
        }
    }

    // Step thread exec version is tied to partition version with RemotablePartition update, may have to decouple in the future
    @Override
    public Integer getStepThreadExecutionEntityVersionField() {
        return partitionVersion;
    }

    /** {@inheritDoc} */
    @Override
    public WSRemotablePartitionState getRemotablePartitionInternalState(final RemotablePartitionKey remotablePartitionKey) {

        // Simply ignore if we don't have the remotable partition table
        if (partitionVersion < 2) {
            return null;
        }

        final EntityManager em = getPsu().createEntityManager();
        try {
            RemotablePartitionEntity rp = new TranRequest<RemotablePartitionEntity>(em) {
                @Override
                public RemotablePartitionEntity call() throws Exception {
                    RemotablePartitionEntity rp = em.find(RemotablePartitionEntity.class, remotablePartitionKey);
                    if (rp == null) {
                        logger.finer("No RemotablePartition found for key = " + remotablePartitionKey
                                     + ", maybe because it was dispatched from a JVM configured to use the older table/entity versions.");
                        // This can be null because if the partition dispatcher uses an older version, and so never created the remotable partition
                        return null;
                    }
                    return rp;
                }
            }.runInNewOrExistingGlobalTran();

            return rp != null ? rp.getInternalStatus() : null;

        } finally {
            em.close();
        }
    }

    @Override
    public List<WSRemotablePartitionExecution> getRemotablePartitionsForJobExecution(final long jobExecutionId) {
        if (partitionVersion < 2) {
            return null;
        }

        final EntityManager em = getPsu().createEntityManager();
        try {
            JobExecutionEntity exec = new TranRequest<JobExecutionEntity>(em) {
                @Override
                public JobExecutionEntity call() {
                    JobExecutionEntity je = em.find(JobExecutionEntityV3.class, jobExecutionId);
                    if (je == null) {
                        logger.finer("No job execution found with execution id = " + jobExecutionId);
                        return null;
                    }
                    return je;
                }
            }.runInNewOrExistingGlobalTran();

            return new ArrayList<WSRemotablePartitionExecution>(exec.getRemotablePartitions());
        } finally {
            em.close();
        }
    }

}
