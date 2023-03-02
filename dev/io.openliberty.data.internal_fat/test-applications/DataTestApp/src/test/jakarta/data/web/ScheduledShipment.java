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
package test.jakarta.data.web;

import java.time.OffsetDateTime;

/**
 * Interface that is implemented by the Shipment entity that declares some of the same accessor methods.
 */
public interface ScheduledShipment {

    public String getDestination();

    public OffsetDateTime getOrderedAt();

    public void setDestination(String destination);

    public void setOrderedAt(OffsetDateTime orderedAt);
}
