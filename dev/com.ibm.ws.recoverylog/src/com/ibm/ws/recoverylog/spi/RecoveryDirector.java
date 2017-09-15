/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: RecoveryDirector
//------------------------------------------------------------------------------
/**
* <p>
* The RecoveryDirector provides support for the registration of those components
* that need to use use the Recovery Log Service (RLS) to store persistent
* information.
* </p>
*
* <p>
* In order to support interaction with the High Availability (HA) framework in
* the future, the RecoveryDirector acts as a bridge between the registered
* components (client services) and the controlling logic that determines when
* recovery processing is needed.
* </p>
*
* <p>
* Client services obtain a reference to the RecoveryDirector through its factory
* class, the RecoveryDirectorFactory, by calling its recoveryDirector method.
* </p>
* 
* <p>
* Client services supply a RecoveryAgent callback object when they register
* that is driven asynchronously by the RLS when recovery processing is required. 
* Upon registration, they are provided with a RecoveryLogManager object through 
* which they interact with the RLS.
* </p>
*/
public interface RecoveryDirector
{
  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.registerService
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Invoked by a client service during its initialization to register with the RLS
  * The client service provides RecoveryAgent callback object that will be invoked
  * each time a FailureScope requires recovery. One registration is required per
  * client service. Any re-registration will result in the
  * ConflictingCredentialsException being thrown.
  * </p>
  * 
  * <p>
  * The client service provides a 'sequence' value that is used by the RLS to
  * determine the order in which registered RecoveryAgents should be directed
  * to recover. The client service whose RecoveryAgent was registered with the 
  * lowest numeric sequence value will be driven first. The remaining
  * RecoveryAgents are driven in ascending sequence value order. RecoveryAgents
  * that are registered with the same sequence value will be driven in 
  * an undefined order.
  * </p>
  *
  * <p>
  * The result of registration is a new instance of the RecoveryLogManager
  * class to control recovery logging on behalf of the client service.
  * </p>
  *
  * <p>
  * The RecoveryAgent object is also used to identify the client service
  * and the registration process will fail if it provides a client service
  * identifier or name that has already been used.
  * </p>
  * 
  * @param recoveryAgent Client service identification and callback object.
  * @param sequence Client service sequence value.
  *
  * @return RecoveryLogManager A RecoveryLogManager object that the client
  *                            service can use to control recovery logging.
  *
  * @exception ConflictingCredentialsException Thrown if the RecoveryAgent identity or
  *                                            name clashes with a client service that
  *                                            is already registered
  * @exception InvalidStateException           Thrown if the registration occurs after
  *                                            the first recovery process has been 
  *                                            started.
  */
  public RecoveryLogManager registerService(RecoveryAgent recoveryAgent,int sequence) throws ConflictingCredentialsException,InvalidStateException;

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.serialRecoveryComplete
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Invoked by a client service to indicate that serial recovery processing for the 
  * unit of recovery, identified by FailureScope has been completed. The client
  * service supplies its RecoveryAgent reference to identify itself.
  * </p>
  *
  * <p>
  * When recovery events occur, each client services RecoveryAgent callback object
  * has its initiateRecovery() method invoked. As a result of this call, the client
  * services has an opportunity to perform any SERIAL recovery processing for that
  * failure scope. Once this is complete, the client calls the serialRecoveryComplete
  * method to give the next client service to handle recovery processing. Recovery
  * processing as a whole may or may not be complete before this call is issued - 
  * it may continue afterwards on a parrallel thread if required. The latter design
  * is prefereable in an HA-enabled environment as controll must be passed back as
  * quickly as possible to avoid the HA framework shutting down the JVM.
  * </p>
  *
  * <p>
  * Regardless of the style adopted, once the recovery process has performed as much
  * processing as can be conducted without any failed resources becoming available
  * again (eg a failed database), the initialRecoveryComplete call must be issued 
  * to indicate this fact. This call is used by the RLS to optomize its interactions
  * with the HA framework.
  * </p>
  *
  * <p>
  * The RecoveryDirector will then pass the recovery request on to other registered
  * client services.
  * </p>
  *
  * @param recoveryAgent The client services RecoveryAgent instance.
  * @param failureScope The unit of recovery that is completed.
  *
  * @exception InvalidFailureScope The supplied FailureScope was not recognized as
  *                                outstanding unit of recovery for the client
  *                                service.
  */
  public void serialRecoveryComplete(RecoveryAgent recoveryAgent,FailureScope failureScope) throws InvalidFailureScopeException;

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.initialRecoveryComplete
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Invoked by a client service to indicate that initial recovery processing for the 
  * unit of recovery, identified by FailureScope has been completed. The client
  * service supplies its RecoveryAgent reference to identify itself.
  * </p>
  *
  * <p>
  * When recovery events occur, each client services RecoveryAgent callback object
  * has its initiateRecovery() method invoked. As a result of this call, the client
  * services has an opportunity to perform any SERIAL recovery processing for that
  * failure scope. Once this is complete, the client calls the serialRecoveryComplete
  * method to give the next client service to handle recovery processing. Recovery
  * processing as a whole may or may not be complete before this call is issued - 
  * it may continue afterwards on a parrallel thread if required. The latter design
  * is prefereable in an HA-enabled environment as controll must be passed back as
  * quickly as possible to avoid the HA framework shutting down the JVM.
  * </p>
  *
  * <p>
  * Regardless of the style adopted, once the recovery process has performed as much
  * processing as can be conducted without any failed resources becoming available
  * again (eg a failed database), the initialRecoveryComplete call must be issued 
  * to indicate this fact. This call is used by the RLS to optomize its interactions
  * with the HA framework.
  * </p>
  *
  * <p>
  * The RecoveryDirector will then pass the recovery request on to other registered
  * client services.
  * </p>
  *
  * @param recoveryAgent The client services RecoveryAgent instance.
  * @param failureScope The unit of recovery that is completed.
  *
  * @exception InvalidFailureScope The supplied FailureScope was not recognized as
  *                                outstanding unit of recovery for the client
  *                                service.
  */
  public void initialRecoveryComplete(RecoveryAgent recoveryAgent,FailureScope failureScope) throws InvalidFailureScopeException;

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.initialRecoveryFailed
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Invoked by a client service to indicate that initial recovery processing for the 
  * unit of recovery, identified by FailureScope has been attempted but failed. The
  * client service supplies its RecoveryAgent reference to identify itself.
  * </p>
  *
  * <p>
  * Invoking this method on the local failure scope will result in the server being
  * termianted (by the HA framework)
  * </p> 
  *
  * @param recoveryAgent The client services RecoveryAgent instance.
  * @param failureScope The unit of recovery that is failed.
  *
  * @exception InvalidFailureScope The supplied FailureScope was not recognized as
  *                                outstanding unit of recovery for the client
  *                                service.
  */
  public void initialRecoveryFailed(RecoveryAgent recoveryAgent,FailureScope failureScope) throws InvalidFailureScopeException;

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.terminationComplete
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Invoked by a client service to indicate recovery processing for the identified
  * FailureScope ceased. The client service supplies its RecoveryAgent reference to
  * identify itself.
  * </p>
  *
  * <p>
  * The RecoveryDirector will then pass the termination request on to other registered
  * client services.
  * </p>
  *
  * @param recoveryAgent The client services RecoveryAgent instance.
  * @param failureScope The unit of recovery that is completed.
  *
  * @exception InvalidFailureScopeException The supplied FailureScope was not recognized as
  *                                         outstanding unit of recovery for the client
  *                                         service.
  */
  public void terminationComplete(RecoveryAgent recoveryAgent,FailureScope failureScope) throws InvalidFailureScopeException;

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.currentFailureScope
  //------------------------------------------------------------------------------
  /**
  * Invoked by a client service to determine the "current" FailureScope. This is 
  * defined as a FailureScope that identifies the current point of execution. In
  * practice this means the current server on distributed or server region on 390.
  *
  * @return FailureScope The current FailureScope.
  */
  public FailureScope currentFailureScope();

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.addCallBack
  //------------------------------------------------------------------------------
  /**
  * Invoked to add a recovery log service callback object that is invoked whenever
  * recovery events occur.
  * 
  * The caller must supply an object that implements the RecoveryLogCallBack 
  * interface.
  *
  * @param callback The new callback object.
  */
  public void addCallBack(RecoveryLogCallBack callback);

