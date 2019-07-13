/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Authentication mechannism type enumeration
 *
 * A separate class because the compiler conflicts between ant and
 * eclipse are very annoying.
 */
@Trivial
public enum AuthType {
    SPNEGO, BASIC, CERT, NONE, UNKNOWN
};
