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
// Class: RecoveryLogAccessControllerImpl
//------------------------------------------------------------------------------
/**
 * WAS implementation of the service interface required by the recovery
 * log component.
 */
public class RecoveryLogAccessControllerImpl implements AccessController
{
//  private static final TraceComponent tc = Tr.register(RecoveryLogAccessControllerImpl.class,
//      TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);


   /**
    * 
    */
   public Object doPrivileged(java.security.PrivilegedExceptionAction action) 
       throws  java.security.PrivilegedActionException
   {
//        if (tc.isEntryEnabled()) Tr.entry(tc, "doPrivileged", action);

//        Object result = com.ibm.ws.security.util.AccessController.doPrivileged(action);
       java.lang.SecurityManager sm = System.getSecurityManager(); 
       if (sm == null) { 
           try { 
               return action.run(); 
           } catch (java.lang.RuntimeException e) { 
               throw e; 
           } catch (Exception e) { 
               throw new java.security.PrivilegedActionException(e); 
           } 
       } else { 
           return java.security.AccessController.doPrivileged(action); 
       } 
//        if(tc.isEntryEnabled()) Tr.exit(tc, "doPrivileged", result);
//        return result;
   }

}