  //------------------------------------------------------------------------------
  // Method: RecoveryDirector.getRecoveryLogConfiguration
  //------------------------------------------------------------------------------
  /**
  * Allows a client service to obtain the recovery log configuration object from
  * the configuration model for the associated failure scope. The recovery log
  * configuration object contains the physical location and size of both 
  * the transactions and cscopes logs.
  *
  * @param failureScope The failure scope for which recovery log configuration
  *                     is to be retrieved.
  *
  * @return Object The recovery log configuration.
  */
  public Object getRecoveryLogConfiguration(FailureScope failureScope);

  //------------------------------------------------------------------------------
  // Method: RecoveryDirectorImpl.getNonNullCurrentFailureScopeIDString
  //------------------------------------------------------------------------------
  /**
  * returns the value of clusterService.clusterToIdentity for the current 
  * failureScope, even if HA is not enabled.
  * 
  * @return String The clusterIdentityString for the current failure scope.
  */
  public String getNonNullCurrentFailureScopeIDString();

  //------------------------------------------------------------------------------
  // Method: RecoveryDirectorImpl.registerRecoveryEventListener
  //------------------------------------------------------------------------------
  /**
  * Register a <code>RecoveryEventListener</code> that will be notified of
  * various recovery events.
  * 
  * @param rel The new recovery event listener.
  */
  public void registerRecoveryEventListener(RecoveryEventListener rel); /* @MD19638A*/

  //------------------------------------------------------------------------------
  // Method: RecoveryDirectorImpl.isHAEnabled()
  //------------------------------------------------------------------------------
  /**
  * This method allows a client service to determine if High Availability support
  * has been enabled for the local cluster. 
  * 
  * @return boolean true if HA support is enabled, otherwise false.
  */
  public boolean isHAEnabled();
}

