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
package test.jakarta.data.jpa.web;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;

/**
 * The only purpose of this repository is to make the Jakarta Data provider aware
 * of the existence of the WorkAddress entity as a subtype of the ShippingAddress entity.
 * TODO is there a way to figure this out automatically?
 */
@Repository
public interface WorkAddresses extends DataRepository<WorkAddress, Long> {
}
