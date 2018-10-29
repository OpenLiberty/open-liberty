/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Describes an operation that is performed with regard to establishing context on a thread.
 */
@Trivial
enum ContextOp {
    /**
     * Thread context of the specified type is cleared from the thread of execution
     * before performing the action/task.
     */
    CLEARED,

    /**
     * Thread context of the specified type is captured from the requesting thread
     * and propagated to the thread of execution before performing the action/task.
     */
    PROPAGATED,

    /**
     * Thread context of the specified type is ignored and left unchanged.
     */
    UNCHANGED
}
