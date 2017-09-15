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
import javax.inject.Inject;

public class SuperClass {

	@Inject
	@BatchProperty(name="numRecords")
	protected String numberRecords;

	@Inject
	@BatchProperty
	private String privateSuper;

	@Inject
	@BatchProperty
	protected String ibmBatch;

	/**
	 * @return the privateSuper
	 */
	public String getPrivateSuper() {
		return privateSuper;
	}

	/**
	 * @return the ibmBatch
	 */
	public String getIbmBatch() {
		return ibmBatch;
	}

	/**
	 * @return the numRecords
	 */
	public String getNumRecords() {
		return numberRecords;
	}

}
