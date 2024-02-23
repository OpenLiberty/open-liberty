/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.websphere.jsonsupport;

/**
 * The JSONMarshallException is thrown when an error is encountered when
 * marshalling or unmarshalling a POJO to or from JSON.
 */
public class JSONMarshallException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a JSONMarshallException with an explanation as to why the JSON
     * parse failed.
     *
     * @param msg Explanation of parse exception
     */
    public JSONMarshallException(final String msg) {
        super(msg);
    }

    /**
     * Creates a JSONMarshallException with an explanation and cause as to why
     * the JSON parse failed.
     *
     * @param msg Explanation of parse exception
     * @param t   The cause of the Exception
     */
    public JSONMarshallException(final String msg, final Throwable t) {
        super(msg, t);
    }

}
