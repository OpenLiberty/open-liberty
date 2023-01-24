/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.config.web;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 *
 */
@MappedSuperclass
public abstract class Person {
    public int age;

    @Column(nullable = true)
    public String firstName;

    @Column(nullable = false)
    public String lastName;

}
