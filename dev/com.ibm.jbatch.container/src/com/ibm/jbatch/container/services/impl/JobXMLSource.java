/*
 * Copyright 2014 International Business Machines Corp.
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
package com.ibm.jbatch.container.services.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.batch.operations.BatchRuntimeException;
import javax.xml.transform.stream.StreamSource;

import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.container.ws.impl.IOUtils;

public class JobXMLSource implements IJobXMLSource {

    private StreamSource strSource;
    private String jobXML;
    private URL url;

    public JobXMLSource(URL url, StreamSource strSource) {
        this.url = url;
        this.strSource = strSource;
    }

    public JobXMLSource(String jsl) {
        this.jobXML = jsl;
    }

    @Override
    public StreamSource getJSLStreamSource() {
        return strSource;
    }

    @Override
    public String getJSLString() {
        if (jobXML == null) {
            try {
                jobXML = readJobXML();
            } catch (Exception e) {
                // By this point we should have already parsed the XML, so should not be later
                // receiving an error looking at the String form.
                throw new BatchRuntimeException(e);
            }
        }
        return jobXML;
    }

    /**
     * @return the stringified job xml, as read from the url.
     */
    private String readJobXML() throws IOException {

        StringWriter sw = new StringWriter();

        IOUtils.copyReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8), sw);

        return sw.toString();
    }

}
