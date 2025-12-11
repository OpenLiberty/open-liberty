/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simple entity with a UUID for an id, and a version
 */
public class Versioned {

    public UUID identifier;

    public String lastName;
    public String firstName;

    public LocalDateTime version;
}
