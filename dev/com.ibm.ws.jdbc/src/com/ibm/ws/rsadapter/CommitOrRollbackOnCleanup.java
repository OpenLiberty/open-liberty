/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.rsadapter;

/**
 * commitOrRollbackOnCleanup specifies the action to take when a connection is destroyed or
 * returned to the pool and a database local transaction (autocommit = false) might still be active.
 * If the database supports unit of work detection, this property is only applied when in a
 * database unit of work.
 */
public enum CommitOrRollbackOnCleanup {
    commit,
    rollback;
}
