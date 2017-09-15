/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.security.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.ROLES;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class WSBatchAuthServiceImpl implements WSBatchAuthService {

    public static final String BATCH_AUTH_ID = "com.ibm.ws.batch";


    private final static Logger logger = Logger.getLogger(WSBatchAuthServiceImpl.class.getCanonicalName(),
                                                          "com.ibm.ws.jbatch.security.resources.BatchSecurityMessages" );

    private IPersistenceManagerService persistenceManagerService ;

    private BatchSecurityHelper batchSecurityHelper = null;

    /**
     * Ref name for SecurityService.
     */
    private static final String SecurityServiceReferenceName = "securityService";

    /**
     * Security Service, from which we obtain the correct AuthorizationService.
     * Note: multiple AuthorizationServices may be loaded; SecurityService sorts out
     * which is the correct to use.
     */
    protected final AtomicServiceReference<SecurityService> securityServiceRef =
        new AtomicServiceReference<SecurityService>(SecurityServiceReferenceName);

    /**
     * DS inject.
     */
    @Reference(name = SecurityServiceReferenceName,
               service = SecurityService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.setReference(reference);
    }

    /**
     * DS un-inject
     */
    protected void unsetSecurityService(ServiceReference<SecurityService> reference) {
        securityServiceRef.unsetReference(reference);
    }

    /**
     * DS activate
     */
    @Activate
    protected void activate(ComponentContext cc) {
        securityServiceRef.activate(cc);
        logger.log(Level.INFO, "BATCH_SECURITY_ENABLED");
    }

    /**
     * DS deactivate
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        logger.log(Level.INFO, "BATCH_SECURITY_DISABLED");
        securityServiceRef.deactivate(cc);       
    }    

    /**
     * @return the Authz service; or null if no authz service is available.
     */
    protected AuthorizationService getAuthorizationService() {
        SecurityService securityService = securityServiceRef.getService();
        return (securityService != null) ? securityService.getAuthorizationService() : null;
    }


    @Reference
    protected void setBatchSecurityHelper(BatchSecurityHelper batchSecurityHelper) {
        this.batchSecurityHelper = batchSecurityHelper;
    }

    @Reference( policyOption = ReferencePolicyOption.GREEDY )
    protected void setIPersistenceManagerService(IPersistenceManagerService pms) {
        this.persistenceManagerService = pms;
    }

    /**
     * DS un-setter.
     */
    protected void unsetBatchSecurityHelper(BatchSecurityHelper batchSecurityHelper) {
        if (this.batchSecurityHelper == batchSecurityHelper) {
            this.batchSecurityHelper = null;
        }
    }

    /**
     * DS un-setter.
     */
    protected void unsetIPersistenceManagerService(IPersistenceManagerService pms) {
        if (this.persistenceManagerService == pms) {
            this.persistenceManagerService = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long authorizedInstanceRead(long instanceId) 
        throws JobSecurityException, NoSuchJobInstanceException {

        String submitter = persistenceManagerService.getJobInstanceSubmitter(instanceId);

        if (this.isAdmin(runAsSubject())) {
        } else if (this.isMonitor(runAsSubject())) {
        } else if (this.isSubmitter(runAsSubject())) {
            //if you don't also own the job you can't view it
            if (!batchSecurityHelper.getRunAsUser().equals(submitter)){
                throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_JOB_INSTANCE",
                                                                     new Object[] {getRunAsUser(), instanceId},
                                                                     "CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}.") );
            }
        } else {
            throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_NO_BATCH_ROLES",
                                                                 new Object[] { getRunAsUser() },
                                                                 "CWWKY0303W: User {0} is not authorized to perform any batch operations." ) );
        }

        return instanceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long authorizedExecutionRead(long executionId) 
        throws JobSecurityException, NoSuchJobExecutionException {

        long instanceId = persistenceManagerService.getJobInstanceIdFromExecutionId(executionId);
        authorizedInstanceRead(instanceId);
        return executionId;
    }

    /**
     * @return Current implementation will create the userId of the Subject currently on the thread.
     */
    @Override
    public String getRunAsUser() {
        return batchSecurityHelper.getRunAsUser();
    }


    @Override
    public void authorizedJobSubmission() throws JobSecurityException {
        if (this.isAdmin(runAsSubject())) {
        } else if (this.isSubmitter(runAsSubject())) {
        } else {
            throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_TO_START_JOB",
                                                                 new Object[] { getRunAsUser() }, 
                                                                 "CWWKY0304W: User {0} is not authorized to start batch jobs." ) );
        }
    }

    @Override
    public long authorizedJobRestartByExecution(long executionId)
        throws NoSuchJobExecutionException,
                          JobSecurityException {

                   return authorizedJobStopRestartByExecution(executionId);
    }

    @Override
    public long authorizedJobRestartByInstance(long instanceId)
        throws NoSuchJobExecutionException,
                          JobSecurityException {

                   return authorizedJobStopRestartByInstance(instanceId);
    }

    @Override
    public long authorizedJobStopByExecution(long executionId)
        throws NoSuchJobExecutionException,
                          JobSecurityException {

                   return authorizedJobStopRestartByExecution(executionId);
    }
    
    @Override
    public long authorizedJobStopByInstance(long instanceId)
        throws JobSecurityException {

                   return authorizedJobStopRestartByInstance(instanceId);
    }

    private long authorizedJobStopRestartByExecution(long executionId) 
               throws NoSuchJobExecutionException, JobSecurityException {

        long instanceId = persistenceManagerService.getJobInstanceIdFromExecutionId(executionId);
        String submitter = persistenceManagerService.getJobInstanceSubmitter(instanceId);
        
        authorizedJobStopRestart(submitter, instanceId);

        return executionId;
    }

    private long authorizedJobStopRestartByInstance(long instanceId) 
            throws JobSecurityException {

    	String submitter = persistenceManagerService.getJobInstanceSubmitter(instanceId);
    	
    	authorizedJobStopRestart(submitter, instanceId);

    	return instanceId;
    }
    
    private void authorizedJobStopRestart(String submitter, long instanceId) 
            throws NoSuchJobExecutionException, JobSecurityException {
    	
    	if (this.isAdmin(runAsSubject())) {
    		logger.finer("Current user " + this.getRunAsUser()+ " is admin, so always authorized");
    	} else if (this.isSubmitter(runAsSubject())) {
    		//if you don't also own the job you can't stop or restart it
    		if (!batchSecurityHelper.getRunAsUser().equals(submitter)){
    			throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_JOB_INSTANCE",
    																new Object[] {getRunAsUser(), instanceId},
    																"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}.") );
    		}
    	}else if (this.isMonitor(runAsSubject())) {
    		throw new JobSecurityException("Current user " + this.getRunAsUser()+ " with role batch_monitor is not authorized to stop or restart jobs.");
    	} else {
    		throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_NO_BATCH_ROLES",
    															new Object[] { getRunAsUser() },
    															"CWWKY0303W: User {0} is not authorized to perform any batch operations." ) );
    	}

    	
    }

    
    @Override
    public long authorizedJobPurgeByInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
        return authorizedJobPurgeAbandonByInstance(instanceId);
    }

    @Override
    public long authorizedJobAbandonByInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {

        return authorizedJobPurgeAbandonByInstance(instanceId);
    }

    private long authorizedJobPurgeAbandonByInstance(long instanceId) throws NoSuchJobInstanceException, JobSecurityException {
    	String submitter = persistenceManagerService.getJobInstanceSubmitter(instanceId);

    	boolean unAuthFlag = false;
    	if(isInAnyBatchRole()){
    		if (this.isAdmin()) {
    			logger.finer("Current user " + this.getRunAsUser()+ " is admin, so always authorized");
    		} 
    		else if (getRunAsUser().equals(submitter)) { 
    			if (!this.isSubmitter()) {
    				unAuthFlag = true;
    			} 
    		}
    		else {
    			unAuthFlag = true;
    		}
    		if(unAuthFlag){
    			throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_JOB_INSTANCE",
    					new Object[] {getRunAsUser(), instanceId},
    					"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}.") );
    		}

    		return instanceId;

    	}else {
    		throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_NO_BATCH_ROLES",
    				new Object[] { getRunAsUser() },
    				"CWWKY0303W: User {0} is not authorized to perform any batch operations." ) );

    	}

    }

    @Override
    public boolean isAdmin() {
        return this.isAdmin(runAsSubject());
    }

    @Override
    public boolean isSubmitter() {
        return this.isSubmitter(runAsSubject());
    }

    @Override
    public boolean isMonitor() {
        return this.isMonitor(runAsSubject());
    }

    @Override
    public boolean isInAnyBatchRole() {
        return this.isInAnyBatchRole(runAsSubject());
    }

    @Override
    public boolean isAuthorizedInstanceRead(long instanceId)
        throws NoSuchJobInstanceException {
        String submitter = persistenceManagerService.getJobInstanceSubmitter(instanceId);

        if (this.isAdmin(runAsSubject())) {
        } else if (this.isMonitor(runAsSubject())) {
        } else if (batchSecurityHelper.getRunAsUser().equals(submitter)) { 
            if (!this.isInAnyBatchRole(runAsSubject())) {
                //We could just check for && isSubmittter, because we already checked for admin or monitor,
                //but logically as long as you are in any batch role you can view your own jobs
                return false;
            }
        } else {
            logger.finer("Current user " + this.getRunAsUser()+ " does not match the tag of record");
            return false;
        }

        return true;
    }

    @Override
    public boolean isAuthorizedExecutionRead(long executionId) throws NoSuchJobExecutionException	 {

        long instanceId = persistenceManagerService.getJobInstanceIdFromExecutionId(executionId);
        return isAuthorizedInstanceRead(instanceId);

    }

    @Override
    public long authorizedStepExecutionRead(long stepExecutionId) {
        // No idea if this is really inefficient, OK, or great.
        long instanceId = persistenceManagerService.getStepExecutionTopLevel(stepExecutionId).getJobExecution().getJobInstance().getInstanceId();
        authorizedInstanceRead(instanceId);
        return stepExecutionId;
    }

    private Subject runAsSubject() {
        return batchSecurityHelper.getRunAsSubject();
    }

    /**
     * @return true if the current subject on thread is a batch admin.
     */
    private boolean isAdmin(Subject runAsSubject) {
        return isInBatchRole(runAsSubject, ROLES.batchAdmin);

    }

    /**
     * @return true if the current subject on thread is a batch submitter.
     */

    private boolean isSubmitter(Subject runAsSubject) {
        return isInBatchRole(runAsSubject, ROLES.batchSubmitter);
    }

    /**
     * @return true if the current subject on thread is a batch monitor.
     */

    private boolean isMonitor(Subject runAsSubject) {
        return isInBatchRole(runAsSubject, ROLES.batchMonitor);
    }

    /**
     * @return true if the given runAsSubject is granted the given role;
     *         false if the Authz Service is not available.
     */
    private boolean isInBatchRole(Subject runAsSubject, ROLES batchRole) {
        AuthorizationService authzService = getAuthorizationService();
        if ( authzService != null ) {
            return authzService.isAuthorized(BATCH_AUTH_ID, 
                    new HashSet<String>(Arrays.asList(batchRole.toString())), 
                    runAsSubject);
        }
        return false;

    }

    /**
     * @return true if the given runAsSubject is granted any batch role (batchAdmin, batchSubmitter, batchMonitor);
     *         false if the Authz Service is not available.
     */
    private boolean isInAnyBatchRole(Subject runAsSubject) {
        AuthorizationService authzService = getAuthorizationService();
        if ( authzService != null ) {
            return authzService.isAuthorized(BATCH_AUTH_ID,
                    new HashSet<String>(Arrays.asList(ROLES.batchAdmin.toString(),
                            ROLES.batchSubmitter.toString(),
                            ROLES.batchMonitor.toString())),
                    runAsSubject);
        }
        return false;
    }

    /**
     * @return the (possibly translated) message from the resource bundle
     */
    private String getFormattedMessage( String msgId, Object[] fillIns, String defaultMsg ) {
        return TraceNLS.getFormattedMessage(WSBatchAuthServiceImpl.class,
                logger.getResourceBundleName(),
                msgId,
                fillIns,
                defaultMsg);
    }


}
