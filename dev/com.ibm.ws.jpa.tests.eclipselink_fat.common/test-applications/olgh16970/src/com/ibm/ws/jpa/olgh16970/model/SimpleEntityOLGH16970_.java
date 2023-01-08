/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jpa.olgh16970.model;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SimpleEntityOLGH16970.class)
public class SimpleEntityOLGH16970_ {
    public static volatile SingularAttribute<SimpleEntityOLGH16970, Long> id;
    public static volatile SingularAttribute<SimpleEntityOLGH16970, Integer> intVal1;
    public static volatile SingularAttribute<SimpleEntityOLGH16970, String> strVal1;
}
