/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.injection.entities.earroot;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.injection.entities.core.CoreInjectionEntity;

@Entity
@DiscriminatorValue("EARROOTEntityA")
public class EARROOTEntityA extends CoreInjectionEntity {

}
