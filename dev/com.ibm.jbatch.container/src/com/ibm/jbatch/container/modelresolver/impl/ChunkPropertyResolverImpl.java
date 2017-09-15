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
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.jsl.model.Chunk;


public class ChunkPropertyResolverImpl extends AbstractPropertyResolver<Chunk> {



    public ChunkPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public Chunk substituteProperties(final Chunk chunk, final Properties submittedProps, final Properties parentProps) {
        /*
    <xs:complexType name="Chunk">
        <xs:sequence>
            <xs:element name="reader" type="jsl:ItemReader" />
            <xs:element name="processor" type="jsl:ItemProcessor" minOccurs="0" maxOccurs="1" />
            <xs:element name="writer" type="jsl:ItemWriter" />
            <xs:element name="checkpoint-algorithm" type="jsl:CheckpointAlgorithm" minOccurs="0"
                maxOccurs="1" />
            <xs:element name="skippable-exception-classes" type="jsl:ExceptionClassFilter"
                minOccurs="0" maxOccurs="1" />
            <xs:element name="retryable-exception-classes" type="jsl:ExceptionClassFilter"
                minOccurs="0" maxOccurs="1" />
            <xs:element name="no-rollback-exception-classes" type="jsl:ExceptionClassFilter"
                minOccurs="0" maxOccurs="1" />
        </xs:sequence>
        <xs:attribute name="checkpoint-policy" use="optional" type="xs:string">
 
        </xs:attribute>
        <xs:attribute name="item-count" use="optional" type="xs:string">
        </xs:attribute>
        <xs:attribute name="time-limit" use="optional" type="xs:string">

        </xs:attribute>
        <xs:attribute name="skip-limit" use="optional" type="xs:string">
        </xs:attribute>
        <xs:attribute name="retry-limit" use="optional" type="xs:string">
        </xs:attribute>
    </xs:complexType>
        */
        
        //resolve all the properties used in attributes and update the JAXB model
        chunk.setCheckpointPolicy(this.replaceAllProperties(chunk.getCheckpointPolicy(), submittedProps, parentProps));
        chunk.setItemCount(this.replaceAllProperties(chunk.getItemCount(), submittedProps, parentProps));
        chunk.setTimeLimit(this.replaceAllProperties(chunk.getTimeLimit(), submittedProps, parentProps));
        chunk.setSkipLimit(this.replaceAllProperties(chunk.getSkipLimit(), submittedProps, parentProps));
        chunk.setRetryLimit(this.replaceAllProperties(chunk.getRetryLimit(), submittedProps, parentProps));

        // Resolve Reader properties
        if (chunk.getReader() != null) {
            PropertyResolverFactory.createReaderPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getReader(), submittedProps, parentProps);
        }
        
        // Resolve Processor properties
        if (chunk.getProcessor() != null) {
            PropertyResolverFactory.createProcessorPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getProcessor(), submittedProps, parentProps);
        }
        
        // Resolve Writer properties
        if (chunk.getWriter() != null) {
            PropertyResolverFactory.createWriterPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getWriter(), submittedProps, parentProps);
        }
        
        // Resolve CheckpointAlgorithm properties
        if (chunk.getCheckpointAlgorithm() != null) {
            PropertyResolverFactory.createCheckpointAlgorithmPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getCheckpointAlgorithm(), submittedProps, parentProps);
        }

        // Resolve SkippableExceptionClasses properties
        if (chunk.getSkippableExceptionClasses() != null) {
            PropertyResolverFactory.createSkippableExceptionClassesPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getSkippableExceptionClasses(), submittedProps, parentProps);
        }
        
        // Resolve RetryableExceptionClasses properties
        if (chunk.getRetryableExceptionClasses() != null) {
            PropertyResolverFactory.createRetryableExceptionClassesPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getRetryableExceptionClasses(), submittedProps, parentProps);
        }
        
        // Resolve NoRollbackExceptionClasses properties
        if (chunk.getNoRollbackExceptionClasses() != null) {
            PropertyResolverFactory.createNoRollbackExceptionClassesPropertyResolver(this.isPartitionedStep).substituteProperties(chunk.getNoRollbackExceptionClasses(), submittedProps, parentProps);
        }
        
        //FIXME There are more properties to add in here for the rest of the chunk elements
        
        return chunk;

    }

}
