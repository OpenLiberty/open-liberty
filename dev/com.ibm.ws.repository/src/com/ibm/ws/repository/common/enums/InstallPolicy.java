/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.repository.common.enums;

/*
 * ------------------------------------------------------------------------------------------------
 * InstallPolicy Enum
 * ------------------------------------------------------------------------------------------------
 */
/**
 * The install policy for an asset. Set to WHEN_SATISFIED if the ESA should be installed if all
 * its dependencies are met and MANUAL if this should not auto install.
 */
public enum InstallPolicy {
    /**
     * The ESA should automatically install if it's provisioning capabilities are satisfied.
     */
    WHEN_SATISFIED,
    /**
     * The ESA should not automatically install
     */
    MANUAL
}