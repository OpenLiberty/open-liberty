/*******************************************************************************
 * Copyright (c) 2012,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.security.GenericCredential;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.j2c.SecurityHelper;
import com.ibm.ws.jca.cm.AbstractConnectionFactoryService;
import com.ibm.ws.kernel.security.thread.ThreadIdentityException;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 *
 * =====================================================================*
 * *
 * The tables below, begining with Table 1., decribe how the z/OS *
 * ThreadIdentitySupport and ThreadSecurity properties work. *
 * *
 * ThreadIdentitySupport may be: NOTALLOWED, ALLOWED, REQUIRED *
 * *
 * ThreadSecurity may be: true or false *
 * *
 * NOTES: *
 * 1. The ThreadidentitySupport and ThreadSecurity properties are *
 * implemented by the combination of methods provided in this *
 * helper. *
 * 2. For performance reasons and because the implementation is *
 * spread across the methods in this helper, the implementation *
 * flow does not necessarily follow the exact flow of the checks *
 * made in the tables. *
 * 3. Although processing is dependent on whether or not security *
 * is enabled, no specific checks are made to determine if *
 * securtity is enabled. Sensitivity to whether security is *
 * enabled is a function of the securityLoginExtension *
 * getLocalOSInvocationSubject() method. When Security is not *
 * enabled, the returned subject and UTOKEN generic credential *
 * will always be for the server identity. Additionaly, *
 * when security is not enabled, the securityLoginExtension *
 * isSyncToThreadEnabled() method will always return false. *
 * When SyncToThreadEnabled is false, the securityLoginExtension *
 * methods to push the subject user identity to the OS thread and *
 * to restore the OS thread user identity will never be invoked. *
 * This will avoid any exception being thrown by security *
 * management. *
 * *
 * =====================================================================*
 *
 *
 * =====================================================================*
 * Table 1. Security State *
 * =====================================================================*
 * Security Enabled? *
 * ---------------------------------------------------------------------*
 * YES | NO *
 * ----------------------------------|----------------------------------*
 * Goto Table 2 | Goto Table 3 *
 * =====================================================================*
 *
 *
 * =====================================================================*
 * Table 2. Security is Enabled *
 * =====================================================================*
 * Container-Manged Alias Specified? *
 * ---------------------------------------------------------------------*
 * NO | YES *
 * ----------------------------------|----------------------------------*
 * Connector ALLOWS or REQUIRES | Connector REQUIRES Thread *
 * Thread Identity to be used when | Identity to be used when *
 * getting a connection? | getting a connection? *
 * ----------------------------------|----------------------------------*
 * | | | *
 * NO | YES | NO | YES *
 * ----------|-----------------------|----------|-----------------------*
 * | Connector requires | | Connector requires *
 * | OS Thread Security? | | OS Thread Security? *
 * |-----------------------| |-----------------------*
 * | NO | YES | | NO | YES *
 * connector |-----|-----------------| Use the |-----|-----------------*
 * processing| | is the Server | specified| | is the Server *
 * dependent:| | Sync-To-Thread | alias | | Sync-To-Thread *
 * |Use | Enabled? | |Use | Enabled? *
 * -may throw|RunAs|-----------------| |RunAs|-----------------*
 * exception|user | NO | YES | |user | NO | YES *
 * |ident|----|------------| |ident|----|------------*
 * -may use |assoc| | | |assoc| | *
 * userid & |with | | | |with | | *
 * password |cur |Use | Use RunAs | |cur |Use | Use RunAs *
 * saved in |thrd |Srvr| identity | |thrd |Srvr| identity *
 * MCF or | |user| associated | | |user| associated *
 * Datasource |id | with the | | |id | with the *
 * | | | current | | | | current *
 * | | | thread | | | | thread *
 * | | | | | | | *
 * | | | | | | | *
 * =====================================================================*
 *
 * =====================================================================*
 * Table 3. Security is Not Enabled *
 * =====================================================================*
 * Container-Manged Alias Specified? *
 * ---------------------------------------------------------------------*
 * NO | YES *
 * ----------------------------------|----------------------------------*
 * Connector ALLOWS or REQUIRES | Connector REQUIRES Thread *
 * Thread Identity to be used when | Identity to be used when *
 * getting a connection? | getting a connection? *
 * ----------------------------------|----------------------------------*
 * | | | *
 * NO | YES | NO | YES *
 * ----------|-----------------------|----------|-----------------------*
 * | | | *
 * connector | Use Server Identity | Use the | Use Server Identity *
 * processing| | specified| *
 * dependent:| | alias | *
 * | | | *
 * -may throw| | | *
 * exception| | | *
 * | | | *
 * -may use | | | *
 * userid & | | | *
 * password | | | *
 * saved in | | | *
 * MCF or | | | *
 * Datasource | | *
 * | | | *
 * =====================================================================*
 * Packaging: Not in j2cClient.jar. Note that an instance of this class
 * is created by the createSecurityHelper of J2CUtilityClass, which is not
 * in the client jar.
 * =====================================================================*
 *
 *
 * <P> The ThreadIdentitySecurityHelper is used when
 * ThreadIdentitySupport is "ALLOWED" or "REQUIRED" by the
 * resource adapter.
 *
 */
