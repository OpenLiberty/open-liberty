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
package io.openliberty.jpa.tests.jpa31.models;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Case3Entity.class)
public class Case3Entity_ {
    public static volatile SingularAttribute<Case3Entity, String> KeyString;
    public static volatile SingularAttribute<Case3Entity, String> itemString1;
    public static volatile SingularAttribute<Case3Entity, String> itemString2;
    public static volatile SingularAttribute<Case3Entity, String> itemString3;
    public static volatile SingularAttribute<Case3Entity, String> itemString4;
    public static volatile SingularAttribute<Case3Entity, Integer> itemInteger1;
}
