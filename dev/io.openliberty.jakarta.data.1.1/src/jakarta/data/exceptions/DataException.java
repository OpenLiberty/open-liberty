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

import jakarta.annotation.Nullable;

/**
 * Method signatures copied from jakarta.data.DataException.
 */
public class DataException extends RuntimeException {
    private static final long serialVersionUID = 468278092602073093L;

    public DataException(@Nullable String message) {
        super(message);
    }

    public DataException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public DataException(@Nullable Throwable cause) {
        super(cause);
    }
}
