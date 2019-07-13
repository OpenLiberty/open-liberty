/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import java.lang.annotation.Annotation;

import javax.annotation.Resource;
import javax.naming.spi.ObjectFactory;
import javax.servlet.sip.TimerService;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;

@Component(service = { ObjectFactoryInfo.class, TimerServiceObjectFactoryInfo.class })
public class TimerServiceObjectFactoryInfo extends ObjectFactoryInfo {
		
	@Override
	/**
	 *  @see com.ibm.wsspi.injectionengine.ObjectFactoryInfo#getAnnotationClass()
	 */
	public Class<? extends Annotation> getAnnotationClass() {
		return Resource.class;
	}

	@Override
	/**
	 *  @see com.ibm.wsspi.injectionengine.ObjectFactoryInfo#getType()
	 */
	public Class<?> getType() {
		return TimerService.class;
	}

	@Override
	/**
	 *  @see com.ibm.wsspi.injectionengine.ObjectFactoryInfo#isOverrideAllowed()
	 */
	public boolean isOverrideAllowed() {
		return false;
	}

	@Override
	/**
	 *  @see com.ibm.wsspi.injectionengine.ObjectFactoryInfo#getObjectFactoryClass()
	 */
	public Class<? extends ObjectFactory> getObjectFactoryClass() {
		return TimerServiceObjectFactory.class;
	}

}
