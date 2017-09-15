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
package com.ibm.jbatch.container.services.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.services.IJobXMLSource;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;

/**
 * Note: this class is unused.
 */
public class DirectoryJobXMLLoaderServiceImpl implements IJobXMLLoaderService {

	private final static String CLASSNAME = DirectoryJobXMLLoaderServiceImpl.class.getName();

	public static final String JOB_XML_DIR_PROP = "com.ibm.jbatch.jsl.directory";
	public static final String JOB_XML_PATH = System.getProperty(JOB_XML_DIR_PROP);


	/**
	 * Assuming we get around to supporting this we want to improve the servicability with 
	 * better exception handling.
	 */
	@Override
	public IJobXMLSource loadJSL(String id) {

		IJobXMLSource jobXML = loadJobFromDirectory(JOB_XML_PATH, id);

		return jobXML;
	}


	private static IJobXMLSource loadJobFromDirectory(String dir, String id) {

		File jobXMLFile = new File (JOB_XML_PATH, id + ".xml");

		StreamSource strSource;
		if (jobXMLFile.exists()) {
			strSource = new StreamSource(jobXMLFile);
		} else {
			throw new BatchContainerRuntimeException("The file: " + jobXMLFile + " doesn't exist");
		}
		strSource.setSystemId(jobXMLFile);

		URL fileURL;
		try {
			fileURL = jobXMLFile.toURI().toURL();
		} catch(IOException e) {
			throw new BatchContainerRuntimeException("Exception converting file to URL", e);
		}
		return new JobXMLSource(fileURL, strSource);
		
	}



	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}


	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}

}
