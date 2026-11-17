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
package com.ibm.ws.jpa.olgh10515.model;

import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SimpleEntityOLGH10515.class)
public class SimpleEntityOLGH10515_ {
    public static volatile SingularAttribute<SimpleEntityOLGH10515, String> id;
    public static volatile SingularAttribute<SimpleEntityOLGH10515, Integer> version;
    public static volatile MapAttribute<SimpleEntityOLGH10515, String, String> origin;
}