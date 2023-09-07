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

/**
 *
 */
public class JAXBXMLSchemaConstants {

    public static final String[] EXPECTED_PURCHASEORDERTYPE_MARSHALLED_RESULT_ARRAY = { "<ns0:PurchaseOrderType xmlns:ns0=\"http://jaxb.web.dataobjects/\" orderDate=\"2018-09-09\">",
                                                                                        "<ns0:shipTo country=\"United States\">", "<ns0:name>Test Name</ns0:name>",
                                                                                        "<ns0:street>505 E Maple</ns0:street>", "<ns0:city>Austin</ns0:city>",
                                                                                        "<ns0:state>Texas</ns0:state>", "<ns0:zip>77777</ns0:zip>", "</ns0:shipTo>",
                                                                                        "<ns0:billTo country=\"United States\">", "<ns0:name>Test Name</ns0:name>",
                                                                                        "<ns0:street>505 E Maple</ns0:street>", "<ns0:city>Austin</ns0:city>",
                                                                                        "<ns0:state>Texas</ns0:state>", "<ns0:zip>77777</ns0:zip>", "</ns0:billTo>",
                                                                                        "<ns0:comment>New Order</ns0:comment>", "<ns0:items>", "<ns0:item>",
                                                                                        "<ns0:productName>Test Product</ns0:productName>", "<ns0:quantity>15</ns0:quantity>",
                                                                                        "<ns0:Price>59872326</ns0:Price>", "<ns0:comment>I'm a test product</ns0:comment>",
                                                                                        "<ns0:shipDate>2018-09-09</ns0:shipDate>", "</ns0:item>", "<ns0:item>",
                                                                                        "<ns0:productName>Test Product 2</ns0:productName>", "<ns0:quantity>1251</ns0:quantity>",
                                                                                        "<ns0:Price>135165.999999999883584678173065185546875</ns0:Price>",
                                                                                        "<ns0:comment>I'm another test product</ns0:comment>",
                                                                                        "<ns0:shipDate>2018-09-09</ns0:shipDate>", "</ns0:item>",
                                                                                        "<ns0:itemNames>Test Product Test Product 2</ns0:itemNames>", "</ns0:items>",
                                                                                        "</ns0:PurchaseOrderType>" };

    public static final String EXPECTED_PURCHASEORDERTYPE_MARSHALLED_RESULT = "<ns0:PurchaseOrderType xmlns:ns0=\"http://jaxb.web.dataobjects/\" orderDate=\"2018-09-09\">\n"
                                                                              + "   <ns0:shipTo country=\"United States\">\n"
                                                                              + "      <ns0:name>Test Name</ns0:name>\n"
                                                                              + "      <ns0:street>505 E Maple</ns0:street>\n"
                                                                              + "      <ns0:city>Austin</ns0:city>\n"
                                                                              + "      <ns0:state>Texas</ns0:state>\n"
                                                                              + "      <ns0:zip>77777</ns0:zip>\n"
                                                                              + "   </ns0:shipTo>\n"
                                                                              + "   <ns0:billTo country=\"United States\">\n"
                                                                              + "      <ns0:name>Test Name</ns0:name>\n"
                                                                              + "      <ns0:street>505 E Maple</ns0:street>\n"
                                                                              + "      <ns0:city>Austin</ns0:city>\n"
                                                                              + "      <ns0:state>Texas</ns0:state>\n"
                                                                              + "      <ns0:zip>77777</ns0:zip>\n"
                                                                              + "   </ns0:billTo>\n"
                                                                              + "   <ns0:comment>New Order</ns0:comment>\n"
                                                                              + "   <ns0:items>\n"
                                                                              + "      <ns0:item>\n"
                                                                              + "         <ns0:productName>Test Product</ns0:productName>\n"
                                                                              + "         <ns0:quantity>15</ns0:quantity>\n"
                                                                              + "         <ns0:Price>59872326</ns0:Price>\n"
                                                                              + "         <ns0:comment>I'm a test product</ns0:comment>\n"
                                                                              + "         <ns0:shipDate>2018-09-09</ns0:shipDate>\n"
                                                                              + "      </ns0:item>\n"
                                                                              + "      <ns0:item>\n"
                                                                              + "         <ns0:productName>Test Product 2</ns0:productName>\n"
                                                                              + "         <ns0:quantity>1251</ns0:quantity>\n"
                                                                              + "         <ns0:Price>135165.999999999883584678173065185546875</ns0:Price>\n"
                                                                              + "         <ns0:comment>I'm another test product</ns0:comment>\n"
                                                                              + "         <ns0:shipDate>2018-09-09</ns0:shipDate>\n"
                                                                              + "      </ns0:item>\n"
                                                                              + "      <ns0:itemNames>Test Product Test Product 2</ns0:itemNames>\n"
                                                                              + "   </ns0:items>\n"
                                                                              + "</ns0:PurchaseOrderType>";

