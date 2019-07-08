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

package org.apache.cxf.binding.soap.tcp;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public final class ChannelIdParser {
    
    private ChannelIdParser() {
        
    }
    
    /**
     * Method for retrieving channel id from OpenChannelResponse message.
     * 
     * @param in a InputStream with message
     * @return channel id value
     */
    public static int getChannelId(InputStream in) {
        XMLStreamReader streamReader = StaxUtils.createXMLStreamReader(in, null);
        
        try {
            while (streamReader.hasNext()) {
                streamReader.next();
                int eventType = streamReader.getEventType();
                if (eventType == XMLStreamReader.START_ELEMENT
                    && streamReader.getLocalName().equals("openChannelResponse")) {
                    while (streamReader.hasNext()) {
                        streamReader.next();
                        eventType = streamReader.getEventType();
                        if (eventType == XMLStreamReader.START_ELEMENT
                            && streamReader.getLocalName().equals("channelId")) {
                            return Integer.parseInt(streamReader.getElementText());
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } finally {
            StaxUtils.close(streamReader);
        }
        
        return 0;
    }
}
