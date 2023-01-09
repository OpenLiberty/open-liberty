/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
public class EmptyResultException extends DataException {
    private static final long serialVersionUID = -4477636987874974491L;

    public EmptyResultException(String message) {
        super(message);
    }

    public EmptyResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyResultException(Throwable cause) {
        super(cause);
    }
}
