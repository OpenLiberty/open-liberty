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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpMessage;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

public final class ChannelService {
    private ChannelService() {
        
    }
    
    public static void service(IoSession session, SoapTcpMessage message) {
        XMLStreamReader xmlReader = null;
        try {
            xmlReader
                = StaxUtils.createXMLStreamReader(message.getContentAsStream(), "UTF-8");
            while (xmlReader.hasNext()) {
                xmlReader.next();
                if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                    if (xmlReader.getLocalName().equals("initiateSession")) {
                        initiateSession(session);
                    } else if (xmlReader.getLocalName().equals("openChannel")) {
                        String targetWSURI = null;
                        List<String> negotiatedMimeTypes = new ArrayList<String>();
                        List<String> negotiatedParams = new ArrayList<String>();
                        while (xmlReader.hasNext()) {
                            xmlReader.next();
                            if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                                if (xmlReader.getLocalName().equals("targetWSURI")) {
                                    targetWSURI = xmlReader.getElementText();
                                } else if (xmlReader.getLocalName().equals("negotiatedMimeTypes")) {
                                    negotiatedMimeTypes.add(xmlReader.getElementText());
                                }  else if (xmlReader.getLocalName().equals("negotiatedParams")) {
                                    negotiatedParams.add(xmlReader.getElementText());
                                }
                            }
                        }
                        openChannel(session, targetWSURI, negotiatedMimeTypes, negotiatedParams);
                    } else  if (xmlReader.getLocalName().equals("closeChannel")) {
                        int channelId = -1;
                        while (xmlReader.hasNext()) {
                            if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT
                                && xmlReader.getLocalName().equals("channelId")) {
                                channelId = Integer.parseInt(xmlReader.getElementText());
                            }
                        }
                        closeChannel(session, channelId);
                    }
                    
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } finally {
            StaxUtils.close(xmlReader);
        }
    }
    
    private static void initiateSession(IoSession session) {
        System.out.println("initiateSession service");
        String response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><initiateSessionResponse xmlns=\"http://servicechannel.tcp.transport.ws.xml.sun.com/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/"
            + "XMLSchema\"/></s:Body></s:Envelope>";
        SoapTcpMessage soapTcpMessage = SoapTcpMessage.createSoapTcpMessage(response, 0);
        IoBuffer buffer = IoBuffer.allocate(512);
        buffer.setAutoExpand(true);
        try {
            SoapTcpUtils.writeSoapTcpMessage(buffer.asOutputStream(), soapTcpMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.flip();
        session.write(buffer);
    }
    
    @SuppressWarnings("unchecked")
    private static void openChannel(IoSession session, String targetWSURI, List<String> negotiatedMimeTypes,
                                    List<String> negotiatedParams) {
        System.out.println("openChannel service");
        List<SoapTcpChannel> channels = (List<SoapTcpChannel>)session.getAttribute("channels");
        int max = 0;
        for (SoapTcpChannel channel : channels) {
            if (channel.getChannelId() > max) {
                max = channel.getChannelId();
            }
        }
        channels.add(new SoapTcpChannel(max + 1, targetWSURI));
        
        String response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
            + "<openChannelResponse xmlns=\"http://servicechannel.tcp.transport.ws.xml.sun.com/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org"
            + "/2001/XMLSchema\"><channelId xmlns=\"\">"
            + (max + 1)
            + "</channelId><negotiatedMimeTypes xmlns=\"\">"
            + "application/soap+xml</negotiatedMimeTypes><negotiatedParams xmlns=\"\">charset</negotia"
            + "tedParams><negotiatedParams xmlns=\"\">SOAPAction</negotiatedParams><negotiatedParams xm"
            + "lns=\"\">action</negotiatedParams></openChannelResponse></s:Body></s:Envelope>";
        SoapTcpMessage soapTcpMessage = SoapTcpMessage.createSoapTcpMessage(response, 0);
        IoBuffer buffer = IoBuffer.allocate(512);
        buffer.setAutoExpand(true);
        try {
            SoapTcpUtils.writeSoapTcpMessage(buffer.asOutputStream(), soapTcpMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.flip();
        session.write(buffer);
    }
    
    @SuppressWarnings("unchecked")
    private static void closeChannel(IoSession session, int channelId) {
        System.out.println("closeChannel service");
        List<SoapTcpChannel> channels = (List<SoapTcpChannel>)session.getAttribute("channels");
        for (SoapTcpChannel channel : channels) {
            if (channel.getChannelId() == channelId) {
                channels.remove(channel);
                break;
            }
        }
        
        String response = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<s:Body><closeChannelResponse xmlns=\"http://servicechannel.tcp.transport.ws.xml.sun.com/\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/"
            + "XMLSchema\"/></s:Body></s:Envelope>";
        SoapTcpMessage soapTcpMessage = SoapTcpMessage.createSoapTcpMessage(response, 0);
        IoBuffer buffer = IoBuffer.allocate(512);
        buffer.setAutoExpand(true);
        try {
            SoapTcpUtils.writeSoapTcpMessage(buffer.asOutputStream(), soapTcpMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.flip();
        session.write(buffer);
    }
}
