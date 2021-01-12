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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

public class StaxInEndingInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(StaxInEndingInterceptor.class); // Liberty change: line is added

    //certain usages of CXF may require the Stax stream to remain open (example: streaming the stax stuff
    //directly to the client applications).  Provide a flag to turn off.
    public static final String STAX_IN_NOCLOSE = StaxInEndingInterceptor.class.getName() + ".dontClose";

    public static final StaxInEndingInterceptor INSTANCE = new StaxInEndingInterceptor();

    @Trivial  // Liberty change: line is added
    public StaxInEndingInterceptor() {
        super(Phase.PRE_INVOKE);
    }
    public StaxInEndingInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(@Sensitive Message message) { // Liberty change: @Sensitive is added to parameter
        LOG.entering("StaxInEndingInterceptor", "handleMessage"); // Liberty change: line is added
        XMLStreamReader xtr = message.getContent(XMLStreamReader.class);
        if (xtr != null && !MessageUtils.getContextualBoolean(message, STAX_IN_NOCLOSE, false)) {
            try {
                StaxUtils.close(xtr);
            } catch (XMLStreamException ex) {
                throw new Fault(ex);
            }
            message.removeContent(XMLStreamReader.class);
        }
        LOG.exiting("StaxInEndingInterceptor", "handleMessage");  // Liberty change: line is added
    }
}
