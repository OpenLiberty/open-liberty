/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.exceptions;

public class BadVersionException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String _minVersion;
    private final String _maxVersion;
    private final String _badVersion;

    public BadVersionException(String min, String max, String bad) {
        super();
        _minVersion = min;
        _maxVersion = max;
        _badVersion = bad;
    }

    public String getMinVersion() {
        return _minVersion;
    }

    public String getMaxVersion() {
        return _maxVersion;
    }

    public String getBadVersion() {
        return _badVersion;
    }
}
