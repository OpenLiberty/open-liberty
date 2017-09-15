/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
// Class: RecoveryLogComponentImpl
//------------------------------------------------------------------------------
/**
 * WAS implementation of the service interface required by the recovery
 * log component.
 */
public class RecoveryLogComponentImpl implements RecoveryLogComponent
{


   public RecoveryLogComponentImpl()
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "RecoveryLogComponentImpl",cs);

//       if (tc.isEntryEnabled()) Tr.exit(tc, "RecoveryLogComponentImpl",cs);
   }

   /**
    * Called to indicate that an IOException was received during a log write operation.
    * This allows the called log service to decide what action to take (eg terminate server).
    * @param fs FailureScope for which a failure was detected
    */
   public void logWriteFailed (FailureScope fs) {}

   /**
    * Query interface for configuration info.  Format of this may be product specific,
    * so calling service must interpret accordingly
    * @param fs FailureScope for which info is requested
    * @return object containing the requested config info
    */
   public Object getRecoveryLogConfig(FailureScope fs)
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "getRecoveryLogConfig",fs);

//       if (tc.isEntryEnabled()) Tr.exit(tc, "getRecoveryLogConfig",config);
       return null;
   }

   /**
    * Query interface for clustering info (WAS specific :-( )
    * @param fs FailureScope for which info is requested
    * @return Identity object for the given FailureScope
    */
   public Object clusterIdentity(FailureScope fs) 
   {
//      if (tc.isEntryEnabled()) Tr.entry(tc, "clusterIdentity", fs);

 
//      if (tc.isEntryEnabled()) Tr.exit(tc, "clusterIdentity",clusterIdentity);
      return null; 
   }



// Methods which should probably disappear and be replaced perhaps by
// using the RecoveryDirector callback mechanism ????

   public void enablePeerRecovery()
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "getRecoveryLogConfig");
//       if (tc.isEntryEnabled()) Tr.exit(tc, "getRecoveryLogConfig");

   }

   public void localRecoveryFailed()
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "localRecoveryFailed");

//       if (tc.isEntryEnabled()) Tr.exit(tc, "localRecoveryFailed");
   }

   public void deactivateGroup(FailureScope fs, int handoverDelay)
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "deactivateGroup",new Object[]{fs, new Integer(handoverDelay)});


//       if (tc.isEntryEnabled()) Tr.exit(tc, "deactivateGroup");
   }

   public void leaveGroup(FailureScope fs)
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "leaveGroup",fs);

//       if (tc.isEntryEnabled()) Tr.exit(tc, "leaveGroup");
   }

   public void terminateServer()
   {
//       if (tc.isEntryEnabled()) Tr.entry(tc, "terminateServer");

//       if (tc.isEntryEnabled()) Tr.exit(tc, "terminateServer");
   }

  //------------------------------------------------------------------------------
  // Method: RecoveryLogComponent.joinCluster(FailureScope fs)
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Internal utility method to join a "self-declare" style cluster for the associated
  * failure scope. The cluster identity can then be retrieved by client services and
  * associated with their end point references (eg IORs) to allow object to be 
  * relocated in the event or peer recovery.
  * </p>
  *
  * @param fs The target failure scope.
  * 
  * @return boolean True if the cluster was joined sucessfully otherwise false.
  */
   public boolean joinCluster(FailureScope fs)
  {
//    if (tc.isEntryEnabled()) Tr.entry(tc, "joinCluster",fs);

    boolean success = true;


//    if (tc.isEntryEnabled()) Tr.exit(tc, "joinCluster",new Boolean(success));
    return success;
  }


  //------------------------------------------------------------------------------
  // Method: RecoveryLogComponent.leaveCluster(FailureScope fs)
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Internal utility method to leave a "self-declare" style cluster for the associated
  * failure scope. The cluster identity is retrieved by client services and associated
  * with their end point references (eg IORs) to allow object to be relocated in the
  * event or peer recovery. If this server is instructed to stop processing recovery
  * for a given server, we must leave the corrisponding cluster.
  * </p>
  *
  * @param fs The target failure scope.
  * 
  * @return boolean True if the cluster was sucessfully left otherwise false.
  */
  public boolean leaveCluster(FailureScope fs)
  {
//    if (tc.isEntryEnabled()) Tr.entry(tc, "leaveCluster", fs);

    boolean success = true;


//    if (tc.isEntryEnabled()) Tr.exit(tc, "leaveCluster", new Boolean(success));
    return success;
  }


   public Object   getAffinityKey(FailureScope fs)
   {
//     if (tc.isEntryEnabled()) Tr.entry(tc, "getAffinityKey", fs);

//     if (tc.isEntryEnabled()) Tr.exit(tc, "getAffinityKey", identity);
     return null;
   }



   public String getNonNullCurrentFailureScopeIDString(String serverName) 
   {
//     if (tc.isEntryEnabled()) Tr.entry(tc, "getNonNullCurrentFailureScopeIDString");
 
  
//     if (tc.isEntryEnabled()) Tr.exit(tc, "getNonNullCurrentFailureScopeIDString", value);
     return "";
   }


   /**
    * Create a unique (perhaps based on uuid) token to identify suspend call
    * @param bytes  serialized form of token for recreation (null indicates new token required)
    * @return requested token
    */
   public RLSSuspendToken createRLSSuspendToken(byte[] bytes)
   {
//     if (tc.isEntryEnabled()) Tr.entry(tc, "createRLSSuspendToken", bytes);
  
//     if (tc.isEntryEnabled()) Tr.exit(tc, "createRLSSuspendToken", token);
     return null;
   }


}