public class ThreadIdentitySecurityHelper implements SecurityHelper {
    private final boolean m_ThreadSecurity;
    private final int m_ThreadIdentitySupport;

    private static TraceComponent tc = Tr.register(ThreadIdentitySecurityHelper.class,
                                                   J2CConstants.traceSpec,
                                                   J2CConstants.messageFile);

    /**
     * Constructor for ThreadIdentitySecurityHelper
     */
    public ThreadIdentitySecurityHelper(int threadIdentitySupport, boolean threadSecurity) {

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "<init>", threadIdentitySupport, threadSecurity);

        m_ThreadIdentitySupport = threadIdentitySupport;
        m_ThreadSecurity = threadSecurity;

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * {@inheritDoc}
     *
     * The afterGettingConnection() method is used to allow special
     * special security processing to be performed after calling
     * a resource adapter to get a connection. If the passed input
     * object is not null, the Subject's user identity was pushed to
     * the OS Thread during beforeGettingConnection() and the object
     * is a credential token that can be used to restore the thread
     * identity. In this case, restore the thread identity to what it
     * was originally.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @param Object credentialToken
     * @return void
     * @exception ResourceException
     *
     */
    @Override
    public void afterGettingConnection(Subject subject, ConnectionRequestInfo reqInfo, Object credentialToken) throws ResourceException {

        final Object credToken = credentialToken;

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "afterGettingConnection", getSubjectString(subject), reqInfo, credentialToken);

