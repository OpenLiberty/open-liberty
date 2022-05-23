/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataTestServlet extends FATServlet {

    @Inject
    PersonRepo people;

    @Inject
    ProductRepo products;

    @Resource
    private UserTransaction tran;

    /**
     * Delete multiple entries.
     */
    @Test
    public void testDeleteMultiple() {
        Product prod1 = new Product();
        prod1.id = "TDM-SE";
        prod1.name = "TestDeleteMultiple Standard Edition";
        prod1.price = 115.99f;
        products.insert(prod1);

        Product prod2 = new Product();
        prod2.id = "TDM-AE";
        prod2.name = "TestDeleteMultiple Advanced Edition";
        prod2.price = 197.99f;
        products.insert(prod2);

        Product prod3 = new Product();
        prod3.id = "TDM-EE";
        prod3.name = "TestDeleteMultiple Expanded Edition";
        prod3.price = 153.99f;
        products.insert(prod3);

        Product prod4 = new Product();
        prod4.id = "TDM-NFE";
        prod4.name = "TestDeleteMultiple Nearly Free Edition";
        prod4.price = 1.99f;
        products.insert(prod4);

        assertEquals(2, products.discontinueProducts(Set.of("TDM-AE", "TDM-NFE", "TDM-NOT-FOUND")));

        // expect that 2 remain
        assertNotNull(products.findItem("TDM-SE"));
        assertNotNull(products.findItem("TDM-EE"));
    }

    /**
     * Search for missing item. Insert it. Search again.
     */
    @Test
    public void testFindCreateFind() {
        assertEquals(null, products.findItem("OL306-233F"));

        Product prod = new Product();
        prod.id = "OL306-233F";
        prod.name = "Something";
        prod.price = 3.99f;
        prod.description = "An item for sale.";

        products.insert(prod);

        Product p = products.findItem("OL306-233F");
        assertEquals(prod.id, p.id);
        assertEquals(prod.name, p.name);
        assertEquals(prod.price, p.price, 0.001f);
        assertEquals(prod.description, p.description);
    }

    /**
     * Search for multiple entries.
     */
    @Test
    public void testFindMultiple() throws Exception {
        assertEquals(Collections.EMPTY_LIST, people.find("TestFindMultiple"));

        Person jane = new Person();
        jane.firstName = "Jane";
        jane.lastName = "TestFindMultiple";
        jane.ssn = 123456789;

        Person joe = new Person();
        joe.firstName = "Joe";
        joe.lastName = "TestFindMultiple";
        joe.ssn = 987654321;

        tran.begin();
        try {
            people.insert(jane);
            people.insert(joe);
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        List<Person> found = people.find("TestFindMultiple");
        assertNotNull(found);
        assertEquals(2, found.size());

        Person p1 = found.get(0);
        Person p2expected;
        assertEquals("TestFindMultiple", p1.lastName);
        if (jane.firstName.equals(p1.firstName)) {
            assertEquals(jane.ssn, p1.ssn);
            p2expected = joe;
        } else {
            assertEquals(joe.ssn, p1.ssn);
            p2expected = jane;
        }

        Person p2 = found.get(1);
        assertEquals(p2expected.lastName, p2.lastName);
        assertEquals(p2expected.firstName, p2.firstName);
        assertEquals(p2expected.ssn, p2.ssn);
    }

    /**
     * Update multiple entries.
     */
    @Test
    public void testUpdateMultiple() {
        assertEquals(0, products.putOnSale("TestUpdateMultiple-match", .10f));

        Product prod1 = new Product();
        prod1.id = "800-2024-S";
        prod1.name = "Small size TestUpdateMultiple-matched item";
        prod1.price = 10.00f;
        products.insert(prod1);

        Product prod2 = new Product();
        prod2.id = "800-3024-M";
        prod2.name = "Medium size TestUpdateMultiple-matched item";
        prod2.price = 15.00f;
        products.insert(prod2);

        Product prod3 = new Product();
        prod3.id = "C6000-814BH0003Y";
        prod3.name = "Medium size TestUpdateMultiple non-matching item";
        prod3.price = 18.00f;
        products.insert(prod3);

        Product prod4 = new Product();
        prod4.id = "800-4024-L";
        prod4.name = "Large size TestUpdateMultiple-matched item";
        prod4.price = 20.00f;
        products.insert(prod4);

        assertEquals(3, products.putOnSale("TestUpdateMultiple-match", .20f));

        Product p1 = products.findItem(prod1.id);
        assertEquals(8.00f, p1.price, 0.001f);

        Product p2 = products.findItem(prod2.id);
        assertEquals(12.00f, p2.price, 0.001f);

        Product p3 = products.findItem(prod3.id);
        assertEquals(prod3.price, p3.price, 0.001f);

        Product p4 = products.findItem(prod4.id);
        assertEquals(16.00f, p4.price, 0.001f);
    }
}
