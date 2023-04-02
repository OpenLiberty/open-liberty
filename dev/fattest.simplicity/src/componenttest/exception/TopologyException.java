/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package componenttest.exception;

public class TopologyException extends Exception {

    public TopologyException() {
        super();
    }

    public TopologyException(String message, Throwable cause) {
        super(message, cause);
    }

    public TopologyException(String message) {
        super(message);

    }

    public TopologyException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 1L;

}
