/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.jmx;

import java.io.Serializable;

/**
 * Indicates that a JMX-related failure occurred; Typically wraps another exception with more specific problem analysis
 * 
 * @author Tim Burns
 */
public class JmxException extends Exception implements Serializable {

    private static final long serialVersionUID = 1L;

    public JmxException() {
        super();
    }

    public JmxException(String message) {
        super(message);
    }

    public JmxException(String message, Throwable cause) {
        super(message, cause);
    }

    public JmxException(Throwable cause) {
        super(cause);
    }

}
