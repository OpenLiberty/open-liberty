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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
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
//import com.ibm.jbatch.container.api.impl.FFDCIgnore;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.ws.ROLES;
import com.ibm.jbatch.container.ws.WSBatchAuthService;
import com.ibm.jbatch.container.ws.WSJobInstance;
import com.ibm.jbatch.container.ws.WSJobRepository;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.jbatch.container.ws.BatchGroupSecurityHelper;
import com.ibm.jbatch.container.ws.JoblogUtil;
import com.ibm.ws.security.AccessIdUtil;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.registry.RegistryHelper;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM" })
public class WSBatchAuthServiceImpl implements WSBatchAuthService {

    public static final String BATCH_AUTH_ID = "com.ibm.ws.batch";


    private final static Logger logger = Logger.getLogger(WSBatchAuthServiceImpl.class.getCanonicalName(),
                                                          "com.ibm.ws.jbatch.security.resources.BatchSecurityMessages" );

    private IPersistenceManagerService persistenceManagerService ;

    private BatchSecurityHelper batchSecurityHelper = null;
    private BatchGroupSecurityHelper batchGroupSecurityHelper = null;

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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setBatchGroupSecurityHelper(BatchGroupSecurityHelper batchGroupSecurityHelper) {
        this.batchGroupSecurityHelper = batchGroupSecurityHelper;
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
        Set<String> listOfGroupsForJobID = null;
        Set<String> listOfGroupsForSubject = null;
        boolean checkGroupSecurity = false;

        if (this.isAdmin(runAsSubject())) {
        } else if (this.isMonitor(runAsSubject())) {
        } else if (this.isSubmitter(runAsSubject())) {
        	boolean submitterRole = this.isSubmitter(runAsSubject());
        	boolean isGroupAdmin = this.isGroupAdmin();
        	boolean isGroupMonitor = this.isGroupMonitor();
            //if you don't also own the job you can't view it
			if (batchSecurityHelper.getRunAsUser().equals(submitter)) {
			} else if (this.isGroupAdmin() || this.isGroupMonitor()) {
				checkGroupSecurity = true;
			} else {

				throw new JobSecurityException(getFormattedMessage("USER_UNAUTHORIZED_JOB_INSTANCE",
						new Object[] { getRunAsUser(), instanceId },
						"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}."));
			}
        } else if (this.isGroupAdmin() || this.isGroupMonitor()) { 
    		checkGroupSecurity = true;
    	}
        else {
            throw new JobSecurityException( getFormattedMessage( "USER_UNAUTHORIZED_NO_BATCH_ROLES",
                                                                 new Object[] { getRunAsUser() },
                                                                 "CWWKY0303W: User {0} is not authorized to perform any batch operations." ) );
        }
        	      	
		if (checkGroupSecurity) {
			
			listOfGroupsForJobID = persistenceManagerService.getJobInstance(instanceId).getGroupNames();
			listOfGroupsForSubject = getSubjectGroups(runAsSubject());
			
			if (subjectInGroups(listOfGroupsForSubject, listOfGroupsForJobID)) {
			} else {
				// user not in any groups listed - disallow access
				// construct message to be displayed

				String log_jobGroups = constructGroupListForAuthFailString(listOfGroupsForJobID);
				if (batchGroupSecurityHelper != null) {
					logger.fine(log_jobGroups);
					logger.warning(getFormattedMessage("USER_GROUP_UNAUTHORIZED_JOB_INSTANCE",
							new Object[] { instanceId, getRunAsUser(),
									constructGroupListForAuthFailString(listOfGroupsForJobID) },
							"CWWKY0305W: Access to job instance {0} denied.  The job has an operation group name defined and the user {1} has batchGroupMonitor or batchGroupAdmin authority but is not a member of the any appropriate group {2}."));
					throw new JobSecurityException(getFormattedMessage("USER_UNAUTHORIZED_JOB_INSTANCE",
							new Object[] { getRunAsUser(), instanceId },
							"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}."));
				}
			}
		}
        
        return instanceId;
    }

    private String constructGroupListForAuthFailString(Set<String> listOfGroups) {
		Iterator it = listOfGroups.iterator();
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		while (it.hasNext()) {
			buf.append(it.next());
			if (it.hasNext()) {
				buf.append(", ");
			}
		}
		buf.append("]");
		return buf.toString();
	}

