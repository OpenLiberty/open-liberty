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
package com.ibm.ws.cdi12.ejbdiscovery.none.ejbs;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 * A stateless bean which shouldn't be seen because it's in a .war with discovery-mode=none
 */
@Stateless
@LocalBean
public class StatelessBean {

}
