/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.timer.ExternTimerService;

@Component(service = { ObjectFactory.class, TimerServiceObjectFactory.class })
public class TimerServiceObjectFactory implements ObjectFactory{
		
	/** trace variable */
	private static final TraceComponent tc = Tr.register(TimerServiceObjectFactory.class);
	
	public Object getObjectInstance(Object obj, 
			Name name, 
			Context nameCtx,
			Hashtable<?, ?> environment) throws Exception {
		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	            Tr.debug(tc, "TimerServiceObjectFactory getInstance");
		return ExternTimerService.getInstance();
	}
	
}