	private Set<String> getSubjectGroups(Subject runAsSubject) {
		if (batchGroupSecurityHelper != null){
			return batchGroupSecurityHelper.getGroupsForSubject(runAsSubject);
		}
		else {
			return new HashSet<String>();
		}
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
    
    private void authorizedWriteOperationAccessCheck(String submitter, long instanceId) 
            throws NoSuchJobExecutionException, JobSecurityException {
		Set<String> listOfGroupsForJobID = null;
		Set<String> listOfGroupsForSubject = null;
		boolean checkGroupSecurity = false;

		if (isInAnyBatchRole()) {
			if (this.isAdmin(runAsSubject())) {
			} else if (this.isSubmitter(runAsSubject())) {
				boolean submitterRole = this.isSubmitter(runAsSubject());
				boolean isGroupAdmin = this.isGroupAdmin();
				boolean isGroupMonitor = this.isGroupMonitor();
				// if you don't also own the job you can't view it
				if (batchSecurityHelper.getRunAsUser().equals(submitter)) {
				} else if (isGroupAdmin) {
					checkGroupSecurity = true;
				}
			} else if (this.isGroupAdmin()) {
				checkGroupSecurity = true;
			} else if (this.isMonitor(runAsSubject()) || this.isGroupMonitor()) {
				throw new JobSecurityException(getFormattedMessage("USER_UNAUTHORIZED_JOB_INSTANCE",
						new Object[] { getRunAsUser(), instanceId },
						"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}."));
			}
		} else {
			throw new JobSecurityException(getFormattedMessage("USER_UNAUTHORIZED_JOB_INSTANCE",
					new Object[] { getRunAsUser(), instanceId },
					"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}."));
		}

		if (checkGroupSecurity) {
			listOfGroupsForJobID = persistenceManagerService.getJobInstance(instanceId).getGroupNames();
			listOfGroupsForSubject = getSubjectGroups(runAsSubject());
			
			if (subjectInGroups(listOfGroupsForSubject, listOfGroupsForJobID)) {
			} else {
				// user not in any groups listed - disallow access
				// construct message to be displayed
				String log_jobGroups = constructGroupListForAuthFailString(listOfGroupsForJobID);
				if (batchGroupSecurityHelper != null) {
					logger.fine(log_jobGroups);
					logger.warning(getFormattedMessage("USER_GROUP_UNAUTHORIZED_JOB_INSTANCE",
							new Object[] { instanceId, getRunAsUser(),
									constructGroupListForAuthFailString(listOfGroupsForJobID) },
							"CWWKY0305W: Access to job instance {0} denied.  The job has an operation group name defined and the user {1} has batchGroupMonitor or batchGroupAdmin authority but is not a member of the following configured groups: {2}."));
					throw new JobSecurityException(getFormattedMessage("USER_UNAUTHORIZED_JOB_INSTANCE",
							new Object[] { getRunAsUser(), instanceId },
							"CWWKY0302W: User {0} is not authorized to perform batch operations associated with job instance {1}."));
				}
			}
		}
    }
    
    private void authorizedJobStopRestart(String submitter, long instanceId) 
            throws NoSuchJobExecutionException, JobSecurityException {
    	authorizedWriteOperationAccessCheck(submitter, instanceId);
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
    	authorizedWriteOperationAccessCheck(submitter, instanceId);
    	return instanceId;
    }

    @Override
    public boolean isGroupAdmin() {
    	return this.isGroupAdmin(runAsSubject());
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
	public boolean isGroupMonitor() {
    	return this.isGroupMonitor(runAsSubject());
	}
    
    @Override
    public boolean isInAnyBatchRole() {
        return this.isInAnyBatchRole(runAsSubject());
    }

    @Override
    @FFDCIgnore(JobSecurityException.class)
    public boolean isAuthorizedInstanceRead(long instanceId)
        throws NoSuchJobInstanceException {
    	    	
    	try {
    		authorizedInstanceRead(instanceId);
    	}
    	catch (JobSecurityException jse){
    		return false;
    	}
    	
    	return true;
    }

	private boolean subjectInGroups(Set<String> listOfGroupsForSubject, Set<String> listOfGroupsForJobID) {
		
		Iterator it = listOfGroupsForSubject.iterator();
		while (it.hasNext()){
			if (listOfGroupsForJobID.contains(it.next())){
				// found a group match, good to go
				return true;
			}
		}
		// no group in user group list was found in the job id group list
		return false;
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
     * @return true if the current subject on thread is a batch group admin.
     */
    private boolean isGroupAdmin(Subject runAsSubject) {
        return isInBatchRole(runAsSubject, ROLES.batchGroupAdmin);

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
     * @return true if the current subject on thread is a batch group monitor.
     */

    private boolean isGroupMonitor(Subject runAsSubject) {
        return isInBatchRole(runAsSubject, ROLES.batchGroupMonitor);
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
                            ROLES.batchMonitor.toString(),
                            ROLES.batchGroupAdmin.toString(),
                            ROLES.batchGroupMonitor.toString())),
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

	@Override
	public Set<String> getGroupsForSubject() {		

		return getSubjectGroups(runAsSubject());
		
	}
	
	@Override
	public Set<String> normalizeGroupNames(Set<String> groupNames) {

        Subject subject = batchSecurityHelper.getRunAsSubject();
        SubjectHelper sh = new SubjectHelper();
        WSCredential cred;
        cred = sh.getWSCredential(subject);
        String realmName = null;
        Iterator it = groupNames.iterator();
        Set<String> longGrpNames = new HashSet<String>();

        UserRegistry ur = null;

        try {
            realmName = sh.getRealm(subject);
            ur = RegistryHelper.getUserRegistry(realmName);
        } catch (WSSecurityException wsecEx) {
            throw new RuntimeException(wsecEx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String longGroupName = null;

        while (it.hasNext()) {

            try {
                String currName = (String) it.next();
                longGroupName = null;

                if (ur.isValidGroup(currName)) {
                    longGroupName = AccessIdUtil.createAccessId("group", realmName, ur.getUniqueGroupId(currName));
                    longGrpNames.add(longGroupName);
                } else {
                    JoblogUtil.logToJobLogAndTraceOnly(Level.FINE, "JOB_GROUP_NAME_NOT_VALID", new Object[] { currName },
                                                       logger);
                }
            } catch (EntryNotFoundException enfEx) {
                throw new RuntimeException(enfEx);
            } catch (CustomRegistryException crEx) {
                throw new RuntimeException(crEx);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }
        return longGrpNames;
    }
}
