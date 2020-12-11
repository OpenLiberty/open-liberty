/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.common.util;

public class TestFailureException extends Exception {

    /**  */
    private static final long serialVersionUID = 1L;

    public TestFailureException() {
        // TODO Auto-generated constructor stub
    }

    public TestFailureException(String message) {
        super(message);
    }

    public TestFailureException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public TestFailureException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

}