/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.probe.session;

import javax.servlet.http.HttpSession;

import com.ibm.ws.request.probe.bci.internal.RequestProbeConstants;
import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;


public class SessionGetAttributeTransformDescriptor implements RequestProbeTransformDescriptor {

    private static final String classToInstrument = "com/ibm/ws/session/http/HttpSessionImpl";
    private static final String methodToInstrument = "getAttribute";
    private static final String descOfMethod = "(Ljava/lang/String;)Ljava/lang/Object;";
    private static final String requestProbeType = "websphere.session.getAttribute";
	
	@Override
	public String getClassName() {
		return classToInstrument;
	}

	@Override
	public String getMethodName() {
		return methodToInstrument;
	}

	@Override
	public String getMethodDesc() {
		return descOfMethod;
	}

	@Override
	public String getEventType() {
		return requestProbeType;
	}
	
	@Override
	public boolean isCounter() {
		return false;
	}

	@Override
	public Object getContextInfo(Object instanceOfThisClass, Object methodArgs) {
		
		HttpSession session = (HttpSession)instanceOfThisClass;
		Object[] sessionArgs = (Object[]) methodArgs;
        return session.getId() + RequestProbeConstants.EVENT_CONTEXT_INFO_SEPARATOR + sessionArgs[0] ;

	}


}
