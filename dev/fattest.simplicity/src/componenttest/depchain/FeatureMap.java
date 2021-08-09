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
package componenttest.depchain;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.log.Log;

public class FeatureMap extends HashMap<String, Feature> {

    private static final long serialVersionUID = 1L;
    private static Class<?> c = FeatureMap.class;
    private static FeatureMap instance = null;

    public static FeatureMap instance(File featureList) throws Exception {
        if (instance != null)
            return instance;
        return instance = new FeatureMap(featureList);
    }

    public static void reset() {
        instance = null;
    }

    private FeatureMap(File featureList) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(featureList);
        NodeList nodes = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if ((n instanceof Element) && (!"defaultConfiguration".equals(n.getNodeName()))) {
                // Parse the feature data from the XML element
                Feature feature = new Feature((Element) n);
                this.put(feature.getSymbolicName(), feature);
                if (feature.getShortName() != null)
                    this.put(feature.getShortName(), feature);
                if (FeatureDependencyProcessor.DEBUG)
                    Log.debug(c, "Found feature: " + feature);
            }
        }
    }

    @Override
    public Feature get(Object key) {
        // Keys are case insensitive
        Feature f = super.get(key);
        if (f != null || key == null || !(key instanceof String))
            return f;
        return super.get(((String) key).toLowerCase());
    }

    @Override
    public Feature put(String key, Feature value) {
        // Keys are case insensitive
        return super.put(key.toLowerCase(), value);
    }

}
