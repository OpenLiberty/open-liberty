/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;

import java.util.Collection;
import java.util.Enumeration;

import javax.servlet.http.HttpServletResponse;
/**
 * 
 * StoredResponseCompat is a interface to allow both return types to co-exist for getHeaderNames
 * 
 * @ibm-api
 */
public interface StoredResponseCompat <T extends Enumeration<String> & Collection<String>> extends HttpServletResponse {
    @Override
    public T getHeaderNames();
}
