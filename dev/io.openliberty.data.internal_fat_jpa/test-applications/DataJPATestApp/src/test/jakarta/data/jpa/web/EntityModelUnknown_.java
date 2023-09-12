/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import jakarta.data.repository.StaticMetamodel;

/**
 * Static metamodel for an non-JPA entity type to ignore.
 */
@StaticMetamodel(java.time.Period.class)
public class EntityModelUnknown_ {
    public static volatile String days;
    public static volatile String months;
    public static volatile String years = "do-not-replace-this";
}