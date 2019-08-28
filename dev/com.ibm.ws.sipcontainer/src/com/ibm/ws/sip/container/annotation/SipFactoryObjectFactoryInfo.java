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
import javax.servlet.sip.SipFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;

@Component(service = { ObjectFactoryInfo.class, SipFactoryObjectFactoryInfo.class })
public class SipFactoryObjectFactoryInfo extends ObjectFactoryInfo {
   			
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
		return SipFactory.class;
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
		return SipFactoryObjectFactory.class;
	}
}
