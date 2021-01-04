/*
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.container.jsl.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import com.ibm.jbatch.container.jsl.JSLValidationEventHandler;
import com.ibm.jbatch.container.jsl.ModelResolver;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.jsl.ValidatorHelper;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;

import test.common.SharedOutputManager;

public class JobModelTest {

    @Test
    public void testModelNoValidate() throws Exception {
        modelNoValidate("com.ibm.jbatch.jsl.model.v1", "test/files/valid.job1.xml");
        modelNoValidate("com.ibm.jbatch.jsl.model.v2", "test/files/valid.job2.xml");
    }

    private void modelNoValidate(String packageName, String fileName) throws Exception {

        JAXBContext ctx = JAXBContext.newInstance(packageName);

        Unmarshaller u = ctx.createUnmarshaller();
        FileInputStream fis = new FileInputStream(new File(fileName));

        // Use this for anonymous type
        //Job job = (Job)u.unmarshal(url.openStream());

        // Use this for named complex type, which is what the spec uses.
        Object elem = u.unmarshal(fis);
        JSLJob job = (JSLJob) ((JAXBElement) elem).getValue();

        assertEquals("job1", job.getId());
        assertEquals(1, job.getExecutionElements().size());
        Step step = (Step) job.getExecutionElements().get(0);
        assertEquals("step1", step.getId());
        Batchlet b = step.getBatchlet();
        assertEquals("step1Ref", b.getRef());
    }

    @Test
    public void testModelValidate() throws Exception {
        modelValidate("com.ibm.jbatch.jsl.model.v1", JobModelHandler.SCHEMA_LOCATION_V1, "test/files/valid.job1.xml");
        modelValidate("com.ibm.jbatch.jsl.model.v2", JobModelHandler.SCHEMA_LOCATION_V2, "test/files/valid.job2.xml");
    }

    private void modelValidate(String packageName, String schema, String fileName) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(packageName);

        Unmarshaller u = ctx.createUnmarshaller();
        u.setSchema(ValidatorHelper.getXJCLSchema(schema));
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        u.setEventHandler(handler);
        FileInputStream fis = new FileInputStream(new File(fileName));

        // Use this for anonymous type
        //Job job = (Job)u.unmarshal(url.openStream());

        // Use this for named complex type, which is what the spec uses.
        Object elem = u.unmarshal(fis);
        assertFalse("XSD invalid, see sysout", handler.eventOccurred());

        JSLJob job = (JSLJob) ((JAXBElement) elem).getValue();

        assertEquals("job1", job.getId());
        assertEquals(1, job.getExecutionElements().size());
        Step step = (Step) job.getExecutionElements().get(0);
        assertEquals("step1", step.getId());
        Batchlet b = step.getBatchlet();
        assertEquals("step1Ref", b.getRef());
    }

    @Test
    public void testModelResolver() throws Exception {
        modelResolver("test/files/valid.job1.xml", true);
        modelResolver("test/files/valid.job2.xml", true);
        modelResolver("test/files/valid.job1.xml", false);
        modelResolver("test/files/valid.job2.xml", false);
    }

    private void modelResolver(String fileName, boolean stream) throws Exception {
        StreamSource streamSource;
        if (stream) {
            streamSource = new StreamSource(new FileInputStream(fileName));
        } else {
            streamSource = new StreamSource(new FileReader(fileName));
        }
        ModelResolver<JSLJob> resolver = ModelResolverFactory.createJobResolver();
        JSLJob job = resolver.resolveModel(streamSource);

        assertEquals("job1", job.getId());
        assertEquals(1, job.getExecutionElements().size());
        Step step = (Step) job.getExecutionElements().get(0);
        assertEquals("step1", step.getId());
        Batchlet b = step.getBatchlet();
        assertEquals("step1Ref", b.getRef());
    }

    @Test
    public void testInvalidCWWKY0003E() throws Exception {
        modelInvalid("com.ibm.jbatch.jsl.model.v1", JobModelHandler.SCHEMA_LOCATION_V1, "test/files/invalid.job1.xml");
        modelInvalid("com.ibm.jbatch.jsl.model.v2", JobModelHandler.SCHEMA_LOCATION_V2, "test/files/invalid.job2.xml");
    }

    private void modelInvalid(String packageName, String schema, String fileName) throws Exception {
        SharedOutputManager outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        try {

            JAXBContext ctx = JAXBContext.newInstance(packageName);

            Unmarshaller u = ctx.createUnmarshaller();
            u.setSchema(ValidatorHelper.getXJCLSchema(schema));
            JSLValidationEventHandler handler = new JSLValidationEventHandler();
            u.setEventHandler(handler);

            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            StreamSource strSource = new StreamSource(fis);
            strSource.setSystemId(f);

            // Use this for anonymous type
            //Job job = (Job)u.unmarshal(url.openStream());

            // Use this for named complex type, which is what the spec uses.
            boolean caughtExc = false;
            try {
                Object elem = u.unmarshal(strSource);
            } catch (UnmarshalException e) {
                caughtExc = true;
            }
            assertTrue("XSD invalid", caughtExc);
            String msg = "CWWKY0003E";
            assertTrue("Unable to find JSL schema-invalid message CWWKY0003E ", outputMgr.checkForMessages(msg));

        } catch (AssertionError err) {
            outputMgr.dumpStreams();
            throw err;
        } finally {
            outputMgr.restoreStreams();
        }
    }

}
