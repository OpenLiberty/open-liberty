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

import com.ibm.jbatch.jsl.model.Analyzer;


public class AnalyzerPropertyResolverImpl extends AbstractPropertyResolver<Analyzer> {

	public AnalyzerPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}


	@Override
	public Analyzer substituteProperties(Analyzer analyzer, Properties submittedProps,
			Properties parentProps) {
        /*
        <xs:complexType name="Analyzer">
            <xs:sequence>
                <xs:element name="properties" type="jsl:Properties"
                    minOccurs="0" maxOccurs="1" />
            </xs:sequence>
            <xs:attribute name="ref" use="required" type="jsl:artifactRef" />
        </xs:complexType>
        */
        
        //resolve all the properties used in attributes and update the JAXB model
        analyzer.setRef(this.replaceAllProperties(analyzer.getRef(), submittedProps, parentProps));

        // Resolve all the properties defined for this artifact
        if (analyzer.getProperties() != null) {
            this.resolveElementProperties(analyzer.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        return analyzer;
	}

}
