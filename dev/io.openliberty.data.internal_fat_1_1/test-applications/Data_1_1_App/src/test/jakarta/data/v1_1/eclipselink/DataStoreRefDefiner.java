/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.eclipselink;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Defines a resource reference to the default data source under the
 * java:app/env/data/dbref name so that Jakarta Data repositories
 * will use it.
 */
@ApplicationScoped
@Resource(name = "java:app/env/data/dbref",
          lookup = "java:comp/DefaultDataSource")
public class DataStoreRefDefiner {
}
