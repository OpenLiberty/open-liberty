/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.work;

/**
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates.
 *         To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class WorkRuntimeException extends RuntimeException {

    public WorkRuntimeException() {
        super();
    }

    public WorkRuntimeException(String msg) {
        super(msg);
    }

    public WorkRuntimeException(String msg, Throwable t) {
        super(msg, t);
    }

    public WorkRuntimeException(Throwable t) {
        super(t);
    }
}
