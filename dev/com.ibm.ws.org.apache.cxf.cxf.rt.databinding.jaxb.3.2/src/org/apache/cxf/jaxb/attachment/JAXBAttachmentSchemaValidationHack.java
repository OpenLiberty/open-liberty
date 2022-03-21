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

package org.apache.cxf.jaxb.attachment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 *
 */
public final class JAXBAttachmentSchemaValidationHack extends AbstractPhaseInterceptor<Message> {
    public static final JAXBAttachmentSchemaValidationHack INSTANCE
        = new JAXBAttachmentSchemaValidationHack();
    private static final String SAVED_DATASOURCES
        = JAXBAttachmentSchemaValidationHack.class.getName() + ".SAVED_DATASOURCES";

    private JAXBAttachmentSchemaValidationHack() {
        super(Phase.POST_PROTOCOL);
    }

    public void handleMessage(Message message) throws Fault {
        // This assumes that this interceptor is only use in IN / IN Fault chains.
        if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.IN, message)
            && message.getAttachments() != null) {
            Collection<AttachmentDataSource> dss = new ArrayList<>();
            for (Attachment at : message.getAttachments()) {
                if (at.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                    AttachmentDataSource ds = (AttachmentDataSource)at.getDataHandler().getDataSource();
                    try {
                        ds.hold(message);
                    } catch (IOException e) {
                        throw new Fault(e);
                    }
                    dss.add(ds);
                }
            }
            if (!dss.isEmpty()) {
                message.put(SAVED_DATASOURCES, dss);
                message.getInterceptorChain().add(EndingInterceptor.INSTANCE);
            }
        }
    }

    static class EndingInterceptor extends AbstractPhaseInterceptor<Message> {
        static final EndingInterceptor INSTANCE = new EndingInterceptor();

        EndingInterceptor() {
            super(Phase.PRE_LOGICAL);
        }

        public void handleMessage(Message message) throws Fault {
            Collection<AttachmentDataSource> dss = CastUtils.cast((List<?>)message.get(SAVED_DATASOURCES));
            for (AttachmentDataSource ds : dss) {
                ds.release();
            }
        }
    }

}
