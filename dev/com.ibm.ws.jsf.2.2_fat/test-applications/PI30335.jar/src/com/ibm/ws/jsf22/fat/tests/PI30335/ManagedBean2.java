/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests.PI30335;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;

@ManagedBean(name="managedBean2")
public class ManagedBean2 {
	
	private static Logger LOGGER = Logger.getLogger(ManagedBean2.class.getSimpleName());
	
	public ManagedBean2() {
	}
	
	@PostConstruct
	public void init() {
		LOGGER.info("@PostConstruct invoked: " + this.getClass().getSimpleName());
	}
	
	

}
