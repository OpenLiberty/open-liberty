/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.handler.fat;

/**
 *
 */
public interface Status {

    int OK = 200;
    int NO_CONTENT = 204;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
}
