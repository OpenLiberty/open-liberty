/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.models;

/**
 * A record class that tests all primitive components
 */
public record Primitive(
                boolean bool,
                byte b,
                char c,
                short s,
                int i,
                long l,
                float f,
                double d) {
}
