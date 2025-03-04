/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import java.util.List;

import jakarta.data.Sort;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

@Repository
public interface Apartments {
    @Delete
    public List<Apartment> removeAll(); //Need to return List<Apartment> since this repository lacks a primary entity

    @Save
    public List<Apartment> saveAll(List<Apartment> entities);

    //Need to have a lifecycle methods for Apartment2 and Apartment3 otherwise
    //Apartment2 and Apartment3 will not be considered entities
    @Save
    public Apartment2 saveAll(Apartment2 entity);

    @Save
    public Apartment3 saveAll(Apartment3 entity);

    // Write queries using every possible persistence field name delimiter
    // https://jakarta.ee/specifications/data/1.0/jakarta-data-1.0#property-name-concatenation

    // Parameter Name (_)
    @Find
    public List<Apartment> findApartmentsByBedroomWidth(int quarters_width);

    // @By (_) (.)
    @Find
    public List<Apartment> findApartmentsByBedroom(@By("quarters_length") int length,
                                                   @By("quarters.width") int width);

    // @OrderBy (_) (.)
    @Find
    @OrderBy("quarters_length")
    public List<Apartment> findAllOrderByBedroomLength();

    @Find
    @OrderBy("quarters.width")
    public List<Apartment> findAllOrderByBedroomWidth();

    // Query (.)
    @Query("FROM Apartment WHERE quarters.length = ?1")
    public List<Apartment> findApartmentsByBedroomLength(int length);

    // Method Name (_) or no delimiter
    public List<Apartment> findByQuarters_Width(int width);

    public List<Apartment> findByQuartersLength(int length);

    // Sort (_) (.)
    @Find
    public List<Apartment> findAllSorted(Sort<?> sort);

    // Write query using mapped superclass field name
    @Find
    public List<Apartment> findByOccupied(@By("isOccupied") boolean state);

    // Write query using embeddable in a mapped superclass
    @Find
    @OrderBy("occupant.firstName")
    @OrderBy("APTID")
    public List<Apartment> findByOccupantLastNameOrderByFirstName(@By("occupant_lastName") String lastName);

    //  Write query using entity that has colliding non-delimited attribute name (quartersWidth)
    @OrderBy("occupant.firstName")
    public List<Apartment> findByQuartersWidth(int width);

    // Write queries using entities that have colliding delimited attribute names
    @Find
    public List<Apartment2> findAllCollidingEmbeddable();

    @Find
    public List<Apartment3> findAllCollidingSuperclass();
}
