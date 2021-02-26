/*******************************************************************************
 * Copyright (c) 2020 IBM Corpo<ration and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal.test;

/**
 *
 */
public class TestAppException extends Exception {

    /**  */
    private static final long serialVersionUID = 1L;

    private int httpStatusCode;

    /**
     * @return the httpStatusCode
     */
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    /**
     * @param httpStatusCode
     */
    public TestAppException(int httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
    }

}
