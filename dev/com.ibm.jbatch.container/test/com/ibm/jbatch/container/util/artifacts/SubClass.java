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
package com.ibm.jbatch.container.util.artifacts;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class SubClass extends BaseClass {

	@Inject
	@BatchProperty
	String noReference = "default";
	

	/**
	 * @return the noReference
	 */
	public String getNoReference() {
		return noReference;
	}

	/**
	 * @return the stepCtx
	 */
	public StepContext getStepCtx() {
		return stepCtx;
	}

	@Inject
	StepContext stepCtx;
}
