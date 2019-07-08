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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrame;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameContentDescription;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpFrameHeader;
import org.apache.cxf.binding.soap.tcp.frames.SoapTcpMessage;
import org.apache.cxf.staxutils.StaxUtils;

public final class SoapTcpUtils {

    private SoapTcpUtils() {
        
    }
    
    public static void writeSoapTcpMessage(final OutputStream out, final SoapTcpMessage msg)
        throws IOException {
        for (SoapTcpFrame frame : msg.getFrames()) {
            writeMessageFrame(out, frame);
        }
    }
    
    /**
     * Method  that writes single SoapTcpFrame
     * @param out
     * @param frame
     * @throws IOException
     */
    public static void writeMessageFrame(final OutputStream out, final SoapTcpFrame frame) throws IOException
    {
        if (frame != null) {
            final SoapTcpFrameHeader header = frame.getHeader();
            final byte payload[] = frame.getPayload();
            if (header != null && payload != null) {
                header.write(out);
                DataCodingUtils.writeInt8(out, payload.length);
                out.write(payload);
                out.flush();
            }
        }
    }
    
    /**
     * Method that reads single SoapTcpFrame
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static SoapTcpFrame readMessageFrame(final InputStream inputStream) throws IOException
    {
        final SoapTcpFrame frame = new SoapTcpFrame();
        final SoapTcpFrameHeader header = new SoapTcpFrameHeader();
        frame.setHeader(header);
        
        final int response[] = new int[2]; //[0] channel-id, [1] message-id
        DataCodingUtils.readInts4(inputStream, response, 2);
        
        frame.setChannelId(response[0]);
        header.setChannelId(response[0]);
        header.setFrameType(response[1]);
        switch(response[1]) {
        case SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE:
            header.setContentDescription(readContentDescription(inputStream));
            break;
        case SoapTcpFrameHeader.MESSAGE_START_CHUNK:
            header.setContentDescription(readContentDescription(inputStream));
            break;
        case SoapTcpFrameHeader.MESSAGE_CHUNK:
            break;
        case SoapTcpFrameHeader.MESSAGE_END_CHUNK:
            break;
        case SoapTcpFrameHeader.ERROR_MESSAGE:
            break;
        case SoapTcpFrameHeader.NULL_MESSAGE:
            break;
        default:
        }
            
        final int payloadLength = DataCodingUtils.readInt8(inputStream);
        final byte payload[] = new byte[payloadLength];
        if (inputStream.read(payload, 0, payload.length) != payloadLength) {
            throw new IOException();
        }
        frame.setPayload(payload);
        
        return frame;
    }
    
    private static SoapTcpFrameContentDescription readContentDescription(final InputStream inputStream)
        throws IOException {
        final int response[] = new int[2];
        DataCodingUtils.readInts4(inputStream, response, 2); //[0] content-id, [1] number-of-parameters
        
        final SoapTcpFrameContentDescription contentDesc = new SoapTcpFrameContentDescription();
        contentDesc.setContentId(response[0]);
        final int numOfParams = response[1];
        
        final Map<Integer, String> parameters = new Hashtable<Integer, String>();
        for (int i = 0; i < numOfParams; i++) {
            DataCodingUtils.readInts4(inputStream, response, 2); //[0] parameter-id, [1] string-length
            if (response[1] > 0) {
                final byte[] buffer = new byte[response[1]];
                if (inputStream.read(buffer) > 0) {
                    final String value = new String(buffer, "UTF-8");
                    parameters.put(Integer.valueOf(response[0]), value);
                    //System.out.println("parameter-id = " + response[0] + " parameter-value = " + value);
                }
            }
        }
        contentDesc.setParameters(parameters);
        
        return contentDesc;
    }

    
    /**
     * Method that parse SoapTcpFrame payload to find important tag. 
     *  
     * @param responseFrame frame that will be examinated
     * @param elementName a tag to look for 
     * @return true If payload contains that tag then method return true
     * otherwise return false;
     */
    public static boolean checkSingleFrameResponse(final SoapTcpFrame responseFrame,
                                                   final String elementName) {
        if (responseFrame != null
            && responseFrame.getHeader().getFrameType() == SoapTcpFrameHeader.SINGLE_FRAME_MESSAGE) {
            ByteArrayInputStream bais = new ByteArrayInputStream(responseFrame.getPayload());
            XMLStreamReader xmlReader = StaxUtils.createXMLStreamReader(bais);
            try {
                while (xmlReader.hasNext()) {
                    xmlReader.next();
                    if (xmlReader.getEventType() == XMLStreamReader.START_ELEMENT
                        && xmlReader.getLocalName().equals(elementName)) {
                        return true;
                    }
                }
            } catch (XMLStreamException e) {
                e.printStackTrace();
            } finally {
                StaxUtils.close(xmlReader);
            }
        }
        return false;
    }
    
    /**
     * Method that print SoapTcpFrame
     * @param out
     * @param frame
     */
    public static void printSoapTcpFrame(final OutputStream out, final SoapTcpFrame frame) {
        if (frame != null) {
            final PrintStream writer = (PrintStream)out;
            writer.println("channel-id: " + frame.getChannelId());
            
            final SoapTcpFrameHeader header = frame.getHeader();
            if (header != null) {
                writer.println("frameType: " + header.getFrameType());
                final SoapTcpFrameContentDescription contentDesc = header.getContentDescription();
                if (contentDesc != null) {
                    writer.println("content-id: " + contentDesc.getContentId());
                    final Map<Integer, String> parameters = contentDesc.getParameters();
                    if (parameters != null) {
                        final Iterator<Integer> keys = parameters.keySet().iterator();
                        writer.println("parameters");
                        while (keys.hasNext()) {
                            final Integer key = keys.next();
                            final String value = parameters.get(key);
                            writer.println(key + " : " + value);
                        }
                    }
                }
            }
            final byte payload[] = frame.getPayload();
            if (payload != null) {
                try {
                    final String messageContent = new String(payload, "UTF-8");
                    writer.println("messageContent:");
                    writer.println(messageContent);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
