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
package com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.ejbJar;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * This bean is here to ensure that the maskedClassEjb.jar contains at least one EJB
 * <p>
 * It is not part of the test.
 */
@Stateless
@LocalBean
public class SessionBean1 {

}
