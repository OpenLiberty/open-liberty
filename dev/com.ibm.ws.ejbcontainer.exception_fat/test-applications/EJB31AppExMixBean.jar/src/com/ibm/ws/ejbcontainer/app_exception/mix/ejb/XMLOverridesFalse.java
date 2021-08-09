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

package com.ibm.ws.ejbcontainer.app_exception.mix.ejb;

import javax.ejb.ApplicationException;

@ApplicationException(inherited = false, rollback = false)
public class XMLOverridesFalse extends RuntimeException {
    private static final long serialVersionUID = 4559777203371348081L;
}
