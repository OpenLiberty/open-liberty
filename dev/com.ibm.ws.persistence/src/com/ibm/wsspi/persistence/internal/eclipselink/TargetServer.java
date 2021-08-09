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
package com.ibm.wsspi.persistence.internal.eclipselink;

import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.platform.server.ServerPlatformBase;
import org.eclipse.persistence.sessions.DatabaseSession;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class TargetServer extends ServerPlatformBase {
     public TargetServer(DatabaseSession newDatabaseSession) {
          super(newDatabaseSession);
     }

     @Override
     public Class<?> getExternalTransactionControllerClass() {
          return TransactionController.class;
     }

     @Override
     public SessionLog getServerLog() {
          return new Log();
     }

}
