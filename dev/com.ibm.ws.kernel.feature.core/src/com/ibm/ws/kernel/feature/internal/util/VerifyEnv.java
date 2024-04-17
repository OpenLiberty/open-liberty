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
    String REPO_PROPERTY_NAME = "featureVerify_repo";
    String REPO_FILE_NAME = System.getenv(REPO_PROPERTY_NAME);

    String RESULTS_SINGLETON_PROPERTY_NAME = "featureVerify_results_singleton";
    String RESULTS_SINGLETON_FILE_NAME = System.getenv(RESULTS_SINGLETON_PROPERTY_NAME);

    String DURATIONS_SINGLETON_PROPERTY_NAME = "featureVerify_durations_singleton";
    String DURATIONS_SINGLETON_FILE_NAME = System.getenv(DURATIONS_SINGLETON_PROPERTY_NAME);

    String RESULTS_SERVLET_PROPERTY_NAME = "featureVerify_results_servlet";
    String RESULTS_SERVLET_FILE_NAME = System.getenv(RESULTS_SERVLET_PROPERTY_NAME);

    String DURATIONS_SERVLET_PROPERTY_NAME = "featureVerify_durations_servlet";
    String DURATIONS_SERVLET_FILE_NAME = System.getenv(DURATIONS_SERVLET_PROPERTY_NAME);
}
