/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

public interface VerifyEnv {
    String REPO_PROPERTY_NAME = "featureVerify.repo";
    String REPO_FILE_NAME = System.getProperty(REPO_PROPERTY_NAME);

    String RESULTS_PROPERTY_NAME = "featureVerify.results";
    String RESULTS_FILE_NAME = System.getProperty(RESULTS_PROPERTY_NAME);
}
