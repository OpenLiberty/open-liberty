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
package org.apache.cxf.jaxws.interceptors;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;

public class SwAInInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(SwAInInterceptor.class); // Liberty Change issue #26529
    public SwAInInterceptor() {
        super(Phase.PRE_INVOKE);
        getBefore().add(HolderInInterceptor.class.getName());
        getAfter().add(StaxInEndingInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        BindingOperationInfo bop = message.getExchange().getBindingOperationInfo();
        if (bop == null) {
            if(isFinestEnabled)  {
                LOG.finest("BindingOperationInfo is null. Returning.");   // Liberty Change issue #26529
            } 
            return;
        }

        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
            if(isFinestEnabled)  {
                LOG.finest("BindingOperationInfo is switched to wrapped version");   // Liberty Change issue #26529
            }
        }

        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getOutput() : bop.getInput();

        if (bmi == null) {
            if(isFinestEnabled)  {
                LOG.finest("BindingMessageInfo is null. Returning.");   // Liberty Change issue #26529
            } 
            return;
        }

        SoapBodyInfo sbi = bmi.getExtensor(SoapBodyInfo.class);

        if (sbi == null || sbi.getAttachments() == null || sbi.getAttachments().isEmpty()) {
            if(isFinestEnabled)  {
                LOG.finest("SoapBodyInfo is null or it doesn't have attachments. Returning.");   // Liberty Change issue #26529
            } 
            return;
        }

        Set<Integer> foundAtts = new HashSet<>();
        MessageContentsList inObjects = MessageContentsList.getContentsList(message);

        for (MessagePartInfo mpi : sbi.getAttachments()) {
            String partName = mpi.getConcreteName().getLocalPart();

            String start = partName + "=";
            boolean found = false;

            if (foundAtts.contains(mpi.getIndex())) {
                continue;
            }
            foundAtts.add(mpi.getIndex());

            for (Attachment a : message.getAttachments()) {
                if (a.getId().startsWith(start)) {
                    DataHandler dh = a.getDataHandler();
                    String ct = dh.getContentType();
                    final Object o;
                    Class<?> typeClass = mpi.getTypeClass();
                    if (DataHandler.class.isAssignableFrom(typeClass)) {
                        o = dh;
                        if(isFinestEnabled)  {
                            LOG.finest("DataHandler that is obtained from attachment is assigned to Object: " + o);   // Liberty Change issue #26529
                        } 
                    } else if (String.class.isAssignableFrom(typeClass)) {
                        try {
                            //o = IOUtils.readBytesFromStream(dh.getInputStream());
                            o = dh.getContent();
                            if(isFinestEnabled)  {
                                LOG.finest("Content of DataHandler that is obtained from attachment is assigned to Object: " + o);   // Liberty Change issue #26529
                            } 
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    } else if (byte[].class.isAssignableFrom(typeClass)) {
                        try {
                            o = IOUtils.readBytesFromStream(dh.getInputStream());
                            if(isFinestEnabled)  {
                                LOG.finest("InputStream read from DataHandler that is obtained from attachment is assigned to Object: " + o);   // Liberty Change issue #26529
                            } 
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    } else if (ct.startsWith("image/")) {
                        try {
                            o = ImageIO.read(dh.getInputStream());
                            if(isFinestEnabled)  {
                                LOG.finest("InputStream read from DataHandler that is obtained from image attachment is assigned to Object: " + o);   // Liberty Change issue #26529
                            } 
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    } else if (ct.startsWith("text/xml") || ct.startsWith("application/xml")) {
                        try {
                            o = new StreamSource(dh.getInputStream());
                            if(isFinestEnabled)  {
                                LOG.finest("StreamSource instantiated with InputStream that is obtained from DataHandler is assigned to Object: " + o);   // Liberty Change issue #26529
                            } 
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    } else {
                        o = dh;
                    }

                    inObjects.put(mpi, o);
                    found = true;
                    if(isFinestEnabled)  {
                        LOG.finest("Object is put in MessageContentsList and found is set to true.");   // Liberty Change issue #26529
                    } 
                    break;
                }
            }

            if (!found) {
                inObjects.put(mpi, null);
                if(isFinestEnabled)  {
                    LOG.finest("Object is not put in MessageContentsList since it's not found.");   // Liberty Change issue #26529
                } 
            }
        }
    }
}
