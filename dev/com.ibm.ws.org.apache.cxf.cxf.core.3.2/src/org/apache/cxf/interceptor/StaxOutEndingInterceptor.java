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
package org.apache.cxf.interceptor;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

public class StaxOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(StaxOutEndingInterceptor.class);

    private String outStreamHolder;
    private String writerHolder;

    @Trivial  // Liberty change: line is added
    public StaxOutEndingInterceptor(String outStreamHolder) {
        this(outStreamHolder, null);
    }

    @Trivial  // Liberty change: line is added
    public StaxOutEndingInterceptor(String outStreamHolder, String writerHolder) {
        super(Phase.PRE_STREAM_ENDING);
        getAfter().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        this.outStreamHolder = outStreamHolder;
        this.writerHolder = writerHolder;
    }

    public void handleMessage(@Sensitive Message message) { // Liberty change: @Sensitive is added to parameter
        try {
            XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
            if (xtw != null) {
                try {
                    xtw.writeEndDocument();
                    xtw.flush();
                } finally {
                    StaxUtils.close(xtw);
                }
            }

            OutputStream os = (OutputStream)message.get(outStreamHolder);
            if (os != null) {
                message.setContent(OutputStream.class, os);
            }
            if (writerHolder != null) {
                Writer w = (Writer)message.get(writerHolder);
                if (w != null) {
                    message.setContent(Writer.class, w);
                }
            }
            message.removeContent(XMLStreamWriter.class);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
        }
    }

}
