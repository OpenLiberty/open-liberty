/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.test.holder;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;

import org.junit.Test;

import componenttest.app.FATServlet;
import hello.Address;
import hello.Header;
import hello.HelloIF;
import hello.Location;
import hello.StateType;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/HolderTestServlet")
public class HolderTestServlet extends FATServlet {

    private static final Logger LOG = Logger.getLogger("HolderTestLogger");

    // Single service client parameters
    private static URL WSDL_URL;
    private static QName qname;
    private static QName portName;
    private static Service service;
    private static HelloIF proxy;

    // Reuse the same Holder objects
    private Holder<Address> address1;
    private Holder<Address> address2;
    private Holder<Header> headerHolder;

    // Reuse the expected values across all tests.
    private final Address expectedAddress1 = createAddress("EN", "Bap Bow", "S Bap St", 421, 84104, StateType.IN);
    private final Address expectedAddress2 = createAddress("EN", "Skippity Bow", "N Fresh St", 1, 84132, StateType.TX);
    private final Address expectedHeaderAddress = createAddress("EN", "Cram City", "W Cram Circle", 7, 84132, StateType.TX);

    // Construct a single instance of the service client
    static {
        try {
            WSDL_URL = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/helloHolder/HelloIF?wsdl").toString());

            qname = new QName("http://hello/", "HelloIF");
            portName = new QName("http://hello/", "HelloIF");
            service = Service.create(qname);
            proxy = service.getPort(portName, HelloIF.class);

            String newTarget = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/helloHolder/HelloIF";
            BindingProvider bp = (BindingProvider) proxy;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /*
     * This test uses two Holder<Address> parameters (Holder<Address> add, Holder<Address> add2).
     *
     * The test sends two parameters to the Endpoint (Holder<Address> add, Holder<Address> add2),
     * where the endpoint makes no modification and returns. The test
     * checks both Holder<Address> add, and Holder<Adders> add2 still contain the original values.
     */
    @Test
    public void testHolderNoUpdate() throws Exception {

        // Set or Reset Holder<Address> address1's value to the same as the expected address
        Holder<Address> address1 = new Holder<Address>();
        Holder<Address> address2 = new Holder<Address>();
        address1.value = expectedAddress1;
        address2.value = expectedAddress2;

        proxy.updateAddress(address1, address2);

        // Compare Address values from Holder to expectedAddress's Address value
        LOG.info("testHolderNoUpdate: Comparing Holder<Address> address1's Address value to the Expected Address Value");
        compareExpectedAddress(address1.value, expectedAddress1);
        LOG.info("testHolderNoUpdate: Comparing Holder<Address> address2's Address value to the Expected Address Value");
        compareExpectedAddress(address2.value, expectedAddress2);
    }

    /*
     * The test sends two parameters to the Endpoint (Holder<Address> address1, Holder<Address> address2) and
     * the endpoint makes updates to each Address value to correct the null value that's set in the Address, and then returns the updated parameters.
     * The test checks the returned address values from both Holder<Header>.value.getAddress() parameters against the expected Address values
     */
    @Test
    public void testNullSingleValueInHoldersWithServerSideUpdate() throws Exception {

        // Set or Reset Holder<Address> address1's value to the same as the expected address
        address1 = new Holder<Address>();
        address2 = new Holder<Address>();

        // Null City and Street Name on Address1 and Address2
        address1.value = createAddress("EN", null, "S Bap St", 421, 84104, StateType.IN);;
        address2.value = createAddress("EN", "Skippity Bow", null, 1, 84132, StateType.TX);

        proxy.updateAddress(address1, address2);

        // Compare Address values from Holder to expectedAddress's Address value
        LOG.info("testNullSingleValueInHoldersWithServerSideUpdate: Comparing Holder<Address> address1's Address value to the Expected Address Value");
        compareExpectedAddress(address1.value, expectedAddress1);
        LOG.info("testNullSingleValueInHoldersWithServerSideUpdate: Comparing Holder<Address> address2's Address value to the Expected Address Value");
        compareExpectedAddress(address2.value, expectedAddress2);
    }

    /*
     * This test passes a Holder<Address> type as both a parameter and a Holder<Header> type
     * The Holder<Header> marshalled XML is automatically put in the SOAPHeader to via the "header = true" as
     * described above
     *
     * The Header type is a more complex data type than Address. It has a Location<Address> type element, as well
     * as a plain Address type
     *
     * The test sends two parameters to the Endpoint (Holder<Address> address1, Holder<Header> headerholder),
     * the endpoint makes no modification and returns.
     *
     */
    @Test
    public void testHolderAndHeaderNoUpdate() throws Exception {

        // Set or Reset Holder<Address> address1's value to the same as the expected address
        address1 = new Holder<Address>();
        address2 = new Holder<Address>();
        headerHolder = new Holder<Header>();
        Location<Address> locationAddress = new Location<Address>();
        Header headerType = new Header();
        address1.value = expectedAddress1;

        // Set or Reset Holder<Header> headerHolder values
        locationAddress.setAddress(expectedAddress2);
        headerType.setAddress(expectedHeaderAddress);
        headerType.setLocation(locationAddress);
        headerHolder.value = headerType;

        proxy.sayHelloHeader(address1, headerHolder);

        // Compare Address values from Holder to expectedAddress's Address value
        LOG.info("testHolderAndHeaderNoUpdate: Comparing Holder<Address> address1's Address value to the Expected Address Value");
        compareExpectedAddress(address1.value, expectedAddress1);
        LOG.info("testHolderAndHeaderNoUpdate: Comparing Holder<Header> headerHolder's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getLocation().getAddress(), expectedAddress2);
        LOG.info("testHolderAndHeaderNoUpdate: Comparing Holder<Address> headerHolder Location's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getAddress(), expectedHeaderAddress);

    }

    /*
     * The test sends Address elements with specifically empty child fields. These Addresses with empty fields are in both in Holder<Address> and Holder<Header>
     * The endpoint checks for these empty fields, and then returns both Holder<Address> and Holder<Header>
     * with non-null field values in the address. The test checks the returned address values from Holder<Header>.value.getAddress(), Holder<Header>.getLocation().getAddress(),
     * and Holder<Address>.value.getAddress() against the expected Address values to ensure they are equal
     */
    @Test
    public void testNullSingleValueInHolderAndHeaderWithServerSideUpdate() throws Exception {

        // Set or Reset Holder<Address> address1's value
        address1 = new Holder<Address>();
        address2 = new Holder<Address>();
        headerHolder = new Holder<Header>();
        Location<Address> locationAddress = new Location<Address>();
        Header headerType = new Header();
        address1.value = createAddress("EN", null, "S Bap St", 421, 84104, StateType.IN);

        // Set null values on Address' then set Holder values
        locationAddress.setAddress(createAddress("EN", "Cram City", "W Cram Circle", 7, 84132, null));
        headerType.setAddress(createAddress("EN", "Skippity Bow", null, 1, 84132, StateType.TX));
        headerType.setLocation(locationAddress);
        headerHolder.value = headerType;

        proxy.sayHelloHeader(address1, headerHolder);

        // Compare Address values from Holders to expectedAddress's Address value
        LOG.info("testNullSingleValueInHolderAndHeaderWithServerSideUpdate: Comparing Holder<Address> address1's Address value to the Expected Address Value");
        compareExpectedAddress(address1.value, expectedAddress1);
        LOG.info("testNullSingleValueInHolderAndHeaderWithServerSideUpdate: Comparing Holder<Header> headerHolder's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getAddress(), expectedAddress2);
        LOG.info("testNullSingleValueInHolderAndHeaderWithServerSideUpdate: Comparing Holder<Address> headerHolder Location's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getLocation().getAddress(), expectedHeaderAddress);

    }

    /*
     * The test sends empty Address elements to the endpoint, the endpoint checks for these empty address, and then returns both Holder<Address> and Holder<Header>
     * with fully initialized address values The test checks the returned address values from Holder<Header>.value.getAddress(), Holder<Header>.getLocation().getAddress(),
     * and Holder<Address>.value.getAddress() against the expected Address values to ensure they are equal
     */
    @Test
    public void testEmptyAddressValuesInHolderAndHeaderWithServerSideUpdate() throws Exception {

        // Set or Reset Holder<Address> address1's value to the same as the expected address
        address1 = new Holder<Address>();
        address2 = new Holder<Address>();
        headerHolder = new Holder<Header>();
        Location<Address> locationAddress = new Location<Address>();
        Header headerType = new Header();
        address1.value = null;
        headerType = new Header();

        // Set or Reset Holder<Header> headerHolder values
        locationAddress.setAddress(new Address());
        headerType.setAddress(new Address());
        headerType.setLocation(locationAddress);
        headerHolder.value = headerType;

        proxy.sayHelloHeader(address1, headerHolder);

        // Compare Address values from Holders to expectedAddress's Address value
        LOG.info("testNullHolderAndNullHeaderWithServerSideUpdate: Comparing Holder<Address> address1's Address value to the Expected Address Value");
        compareExpectedAddress(address1.value, expectedAddress1);
        LOG.info("testNullHolderAndNullHeaderWithServerSideUpdate: Comparing Holder<Header> headerHolder's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getAddress(), expectedAddress2);
        LOG.info("testNullHolderAndNullHeaderWithServerSideUpdate: Comparing Holder<Address> headerHolder Location's Address value to the Expected Address Value");
        compareExpectedAddress(headerHolder.value.getLocation().getAddress(), expectedHeaderAddress);

    }

    /**
     * Method for asserting that the Lang, City, Street Name, Street Num, Zip, State values from a returned Address and
     * expected Address match
     *
     * @param address12
     * @param expectedAddress1
     */
    private void compareExpectedAddress(Address address, Address expectedAddress) {
        assertEquals("Expected " + address.getLang() + " to match " + expectedAddress.getLang(), address.getLang(), expectedAddress.getLang());
        assertEquals("Expected " + address.getCity() + " to match " + expectedAddress.getCity(), address.getCity(), expectedAddress.getCity());
        assertEquals("Expected " + address.getStreetName() + " to match " + expectedAddress.getStreetName(), address.getStreetName(), expectedAddress.getStreetName());
        assertEquals("Expected " + address.getStreetNum() + " to match " + expectedAddress.getStreetNum(), address.getStreetNum(), expectedAddress.getStreetNum());
        assertEquals("Expected " + address.getZip() + " to match " + expectedAddress.getZip(), address.getZip(), expectedAddress.getZip());
        assertEquals("Expected " + address.getState() + " to match " + expectedAddress.getState(), address.getState(), expectedAddress.getState());
    }

    private Address createAddress(String lang, String city, String streetName, int streetNum, int zip, StateType state) {
        Address add = new Address();
        add.setCity(city);
        add.setLang(lang);
        add.setStreetName(streetName);
        add.setStreetNum(streetNum);
        add.setZip(zip);
        add.setState(state);
        return add;
    }

}
