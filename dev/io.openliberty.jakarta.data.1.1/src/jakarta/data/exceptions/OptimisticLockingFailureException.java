/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.exceptions;

/**
 * copied from Jakarta Data git repository
 */
public class OptimisticLockingFailureException extends DataException {
    private static final long serialVersionUID = 1982179693469903341L;

    public OptimisticLockingFailureException(String message) {
        super(message);
    }

    public OptimisticLockingFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimisticLockingFailureException(Throwable cause) {
        super(cause);
    }
}
