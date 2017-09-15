/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.store;

public class MessageEvents
{  
  public final static int PRE_COMMIT_ADD = 0;
  public final static int PRE_COMMIT_REMOVE = 1;
  public final static int POST_COMMIT_ADD = 2;
  public final static int POST_COMMIT_REMOVE = 3;
  public final static int POST_ROLLBACK_ADD = 4;
  public final static int POST_ROLLBACK_REMOVE = 5;
  public final static int UNLOCKED = 6;
  public final static int REFERENCES_DROPPED_TO_ZERO = 7;  
  public final static int PRE_PREPARE_TRANSACTION = 8;
  public final static int POST_COMMITTED_TRANSACTION = 9;  
  public final static int EXPIRY_NOTIFICATION = 10;
  public final static int COD_CALLBACK = 11;
  public final static int PRE_UNLOCKED = 12;
  
  //***Unused events***
  //public void eventCommitAdd(final Transaction transaction);
  //public void eventCommitRemove(final Transaction transaction);
  //public void eventCommitUpdate(final Transaction transaction);
  //public void eventRollbackAdd(final Transaction transaction);
  //public void eventRollbackRemove(final Transaction transaction);
  //public void eventPostCommitUpdate(Transaction transaction);
  //public void eventPostRollbackUpdate(Transaction transaction);
  //public void eventPrecommitUpdate(final Transaction transaction);
  //public void eventRestored();
  //public void eventRollbackUpdate(final Transaction transaction);
}
