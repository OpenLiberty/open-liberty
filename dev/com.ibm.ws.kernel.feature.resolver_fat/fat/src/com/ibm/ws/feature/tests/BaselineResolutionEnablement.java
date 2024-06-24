/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.feature.tests;

public interface BaselineResolutionEnablement {
    boolean enableServletGeneration = false;
    boolean enableSingletonGeneration = false;

    boolean enableMicroProfile = false;
    boolean enableServlet = false;
    boolean enableSingleton = false;
    boolean enableVersionlessSingleton = true;
}
