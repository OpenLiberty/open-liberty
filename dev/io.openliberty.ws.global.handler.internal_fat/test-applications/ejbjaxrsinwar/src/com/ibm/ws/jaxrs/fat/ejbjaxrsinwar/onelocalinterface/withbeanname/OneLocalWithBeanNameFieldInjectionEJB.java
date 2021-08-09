/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname;

import javax.ejb.Local;
import javax.ejb.Stateless;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.LocalInterfaceEJBWithJAXRSFieldInjectionResource;

@Stateless(name = "MyOneLocalWithBeanNameFieldInjectionEJB")
@Local(value = OneLocalWithBeanNameFieldInjectionView.class)
public class OneLocalWithBeanNameFieldInjectionEJB extends
                LocalInterfaceEJBWithJAXRSFieldInjectionResource {

}
