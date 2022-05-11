/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import jakarta.enterprise.context.ApplicationScoped;

import io.openliberty.data.Data;

/**
 *
 */
@ApplicationScoped
@Data
public class PersonRepo {
    public String findByName(String firstName, String lastName) {
        return "defaultValue";
    }
}
