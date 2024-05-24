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
package io.openliberty.microprofile.openapi20.fat.cache.filter;

import org.eclipse.microprofile.openapi.OASFilter;

public class MyOASFilter implements OASFilter {

    // This filter doesn't actually do anything
    // But it *could* be reading anything from the environment
    // So if it's configured to be used, we can't cache the model
}