        if (credToken != null) {

            try {
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(
                                                  new PrivilegedExceptionAction() {
                                                      @Override
                                                      public Object run() throws Exception {
                                                          ThreadIdentityManager.resetChecked(credToken);
                                                          return null;
                                                      }
                                                  });
                } else {
                    ThreadIdentityManager.resetChecked(credToken);
                }

            } catch (PrivilegedActionException pae) {
                FFDCFilter.processException(pae, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.afterGettingConnection", "37", this);
                Tr.error(tc, "FAILED_DOPRIVILEGED_J2CA0060", pae);
                Exception e = pae.getException();
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.afterGettingConnection() failed attempting to restore user identity to the OS Thread");
                re.initCause(e);
                throw re;

            } catch (IllegalStateException ise) {
                FFDCFilter.processException(ise, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.afterGettingConnection", "38", this);
                Object[] parms = new Object[] { "ThreadIdentitySecurityHelper.afterGettingConnection()", ise };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.afterGettingConnection() failed attempting to restore user identity to the OS Thread");
                re.initCause(ise);
                throw re;
            } catch (ThreadIdentityException tie) {
                FFDCFilter.processException(tie, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.afterGettingConnection", "39", this);
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.afterGettingConnection() failed attempting to restore user identity to the OS Thread");
                re.initCause(tie);
                throw re;
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "afterGettingConnection() restored OS thread identity");
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "afterGettingConnection");
    }

    /**
     * {@inheritDoc}
     *
     * The beforeGettingConnection() method is used to allow
     * special security processing to be performed prior to calling
     * a resource adapter to get a connection. If ThreadIdentitySupport
     * is "ALLOWED" or "REQUIRED" and the input Subject is not null
     * (i.e., res-auth=Container), search the credentials for a
     * UTOKEN generic credential. If found and ThreadSecurity is true,
     * indicating the adapter will get the connection user identity
     * from the OS thread ACEE, push the Subject to the OS Thread in the
     * form of an ACEE if SyncToThread is enabled. In the case where
     * SyncToThread is not enabled, the connector by default will
     * get Server identity since no user identity will have been
     * pushed to the OS thread.
     *
     * In the case, where the input Subject is null (i.e.,
     * res-auth=Application or Servlet) and the resource adapter
     * requires OS ThreadSecurity when a getConnection is issued without
     * a userid/password, push the Server identity to the OS thread in the
     * form of an ACEE if the Server is enabled to allow the subject
     * to be sync'd to thread. If SyncToThread is not allowed, the
     * connector by default will get Server identity since no user
     * identity will have been pushed to the current OS thread.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @return Object if non-null, the user identity defined by the
     *         Subject was pushed to thread. The Object in
     *         this case needs to be passed as input to
     *         afterGettingConnection method processing and
     *         will be used to restore the thread identity
     *         back to what it was.
     * @exception ResourceException
     *
     */
    @Override
    public Object beforeGettingConnection(Subject subject, ConnectionRequestInfo reqInfo) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "beforeGettingConnection", getSubjectString(subject), reqInfo);

        Object retObject = null;
        final Subject subj = subject;

        // If Security is enabled, continue. Otherwise exit immediately
        // with a null object.

