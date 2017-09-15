/*******************************************************************************
 * Copyright (c) 2002, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.jtaextensions;

/**
 * An object implementing this interface is enlisted once through the 
 * {@link com.ibm.websphere.jtaextensions.ExtendedJTATransaction ExtendedJTATransaction}
 * interface and receives notification of the 
 * completion of each subsequent transaction mediated by the transaction 
 * manager in the local JVM. 
 * While this object may execute in a J2EE server, there is no specific J2EE 
 * component active when this object is called and so it has limited <i>direct</i> 
 * access to any J2EE resources.  Specifically, it has no access to the <code>java:</code> 
 * namespace or to any container-mediated resource.  It may cache a reference 
 * to J2EE component, for example a stateless SessionBean, that it delegates 
 * to. Such an EJB would then have all the normal access to J2EE 
 * resources and could be used, for example, to acquire a JDBC connection and 
 * flush updates to a database during <code>beforeCompletion</code>. 
 *
 * @ibm-api
 * @ibm-was-base
 * @ibm-user-implements
 * 
 */
public interface SynchronizationCallback
{

   /**
    *
    * Called before each transaction begins commit processing. Provides
    * an opportunity, for example, to flush data to a persistent store
    * prior to the start of two-phase commit.
    * This method is not called prior to a request to rollback or if the transaction 
    * has been marked rollbackOnly.  
    * The identity of the transaction about to complete is indicated through 
    * both the <code>globallId</code> and <code>localId</code>
    * (either of which can be used by the callback).  
    *
    * @param localId the process-unique id of the transaction about to complete.
    * @param globalId the global transaction identifier, derived from the
    *    <code>PropagationContext</code> of the global transaction
    *    of the transaction about to complete.
    *
    */
    void beforeCompletion(int localId, byte[] globalId);

   /**
    * Called after each transaction is completed.  The transaction is not active 
    * on the thread at this point.  The identity of the transaction just 
    * completed is indicated through both the <code>globallId</code> and <code>localId</code> 
    * (either of which can be used by an callback).  
    *
    * @param localId the process-unique id of the transaction just completed.
    * @param globalId the global transaction identifier, derived from the
    *    <code>PropagationContext</code> of the global transaction
    *    of the transaction just completed.
    * @param committed boolean that is <b>true</b> if the transaction outcome was
    *    committed or <b>false</b> otherwise.
    *
    */
   void afterCompletion(int localId, byte[] globalId, boolean committed);
}

