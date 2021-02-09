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
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;

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
    
    public static Object createJAXBBean(JAXBContext context, 
                                        String s,
                                        Class<?> c) {
        
        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        Unmarshaller u;
        try {
            Object obj;
            u = context.createUnmarshaller();
            if (c != null) {
                obj = u.unmarshal(data, c);
            } else {
                obj = u.unmarshal(data);
            }
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();

            }
            return obj;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } finally {
            StaxUtils.close(data);
        }
    }

}
