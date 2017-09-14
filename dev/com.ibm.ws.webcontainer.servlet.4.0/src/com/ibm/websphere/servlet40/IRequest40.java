/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet40;

import java.util.HashMap;

import com.ibm.websphere.servlet31.request.IRequest31;
import com.ibm.wsspi.http.HttpRequest;

/**
 *
 */
public interface IRequest40 extends IRequest31 {

    public HttpRequest getHttpRequest();

    public HashMap<String, String> getTrailers();

}
