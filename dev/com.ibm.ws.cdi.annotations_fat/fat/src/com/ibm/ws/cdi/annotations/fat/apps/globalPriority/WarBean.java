/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.globalPriority;

import static com.ibm.ws.cdi.annotations.fat.apps.globalPriority.lib.RelativePriority.LOW_PRIORITY;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;

import com.ibm.ws.cdi.annotations.fat.apps.globalPriority.lib.AbstractBean;

@Alternative
@Priority(LOW_PRIORITY)
@FromWar
public class WarBean extends AbstractBean {}
