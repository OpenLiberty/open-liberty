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

package com.ibm.ws.repository.exceptions;

import com.ibm.ws.repository.connections.RepositoryConnection;

public class RepositoryHttpException extends RepositoryBackendIOException {

    private static final long serialVersionUID = 7084745358560323741L;
    private final int _httpRespCode;

    public RepositoryHttpException(String message, int httpRespCode, RepositoryConnection connection) {
        super(message, connection);
        _httpRespCode = httpRespCode;
    }

    /**
     * @return the _httpRespCode
     */
    public int get_httpRespCode() {
        return _httpRespCode;
    }

}
