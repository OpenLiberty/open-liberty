/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.datastore.ejb;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class EJBModuleDSDEntity {

    @Id
    public int id;

    public String value;

    public static EJBModuleDSDEntity of(int id, String value) {
        EJBModuleDSDEntity entity = new EJBModuleDSDEntity();
        entity.id = id;
        entity.value = value;
        return entity;
    }

    @Override
    public String toString() {
        return "EJBModuleDSDEntity id=" + id + "; value=" + value;
    }
}
