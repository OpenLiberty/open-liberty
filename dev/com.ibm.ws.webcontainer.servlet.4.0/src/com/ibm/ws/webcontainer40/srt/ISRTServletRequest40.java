/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.htmln *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt;

import javax.servlet.http.HttpServletMapping;

import com.ibm.ws.webcontainer.srt.ISRTServletRequest;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;

public interface ISRTServletRequest40 extends ISRTServletRequest {

    HttpServletMapping getCurrentHttpServletMapping(WebAppDispatcherContext40 dispatchContext);
}
