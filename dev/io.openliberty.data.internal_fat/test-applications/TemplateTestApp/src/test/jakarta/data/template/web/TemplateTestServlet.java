/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.template.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.Resource;
import jakarta.data.Entities;
import jakarta.data.Template;
import jakarta.data.exceptions.DataException;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@Entities({ House.class, Vehicle.class })
@WebServlet("/*")
public class TemplateTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final long TIMEOUT_MINUTES = 2L;

    @Inject
    Template template;

    @Resource
    private UserTransaction tran;

    /**
     * Unannotated entity with an attribute that is an embeddable type.
     */
    @Test
    public void testEmbeddable() {
        template.delete(House.class, "TestEmbeddable-304-2288-60");

        House h = new House();
        h.area = 1800;
        h.garage = new Garage();
        h.garage.area = 200;
        h.garage.door = new GarageDoor();
        h.garage.door.setHeight(8);
        h.garage.door.setWidth(10);
        h.garage.type = Garage.Type.Attached;
        h.kitchen = new Kitchen();
        h.kitchen.length = 15;
        h.kitchen.width = 12;
        h.lotSize = 0.19f;
        h.numBedrooms = 4;
        h.parcelId = "TestEmbeddable-304-2288-60";
        h.purchasePrice = 162000;
        h.sold = Year.of(2018);

        h = template.insert(h);

        Optional<House> found = template.find(House.class, "TestEmbeddable-304-2288-60");
        h = found.get();

        assertNotNull(h.kitchen);
        assertEquals(15, h.kitchen.length);
        assertEquals(12, h.kitchen.width);

        assertNotNull(h.garage);
        assertEquals(200, h.garage.area);
        assertEquals(Garage.Type.Attached, h.garage.type);

        assertNotNull(h.garage.door);
        assertEquals(8, h.garage.door.getHeight());
        assertEquals(10, h.garage.door.getWidth());

        template.delete(House.class, "TestEmbeddable-304-2288-60");
    }

    /**
     * Uses a template to insert, update, find, and delete entities.
     */
    @Test
    public void testEntity1() {
        // find none
        Optional<House> found = template.find(House.class, "001-203-401");
        assertEquals(true, found.isEmpty());

        // insert
        House h1 = new House();
        h1.area = 1500;
        h1.lotSize = 0.18f;
        h1.numBedrooms = 3;
        h1.parcelId = "001-203-401";
        h1.purchasePrice = 125000.00f;
        h1.sold = Year.of(2015);

        House h = template.insert(h1);

        assertEquals("001-203-401", h.parcelId);

        // update
        h1.numBedrooms = 4;
        h1.purchasePrice = 136000.00f;
        h1.sold = Year.of(2016);

        h = template.update(h1);

        assertEquals(1500, h.area);
        assertEquals(0.18f, h.lotSize, 0.001f);
        assertEquals(4, h.numBedrooms);
        assertEquals("001-203-401", h.parcelId);
        assertEquals(136000.00f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2016), h.sold);

        // insert multiple
        House h2 = new House();
        h2.area = 1200;
        h2.lotSize = 0.21f;
        h2.numBedrooms = 2;
        h2.parcelId = "001-203-402";
        h2.purchasePrice = 112000.00f;
        h2.sold = Year.of(2012);

        House h3 = new House();
        h3.area = 1300;
        h3.lotSize = 0.13f;
        h3.numBedrooms = 3;
        h3.parcelId = "001-203-403";
        h3.purchasePrice = 113000.00f;
        h3.sold = Year.of(2013);

        Iterable<House> inserted = template.insert(List.of(h2, h3));

        Iterator<House> i = inserted.iterator();
        assertEquals(true, i.hasNext());
        assertEquals(h2.parcelId, i.next().parcelId);
        assertEquals(true, i.hasNext());
        assertEquals(h3.parcelId, i.next().parcelId);
        assertEquals(false, i.hasNext());

        // attempt to insert duplicate
        try {
            h = template.insert(h3);
            fail("Inserted duplicate: " + h);
        } catch (DataException x) {
            // expected
            if (x.getCause() == null)
                throw x;
        }

        // find
        found = template.find(House.class, h1.parcelId);

        h = found.get();
        assertEquals(h1.area, h.area);
        assertEquals(h1.lotSize, h.lotSize, 0.001f);
        assertEquals(h1.numBedrooms, h.numBedrooms);
        assertEquals(h1.parcelId, h.parcelId);
        assertEquals(h1.purchasePrice, h.purchasePrice, 0.001f);
        assertEquals(h1.sold, h.sold);

        // update multiple
        h2.purchasePrice = 152000.00f;
        h2.sold = Year.of(2022);

        h1.purchasePrice = 191000.00f;
        h1.sold = Year.of(2019);

        Iterable<House> updated = template.update(List.of(h1, h2));

        Iterator<House> u = updated.iterator();
        assertEquals(true, u.hasNext());
        assertEquals(191000.00f, u.next().purchasePrice, 0.001f);
        assertEquals(true, u.hasNext());
        assertEquals(Year.of(2022), u.next().sold);
        assertEquals(false, u.hasNext());

        // delete
        template.delete(House.class, h1.parcelId);

        // find none
        found = template.find(House.class, h1.parcelId);
        assertEquals(true, found.isEmpty());

        // delete nothing
        template.delete(House.class, h1.parcelId);
    }

    /**
     * Use the same template to insert, update, find, and delete a different type of entity.
     */
    @Test
    public void testEntity2() {
        Vehicle v1 = new Vehicle();
        v1.make = "Honda";
        v1.model = "Accord";
        v1.numSeats = 5;
        v1.price = 26000f;
        v1.vinId = "TE201234567890001";

        Vehicle v2 = new Vehicle();
        v2.make = "Ford";
        v2.model = "F-150";
        v2.numSeats = 3;
        v2.price = 32000f;
        v2.vinId = "TE201234567890002";

        Vehicle v3 = new Vehicle();
        v3.make = "Toyota";
        v3.model = "Camry";
        v3.numSeats = 5;
        v3.price = 25000f;
        v3.vinId = "TE201234567890003";

        // insert multiple with time-to-live
        Iterable<Vehicle> inserted = template.insert(List.of(v1, v2, v3), Duration.of(150, ChronoUnit.SECONDS));
        Iterator<Vehicle> i = inserted.iterator();
        assertEquals(true, i.hasNext());
        assertEquals("Honda", i.next().make);
        assertEquals(true, i.hasNext());
        assertEquals(3, i.next().numSeats);
        assertEquals(true, i.hasNext());
        assertEquals(25000f, i.next().price, 0.001f);
        assertEquals(false, i.hasNext());

        // delete
        template.delete(Vehicle.class, v1.vinId);

        // find none
        Optional<Vehicle> found = template.find(Vehicle.class, v1.vinId);
        assertEquals(false, found.isPresent());

        // update
        v3.price += 500f;
        v3 = template.update(v3);
        assertEquals("TE201234567890003", v3.vinId);

        // find
        found = template.find(Vehicle.class, "TE201234567890003");
        assertEquals(true, found.isPresent());
        assertEquals(25500f, found.get().price, 0.001f);
    }

    /**
     * Intermix two different types of entities in same inserts and updates.
     */
    @Test
    public void testMixedEntities() {
        House h1 = new House();
        h1.area = 1900;
        h1.lotSize = 0.19f;
        h1.numBedrooms = 4;
        h1.parcelId = "111-222-333";
        h1.purchasePrice = 219000.00f;
        h1.sold = Year.of(2019);

        Vehicle v1 = new Vehicle();
        v1.make = "Nissan";
        v1.model = "Altima";
        v1.numSeats = 5;
        v1.price = 24000f;
        v1.vinId = "TME09876543210001";

        Iterable<Object> inserted = template.insert(List.of(h1, v1));
        Iterator<Object> i = inserted.iterator();
        assertEquals(true, i.hasNext());
        assertEquals(Year.of(2019), ((House) i.next()).sold);
        assertEquals(true, i.hasNext());
        assertEquals(24000f, ((Vehicle) i.next()).price, 0.001f);
        assertEquals(false, i.hasNext());

        h1.purchasePrice = 241000f;
        h1.sold = Year.of(2021);

        v1.price += 500f;

        Iterable<Object> updated = template.update(List.of(v1, h1));
        Iterator<Object> u = updated.iterator();
        assertEquals(true, u.hasNext());
        assertEquals(24500f, ((Vehicle) u.next()).price, 0.001f);
        assertEquals(true, u.hasNext());
        assertEquals(Year.of(2021), ((House) u.next()).sold);
        assertEquals(false, u.hasNext());
    }

    // TODO enable if we find a way to specify a lock timeout on a JPA merge (insert).
    private static final boolean testTimeToLiveIsDisabled = true;

    /**
     * Attempt overlapping insert of data with same primary key from multiple threads.
     * Expect one to time out and the other to succeed.
     */
    @Test
    public void testTimeToLive() throws ExecutionException, HeuristicMixedException, HeuristicRollbackException, //
                    IllegalStateException, InterruptedException, NotSupportedException, //
                    RollbackException, SecurityException, SystemException, TimeoutException {
        CountDownLatch timedOut = new CountDownLatch(1);

        House h1 = new House();
        h1.area = 1800;
        h1.lotSize = 0.17f;
        h1.numBedrooms = 3;
        h1.parcelId = "765-432-001";
        h1.purchasePrice = 141000.00f;
        h1.sold = Year.of(2014);

        House h2 = new House();
        h2.area = 1800;
        h2.lotSize = 0.17f;
        h2.numBedrooms = 3;
        h2.parcelId = "765-432-001";
        h2.purchasePrice = 142000.00f; // differs from previous here
        h2.sold = Year.of(2014);

        CompletableFuture<House> thread2future = CompletableFuture.supplyAsync(() -> {
            try {
                tran.begin();
                try {
                    if (testTimeToLiveIsDisabled)
                        throw new RuntimeException("Test is mostly disabled; Pretending to time out an operation that would be blocked in the database.");
                    House inserted = template.insert(h2, Duration.of(5, ChronoUnit.SECONDS));
                    timedOut.await(TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    return inserted;
                } catch (Throwable x) {
                    timedOut.countDown();
                    throw new CompletionException(x);
                } finally {
                    if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                        tran.rollback();
                    else
                        tran.commit();
                }
            } catch (HeuristicMixedException | HeuristicRollbackException | IllegalStateException | NotSupportedException | RollbackException | SecurityException
                            | SystemException x) {
                throw new CompletionException(x);
            }
        });

        House h = null;
        Throwable exception = null;

        tran.begin();
        try {
            h = template.insert(h1, Duration.of(5, ChronoUnit.SECONDS));
            timedOut.await(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (Throwable x) {
            timedOut.countDown();
            exception = x;
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        if (h == null) {
            h = thread2future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            assertEquals(h2.purchasePrice, h.purchasePrice, 0.001f);
            throw new CompletionException(exception);
        } else {
            assertEquals(h1.purchasePrice, h.purchasePrice, 0.001f);
            try {
                House duplicate = thread2future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
                fail("Unexpected second insert: " + duplicate);
            } catch (ExecutionException x) {
                // expected
            }
        }
    }
}
