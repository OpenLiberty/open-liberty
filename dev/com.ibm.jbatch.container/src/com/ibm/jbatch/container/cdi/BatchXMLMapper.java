/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.cdi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public class BatchXMLMapper {

    private final static Logger logger = Logger.getLogger(BatchXMLMapper.class.getName());

    private final static String BATCH_XML = "META-INF/batch.xml";
    private final static QName BATCH_ROOT_ELEM = new QName("http://xmlns.jcp.org/xml/ns/javaee", "batch-artifacts");

    private ArtifactMap artifactMap;

    private static final Map<ClassLoader, ArtifactMap> loader2ArtifactMap = new WeakHashMap<ClassLoader, ArtifactMap>();
    private final ClassLoader loader;

    public BatchXMLMapper(ClassLoader loader) {
        this.loader = loader;
        artifactMap = loader2ArtifactMap.get(loader);
        if (artifactMap == null) {
            artifactMap = populateArtifactMap(loader);
        }
    }

    /**
     * Get Class object, for the FQCN value in the key-value pair within
     * a batch.xml (loaded as a resource by the classloader which is this object's classloader field).
     * to by the key
     *
     * @param id The key in the key-value pair within batch.xml
     * @return Class object (class loaded by the classloader which is this object's classloader field).
     */

    public Class<?> getArtifactById(String id) {
        // Will always be initialized by this point since done in constructor
        return artifactMap.idToArtifactClassMap.get(id);
    }

    private static synchronized ArtifactMap populateArtifactMap(ClassLoader loader) {

        // See if another thread already initialized this loader's map
        ArtifactMap artifactMap = loader2ArtifactMap.get(loader);

        if (artifactMap != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Map already initialized for loader: " + loader);
            }
            return artifactMap;
        } else {
            artifactMap = new ArtifactMap(loader);
        }

        InputStream is = loader.getResourceAsStream(BATCH_XML);
        if (is != null) {
            artifactMap = populateArtifactMapFromStream(artifactMap, is);
            loader2ArtifactMap.put(loader, artifactMap);
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Did not find batch.xml in classloader: " + loader);
            }
        }
        return artifactMap;
    }

/*
 * Non-validating (e.g. that the artifact type is correct) load
 *
 * TODO - add some logging to the parsing
 */
    protected static ArtifactMap populateArtifactMapFromStream(ArtifactMap tempMap, InputStream is) {
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
            //   <item-processor id=MyItemProcessor class=jsr352/sample/MyItemProcessorImpl/>
            //   ..
            // </batch-artifacts>
            //
            // and have much simpler logic than general-purpose parsing would
            // require.
            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();

                // Until we reach end of document
                if (event == XMLStreamConstants.END_DOCUMENT) {
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
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (!processedRoot) {
                        QName rootQName = xmlStreamReader.getName();
                        if (!rootQName.equals(BATCH_ROOT_ELEM)) {
                            throw new IllegalStateException("Expecting document with root element QName: " + BATCH_ROOT_ELEM
                                                            + ", but found root element with QName: " + rootQName);
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
                        while (event != XMLStreamConstants.END_ELEMENT) {
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

    private static class ArtifactMap {

        private final ClassLoader loader;

        private final Map<String, Class<?>> idToArtifactClassMap = new HashMap<String, Class<?>>();

        // Maps to a list of types not a single type since there's no reason a single artifact couldn't be annotated
        // with >1 batch artifact annotation type.
        private final Map<String, List<String>> idToArtifactTypeListMap = new HashMap<String, List<String>>();

        public ArtifactMap(ClassLoader loader) {
            this.loader = loader;
        }

        /*
         * Init already synchronized, so no need to synch further
         */
        void addEntry(String batchTypeName, String id, String className) {
            try {
                if (!idToArtifactClassMap.containsKey(id)) {
                    Class<?> artifactClass = loader.loadClass(className);

                    idToArtifactClassMap.put(id, artifactClass);
                    List<String> typeList = new ArrayList<String>();
                    typeList.add(batchTypeName);
                    idToArtifactTypeListMap.put(id, typeList);
                } else {

                    Class<?> artifactClass = loader.loadClass(className);

                    // Already contains entry for this 'id', let's make sure it's the same Class
                    // which thus must implement >1 batch artifact "type" (i.e. contains >1 batch artifact annotation).
                    if (!idToArtifactClassMap.get(id).equals(artifactClass)) {
                        Class<?> alreadyLoaded = idToArtifactClassMap.get(id);
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

        private List<String> getBatchTypeList(String id) {
            return idToArtifactTypeListMap.get(id);
        }
    }

}
