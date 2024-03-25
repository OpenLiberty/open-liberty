/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package jaxb.web.utils;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jaxb.web.dataobjects.Item;
import jaxb.web.dataobjects.Items;
import jaxb.web.dataobjects.ObjectFactory;
import jaxb.web.dataobjects.PurchaseOrderType;
import jaxb.web.dataobjects.ShippingAddress;

/**
 *
 */
public class JAXBContextUtils {

    public static final String JAXB_CONTEXT_FACTORY = "jakarta.xml.bind.JAXBContextFactory";
    public static final String MOXY_JAXB_CONTEXT_FACTORY = "org.eclipse.persistence.jaxb.XMLBindingContextFactory";
    public static Items ITEMS = null;
    public static ShippingAddress SHIPPING_ADDRESS = null;
    public static PurchaseOrderType PURCHASE_ORDER_TYPE = null;
    private static XMLGregorianCalendar shipDate = null;

    public static JAXBContext setupJAXBContext() throws Exception {
        return JAXBContext.newInstance("jaxb.web.dataobjects");
    }

    public static JAXBContext setupJAXBContextWithPropertyMap(String impl, ClassLoader classLoader) throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(JAXB_CONTEXT_FACTORY, impl);
        return JAXBContext.newInstance("jaxb.web.dataobjects",
                                       classLoader, properties);
    }

    public static JAXBContext setupJAXBContextWithSystemProperty(String impl, ClassLoader classLoader) throws Exception {

        Properties props = System.getProperties();
        props.setProperty(JAXB_CONTEXT_FACTORY, impl);

        return JAXBContext.newInstance("jaxb.web.dataobjects", classLoader);
    }

    public static JAXBContext setupJAXBContextWithSystemProperty(String impl) throws Exception {

        Properties props = System.getProperties();
        props.setProperty(JAXB_CONTEXT_FACTORY, impl);

        return JAXBContext.newInstance("jaxb.web.dataobjects");
    }

    /**
     * @param items2
     * @return
     */
    public static String marshallToString(Object object, Marshaller m) throws Exception {

        StringWriter stringWriter = new StringWriter();

        m.marshal(object, stringWriter);

        return stringWriter.toString();
    }

    /**
     * @return
     */
    public static Items getItems() {
        if (ITEMS == null) {
            Items tempItems = new Items();
            List<Item> itemList = createItemsList();
            tempItems.setItem(itemList);
            List<String> itemNames = createItemNames(itemList);
            tempItems.setItemNames(itemNames);
            ITEMS = tempItems;
            return ITEMS;
        } else {
            return ITEMS;
        }
    }

    /**
     * @param itemList
     * @return
     */
    public static List<String> createItemNames(List<Item> itemList) {
        List<String> itemNames = new ArrayList<String>();
        for (Item i : itemList) {
            String name = i.getProductName();
            itemNames.add(name);
        }
        // TODO Auto-generated method stub
        return itemNames;
    }

    /**
     * @return
     */
    public static PurchaseOrderType getPurchaseOrderType() {
        if (PURCHASE_ORDER_TYPE == null) {
            PurchaseOrderType pot = new PurchaseOrderType();
            pot.setBillTo(getShippingAddress());
            pot.setItems(getItems());
            pot.setComment("New Order");
            pot.setOrderDate(getDate());
            pot.setShipTo(getShippingAddress());
            PURCHASE_ORDER_TYPE = pot;
            return PURCHASE_ORDER_TYPE;
        } else {
            return PURCHASE_ORDER_TYPE;
        }
    }

    /**
     *
     * @return
     */
    // Synchronized added to prevent intermittent duplicate list creation on Windows OS
    public static synchronized List<Item> createItemsList() {
        List<Item> itemList = new ArrayList<Item>(2); // Only and only 2 items
        // Better to call one constructor method and set all fields than set each fields one by one
        itemList.add(new Item("Test Product", 15, new BigDecimal(59872325.999999999898), "I'm a test product", getDate()));
        itemList.add(new Item("Test Product 2", 1251, new BigDecimal(135165.999999999898), "I'm another test product", getDate()));

        return itemList;
    }

    /**
     * @return
     */
    public static ShippingAddress getShippingAddress() {
        if (SHIPPING_ADDRESS == null) {
            ShippingAddress sa = new ShippingAddress();
            sa.setCity("Austin");
            sa.setCountry("United States");
            sa.setName("Test Name");
            sa.setStreet("505 E Maple");
            sa.setState("Texas");
            sa.setZip(new BigDecimal(77777));
            SHIPPING_ADDRESS = sa;
            return SHIPPING_ADDRESS;
        } else {
            return SHIPPING_ADDRESS;
        }
    }

    public static XMLGregorianCalendar getDate() {
        if (shipDate == null) {

            String pattern = "yyyy-MM-dd";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            Date date;
            XMLGregorianCalendar sd = null;
            try {
                date = simpleDateFormat.parse("2018-09-09");

                sd = DatatypeFactory.newInstance().newXMLGregorianCalendar(new SimpleDateFormat("yyyy-MM-dd").format(date));
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            shipDate = sd;

            return sd;
        } else {
            return shipDate;
        }
    }

    /**
     * @param thirdPartyPropertyMapContext
     * @return
     * @throws Exception
     */
    public static String marshallForTest(JAXBContext thirdPartyPropertyMapContext) throws Exception {
        //Items items = of.createItems();

        Marshaller marshaller = thirdPartyPropertyMapContext.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        ObjectFactory of = new ObjectFactory();
        //setupJaxbDataObjects(tempItems);

        return marshallToString(of.createPurchaseOrderType(getPurchaseOrderType()), marshaller);

    }

    /**
     * @param unmarshalledItems
     * @param items
     * @return
     */
    public static boolean compareItems(Items unmarshalledItems, Items items) {
        List<Item> unmarshalledItemsList = unmarshalledItems.getItem();
        List<Item> itemsList = items.getItem();

        if (unmarshalledItemsList.size() != itemsList.size())
            return false;
        else {
            for (int i = 0; i < unmarshalledItemsList.size(); i++) {
                if (!unmarshalledItemsList.get(i).getComment().equals(itemsList.get(i).getComment())) {
                    System.out.println("JAXBUtils.compareItems(): Unmarhalled Comment value, " + unmarshalledItemsList.get(i).getComment()
                                       + ", doesn't equal expected Comment value "
                                       + itemsList.get(i).getComment());
                    return false;
                } else if (!unmarshalledItemsList.get(i).getPrice().equals(itemsList.get(i).getPrice())) {
                    System.out.println("JAXBUtils.compareItems(): Unmarhalled Price value, " + unmarshalledItemsList.get(i).getPrice()
                                       + ", doesn't equal expected Price value "
                                       + itemsList.get(i).getPrice());
                    return false;
                } else if (!unmarshalledItemsList.get(i).getProductName().equals(itemsList.get(i).getProductName())) {
                    System.out.println("JAXBUtils.compareItems(): Unmarhalled ProductName value, " + unmarshalledItemsList.get(i).getProductName()
                                       + ", doesn't equal expected ProductName value "
                                       + itemsList.get(i).getProductName());
                    return false;
                } else if (unmarshalledItemsList.get(i).getQuantity() != itemsList.get(i).getQuantity()) {
                    System.out.println("JAXBUtils.compareItems(): Unmarhalled Quantity value, " + unmarshalledItemsList.get(i).getQuantity()
                                       + ", doesn't equal expected Quantity value "
                                       + itemsList.get(i).getQuantity());
                    return false;
                } else if (!unmarshalledItemsList.get(i).getShipDate().equals(itemsList.get(i).getShipDate())) {
                    System.out.println("JAXBUtils.compareItems(): Unmarhalled ShipDate value, " + unmarshalledItemsList.get(i).getShipDate()
                                       + ", doesn't equal expected ShipDate value "
                                       + itemsList.get(i).getShipDate());
                    return false;
                }
            }
        }
        System.out.println("JAXBUtils.compareItems(): All values were equal, returning true");
        return true;
    }

    /**
     * @param unmarshalledShippingAddress
     * @param createShippingAddress
     * @return
     */
    public static boolean compareShippingAddress(ShippingAddress unmarshalledShippingAddress, ShippingAddress createdShippingAddress) {
        if (!unmarshalledShippingAddress.getCity().equals(createdShippingAddress.getCity())) {
            System.out.println("JAXBUtils.compareShippingAddresss(): Unmarhalled City value, " + unmarshalledShippingAddress.getCity()
                               + ", doesn't equal expected City value "
                               + createdShippingAddress.getCity());
            return false;
        } else if (!unmarshalledShippingAddress.getCountry().equals(createdShippingAddress.getCountry())) {
            System.out.println("JAXBUtils.compareShippingAddresss(): Unmarhalled Country value, " + unmarshalledShippingAddress.getCountry()
                               + ", doesn't equal expected Country value "
                               + createdShippingAddress.getCountry());
            return false;
        } else if (!unmarshalledShippingAddress.getName().equals(createdShippingAddress.getName())) {
            System.out.println("JAXBUtils.compareName(): Unmarhalled Name value, " + unmarshalledShippingAddress.getName()
                               + ", doesn't equal expected Name value "
                               + createdShippingAddress.getName());
            return false;
        } else if (!unmarshalledShippingAddress.getState().equals(createdShippingAddress.getState())) {
            System.out.println("JAXBUtils.compareShippingAddresss(): Unmarhalled State value, " + unmarshalledShippingAddress.getState()
                               + ", doesn't equal expected State value "
                               + createdShippingAddress.getState());
            return false;
        } else if (!unmarshalledShippingAddress.getStreet().equals(createdShippingAddress.getStreet())) {
            System.out.println("JAXBUtils.compareShippingAddresss(): Unmarhalled Street value, " + unmarshalledShippingAddress.getStreet()
                               + ", doesn't equal expected Street value "
                               + createdShippingAddress.getStreet());
            return false;
        } else if (!unmarshalledShippingAddress.getZip().equals(createdShippingAddress.getZip())) {
            System.out.println("JAXBUtils.compareShippingAddresss(): Unmarhalled Zip value, " + unmarshalledShippingAddress.getZip()
                               + ", doesn't equal expected Zip value "
                               + createdShippingAddress.getZip());
            return false;
        } else {
            System.out.println("JAXBUtils.compareShippingAddresss(): All values were equal, returning true");
            return true;
        }
    }

    /**
     * @param unmarshalledPurchaseOrderType
     * @param purchaseOrderType
     * @return
     * @throws JAXBException
     */
    public static void comparePurchaseOrderType(Unmarshaller unmarshaller) throws JAXBException {

        JAXBElement<PurchaseOrderType> potElement = (JAXBElement<PurchaseOrderType>) unmarshaller
                        .unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_PURCHASEORDERTYPE_MARSHALLED_RESULT));
        PurchaseOrderType unmarshalledPurchaseOrderType = potElement.getValue();

        boolean isEqual = true;

        if (!unmarshalledPurchaseOrderType.getComment().equals(unmarshalledPurchaseOrderType.getComment())) {
            System.out.println("JAXBUtils.comparePurchaseOrderType(): Unmarhalled comment value, " + unmarshalledPurchaseOrderType.getComment()
                               + ", doesn't equal expected comment value "
                               + unmarshalledPurchaseOrderType.getComment());
            isEqual = false;
        } else if (!unmarshalledPurchaseOrderType.getOrderDate().equals(unmarshalledPurchaseOrderType.getOrderDate())) {
            System.out.println("JAXBUtils.comparePurchaseOrderType(): Unmarhalled OrderDate value, " + unmarshalledPurchaseOrderType.getOrderDate()
                               + ", doesn't equal expected comment value "
                               + unmarshalledPurchaseOrderType.getOrderDate());
            isEqual = false;
        } else if (compareShippingAddress(unmarshalledPurchaseOrderType.getShipTo(), getPurchaseOrderType().getShipTo()) == false)
            isEqual = false;
        else if (compareItems(unmarshalledPurchaseOrderType.getItems(), getPurchaseOrderType().getItems()) == false)
            isEqual = false;

        assertTrue("Expected unmarshalled version of the PurchaseOrderType to match the created version, but they did not",
                   isEqual);

    }

    /**
     * @param searchString    String to search
     * @param expectedStrings Array of strings to search in searchString
     * @return boolean Return false if any string in expectedStrings does not exist in searchString
     */
    public static String searchArrayInString(String searchString, String[] expectedStrings) {
        for (String expString : expectedStrings) {
            if (!searchString.contains(expString)) {
                return expString;
            }
        }
        return null;
    }

    public static String arrayToString(String[] stringArray) {
        StringBuilder sb = new StringBuilder();
        for (String string : stringArray) {
            sb.append(string);
        }
        return sb.toString();
    }
}
