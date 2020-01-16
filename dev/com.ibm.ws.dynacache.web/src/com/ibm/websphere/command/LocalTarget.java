/*******************************************************************************
 * Copyright (c) 2000 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.command;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.command.CommandException;

import java.rmi.RemoteException;

/**
 * The LocalTarget class provides an implementation of the
 * CommandTarget interface. It provides an executeCommand() method
 * that runs the command locally, in the client's JVM.</p>
 * <p>
 * Applications that need to run commands remotely must override
 * this implementation.</p>
 * 
 * @ibm-api
 */
public class LocalTarget implements CommandTarget {
   private static final TraceComponent _tc = Tr.register(LocalTarget.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

  /**
   * Runs a command locally (in the client's JVM).
   *
   * @param command The targetable command to be run.
   * @return The command after it has been run.
   * If the hasOutputProperties() method on the command returns
   * <code>false</code>, this method can return null as a performance
   * optimization.
   * @exception RemoteException The superclass for all remote exceptions.
   * This implementation should never throw a remote exception.
   * @exception CommandException The superclass for all command exceptions.
   */
  public com.ibm.websphere.command.TargetableCommand
  executeCommand(com.ibm.websphere.command.TargetableCommand command)
  throws java.rmi.RemoteException, com.ibm.websphere.command.CommandException
  {
      if (_tc.isEntryEnabled()) Tr.entry(_tc, "executeCommand", command);

   try {
     command.performExecute();
   }
        catch (Exception ex) {
     //Avoid wrappering layers upon layers
     com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.websphere.command.LocalTarget.executeCommand", "52", this);
     if (ex instanceof java.rmi.RemoteException) {
      java.rmi.RemoteException remoteException = (java.rmi.RemoteException) ex;
      if (remoteException.detail != null) {
        throw new com.ibm.websphere.command.CommandException(remoteException.detail.getMessage(),remoteException.detail);
      }
     }
     throw new com.ibm.websphere.command.CommandException(ex.getMessage(),ex);
   }

   if (command.hasOutputProperties()) {
     return command;
   } else
     return null;
  }
}
