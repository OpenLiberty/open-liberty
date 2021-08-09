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
package com.ibm.ws.repository.common.enums;

/**
 * License Enum
 *
 * This is an Enum of known license types. See
 * http://www-03.ibm.com/software/sla/sladb.nsf/viewbla/
 */
public enum LicenseType {
    IPLA, // International Program License Agreement - applies to warranted IBM programs
    ILAN, // International License Agreement for Non-Warranted Programs
    ILAE, // International License Agreement for Evaluation of Programs
    ILAR, // International License Agreement for Early Release of Programs
    UNSPECIFIED; // Samples are uploaded with an UNSPECIFIED license type
}