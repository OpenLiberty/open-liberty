/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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

/**
 * For testing entity attribute names with reserved keywords in them.
 */
public class Thing {

    // single character attribute name, for testing the difference between findByALike and findByAlike
    public String a;

    // ending with "like" does not conflict with reserved word "Like" due to case difference.
    public Boolean alike;

    // starting with reserved word "And" is okay in at least some cases.
    // For example, "Android" in the method findByBrandOrNotesContainsOrAndroid
    public boolean android;

    // ending with "and" does not conflict with reserved word "And" due to case difference.
    public String brand;

    // starting with reserved word "Desc" is okay (test this with OrderBy)
    public String description;

    // ending with "or" does not conflict with reserved word "Or" due to case difference.
    public Integer floor;

    // starting with reserved word "In" is okay
    public String info;

    // starting with reserved word "Not" is okay
    public String notes;

    // Due to reserved word "Or" within attribute name, this cannot be used in query by method name.
    // Test with @Query instead.
    public int purchaseOrder;

    // starting with reserved word "Or" is okay in at least some cases.
    // For example, "OrderNumber" in the method findByFloorNotAndInfoLikeAndOrderNumberLessThan
    public long orderNumber;

    // The algorithm that infers IDs should choose this as the ID over "android" (ends with "id")
    // because ending with "Id" has higher precedence than ending with "id".
    public long thingId;

    public Thing() {
    }

    public Thing(long thingId, String a, Boolean alike,
                 boolean android, String brand, String description,
                 Integer floor, String info, String notes,
                 int purchaseOrder, long orderNumber) {
        this.a = a;
        this.alike = alike;
        this.android = android;
        this.brand = brand;
        this.description = description;
        this.floor = floor;
        this.info = info;
        this.notes = notes;
        this.purchaseOrder = purchaseOrder;
        this.orderNumber = orderNumber;
        this.thingId = thingId;
    }
}