        if (ThreadIdentityManager.isThreadIdentityEnabled()) {

            // Check if the current resource adapter MCF configuration
            // supports using ThreadIdentity. If so, continue processing.
            // Otherwise get out.

            if ((m_ThreadIdentitySupport != AbstractConnectionFactoryService.THREAD_IDENTITY_NOT_ALLOWED)) {

                if (subj != null) {
                    // resauth = CONTAINER

                    // Check if UTOKEN Generic Credential was found. If so,
                    // the subject being processed represents the user identity
                    // currently associated with the thread we are running under
                    // (i.e., was established by finalizeSubject()). In this case,
                    // if SyncToThread is enabled for the server and this resource
                    // adapter (e.g., DB2 or IMS DL/I) indicates it needs the user
                    // identity associated with the thread actually pushed to the
                    // OS thread, then push the user identity to the OS thread.
                    // Under z/OS, this will create an ACEE for the user identity
                    // on the z/OS Thread. Connectors like the DB2 390 Local JDBC
                    // Provider and the IMS DL/I Connector in the case of a
                    // getConnection() request without any userid/password base
                    // the connection owner on the user represented by the
                    // user identity of the current z/OS thread.
                    //
                    //
                    // It should be noted that if SyncToThread is not enabled
                    // for the server, we cannot push the user identity to the
                    // OS thread. In this case, the WAS z/OS server will not
                    // have a security ACEE at all. In this situation,
                    // Connectors like the DB2 390 Local JDBC Provider and the IMS
                    // DL/I Connector will end up using the Server identity as
                    // user associated with the connection. Even though server
                    // identity will be used, the Subject used by
                    // Connection Management will be left set to the
                    // user identity associated with the current
                    // thread (e.g., RunAs Caller or Role user) and
                    // connection pooling will be based on this
                    // subject as opposed to using the Server identity.

                    if (doesSubjectContainUTOKEN(subj)) {

                        // We are using the user identity associated with the thread.
                        // The Subject contains a UTOKEN credential representing that
                        // user identity.

                        if (m_ThreadSecurity) {

                            // Connector requires that user identity be pushed to
                            // OS Thread.

                            if (ThreadIdentityManager.isJ2CThreadIdentityEnabled()) {

                                // J2C SyncToThread is enabled for Server.
                                // Push Subject to thread

                                retObject = setJ2CThreadIdentity(subj);

                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "beforeGettingConnection() pushed the user identity associated with the thread to the OS Thread:  ",
                                             new Object[] { getSubjectString(subj) });
                                }

                            } else {

                                // J2C SyncToThread not enabled for Server.
                                // The adapter Will run with server identity
                                // since we are not able to push the Subject's
                                // user identity to the current OS thread as an ACEE.

                                if (tc.isDebugEnabled()) {
                                    Tr.debug(
                                             tc,
                                             "beforeGettingConnection() could not push user identity associated with the thread to the OS Thread  because server was not enabled for SyncToThread.");
                                }

                                // Now we may need to synch server ID to thread.
                                // We found a UTOKEN credential on the Subject,
                                // which indicates that no container-managed alias was
                                // specified, (in which case finalizeSubject would not
                                // have gone through the process of getting a Subject with
                                // UTOKEN credential).  So if (Connection Mgmt) SyncToThread is
                                // not enabled but Application SynchToThread is enabled, we
                                // would end up using the RunAs anyway, from the Application Synch,
                                // even though (Connection Mgmt) SyncToThread was disabled.
                                //
                                // We wish to control the id used to get a connection within Connection
                                // Mgmt code alone and to isolate the behavior from any settings not
                                // related to Connection Mgmt.  If no Application Synch were done,
                                // a Thread Security-enabled connector would use server id to get a
                                // connection (when no container alias was specified and when no Connection Mgmt
                                // synch was done).  So we add this code to ensure that the server id
                                // is also used to get a connection if an Application Synch has
                                // been performed (and no container alias was specified and when no Connection Mgmt
                                // synch was done)
                                //
                                // This is only worth doing if the Appliation Synch has been enabled.  Though we
                                // won't determine if the Application Synch has actually been done.

                                if (ThreadIdentityManager.isAppThreadIdentityEnabled()) {

                                    if (tc.isDebugEnabled()) {
                                        Tr.debug(tc, "beforeGettingConnection() pushing server identity to the OS Thread because Application SyncToThread is enabled.");
                                    }

                                    // get Server subject and push it to the current OS thread
                                    retObject = ThreadIdentityManager.runAsServer();
                                }
                            }

                        } else {
                            // OS ThreadSecurity is not enabled
                            // The adapter will use the user identity
                            // from the Subject that is associated with the
                            // current thread (i.e., the subject that is
                            // passed to the adapter).
                        }

                    } else {
                        // Subject doesn't have a UTOKEN GenericCredential.
                        // Check if we have an unexpected error situation.
                        checkForUTOKENNotFoundError(subj);
                    }

                } else {
                    // resauth = APPLICATION

                    // When resauth is Application and the current
                    // connector is one that supports using ThreadSecurity (i.e,
                    // associates the user identity from the OS Thread with the
                    // connection when getConnection() is done without
                    // a userid/password), check if the server is enabed to perform
                    // SynctoThread. If so, get the Server identity and push
                    // the Server identity to the current OS Thread.
                    //
                    // The reason for this is to ensure consistency in terms of
                    // the default user identity used by the connector.
                    // Whenever a connector that supports ThreadSecurity defaults,
                    // we want to ensure the default is Server identity. For
                    // example, in the case of resauth=Container, if we try
                    // to push the current user identity asscoiated with the
                    // thread to the OS thread, but the server is not enabled
                    // to perform SyncToThread, we cannot push the user identity
                    // to the thread. Thus, in this case, the connector
                    // will default to using the Server identity as the owner of
                    // the connection that is allocated. Similarly, when
                    // resauth=Application and the getConnection() request is
                    // issued without a userid/password, we want to ensure that
                    // the connector defaults to using Server Identity. Thus,
                    // to make this happen, we push the Server identity to the
                    // current OS thread whenever resauth=Application and the
                    // connector supports ThreadSecurity. If it so happens
                    // that the application ends up issuing getConnection() with
                    // a userid/password, the fact that we have pushed the
                    // Server identity to the OS thread will not impact processing.
                    // Later, during afterGettingConnection() processing, the
                    // OS Thread Identity will then be returned to what it was.

                    if (m_ThreadSecurity && ThreadIdentityManager.isThreadIdentityEnabled()) {
                        // Get Server subject and push it to the current OS thread
                        retObject = ThreadIdentityManager.runAsServer();
                    }
                }
            }

        } else {
            // Security not enabled
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "beforeGettingConnection() processing skipped. Security not enabled.");
            }

            // NOTE: In the case where Security is not enabled, if
            // no Container-managed alias was specified and the
            // connector ALLOWS or REQUIRES ThreadIdentitySupport,
            // any connection obtained will be associated with
            // server identity.
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "beforeGettingConnection", retObject);
        return retObject;
    }

    /**
     * {@inheritDoc}
     *
     * When (1) the ThreadIdentitySupport for the ManagedConnectionFactory
     * is "REQUIRED" OR (2) when the ThreadIdentitySupport is "ALLOWED
     * and the input Subject has no private credentials because no
     * Container-managed alias was specified, default the Subject to
     * the user identity associated with the current thread. A new Subject
     * containing the current user identity will be created with a UTOKEN
     * Generic Credential and passed back for use while getting a
     * connection.
     *
     *
     * The primary intent of this method is to allow the Subject to be
     * defaulted.
     *
     * @param Subject subject
     * @param ConnectionRequestInfo reqInfo
     * @param cmConfigData to determine whether to call getJ2CInvocationSubject
     * @return Subject
     * @exception ResourceException
     *
     */
    @Override
    public Subject finalizeSubject(Subject subject, ConnectionRequestInfo reqInfo, CMConfigData cmConfigData) throws ResourceException {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "finalizeSubject", getSubjectString(subject), reqInfo);

        Subject helperSubject = subject; // To start, default helper subject to the input subject

        if (cmConfigData.getAuth() == J2CConstants.AUTHENTICATION_CONTAINER) {
            // resauth=Container, so continue.

            if (m_ThreadIdentitySupport == AbstractConnectionFactoryService.THREAD_IDENTITY_ALLOWED) {

                // The current resource adapter MCF Configuration supports
                // using ThreadIdentity.

                // Check if a container-managed alias is configured.  If yes, the alias
                // will be used.  If no, then use the invocation subject.
                String containerAlias = getAliasToFinalize(cmConfigData);
                if (containerAlias == null || containerAlias.equals("")) {

                    // No Container-managed alias was specified.
                    // Additionally, the resource adapter supports
                    // using the user associated with the current
                    // thread as the user identity that is to be
                    // associated with the connection. In this case
                    // we will default the Subject to a valid Subject
                    // that represents the user associated with the
                    // current thread and it will contain a UTOKEN
                    // Generic Credential in its set of private
                    // credentials. This type of credential is what
                    // triggers an adapter to know it is using the
                    // user identity associated with the current thread
                    // as the user to be associated with the connection.

                    helperSubject = getJ2CInvocationSubject();

                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "finalizeSubject(): No user identity was specifed. User identity has been defaulted to current thread identity");
                    }
                }

            } else if (m_ThreadIdentitySupport == AbstractConnectionFactoryService.THREAD_IDENTITY_REQUIRED) {

                // The connector this helper is associated with
                // always requires that the user associated with
                // the current thread is to be used as the identity
                // associated with the connection. In this case,
                // we will set the Subject to a valid Subject
                // that represents the user associated with the
                // current thread and it will contain a UTOKEN
                // Generic Credential in its set of private
                // credentials. This type of credential is what
                // triggers the adapter to know it is using the
                // user identity associated with the current thread
                // as the user to be associated with the connection.
                //
                // NOTE: in this case, the user identity that may be
                //       specified by a Container-managed alias will
                //       be ignored/overridden.

                helperSubject = getJ2CInvocationSubject();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "finalizeSubject(): Connector REQUIRED specified user identity to be overridden by the current thread identity");
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "finalizeSubject", getSubjectString(helperSubject));
        return helperSubject;
    }

    /**
     * The getSubjectString() method uses a doPrivileged wrapper
     * to call toString() on a Subject passed as an argument, and
     * the result is returned. This method is designed to be
     * called in cases where the Subject needs to be traced, but
     * the calling classes are not required to have permission to
     * access the subject. PrivilegedActionExceptions received
     * while calling toString() on the Subject are not rethrown
     * so as not to interrupt the flow of the calling method. If
     * a null Subject is passed as an argument, a value of null
     * is returned.
     *
     * @param Subject subject
     * @return String
     */
    private String getSubjectString(Subject subject) {

        String returnVal = null;

        if (subject != null) {

            if (System.getSecurityManager() != null) {

                // Java 2 Security enabled
                final Subject newSubject = subject;
                PrivilegedExceptionAction privExAction = new PrivilegedExceptionAction() {
                    @Override
                    public Object run() throws Exception {
                        return newSubject.toString();
                    }
                };

                try {
                    returnVal = (String) AccessController.doPrivileged(privExAction);
                } catch (PrivilegedActionException pae) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception received in getSubjectString:", pae);
                    }
                    returnVal = "Subject cannot be traced due to a PrivilegedActionException";
                }

            } // end security manager != null

            else {
                returnVal = subject.toString();
            }

        } // end subject != null

        return returnVal;

    }

    /**
     * Get the jaas alias from the config.
     */
    private String getAliasToFinalize(CMConfigData cmConfigData) {

        String alias = null;
        if (cmConfigData == null)
            return alias; // Not expected, but being safe

        // Check for DefaultPrincipalMapping alias from res-ref:
        final String DEFAULT_PRINCIPAL_MAPPING = "DefaultPrincipalMapping";
        final String MAPPING_ALIAS = "com.ibm.mapping.authDataAlias";

        String loginConfigurationName = cmConfigData.getLoginConfigurationName();
        if ((loginConfigurationName != null) && (!loginConfigurationName.equals(""))) {
            if (loginConfigurationName.equals(DEFAULT_PRINCIPAL_MAPPING)) {
                HashMap loginConfigProps = cmConfigData.getLoginConfigProperties();
                if ((loginConfigProps != null) && (!loginConfigProps.isEmpty())) {
                    alias = (String) loginConfigProps.get(MAPPING_ALIAS);
                }
            }

        }

        if (alias == null) {
            // Check for container-managed auth alias from CF/DS:
            alias = cmConfigData.getContainerAlias();
        }

        return alias;
    }

    /**
     * Retrieve the invocation subject on the thread and J2C-ize it (which means fit it
     * with a GenericCredential representing the user's native UTOKEN on z/OS).
     *
     * The J2C invocation subject is used on the subsequent call to beforeGettingConnection.
     *
     * Basically this method is just a wrapper around ThreadIdentityManager.getJ2CInvocationSubject
     * with Java 2 SecurityManager handling.
     *
     * @return Subject the J2C invocation subject
     *
     * @throws ResourceException
     */
    private Subject getJ2CInvocationSubject() throws ResourceException {

        Subject j2cSubject = null;

        if (System.getSecurityManager() != null) {
            try {
                j2cSubject = (Subject) AccessController.doPrivileged(
                                                                     new PrivilegedExceptionAction() {
                                                                         @Override
                                                                         public Object run() throws Exception {
                                                                             return ThreadIdentityManager.getJ2CInvocationSubject();
                                                                         }
                                                                     });
            } catch (PrivilegedActionException pae) {
                FFDCFilter.processException(pae, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.finalizeSubject", "826", this);
                Tr.error(tc, "FAILED_DOPRIVILEGED_J2CA0060", pae);
                Exception e = pae.getException();
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.finalizeSubject() failed attempting to get local OS invocation subject");
                re.initCause(e);
                throw re;
            } catch (IllegalStateException ise) {
                FFDCFilter.processException(ise, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.finalizeSubject", "826", this);
                Object[] parms = new Object[] { "ThreadIdentitySecurityHelper.finalizeSubject()", ise };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.finalizeSubject() failed attempting to get local OS invocation subject");
                re.initCause(ise);
                throw re;
            }

        } else {
            j2cSubject = ThreadIdentityManager.getJ2CInvocationSubject();
        }

        return j2cSubject;
    }

    /**
     * Search for a GenericCredential representing a native z/OS UTOKEN in the
     * Subject's set of private credentials. It would have been put there by
     * a previous call to finalizeSubject.
     *
     * @return true if the subject contains a UTOKEN; false otherwise.
     *
     */
    private boolean doesSubjectContainUTOKEN(Subject subj) throws ResourceException {

        Set privateGenericCredentials = getPrivateGenericCredentials(subj);
        final Iterator iter = privateGenericCredentials.iterator();

        boolean subjectHasUtokenCred = false;
        GenericCredential credential = null;

        while (iter.hasNext()) {
            if (System.getSecurityManager() != null) {
                try {
                    credential = (GenericCredential) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        @Override
                        public Object run() throws Exception {
                            return iter.next();
                        }
                    });
                } catch (java.security.PrivilegedActionException pae) {
                    FFDCFilter.processException(pae, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.beforeGettingConnection", "19", this);
                    Tr.error(tc, "FAILED_DOPRIVILEGED_J2CA0060", pae);
                    Exception e = pae.getException();
                    ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() failed attempting to access Subject's credentials");
                    re.initCause(e);
                    throw re;
                }

            } else {
                credential = (GenericCredential) iter.next();
            }

            if (credential.getMechType().equals("oid:1.3.18.0.2.30.1")) {
                subjectHasUtokenCred = true;
                break;
            }
        }
        return subjectHasUtokenCred;
    }

    /**
     * Return the set of private credentials of type GenericCredential from the
     * given subject.
     *
     * Basically this method is a wrapper around Subject.getPrivateCredentials
     * with Java 2 Security handling.
     *
     */
    private Set getPrivateGenericCredentials(final Subject subj) throws ResourceException {

        Set privateGenericCredentials = null;

        if (System.getSecurityManager() != null) {
            try {
                privateGenericCredentials = (Set) AccessController.doPrivileged(
                                                                                new PrivilegedExceptionAction() {
                                                                                    @Override
                                                                                    public Object run() throws Exception {
                                                                                        return subj.getPrivateCredentials(GenericCredential.class);
                                                                                    }
                                                                                });
            } catch (PrivilegedActionException pae) {
                FFDCFilter.processException(pae, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.beforeGettingConnection", "18", this);
                Tr.error(tc, "FAILED_DOPRIVILEGED_J2CA0060", pae);
                Exception e = pae.getException();
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper failed attempting to access Subject's credentials");
                re.initCause(e);
                throw re;
            }

        } else {
            privateGenericCredentials = subj.getPrivateCredentials(GenericCredential.class);
        }

        return privateGenericCredentials;
    }

    /**
     * Apply the given subject's identity to the thread.
     *
     * @return The identity token that must be supplied on the subsequent call to reset
     *         the thread identity.
     */
    private Object setJ2CThreadIdentity(final Subject subj) throws ResourceException {

        Object retObject = null;

        try {
            if (System.getSecurityManager() != null) {
                retObject = AccessController.doPrivileged(
                                                          new PrivilegedExceptionAction() {
                                                              @Override
                                                              public Object run() throws Exception {
                                                                  return ThreadIdentityManager.setJ2CThreadIdentity(subj);
                                                              }
                                                          });
            } else {
                retObject = ThreadIdentityManager.setJ2CThreadIdentity(subj);
            }
        } catch (PrivilegedActionException pae) {
            FFDCFilter.processException(pae, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.beforeGettingConnection", "11", this);
            Tr.error(tc, "FAILED_DOPRIVILEGED_J2CA0060", pae);
            Exception e = pae.getException();
            ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() failed attempting to push the current user identity to the OS Thread");
            re.initCause(e);
            throw re;
        } catch (IllegalStateException ise) {
            FFDCFilter.processException(ise, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.beforeGettingConnection", "20", this);
            Object[] parms = new Object[] { "ThreadIdentitySecurityHelper.beforeGettingConnection()", ise };
            Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
            ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() failed attempting to push the current user identity to the OS Thread");
            re.initCause(ise);
            throw re;
        } catch (ThreadIdentityException tie) {
            FFDCFilter.processException(tie, "com.ibm.ejs.j2c.ThreadIdentitySecurityHelper.beforeGettingConnection", "21", this);
            ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() failed attempting to push the current user identity to the OS Thread");
            re.initCause(tie);
            throw re;
        }

        return retObject;
    }

    /**
     * Check for an unexpected condition when a UTOKEN is not found in the Subject.
     *
     * @throws ResourceException if the condition is unexpected.
     */
    private void checkForUTOKENNotFoundError(Subject subj) throws ResourceException {

        // A Subject without a UTOKEN genericCredential can
        // legitimately occur in the case where the resource
        // adapter has indicated Thread Identity Support
        // is "ALLOWED, but a valid Container-managed alias
        // was specified. On the other hand, at this ponit, if
        // either of the following two cases exist when there is
        // no UTOKEN genericCredential, then we have an
        // unexpected error and need to throw an exception:
        //
        //   1. The resource adapter REQUIRES that Thread Identity
        //      be used.
        //
        //   2. The Subject has no private credentials at all
        //

        // Check if ThreadIdentitySupport is "REQUIRED"

        if (m_ThreadIdentitySupport == AbstractConnectionFactoryService.THREAD_IDENTITY_REQUIRED) {

            // ThreadIdentitySupport indicates that the use of thread
            // identity is "REQUIRED" by the connector, but the
            // Subject doesn't contain a UTOKEN GenericCredential.
            // This should not ever occur because finalizeSubject()
            // should have created a Subject with a UTOKEN
            // GenericCredential when ThreadidentitySupport is
            // "REQUIRED". Thus, throw an exception to terminate
            // processing right here.
            //

            try {
                IllegalStateException e = new IllegalStateException("ThreadIdentitySecurityHelper.beforeGettingConnection() detected Subject not setup for using thread identity, but the connector requires thread identity be used.");
                Object[] parms = new Object[] { "ThreadIdentitySecurityHelper.beforeGettingConnection()", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
            } catch (IllegalStateException ise) {
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() detected Subject with illegal state");
                re.initCause(ise);
                throw re;
            }

        } // end if m_ThreadIdentitySupport.equals REQUIRED

        // Check if Subject has at least one a private credential

        Set privateCredentials = subj.getPrivateCredentials();
        Iterator privateIterator = privateCredentials.iterator();

        if (!privateIterator.hasNext()) { // if no private credentials

            // There is not only no UTOKEN generic credential, but
            // also, there are no private credentials at all. This
            // should not happen. If there are no private credentials,
            // the finalizeSubject() processing should have built
            // a UTOKEN generic credential.
            //

            try {
                IllegalStateException e = new IllegalStateException("ThreadIdentitySecurityHelper.beforeGettingConnection() detected Subject with no credentials.");
                Object[] parms = new Object[] { "ThreadIdentitySecurityHelper.beforeGettingConnection()", e };
                Tr.error(tc, "ILLEGAL_STATE_EXCEPTION_J2CA0079", parms);
                throw e;
            } catch (IllegalStateException ise) {
                ResourceException re = new ResourceException("ThreadIdentitySecurityHelper.beforeGettingConnection() detected Subject with illegal state");
                re.initCause(ise);
                throw re;
            }

        } // end if no private credentials
    }

}
