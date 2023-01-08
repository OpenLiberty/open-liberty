/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.actions;

public class JwtBuilderClaimRepeatActions {

    public static final String CollectionID = "Collection";
    public static final String SingleID = "Single";

    public static RunAsCollection asCollection() {

        return new RunAsCollection(true, CollectionID);
    }

    public static RunAsCollection asSingle() {

        return new RunAsCollection(false, SingleID);
    }
}
