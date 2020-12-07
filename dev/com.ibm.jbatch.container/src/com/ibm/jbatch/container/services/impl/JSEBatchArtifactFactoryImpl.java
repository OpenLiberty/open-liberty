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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchConfig;

public class JSEBatchArtifactFactoryImpl implements IBatchArtifactFactory, XMLStreamConstants {

	private final static Logger logger = Logger.getLogger(JSEBatchArtifactFactoryImpl.class.getName());
	private final static String CLASSNAME = JSEBatchArtifactFactoryImpl.class.getName();

	// TODO - surface constants
	private final static String BATCH_XML = "META-INF/batch.xml";
	private final static QName BATCH_ROOT_ELEM = new QName("http://xmlns.jcp.org/xml/ns/javaee", "batch-artifacts");
        private final static QName BATCH_ROOT_ELEM_2 = new QName("https://jakarta.ee/xml/ns/jakartaee", "batch-artifacts");

	// TODO - synchronize appropriately once we learn more about usage
	private boolean loaded = false;
	private volatile ArtifactMap artifactMap = null;

	// Uses TCCL
	@Override
	public Object load(String batchId) {
		String methodName = "load";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(CLASSNAME, methodName, "Loading batch artifact id = " + batchId);
		}

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("TCCL = " + tccl);
		}

		initArtifactMapFromClassLoader(tccl);

		Object loadedArtifact = artifactMap.getArtifactById(batchId);

		if (loadedArtifact == null) {
			throw new IllegalArgumentException("Could not load any artifacts with batch id=" + batchId);
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", loaded artifact instance: " +
					loadedArtifact + " of type: " + loadedArtifact.getClass().getCanonicalName());
		}
		return loadedArtifact;
	}

	private void initArtifactMapFromClassLoader(ClassLoader loader) {
		/*
		 * Following pattern in:
		 *   http://en.wikipedia.org/wiki/Double-checked_locking
		 */
		ArtifactMap tempMap = artifactMap;
		if (tempMap == null) {
			synchronized(this) {
				tempMap = artifactMap;
				if (tempMap == null) {
					tempMap = new ArtifactMap();                    
					InputStream is = getBatchXMLStreamFromClassLoader(loader);
					artifactMap = populateArtifactMapFromStream(tempMap, is);					
				}
			}
		}
	}

	protected InputStream getBatchXMLStreamFromClassLoader(ClassLoader loader) {
		InputStream is = loader.getResourceAsStream(BATCH_XML);

		if (is == null) {
			throw new IllegalStateException("Unable to load batch.xml");
		}

		return is;
	}

	/*
	 * Non-validating (e.g. that the artifact type is correct) load
	 * 
	 * TODO - add some logging to the parsing
	 */
	protected ArtifactMap populateArtifactMapFromStream(ArtifactMap tempMap, InputStream is) {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();


		try {
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(is);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Loaded XMLStreamReader = " + xmlStreamReader);
			}

			boolean processedRoot = false;

			// We are going to take advantage of the simplified structure of a
			// line
			// E.g.:
			// <batch-artifacts>
			//   <ref id="myItemProcessor" class="jsr352/sample/MyItemProcessorImpl" />
			//   ..
			// </batch-artifacts>
			//
			// and have much simpler logic than general-purpose parsing would
			// require.
			while (xmlStreamReader.hasNext()) {
				int event = xmlStreamReader.next();

				// Until we reach end of document
				if (event == END_DOCUMENT) {
					break;
				}

				// At this point we have either:
				//    A) just passed START_DOCUMENT, and are at START_ELEMENT for the root, 
				//       <batch-artifacts>, or 
				//    B) we have just passed END_ELEMENT for one of the artifacts which is a child of
				//       <batch-artifacts>.
				//   
				//  Only handle START_ELEMENT now so we can skip whitespace CHARACTERS events.
				//
				if (event == START_ELEMENT) {
					if (!processedRoot) {
						QName rootQName = xmlStreamReader.getName();
			                        if (!rootQName.equals(BATCH_ROOT_ELEM) && !rootQName.equals(BATCH_ROOT_ELEM_2)) {
			                            throw new IllegalStateException("Expecting document with root element QName: " + BATCH_ROOT_ELEM
			                                                            + " or " + BATCH_ROOT_ELEM_2 + ", but found root element with QName: " + rootQName);
						} else {
							processedRoot = true;
						}
					} else {

						// Should only need localName
						String annotationShortName = xmlStreamReader.getLocalName();
						String id = xmlStreamReader.getAttributeValue(null, "id");
						String className = xmlStreamReader.getAttributeValue(null, "class");
						tempMap.addEntry(annotationShortName, id, className);

						// Ignore anything else (text/whitespace) within this
						// element
						while (event != END_ELEMENT) {
							event = xmlStreamReader.next();
						}
					}
				}
			}
			xmlStreamReader.close();
			is.close();
			return tempMap;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class ArtifactMap {

		private Map<String, Class> idToArtifactClassMap = new HashMap<String, Class>();

		// Maps to a list of types not a single type since there's no reason a single artifact couldn't be annotated
		// with >1 batch artifact annotation type.
		private Map<String, List<String>> idToArtifactTypeListMap = new HashMap<String, List<String>>();

		/*
		 * Init already synchronized, so no need to synch further
		 */
		private void addEntry(String batchTypeName, String id, String className) {
			try {
				if (!idToArtifactClassMap.containsKey(id)) {
					Class<?> artifactClass = Thread.currentThread().getContextClassLoader().loadClass(className);

					idToArtifactClassMap.put(id, artifactClass);
					List<String> typeList = new ArrayList<String>();
					typeList.add(batchTypeName);                    
					idToArtifactTypeListMap.put(id, typeList);                    
				} else {

					Class<?> artifactClass = Thread.currentThread().getContextClassLoader().loadClass(className);

					// Already contains entry for this 'id', let's make sure it's the same Class
					// which thus must implement >1 batch artifact "type" (i.e. contains >1 batch artifact annotation).
					if (!idToArtifactClassMap.get(id).equals(artifactClass)) {
					    Class alreadyLoaded = idToArtifactClassMap.get(id); 
					    String msg = "Attempted to load batch artifact with id: " + id + ", and className: " + className + 
					                 ". Found: " + artifactClass + ", however the artifact id: " + id + 
					                 " is already associated with: " + alreadyLoaded + ", of className: " + alreadyLoaded.getCanonicalName();

					    throw new IllegalArgumentException(msg);
					}
					List<String> typeList = idToArtifactTypeListMap.get(id);
					typeList.add(batchTypeName);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Object getArtifactById(String id) {

			Object artifactInstance = null;

			try {
				Class clazz = idToArtifactClassMap.get(id);
				if (clazz != null) {
					artifactInstance = (idToArtifactClassMap.get(id)).newInstance();	
				}
			} catch (IllegalAccessException e) {
				throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + id, e);
			} catch (InstantiationException e) {
				throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + id, e);
			}


			return artifactInstance;
		}

		private List<String> getBatchTypeList(String id) {
			return idToArtifactTypeListMap.get(id);
		}

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
