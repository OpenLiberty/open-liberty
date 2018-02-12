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

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.ibm.jbatch.container.jsl.JSLValidationEventHandler;
import com.ibm.jbatch.container.jsl.ModelResolver;
import com.ibm.jbatch.container.jsl.ValidatorHelper;
import com.ibm.jbatch.jsl.model.JSLJob;

public class JobModelResolverImpl implements ModelResolver<JSLJob> {

    private final static String sourceClass = JobModelResolverImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    public JobModelResolverImpl() {
        super();
    }

    @Override
    public JSLJob resolveModel(final StreamSource source) {
        return unmarshalJobXML(source);
    }

    private JSLJob unmarshalJobXML(final StreamSource source) {
        Object result = null;
        JSLJob job = null;
        InputStream is = null;
        JSLValidationEventHandler handler = new JSLValidationEventHandler();

        try {
            //Defect 178383: get the input stream from the source and ensure it is closed
            //in the finally block to release the xml from being locked
            is = source.getInputStream();

            logger.fine("JobModelResolver start unmarshal");
            final ClassLoader currentClassLoader = AccessController.doPrivileged(
                                                                                 new PrivilegedAction<ClassLoader>() {
                                                                                     @Override
                                                                                     public ClassLoader run() {
                                                                                         return JSLJob.class.getClassLoader();
                                                                                     }
                                                                                 });

            logger.fine("JobModelResolver classloader obtained.");

            JAXBContext ctx = null;
            try {
                ctx = AccessController.doPrivileged(
                                                    new PrivilegedExceptionAction<JAXBContext>() {
                                                        @Override
                                                        public JAXBContext run() throws JAXBException {
                                                            return JAXBContext.newInstance("com.ibm.jbatch.jsl.model", currentClassLoader);
                                                        }
                                                    });
            } catch (PrivilegedActionException e) {
                throw new IllegalArgumentException("Exception creating JAXBContext to unmarshal jobXML", e.getCause());
            }

            logger.fine("JobModelResolver JAXBContext obtained.");

            final Unmarshaller u = ctx.createUnmarshaller();
            u.setSchema(ValidatorHelper.getXJCLSchema());

            u.setEventHandler(handler);

            try {
                result = AccessController.doPrivileged(
                                                       new PrivilegedExceptionAction<Object>() {
                                                           @Override
                                                           public Object run() throws JAXBException {
                                                               return u.unmarshal(source);
                                                           }
                                                       });
            } catch (PrivilegedActionException e) {
                throw new IllegalArgumentException("Exception unmarshalling jobXML", e.getCause());
            }

        } catch (JAXBException e) {
            throw new IllegalArgumentException("Exception unmarshalling jobXML", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new IllegalArgumentException("Exception closing the Input Stream", e);
                }
            }
        }

        if (handler.eventOccurred()) {
            // Not sure we'd get here.
            throw new IllegalArgumentException("JSL invalid per schema");
        }

        job = ((JAXBElement<JSLJob>) result).getValue();

