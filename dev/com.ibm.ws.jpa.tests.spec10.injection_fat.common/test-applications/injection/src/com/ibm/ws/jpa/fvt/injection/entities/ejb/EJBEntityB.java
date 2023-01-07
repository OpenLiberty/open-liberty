/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.injection.entities.ejb;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.injection.entities.core.CoreInjectionEntity;

@Entity
@DiscriminatorValue("EJBEntityB")
public class EJBEntityB extends CoreInjectionEntity {

}
