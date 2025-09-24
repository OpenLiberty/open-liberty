/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package test.jakarta.data.jpa.hibernate.web;

import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataJPAHibernateTestServlet extends FATServlet {

    @Inject
    Cities cities;

    @Resource
    UserTransaction tran;

    @Override
    public void init(ServletConfig config) throws ServletException {
        cities.save(new City("Rochester", "Minnesota", 121395, Set.of(507)));
        cities.save(new City("Rochester", "New York", 211328, Set.of(585)));
        cities.save(new City("Springfield", "Missouri", 169176, Set.of(417)));
        cities.save(new City("Springfield", "Illinois", 114394, Set.of(217, 447)));
        cities.save(new City("Springfield", "Massachusetts", 155929, Set.of(413)));
        cities.save(new City("Springfield", "Oregon", 59403, Set.of(458, 541)));
        cities.save(new City("Springfield", "Ohio", 58662, Set.of(326, 937)));
        cities.save(new City("Kansas City", "Missouri", 508090, Set.of(816, 975)));
        cities.save(new City("Kansas City", "Kansas", 156607, Set.of(913)));
    }

    @Test
    public void testGeneratedClassesAvailable() {
        try {
            Class.forName("test.jakarta.data.jpa.hibernate.web._City");
        } catch (ClassNotFoundException e) {
            fail("Static metamodel class _City (for Data) was not generated or available at runtime.");
        }

        try {
            Class.forName("test.jakarta.data.jpa.hibernate.web.City_");
        } catch (ClassNotFoundException e) {
            fail("Static metamodel class City_ (for Persistence) was not generated or available at runtime.");
        }

        try {
            Class.forName("test.jakarta.data.jpa.hibernate.web.Cities_");
        } catch (ClassNotFoundException e) {
            fail("Static repository class Cities_ (for Data) was not generated or available at runtime.");
        }
    }

    @Test
    public void testBasicRepositorySaveFindDelete() {
        City Milwaukee = new City("Milwaukee", "Wisconsin", 577222, Set.of(414));

        // save
        City saved = cities.save(Milwaukee);
        assertEquals(Milwaukee.areaCodes, saved.areaCodes);
        assertEquals(Milwaukee.name, saved.name);
        assertEquals(Milwaukee.stateName, saved.stateName);
        assertEquals(Milwaukee.population, saved.population);

        // findById
        City found = cities.findById(Milwaukee.getIdClass())
                        .orElseThrow(() -> new AssertionError("Could not find entity by id"));
        assertEquals(Milwaukee.areaCodes, found.areaCodes);
        assertEquals(Milwaukee.name, found.name);
        assertEquals(Milwaukee.stateName, found.stateName);
        assertEquals(Milwaukee.population, found.population);

        // delete
        cities.delete(Milwaukee);
        cities.findById(Milwaukee.getIdClass())
                        .ifPresent(city -> {
                            throw new AssertionError("Found entity after it was deleted: " + city.toString());
                        });
    }

    @Test
    public void testBasicRepositoryDeleteById() throws Exception {
        City GreenBay = new City("Green Bay", "Wisconsin", 107395, Set.of(920));
        cities.save(GreenBay);

        //NOTE: tran.begin() / tran.commit() requires hibernate property hibernate.allow_update_outside_transaction=true
        //see https://hibernate.atlassian.net/browse/HHH-18260
        tran.begin();
        cities.deleteById(GreenBay.getIdClass());
        tran.commit();

        cities.findById(GreenBay.getIdClass()).ifPresent(city -> {
            throw new AssertionError("Found entity after it was deleted: " + city.toString());
        });
    }

    @Test
    public void testBasicRepositorySaveAllFindAllDeleteAll() {
        List<City> sdCitiesCopy; //shallow copy of sdCitites
        final List<City> sdCities = List.of(
                                            new City("Sioux Falls", "South Dakota", 192517, Set.of(605)),
                                            new City("Rapid City", "South Dakota", 74703, Set.of(605)),
                                            new City("Brookings", "South Dakota", 23377, Set.of(605)),
                                            new City("Watertown", "South Dakota", 22655, Set.of(605)),
                                            new City("Spearfish", "South Dakota", 12193, Set.of(605)),
                                            new City("Aberdeen", "South Dakota", 28324, Set.of(605)),
                                            new City("Mitchell", "South Dakota", 15660, Set.of(605)),
                                            new City("Pierre", "South Dakota", 14091, Set.of(605)));

        // saveAll
        sdCitiesCopy = new ArrayList<>(sdCities);
        final List<City> savedCities = cities.saveAll(sdCities);
        for (City saved : savedCities) {
            assertTrue("Saved entity that was not an element of the original list: " + saved.toString(), sdCitiesCopy.remove(saved));
        }
        assertEquals(0, sdCitiesCopy.size());

        // findAll
        sdCitiesCopy = new ArrayList<>(sdCities);
        final List<City> foundCities = cities.findAll().filter(city -> city.stateName.equals("South Dakota")).toList();
        for (City found : foundCities) {
            assertTrue("Found entity that was not an element of the original list: " + found.toString(), sdCitiesCopy.remove(found));
        }
        assertEquals("Elements of original list were not found in database: " + sdCitiesCopy.toString(), 0, sdCitiesCopy.size());

        // deleteAll
        cities.deleteAll(sdCities);
        assertFalse(cities.findAll().anyMatch(city -> city.stateName.equals("South Dakota")));
    }

    @Test
    @SkipIfSysProp(DB_SQLServer) //TODO remove once https://hibernate.atlassian.net/browse/HHH-19713 is fixed
    public void testBasicRepositoryFindAllWithPages() {
        Queue<Integer> expectedPopulationOrder = new LinkedList<>();
        expectedPopulationOrder.offer(508090);
        expectedPopulationOrder.offer(211328);
        expectedPopulationOrder.offer(169176);
        expectedPopulationOrder.offer(156607);
        expectedPopulationOrder.offer(155929);
        expectedPopulationOrder.offer(121395);
        expectedPopulationOrder.offer(114394);
        expectedPopulationOrder.offer(59403);
        expectedPopulationOrder.offer(58662);

        PageRequest request = PageRequest.ofSize(3);
        Order<City> order = Order.by(_City.population.desc());

        Page<City> page = cities.findAll(request, order);

        // Page assertions
        assertEquals(9, page.totalElements());
        assertEquals(3, page.totalPages());

        // Order assertion
        do {
            page = cities.findAll(request, order);
            Iterator<City> it = page.iterator();
            while (it.hasNext()) {
                assertEquals("Incorrect order of results during pagination",
                             expectedPopulationOrder.poll().intValue(), it.next().population);
            }
        } while ((request = page.hasNext() ? page.nextPageRequest() : null) != null);

        // Completion assertion
        assertEquals("Incomplete set of results during pagination", 0, expectedPopulationOrder.size());
    }

}