    public static final String EXPECTED_ITEM_MARSHALLED_RESULT = "<ns0:items xmlns:ns0=\"http://jaxb.web.dataobjects/\">\n"
                                                                 + "   <ns0:item>\n"
                                                                 + "      <ns0:productName>Test Product</ns0:productName>\n"
                                                                 + "      <ns0:quantity>15</ns0:quantity>\n"
                                                                 + "      <ns0:Price>59872326</ns0:Price>\n"
                                                                 + "      <ns0:comment>I'm a test product</ns0:comment>\n"
                                                                 + "      <ns0:shipDate>2018-09-09-05:00</ns0:shipDate>\n"
                                                                 + "   </ns0:item>\n"
                                                                 + "   <ns0:item>\n"
                                                                 + "      <ns0:productName>Test Product 2</ns0:productName>\n"
                                                                 + "      <ns0:quantity>1251</ns0:quantity>\n"
                                                                 + "      <ns0:Price>135165.999999999883584678173065185546875</ns0:Price>\n"
                                                                 + "      <ns0:comment>I'm another test product</ns0:comment>\n"
                                                                 + "      <ns0:shipDate>2018-09-09-05:00</ns0:shipDate>\n"
                                                                 + "   </ns0:item>\n"
                                                                 + "</ns0:items>";

    public static final String EXPECTED_SHIPPINGADDRESS_MARSHALLED_RESULT = "<ns0:ShippingAddress xmlns:ns0=\"http://jaxb.web.dataobjects/\" country=\"United States\">\n"
                                                                            + "   <ns0:name>Test Name</ns0:name>\n"
                                                                            + "   <ns0:street>505 E Maple</ns0:street>\n"
                                                                            + "   <ns0:city>Austin</ns0:city>\n"
                                                                            + "   <ns0:state>Texas</ns0:state>\n"
                                                                            + "   <ns0:zip>77777</ns0:zip>\n"
                                                                            + "</ns0:ShippingAddress>";

    public final static String[] EXPECTED_SCHEMA_CONTENTS = { ":schema", "jaxb.web.dataobjects", "ws-i.org/profiles/basic/1.1/xsd", "www.w3.org/2001/XMLSchema",
                                                              ":complexType", ":element", ":simpleType", "PurchaseOrderType", "country", "http://www.w3.org/2005/05/xmlmime",
                                                              "ws-i.org/profiles/basic/1.1/swaref.xsd", ":sequence" };

    public static final String EXPECTED_RI_PURCHASEORDERTYPE_MARSHALLED_RESULT = "<ns0:PurchaseOrderType xmlns:ns0=\"http://jaxb.web.dataobjects/\" orderDate=\"2018-09-09\">\n"
                                                                                 + "   <ns0:shipTo country=\"United States\">\n"
                                                                                 + "      <ns0:name>Test Name</ns0:name>\n"
                                                                                 + "      <ns0:street>505 E Maple</ns0:street>\n"
                                                                                 + "      <ns0:city>Austin</ns0:city>\n"
                                                                                 + "      <ns0:state>Texas</ns0:state>\n"
                                                                                 + "      <ns0:zip>77777</ns0:zip>\n"
                                                                                 + "   </ns0:shipTo>\n"
                                                                                 + "   <ns0:billTo country=\"United States\">\n"
                                                                                 + "      <ns0:name>Test Name</ns0:name>\n"
                                                                                 + "      <ns0:street>505 E Maple</ns0:street>\n"
                                                                                 + "      <ns0:city>Austin</ns0:city>\n"
                                                                                 + "      <ns0:state>Texas</ns0:state>\n"
                                                                                 + "      <ns0:zip>77777</ns0:zip>\n"
                                                                                 + "   </ns0:billTo>\n"
                                                                                 + "   <ns0:comment>New Order</ns0:comment>\n"
                                                                                 + "   <ns0:items>\n"
                                                                                 + "      <ns0:item>\n"
                                                                                 + "         <ns0:productName>Test Product</ns0:productName>\n"
                                                                                 + "         <ns0:quantity>15</ns0:quantity>\n"
                                                                                 + "         <ns0:Price>59872326</ns0:Price>\n"
                                                                                 + "         <ns0:comment>I'm a test product</ns0:comment>\n"
                                                                                 + "         <ns0:shipDate>2018-09-09</ns0:shipDate>\n"
                                                                                 + "      </ns0:item>\n"
                                                                                 + "      <ns0:item>\n"
                                                                                 + "         <ns0:productName>Test Product 2</ns0:productName>\n"
                                                                                 + "         <ns0:quantity>1251</ns0:quantity>\n"
                                                                                 + "         <ns0:Price>135165.999999999883584678173065185546875</ns0:Price>\n"
                                                                                 + "         <ns0:comment>I'm another test product</ns0:comment>\n"
                                                                                 + "         <ns0:shipDate>2018-09-09</ns0:shipDate>\n"
                                                                                 + "      </ns0:item>\n"
                                                                                 + "      <ns0:itemNames>Test Product Test Product 2</ns0:itemNames>\n"
                                                                                 + "   </ns0:items>\n"
                                                                                 + "</ns0:PurchaseOrderType>";

}
