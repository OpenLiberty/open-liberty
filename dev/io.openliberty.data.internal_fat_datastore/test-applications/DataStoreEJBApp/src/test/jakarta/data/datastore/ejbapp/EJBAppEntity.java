/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejbapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class EJBAppEntity {

    @Id
    public String testName;

    public int nameLength;

    public static EJBAppEntity of(String testName) {
        EJBAppEntity entity = new EJBAppEntity();
        entity.testName = testName;
        entity.nameLength = testName.length();
        return entity;
    }

    @Override
    public String toString() {
        return "EJBAppEntity testName=" + testName + "; nameLength=" + nameLength;
    }
}
