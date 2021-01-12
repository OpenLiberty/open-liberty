/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.configuration.spring;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Trivial;
/**
 *
 */
@Trivial
public final class JAXBBeanFactory {
    private JAXBBeanFactory() {
        //nothing
    }

    public static <T> T createJAXBBean(JAXBContext context,
                                        String s,
                                        Class<T> c) {

        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        try {

            T obj = null;
            if (c != null) {
                obj = JAXBUtils.unmarshall(context, data, c).getValue();
            } else {
                Object o = JAXBUtils.unmarshall(context, data);
                if (o instanceof JAXBElement<?>) {
                    JAXBElement<?> el = (JAXBElement<?>)o;
                    @SuppressWarnings("unchecked")
                    T ot = (T)el.getValue();
                    obj = ot;
                }
            }
            return obj;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                StaxUtils.close(data);
            } catch (XMLStreamException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
