/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.auth;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.intfc.SubjectManagerService;

public class WSSubjectHelper {
	public static void setup(ComponentContext ctx, ServiceReference<SubjectManagerService> ref) {
		WSSubject wss = new WSSubject();
		wss.activate(ctx);
		wss.setSubjectManagerService(ref);
	}
}
