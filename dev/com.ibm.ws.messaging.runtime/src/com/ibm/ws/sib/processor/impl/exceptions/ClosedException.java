/*
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.ws.sib.processor.impl.exceptions;


/**
 * The Component being called is closed
 */
public class ClosedException extends Exception {
    private static final long serialVersionUID = -7787063452957945077L;

    public ClosedException() {}

    public ClosedException(Throwable arg) {
        super(arg);
    }

    @Deprecated
    public ClosedException(String arg) {
        super(arg);
    }
}
