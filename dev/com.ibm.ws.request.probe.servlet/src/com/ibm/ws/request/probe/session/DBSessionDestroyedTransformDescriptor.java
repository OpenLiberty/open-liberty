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

import com.ibm.wsspi.request.probe.bci.RequestProbeTransformDescriptor;
import com.ibm.wsspi.session.ISession;


public class DBSessionDestroyedTransformDescriptor implements RequestProbeTransformDescriptor {

    private static final String classToInstrument = "com/ibm/ws/session/SessionStatistics";
    private static final String methodToInstrument = "sessionDestroyed";
    private static final String descOfMethod = "(Lcom/ibm/wsspi/session/ISession;)V";
    private static final String requestProbeType = "websphere.session.dbSessionDestroyed";
	
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
		return true;
	}

	@Override
	public Object getContextInfo(Object instanceOfThisClass, Object methodArgs) {
		
		Object[] sessionArgs = (Object[]) methodArgs;
		ISession session = (ISession) sessionArgs[0];
        return session.getIStore().getId() ;

	}


}
