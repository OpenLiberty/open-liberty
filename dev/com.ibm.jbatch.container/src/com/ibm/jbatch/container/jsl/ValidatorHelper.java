/*
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.jsl;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class ValidatorHelper {

    private static HashMap<String, Schema> schemas = new HashMap<>();

    private static SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    /**
     * This method must be synchronized as it is possible for two threads to
     * enter concurrently while schema == null and both will try to load and
     * parse the schema, which may cause a parsing exception:
     * org.xml.sax.SAXException: FWK005 parse may not be called while parsing.
     */
    public static synchronized Schema getXJCLSchema(String schemaLocation) {
        Schema schema = schemas.get(schemaLocation);
        if (schema == null) {
            final URL url = ValidatorHelper.class.getResource(schemaLocation);
            try {
                schema = AccessController.doPrivileged(
                                                       new PrivilegedExceptionAction<Schema>() {
                                                           @Override
                                                           public Schema run() throws SAXException {
                                                               return sf.newSchema(url);
                                                           }
                                                       });
                schemas.put(schemaLocation, schema);
            } catch (PrivilegedActionException e) {
                throw new RuntimeException(e.getCause());
            }

        }
        return schema;
    }
}
