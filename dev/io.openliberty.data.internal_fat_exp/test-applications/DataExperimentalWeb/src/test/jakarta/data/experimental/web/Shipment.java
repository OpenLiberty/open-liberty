/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package test.jakarta.data.experimental.web;

import java.time.OffsetDateTime;

/**
 * A simple unannotated entity with public accessor methods.
 */
public class Shipment implements ScheduledShipment {

    public record Instructions(
                    String handlingRequirements,
                    String deliveryRequirements,
                    boolean needsSignature) {
    }

    private long id;

    private String destination;

    private Instructions instructions;

    private String location;

    private OffsetDateTime orderedAt, shippedAt, canceledAt, deliveredAt;

    private String status;

    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    public long getId() {
        return id;
    }

    public Instructions getInstructions() {
        return instructions;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public OffsetDateTime getOrderedAt() {
        return orderedAt;
    }

    public OffsetDateTime getShippedAt() {
        return shippedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setCanceledAt(OffsetDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    @Override
    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setInstructions(Instructions value) {
        this.instructions = value;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public void setOrderedAt(OffsetDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public void setShippedAt(OffsetDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
