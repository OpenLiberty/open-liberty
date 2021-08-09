/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

/**
 * @author skurz
 */
public class TopLevelStepInstanceKey extends StepThreadInstanceKey {

	private static final long serialVersionUID = 1L;

	public TopLevelStepInstanceKey(long topLevelJobInstanceId, String stepName) {
		super(topLevelJobInstanceId, stepName, TOP_LEVEL_THREAD);
	}
}
