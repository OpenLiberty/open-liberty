/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package test.jakarta.data.web.eclipselink;

import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static jakarta.data.repository.By.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static test.jakarta.data.web.Assertions.assertIterableEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import test.jakarta.data.web.Receipt;
import test.jakarta.data.web.eclipselink.Animal.ScientificName;

/**
 * For tests that only run on the built-in Persistence provider, which is
 * EclipseLink. The built-in provider is needed for record entities and
 * any repositories that specify a DataSource as their dataStore.
 */
@DataSourceDefinition(name = "java:app/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      properties = "createDatabase=create")
@Resource(name = "java:module/env/jdbc/DerbyDataSourceRef",
          lookup = "java:app/jdbc/DerbyDataSource")
@Resource(name = "java:module/env/data/DataStoreRef",
          lookup = "java:comp/DefaultDataSource")
@SuppressWarnings("serial")
@WebServlet("/DataEclipseLinkServlet")
public class DataEclipseLinkServlet extends FATServlet {
    private final long TIMEOUT_MINUTES = 2;

    @Inject
    Animals animals;

    @Inject
    Cylinders cylinders;

    @Inject
    Ratings ratings;

    @Inject
    Receipts receipts;

    /**
     * Prepopulate read only data, if any, that is needed by the tests here:
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    @Test //TODO
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29475")
    public void testFetchTypeDefault() {
        ratings.clear();

        Rating.Reviewer.Name name1 = new Rating.Reviewer.Name(//
                        "Rex", "TestFetchTypeDefault");
        Rating.Reviewer user1 = new Rating.Reviewer(//
                        name1, "rex@openliberty.io");
        Rating.Item toaster = new Rating.Item("toaster", 28.98f);
        Set<String> comments = Set.of("Burns everything.",
                                      "Often gets stuck.",
                                      "Bagels don't fit.");

        ratings.add(new Rating(1000, toaster, 2, user1, comments));

        Rating user1Rating = ratings.get(1000).orElseThrow();

        assertFalse("Expected comments to be populated when using fetch type eager",
                    user1Rating.comments().isEmpty());
        assertEquals("Expected comments to be populated when using fetch type eager",
                     comments,
                     user1Rating.comments());
    }

    /**
     * Find-and-delete returning a record.
     */
    @Test
    public void testFindAndDeleteRecord() {
        assertEquals(false, receipts.deleteByPurchaseId(600L).isPresent());

        receipts.save(new Receipt(600L, "C1510-13-600", 6.89f));

        Receipt r = receipts.deleteByPurchaseId(600L).orElseThrow();
        assertEquals(600L, r.purchaseId());
        assertEquals("C1510-13-600", r.customer());
        assertEquals(6.89f, r.total(), 0.001f);
    }

    /**
     * Find-and-delete returning multiple records.
     */
    @Test
    public void testFindAndDeleteRecords() {
        assertIterableEquals(Collections.EMPTY_SET,
                             receipts.discardFor("C1510-13-999"));

        receipts.save(new Receipt(909L, "C1510-13-999", 9.09f));
        receipts.save(new Receipt(900L, "C1510-13-900", 9.00f));
        receipts.save(new Receipt(999L, "C1510-13-999", 9.99f));
        receipts.save(new Receipt(990L, "C1510-13-999", 9.90f));

        Collection<Receipt> deleted = receipts.discardFor("C1510-13-999");

        assertEquals(deleted.toString(), 3, deleted.size());

        List<Receipt> list = deleted.stream()
                        .sorted(Comparator.comparing(Receipt::purchaseId))
                        .toList();

        Receipt r = list.get(0);
        assertEquals(909, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.09f, r.total(), 0.001f);

        r = list.get(1);
        assertEquals(990, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.90f, r.total(), 0.001f);

        r = list.get(2);
        assertEquals(999, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.99f, r.total(), 0.001f);

        deleted = receipts.discardFor("C1510-13-900");

        assertEquals(deleted.toString(), 1, deleted.size());

        r = deleted.iterator().next();
        assertEquals(900, r.purchaseId());
        assertEquals("C1510-13-900", r.customer());
        assertEquals(9.00f, r.total(), 0.001f);
    }

    /**
     * Find a record entity based on its embedded id, which is also a record.
     * This test covers finding and returning an entity in addition to
     * finding an entity to save/update/delete.
     */
    @Test
    public void testFindByEmbeddedId() {
        List<Animal> found = animals.findAll().toList();
        if (!found.isEmpty())
            animals.deleteAll(found);

        Animal redFox = animals.insert(Animal.of("red fox",
                                                 "Vulpes", "vulpes"));
        Animal grayFox = animals.insert(Animal.of("gray fox",
                                                  "Urocyon", "cinereoargenteus"));
        Animal foxSquirrel = animals.insert(Animal.of("Fox squirrel",
                                                      "Sciurus", "niger"));
        Animal graySquirrel = animals.insert(Animal.of("gray squirrel",
                                                       "Sciurus", "carolinensis"));
        Animal redSquirrel = animals.insert(Animal.of("red squirrel",
                                                      "Tamiasciurus", "hudsonicus"));

        assertEquals("red fox", redFox.commonName());
        assertEquals("Vulpes", redFox.id().genus());
        assertEquals("vulpes", redFox.id().species());
        assertEquals(1, redFox.version());

        assertEquals("Fox squirrel", foxSquirrel.commonName());
        assertEquals("Sciurus", foxSquirrel.id().genus());
        assertEquals("niger", foxSquirrel.id().species());
        assertEquals(1, foxSquirrel.version());

        // TODO enable once #29460 is fixed
        //assertEquals(List.of("Sciurus carolinensis",
        //                     "Sciurus niger"),
        //             animals.ofGenus("Sciurus")
        //                             .map(n -> n.genus() + ' ' + n.species())
        //                             .collect(Collectors.toList()));

        //ScientificName grayFoxId = new ScientificName("Urocyon", "cinereoargenteus");
        //grayFox = animals.findById(grayFoxId).orElseThrow();
        //assertEquals("gray fox", grayFox.commonName());
        //assertEquals("Urocyon", grayFox.id().genus());
        //assertEquals("cinereoargenteus", grayFox.id().species());

        //ScientificName graySquirrelId = new ScientificName("Sciurus", "carolinensis");
        //graySquirrel = animals.findById(graySquirrelId).orElseThrow();
        //assertEquals("gray squirrel", graySquirrel.commonName());
        //assertEquals("Sciurus", graySquirrel.id().genus());
        //assertEquals("carolinensis", graySquirrel.id().species());

        //foxSquirrel = foxSquirrel.withCommonName("FOX SQUIRREL");
        //foxSquirrel = animals.save(foxSquirrel);
        //assertEquals("FOX SQUIRREL", foxSquirrel.commonName());
        //assertEquals("Sciurus", foxSquirrel.id().genus());
        //assertEquals("niger", foxSquirrel.id().species());
        //assertEquals(2, foxSquirrel.version());

        //foxSquirrel = foxSquirrel.withCommonName("fox squirrel");
        //foxSquirrel = animals.update(foxSquirrel);
        //assertEquals("fox squirrel", foxSquirrel.commonName());
        //assertEquals("Sciurus", foxSquirrel.id().genus());
        //assertEquals("niger", foxSquirrel.id().species());
        //assertEquals(3, foxSquirrel.version());

        animals.deleteById(new ScientificName("Sciurus", "niger"));

        assertEquals(4L, animals.countByIdNotNull());

        animals.delete(redSquirrel);

        assertEquals(false, animals.existsById(redSquirrel.id()));

        //found = animals.findAll().toList(); TODO replace next line
        found = List.of(redFox, grayFox, graySquirrel);
        assertEquals(found.toString(), 3, found.size());
        animals.deleteAll(found);
    }

    /**
     * Use a repository that has multiple embeddable attributes of the same type.
     */
    @Test
    public void testMultipleEmbeddableAttributesOfSameType() {
        Cylinder cyl1, cyl2, cyl3, cyl4, cyl5;

        //                                    Id     a.x, a.y, b.x, b.y, c.x, c.y
        cylinders.upsert(cyl1 = new Cylinder("CYL1", 100, 287, 372, 833, 509, 424),
                         cyl2 = new Cylinder("CYL2", 790, 857, 942, 143, 509, 424),
                         cyl3 = new Cylinder("CYL3", 340, 101, 100, 919, 629, 630),
                         cyl4 = new Cylinder("CYL4", 100, 684, 974, 516, 453, 163),
                         cyl5 = new Cylinder("CYL5", 412, 983, 276, 413, 629, 630));

        assertEquals(5, cylinders.countValid());

        assertEquals(List.of(cyl5.toString(), cyl3.toString()),
                     cylinders.centeredAt(629, 630)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(cyl2.toString(), cyl1.toString()),
                     cylinders.centeredAt(509, 424)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(cyl3.toString(), cyl1.toString(), cyl4.toString()),
                     cylinders.findBySideAXOrSideBXOrderBySideBYDesc(100, 100)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(Long.valueOf(5), cylinders.eraseAll());
    }

    /**
     * Tests all BasicRepository methods with a record as the entity.
     */
    @Test
    public void testRecordBasicRepositoryMethods() {
        receipts.deleteByTotalLessThan(1000000.0f);

        receipts.save(new Receipt(100L, "C0013-00-031", 101.90f));
        receipts.saveAll(List.of(new Receipt(200L, "C0022-00-022", 202.40f),
                                 new Receipt(300L, "C0013-00-031", 33.99f),
                                 new Receipt(400L, "C0045-00-054", 44.49f),
                                 new Receipt(500L, "C0045-00-054", 155.00f)));

        assertEquals(true, receipts.existsByPurchaseId(300L));
        assertEquals(5L, receipts.count());

        Receipt receipt = receipts.findById(200L).orElseThrow();
        assertEquals(202.40f, receipt.total(), 0.001f);

        assertIterableEquals(List.of("C0013-00-031:300",
                                     "C0022-00-022:200",
                                     "C0045-00-054:500"),
                             receipts.findByPurchaseIdIn(List.of(200L, 300L, 500L))
                                             .map(r -> r.customer() + ":" +
                                                       r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteByPurchaseIdIn(List.of(200L, 500L));

        assertIterableEquals(List.of("C0013-00-031:100",
                                     "C0013-00-031:300",
                                     "C0045-00-054:400"),
                             receipts.findAll()
                                             .map(r -> r.customer() + ":" +
                                                       r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteById(100L);

        assertEquals(2L, receipts.count());

        receipts.delete(new Receipt(400L, "C0045-00-054", 44.49f));

        assertEquals(false, receipts.existsByPurchaseId(400L));

        receipts.saveAll(List.of(new Receipt(600L, "C0067-00-076", 266.80f),
                                 new Receipt(700L, "C0067-00-076", 17.99f),
                                 new Receipt(800L, "C0088-00-088", 88.98f)));

        receipts.deleteAll(List.of(new Receipt(300L, "C0013-00-031", 33.99f),
                                   new Receipt(700L, "C0067-00-076", 17.99f)));

        assertEquals(2L, receipts.count());

        assertEquals(true, receipts.deleteByTotalLessThan(1000000.0f));

        assertEquals(0L, receipts.count());
    }

    /**
     * Tests all CrudRepository methods (apart from those inherited from
     * BasicRepository) with a record as the entity.
     */
    @SkipIfSysProp({
                     DB_Postgres, //Failing on Postgres due to eclipselink issue:  https://github.com/OpenLiberty/open-liberty/issues/28380
                     DB_SQLServer //Failing on SQLServer due to eclipselink issue: https://github.com/OpenLiberty/open-liberty/issues/28737
    })
    @Test
    public void testRecordCrudRepositoryMethods() {
        receipts.deleteByTotalLessThan(1000000.0f);

        Receipt r = receipts.insert(new Receipt(1200L, "C0002-12-002", 102.20f));
        assertNotNull(r);
        assertEquals(1200L, r.purchaseId());
        assertEquals("C0002-12-002", r.customer());
        assertEquals(102.20f, r.total(), 0.001f);

        List<Receipt> inserted = receipts.insertAll(List
                        .of(new Receipt(1300L, "C0033-13-003", 130.13f),
                            new Receipt(1400L, "C0040-14-004", 14.40f),
                            new Receipt(1500L, "C0005-15-005", 105.50f),
                            new Receipt(1600L, "C0006-16-006", 600.16f)));

        assertEquals(4, inserted.size());
        assertNotNull(r = inserted.get(0));
        assertEquals(1300L, r.purchaseId());
        assertEquals("C0033-13-003", r.customer());
        assertEquals(130.13f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(1));
        assertEquals(1400L, r.purchaseId());
        assertEquals("C0040-14-004", r.customer());
        assertEquals(14.40f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(2));
        assertEquals(1500L, r.purchaseId());
        assertEquals("C0005-15-005", r.customer());
        assertEquals(105.50f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(3));
        assertEquals(1600L, r.purchaseId());
        assertEquals("C0006-16-006", r.customer());
        assertEquals(600.16f, r.total(), 0.001f);

        try {
            receipts.insert(new Receipt(1200L, "C0002-10-002", 22.99f));
            fail("Inserted an entity with an Id that already exists.");
        } catch (EntityExistsException x) {
            // expected
        }

        // Ensure that the entity that already exists was not modified by insert
        r = receipts.findById(1200L).orElseThrow();
        assertEquals(1200L, r.purchaseId());
        assertEquals("C0002-12-002", r.customer());
        assertEquals(102.20f, r.total(), 0.001f);

        try {
            receipts.insertAll(List.of(new Receipt(1700L, "C0017-17-007", 177.70f),
                                       new Receipt(1500L, "C0055-15-005", 55.55f),
                                       new Receipt(1800L, "C0008-18-008", 180.18f)));
            fail("insertAll must fail when one of the entities has an Id" +
                 " that already exists.");
        } catch (EntityExistsException x) {
            // expected
        }

        // Ensure that insertAll inserted no entities when one had an Id
        // that already exists
        assertEquals(false, receipts.findById(1700L).isPresent());
        assertEquals(false, receipts.findById(1800L).isPresent());

        // Ensure that the entity that already exists was not modified by insertAll
        r = receipts.findById(1500L).orElseThrow();
        assertEquals(1500L, r.purchaseId());
        assertEquals("C0005-15-005", r.customer());
        assertEquals(105.50f, r.total(), 0.001f);

        // Update single entity that exists
        Receipt updated = receipts.update(new Receipt(1600L, "C0060-16-006", 600.16f));

        assertEquals("C0060-16-006", updated.customer());
        assertEquals(1600L, updated.purchaseId());
        assertEquals(600.16f, updated.total(), 0.001f);

        // Update multiple entities, if they exist
        try {
            receipts.updateAll(List.of(new Receipt(1400L, "C0040-14-044", 14.49f),
                                       new Receipt(1900L, "C0009-19-009", 199.99f),
                                       new Receipt(1200L, "C0002-12-002", 112.29f)));
            fail("Attempt to update multiple entities where one does not exist" +
                 " must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        List<Receipt> updates = receipts.updateAll(List
                        .of(new Receipt(1400L, "C0040-14-044", 14.41f),
                            new Receipt(1200L, "C0002-12-002", 112.20f)));
        Iterator<Receipt> updatesIt = updates.iterator();
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals(1400L, updated.purchaseId());
        assertEquals("C0040-14-044", updated.customer());
        assertEquals(14.41f, updated.total(), 0.001f);
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals(1200L, updated.purchaseId());
        assertEquals("C0002-12-002", updated.customer());
        assertEquals(112.20f, updated.total(), 0.001f);
        assertEquals(false, updatesIt.hasNext());

        // Verify the updates
        assertEquals(List.of(new Receipt(1200L, "C0002-12-002", 112.20f), // updated by updateAll
                             new Receipt(1300L, "C0033-13-003", 130.13f),
                             new Receipt(1400L, "C0040-14-044", 14.41f), // updated by updateAll
                             new Receipt(1500L, "C0005-15-005", 105.50f),
                             new Receipt(1600L, "C0060-16-006", 600.16f)), // updated by update
                     receipts.findAll()
                                     .sorted(Comparator.comparing(Receipt::purchaseId))
                                     .collect(Collectors.toList()));

        assertEquals(true, receipts.deleteByTotalLessThan(1000000.0f));

        assertEquals(0L, receipts.count());
    }

    /**
     * Tests that a record entity can be specified in the FROM clause of JDQL.
     */
    @Test
    public void testRecordInFromClause() {
        receipts.deleteByTotalLessThan(2000.0f);

        receipts.saveAll(List.of(new Receipt(2000L, "C2000-00-123", 20.98f),
                                 new Receipt(2001L, "C2000-00-123", 15.99f)));

        assertEquals(20.98f, receipts.totalOf(2000L), 0.001f);
        assertEquals(15.99f, receipts.totalOf(2001L), 0.001f);

        assertEquals(true, receipts.addTax(2001L, 0.0813f));

        assertEquals(17.29f, receipts.totalOf(2001L), 0.001f);

        assertEquals(2, receipts.removeIfTotalUnder(2000.0f));
    }

    /**
     * Use repository methods that have various return types for a record entity.
     */
    @Test
    public void testRecordReturnTypes() throws Exception {
        receipts.removeIfTotalUnder(1000000.0f);

        receipts.insertAll(List.of(new Receipt(3000L, "RRT10155", 100.98f),
                                   new Receipt(3001L, "RRT10155", 48.99f),
                                   new Receipt(3002L, "RRT20618", 12.98f),
                                   new Receipt(3003L, "RRT10155", 34.97f),
                                   new Receipt(3004L, "RRT10155", 4.15f),
                                   new Receipt(3005L, "RRT10155", 51.95f),
                                   new Receipt(3006L, "RRT20618", 629.99f),
                                   new Receipt(3007L, "RRT10155", 71.79f),
                                   new Receipt(3008L, "RRT20618", 8.98f),
                                   new Receipt(3009L, "RRT10155", 99.94f),
                                   new Receipt(3010L, "RRT10155", 10.49f),
                                   new Receipt(3011L, "RRT10155", 101.92f),
                                   new Receipt(3012L, "RRT20618", 12.99f),
                                   new Receipt(3013L, "RRT30033", 31.99f),
                                   new Receipt(3014L, "RRT10155", 434.99f),
                                   new Receipt(3015L, "RRT10155", 55.59f)));

        // various forms of completion stage results
        CompletableFuture<Receipt> futureResult = receipts.findByPurchaseId(3013L);
        CompletionStage<Optional<Receipt>> futureOptionalPresent = //
                        receipts.findIfPresentByPurchaseId(3014L);
        CompletionStage<Optional<Receipt>> futureOptionalMissing = //
                        receipts.findIfPresentByPurchaseId(3116L);
        CompletableFuture<List<Receipt>> futureList = //
                        receipts.forCustomer("RRT20618",
                                             Order.by(Sort.desc("total")));

        // single record
        Receipt receipt = receipts.withPurchaseNum(3015L);
        assertEquals("RRT10155", receipt.customer());
        assertEquals(55.59f, receipt.total(), 0.001f);

        // array of record
        Receipt[] array = receipts.forCustomer("RRT20618");
        assertEquals(Arrays.toString(array), 4, array.length);
        assertEquals(3002L, array[0].purchaseId());
        assertEquals(3006L, array[1].purchaseId());
        assertEquals(3008L, array[2].purchaseId());
        assertEquals(3012L, array[3].purchaseId());

        // page of record
        PageRequest pageReq = PageRequest.ofSize(5);

        Page<Receipt> page1 = receipts.forCustomer("RRT10155",
                                                   pageReq,
                                                   Sort.asc("total"));
        assertEquals(List.of(3004L, 3010L, 3003L, 3001L, 3005L),
                     page1.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        Page<Receipt> page2 = receipts.forCustomer("RRT10155",
                                                   page1.nextPageRequest(),
                                                   Sort.asc("total"));
        assertEquals(List.of(3015L, 3007L, 3009L, 3000L, 3011L),
                     page2.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        Page<Receipt> page3 = receipts.forCustomer("RRT10155",
                                                   page2.nextPageRequest(),
                                                   Sort.asc("total"));
        assertEquals(List.of(3014L),
                     page3.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        // cursored page of record
        PageRequest above3006 = PageRequest
                        .ofSize(3)
                        .afterCursor(Cursor.forKey(3006L));

        CursoredPage<Receipt> pageAbove3006 = //
                        receipts.forCustomer("RRT10155",
                                             above3006,
                                             Sort.asc("purchaseId"));
        assertEquals(List.of(3007L, 3009L, 3010L),
                     pageAbove3006.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageAbove3010 = //
                        receipts.forCustomer("RRT10155",
                                             pageAbove3006.nextPageRequest(),
                                             Sort.asc("purchaseId"));
        assertEquals(List.of(3011L, 3014L, 3015L),
                     pageAbove3010.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageBelow3007 = //
                        receipts.forCustomer("RRT10155",
                                             pageAbove3006.previousPageRequest(),
                                             Sort.asc("purchaseId"));
        assertEquals(List.of(3003L, 3004L, 3005L),
                     pageBelow3007.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageBelow3003 = //
                        receipts.forCustomer("RRT10155",
                                             pageBelow3007.previousPageRequest(),
                                             Sort.asc("purchaseId"));
        assertEquals(List.of(3000L, 3001L),
                     pageBelow3003.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        // completable future single result that was requested earlier
        assertEquals(31.99f,
                     futureResult.get(TIMEOUT_MINUTES, TimeUnit.MINUTES).total(),
                     0.001f);

        // completable future list of results that were requested earlier
        assertEquals(List.of(3006L, 3012L, 3002L, 3008L),
                     futureList.get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                                     .stream()
                                     .map(Receipt::purchaseId)
                                     .collect(Collectors.toList()));

        // completion stage optional result that was requested earlier
        Receipt r3014 = futureOptionalPresent.toCompletableFuture()
                        .get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                        .orElseThrow();
        assertEquals(3014L, r3014.purchaseId());
        assertEquals("RRT10155", r3014.customer());
        assertEquals(434.99f, r3014.total(), 0.001f);

        assertEquals(false, futureOptionalMissing.toCompletableFuture()
                        .get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                        .isPresent());

        assertEquals(1L, receipts.removeByPurchaseId(3000L));

        assertEquals(Set.of(3002L, 3010L, 3012L),
                     receipts.removeByTotalBetween(10.00f, 20.00f));

        // remove data to avoid interference with other tests
        assertEquals(12, receipts.removeIfTotalUnder(1000000.0f));
    }

    /**
     * Use a record entity that has embeddable attributes.
     */
    @Test
    public void testRecordWithEmbeddables() {
        ratings.clear();

        Rating.Reviewer.Name name1 = new Rating.Reviewer.Name(//
                        "Rex", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name2 = new Rating.Reviewer.Name(//
                        "Rhonda", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name3 = new Rating.Reviewer.Name(//
                        "Rachel", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name4 = new Rating.Reviewer.Name(//
                        "Ryan", "TestRecordWithEmbeddables");

        Rating.Reviewer user1 = new Rating.Reviewer(name1, "rex@openliberty.io");
        Rating.Reviewer user2 = new Rating.Reviewer(name2, "rhonda@openliberty.io");
        Rating.Reviewer user3 = new Rating.Reviewer(name3, "rachel@openliberty.io");
        Rating.Reviewer user4 = new Rating.Reviewer(name4, "ryan@openliberty.io");

        Rating.Item blender = new Rating.Item("blender", 41.99f);
        Rating.Item toaster = new Rating.Item("toaster", 28.98f);
        Rating.Item microwave = new Rating.Item("microwave", 63.89f);

        ratings.add(new Rating(1000, toaster, 2, user4, //
                        Set.of("Burns everything.",
                               "Often gets stuck.",
                               "Bagels don't fit.")));
        ratings.add(new Rating(1001, blender, 0, user4, //
                        Set.of("Broke after first use.")));
        ratings.add(new Rating(1002, microwave, 2, user4, //
                        Set.of("Uneven cooking.",
                               "Too noisy.")));
        ratings.add(new Rating(1003, microwave, 4, user3, //
                        Set.of("Good at reheating leftovers.")));
        ratings.add(new Rating(1004, microwave, 5, user2, //
                        Set.of()));
        ratings.add(new Rating(1005, microwave, 3, user1, //
                        Set.of("It works okay.")));
        ratings.add(new Rating(1006, toaster, 4, user1, //
                        Set.of("It toasts things.")));
        ratings.add(new Rating(1007, blender, 3, user1, //
                        Set.of("Too noisy.", "It blends things. Sometimes.")));
        ratings.add(new Rating(1008, blender, 5, user2, //
                        Set.of("Nice product!")));
        ratings.add(new Rating(1009, toaster, 5, user2, //
                        Set.of("Nice product!")));
        ratings.add(new Rating(1010, toaster, 3, user3, //
                        Set.of("Timer malfunctions on occasion, but it otherwise works.")));

        assertEquals(Set.of("Uneven cooking.", "Too noisy."),
                     ratings.getComments(1002));

        // TODO enable once EclipseLink bug #28589 is fixed
        // java.lang.IllegalArgumentException: An exception occurred while creating a query in EntityManager:
        // Exception Description: Problem compiling
        // [SELECT NEW test.jakarta.data.web.Rating(o.id, o.item, o.numStars, o.reviewer, o.comments)
        //  FROM RatingEntity o WHERE (o.item.price BETWEEN ?1 AND ?2) ORDER BY o.reviewer.email]. [78, 88]
        // The state field path 'o.comments' cannot be resolved to a collection type.
        //assertEquals(List.of("Rachel", "Rex", "Ryan"),
        //             ratings.findByItemPriceBetween(40.00f, 50.00f,
        //                                            Sort.asc("reviewer.email"))
        //                             .map(r -> r.reviewer().firstName())
        //                             .collect(Collectors.toList()));

        //assertEquals(List.of(1007, 1002),
        //             ratings.findByCommentsContainsOrderByIdDesc("Too noisy.")
        //                             .map(Rating::id)
        //                             .collect(Collectors.toList()));

        //assertEquals(List.of("toaster", "blender", "microwave"),
        //             ratings.search(3)
        //                             .map(r -> r.item().name)
        //                             .collect(Collectors.toList()));

        assertEquals(11L, ratings.clear());
    }

    /**
     * Tests JPQL find operation with a subquery, such that there are three FROM
     * clauses: one in the main query and two in subqueries.
     */
    @Test
    public void testSubqueryInFind() {
        receipts.removeIfTotalUnder(Float.MAX_VALUE);

        receipts.insertAll(List.of(new Receipt(6001, "TSIF-1", 41.99f),
                                   new Receipt(6002, "TSIF-2", 22.99f),
                                   new Receipt(6003, "TSIF-3", 23.99f),
                                   new Receipt(6004, "TSIF-4", 84.99f),
                                   new Receipt(6005, "TSIF-5", 95.99f)));

        assertEquals(List.of(6002L, 6003L, 6001L),
                     receipts.withBelowAverageTotal()
                                     .map(Receipt::purchaseId)
                                     .collect(Collectors.toList()));

        assertEquals(5L,
                     receipts.removeIfTotalUnder(Float.MAX_VALUE));
    }

    /**
     * Tests JPQL DELETE with a subquery, such that there are two FROM clauses:
     * one in the main query and one in the subquery.
     */
    @Test
    public void testSubqueryInDelete() {
        receipts.removeIfTotalUnder(Float.MAX_VALUE);

        receipts.insertAll(List.of(new Receipt(7001, "TSID-1", 13.99f),
                                   new Receipt(7002, "TSID-2", 82.99f),
                                   new Receipt(7003, "TSID-3", 93.99f),
                                   new Receipt(7004, "TSID-4", 24.99f),
                                   new Receipt(7005, "TSID-5", 75.99f)));

        assertEquals(2L,
                     receipts.removeByBelowAverageTotal());

        assertEquals(3L,
                     receipts.removeIfTotalUnder(Float.MAX_VALUE));
    }

    /**
     * Tests JPQL UPDATE with a subquery, such that there are two FROM clauses:
     * one in the main query and one in the subquery.
     */
    @Test
    public void testSubqueryInUpdate() {
        receipts.removeIfTotalUnder(Float.MAX_VALUE);

        receipts.insertAll(List.of(new Receipt(8001, "TSIU-1", 61.98f),
                                   new Receipt(8002, "TSIU-2", 32.98f),
                                   new Receipt(8003, "TSIU-3", 23.98f),
                                   new Receipt(8004, "TSIU-4", 74.98f),
                                   new Receipt(8005, "TSIU-5", 65.98f)));

        assertEquals(3L,
                     receipts.increaseIfAboveAverageTotal(1.08f));

        Receipt r3 = receipts.findById(8003L).orElseThrow();
        assertEquals(23.98f, r3.total(), 0.01f);

        Receipt r4 = receipts.findById(8004L).orElseThrow();
        assertEquals(80.98f, r4.total(), 0.01f);

        assertEquals(5L,
                     receipts.removeIfTotalUnder(Float.MAX_VALUE));
    }

    /**
     * Obtain total counts of elements when various JDQL queries are supplied
     * that lack different optional clauses.
     */
    @Test
    public void testTotalCountsForQueriesWithSomeClausesOmitted() {
        receipts.removeIfTotalUnder(Float.MAX_VALUE);

        receipts.insertAll(List.of(new Receipt(5001, "TCFQWSCO-1", 51.01f),
                                   new Receipt(5002, "TCFQWSCO-2", 52.42f),
                                   new Receipt(5003, "TCFQWSCO-3", 50.33f),
                                   new Receipt(5004, "TCFQWSCO-2", 52.24f),
                                   new Receipt(5005, "TCFQWSCO-5", 56.95f)));

        Page<Receipt> page1;
        PageRequest page1req = PageRequest.ofSize(3).withTotal();

        // query with no clauses
        page1 = receipts.all(page1req, Order.by(Sort.asc(ID)));
        assertEquals(5, page1.totalElements());

        receipts.insert(new Receipt(5006, "TCFQWSCO-5", 56.56f));

        // query with FROM clause only
        page1 = receipts.all(page1req, Sort.desc(ID));
        assertEquals(6, page1.totalElements());

        receipts.insert(new Receipt(5007, "TCFQWSCO-7", 57.17f));

        // query with FROM clause only
        page1 = receipts.sortedByTotalIncreasing(page1req);
        assertEquals(7, page1.totalElements());
        assertEquals(5003, page1.iterator().next().purchaseId());

        receipts.insert(new Receipt(5008, "TCFQWSCO-8", 58.88f));

        // query with SELECT clause only
        Page<Float> amountsPage1;
        amountsPage1 = receipts.totals(page1req, Sort.asc(ID));
        assertEquals(8, amountsPage1.totalElements());

        receipts.insert(new Receipt(5009, "TCFQWSCO-9", 59.09f));

        // query with SELECT and ORDER BY clauses only
        amountsPage1 = receipts.totalsDecreasing(page1req);
        assertEquals(9, amountsPage1.totalElements());

        assertEquals(9, receipts.removeIfTotalUnder(Float.MAX_VALUE));
    }

}
