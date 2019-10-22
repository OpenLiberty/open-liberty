/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal.dispatcher;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import javax.batch.operations.BatchRuntimeException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.ibm.jbatch.container.ws.PartitionPlanConfig;
import com.ibm.jbatch.jsl.model.Step;


/**
 * The serialized object payload for the ObjectMessage used by JMS startPartition messages.
 */
public class StartPartitionPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private PartitionPlanConfig partitionPlanConfig;
    
    private String stepXml;
    
    private byte[] securityContext;

    /**
     * CTOR.
     */
    public StartPartitionPayload(PartitionPlanConfig partitionPlanConfig, Step step, byte[] securityContext) {
        this.partitionPlanConfig = partitionPlanConfig;
        this.stepXml = marshalStep( step );
        this.securityContext = securityContext;
    }
    
    public PartitionPlanConfig getPartitionPlanConfig() {
        return partitionPlanConfig;
    }
    
    public Step getStep() {
        return unmarshalStep( stepXml ) ;
    }
  
    public byte[] getSecurityContext() {
        return securityContext;
    }
    
    /**
     * @return The given Step in stringified XML
     */
    private String marshalStep( Step step ) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance( Step.class.getPackage().getName(), Step.class.getClassLoader() );
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter sw = new StringWriter();
            marshaller.marshal( step, sw );
            return sw.toString();
        } catch (JAXBException je) {
            throw new BatchRuntimeException("Could not marshal Step to XML: " + step, je);
        }
    }
    
    /**
     * @return the given stepXml as a Step object
     */
    private Step unmarshalStep(String stepXml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance( Step.class.getPackage().getName(), Step.class.getClassLoader() );
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Step) unmarshaller.unmarshal( new StringReader(stepXml) );
        } catch (JAXBException je) {
            throw new BatchRuntimeException("Could not unmarshal Step XML: " + stepXml, je);
        }
    }
}