        return job;
    }

    /*
     * Let's not worry about inheritance
     *
     * private JSLJob getJslJobInheritance(String jobId) throws IOException {
     *
     * JSLJob jslJob = null;
     * InputStream indexFileUrl = JobModelResolverImpl.class.getResourceAsStream("/META-INF/jobinheritance");
     *
     * if (indexFileUrl != null) {
     * Properties index = new Properties();
     * index.load(indexFileUrl);
     *
     * if (index.getProperty(jobId) != null) {
     * URL parentUrl = JobModelResolverImpl.class.getResource(index.getProperty(jobId));
     * String parentXml = readJobXML(parentUrl.getFile());
     *
     * jslJob = resolveModel(parentXml);
     * }
     * }
     * return jslJob;
     * }
     */

    // FIXME HashMap<String, Split> splitid2InstanceMap = new HashMap<String,Split>();
    // FIXME HashMap<String, Flow> flowid2InstanceMap = new HashMap<String,Flow>();

    //
    // This is where we will implement job/step inheritance, though we don't at
    // the moment.
    //
    /*
     * public static ResolvedJob resolveJob(Job job) {
     * ArrayList<ResolvedStep> steps = new ArrayList<ResolvedStep>();
     * ArrayList<ResolvedDecision> decisions = new ArrayList<ResolvedDecision>();
     * ArrayList<ResolvedSplit> splits = new ArrayList<ResolvedSplit>();
     * ArrayList<ResolvedFlow> flows = new ArrayList<ResolvedFlow>();
     *
     * ResolvedJob resolvedJob = new ResolvedJob(job.getId(), steps, decisions, splits, flows);
     *
     * for (Object next : job.getControlElements()) {
     * if (next instanceof Step) {
     * steps.add(new ResolvedStep(resolvedJob, (Step) next));
     * } else if (next instanceof Decision) {
     * decisions.add(new ResolvedDecision(resolvedJob, (Decision) next));
     * } else if (next instanceof Split) {
     * splits.add(new ResolvedSplit(resolvedJob, (Split) next));
     * } else if (next instanceof Flow) {
     * flows.add(new ResolvedFlow(resolvedJob, (Flow) next));
     * }
     * }
     *
     * return resolvedJob;
     * }
     *
     *
     * //FIXME We started implementing job inheritance here. Set to private so no one uses this yet.
     * private static ResolvedJob resolveModel(Job leafJob) {
     * String parentID = leafJob.getParent();
     *
     * Job resolvedJob = resolveModel(leafJob, parentID);
     * // FIXME you need to create a new ResolvedJob here.
     * return null;
     *
     * }
     *
     * private static Job resolveModel(Job leafJob, String parentID) {
     *
     * if (!parentID.equals("")) {
     * Job parentJob = jobid2InstanceMap.get(parentID);
     * if (parentJob == null) {
     * throw new BatchContainerRuntimeException(new IllegalArgumentException(), "The parent job id '" + parentID + "' on Job id '"
     * + leafJob.getParent() + " cannot be found");
     * }
     *
     * // add all the attributes, steps, flows, and splits from the parent
     * // to child if they don't exist on child
     * leafJob.getControlElements().addAll(parentJob.getControlElements());
     *
     * return resolveModel(leafJob, parentJob.getParent());
     *
     * }
     *
     * for (Object next : leafJob.getControlElements()) {
     * if (next instanceof Step) {
     * resolveModel((Step)next);
     * } else if (next instanceof Split) {
     * //resolveModel((Split)next);
     * } else if (next instanceof Flow) {
     * //resolveModel((Flow)next);
     * }
     *
     * }
     *
     * return leafJob;
     *
     * }
     *
     * //FIXME Set to private so no one uses this yet.
     * private static ResolvedStep resolveModel(Step leafStep) {
     *
     * String parentID = leafStep.getParent();
     *
     * Step resolvedStep = resolveModel(leafStep, parentID);
     *
     * // FIXME you need to clone the step to a resolved step
     * return null;
     *
     * }
     *
     * private static Step resolveModel(Step leafStep, String parentID) {
     * if (!parentID.equals("")) {
     * Step parentStep = stepid2InstanceMap.get(parentID);
     * if (parentStep == null) {
     * throw new BatchContainerRuntimeException(new IllegalArgumentException(), "The parent step id '" + parentID
     * + "' on Step id '" + leafStep.getParent() + " cannot be found");
     * }
     *
     * // add all the attributes, batchlets, chunks...etc from a parent
     * // step if they don't
     * // exist on the child step
     * // leafStep.getXXX().addAll(parentStep.getXXX());
     *
     * return resolveModel(leafStep, parentStep.getParent());
     *
     * }
     *
     * // batchlet
     * //
     *
     * // resolve chunks
     *
     * // next, startlimit ...etc
     *
     * // FIXME ...
     *
     * return leafStep;
     * }
     */

}
