/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

/**
 * Constants related to logging audit records.
 */
public interface AuditConstants {

    public static final String CREATE_AUDIT = "create";
    public static final String GET_AUDIT = "get";
    public static final String UPDATE_AUDIT = "update";
    public static final String DELETE_AUDIT = "delete";
    public static final String SEARCH_AUDIT = "search";

    public static final String VMMSERVICE = "vmmservice";
    public static final String URBRIDGE = "urbridge";

}
