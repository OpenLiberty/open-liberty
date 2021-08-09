/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
interface InternalWsResource extends WsResource, FFDCSelfIntrospectable {

    String getNormalizedPath();

    String getRawRepositoryPath();

    SymbolicRootResource getSymbolicRoot();
}
