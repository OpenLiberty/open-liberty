/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.cdi.weld;

import javax.enterprise.inject.spi.BeanManager;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.wsoc.injection.InjectionProvider12;
import com.ibm.ws.wsoc.injection.InjectionService12;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class ServiceManager implements InjectionService12 {

	private static final AtomicServiceReference<CDIService> cdiService = new AtomicServiceReference<CDIService>(
			"cdiService");

	private final AtomicServiceReference<InjectionEngine> injectionEngineSRRef = new AtomicServiceReference<InjectionEngine>(
			"injectionEngine");

	private final InjectionImpl injectionImpl = new InjectionImpl();

	/**
	 * DS method for activating this component.
	 * 
	 * @param context
	 */
	protected synchronized void activate(ComponentContext context) {
		cdiService.activate(context);
		injectionEngineSRRef.activate(context);
	}

	/**
	 * DS method for deactivating this component.
	 * 
	 * @param context
	 */
	protected synchronized void deactivate(ComponentContext context) {
		cdiService.deactivate(context);
		injectionEngineSRRef.deactivate(context);
	}

	protected void setCdiService(ServiceReference<CDIService> ref) {
		cdiService.setReference(ref);
	}

	protected void unsetCdiService(ServiceReference<CDIService> ref) {
		cdiService.unsetReference(ref);
	}

	protected void setInjectionEngine(ServiceReference<InjectionEngine> ref) {
		this.injectionEngineSRRef.setReference(ref);
	}

	protected void unsetInjectionEngine(ServiceReference<InjectionEngine> ref) {
		this.injectionEngineSRRef.unsetReference(ref);
	}

	public static BeanManager getCurrentBeanManager() {
		return cdiService.getServiceWithException().getCurrentBeanManager();
	}

	@Override
	public InjectionProvider12 getInjectionProvider() {
		return injectionImpl;
	}

}
