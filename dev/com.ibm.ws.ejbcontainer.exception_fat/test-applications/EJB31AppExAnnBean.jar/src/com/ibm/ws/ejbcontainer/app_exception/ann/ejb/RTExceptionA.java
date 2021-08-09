/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.app_exception.ann.ejb;

import javax.ejb.ApplicationException;

@ApplicationException(inherited = true, rollback = true)
public class RTExceptionA extends RuntimeException {
    private static final long serialVersionUID = -2458907726738702157L;
}
