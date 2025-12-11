/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * Entity that has a many-to-many mapping with the Customer entity.
 */
@Entity
public class DeliveryLocation {
    public enum Type {
        BUSINESS,
        HOME
    }

    @ManyToMany(cascade = CascadeType.ALL)
    public Set<Customer> customers;

    @Id
    public long locationId;

    @Column
    public int houseNum;

    @Column
    public Street street;

    @Column
    public Type type;

    public DeliveryLocation() {
    }

    public DeliveryLocation(long id, int houseNum, Street street, Type type, Customer... customers) {
        this.locationId = id;
        this.houseNum = houseNum;
        this.street = street;
        this.type = type;
        this.customers = new LinkedHashSet<>();
        for (Customer c : customers) {
            this.customers.add(c);
            c.deliveryLocations.add(this);
        }
    }

    public void addCustomer(Customer customer) {
        customers.add(customer);
        customer.deliveryLocations.add(this);
    }

    public void removeCustomer(Customer customer) {
        customers.remove(customer);
        customer.deliveryLocations.remove(this);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("DeliveryLocation #")
                        .append(locationId)
                        .append(' ')
                        .append(type)
                        .append(' ')
                        .append(houseNum)
                        .append(' ')
                        .append(street);
        return s.toString();
    }
}
