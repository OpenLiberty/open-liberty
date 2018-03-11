package com.ibm.ws.wlp.feature.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Manifest;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

public class FeatureBuilder extends Builder {

    public Map.Entry<String, Attrs> getSubsystemSymbolicName() {
        Parameters p = getParameters(FeatureBnd.SUBSYSTEM_SYMBOLIC_NAME);
        if (p.isEmpty())
            return null;
        return p.entrySet().iterator().next();

    }

    public Parameters getSubsystemContent() {
        Parameters p = getParameters("Subsystem-Content");
        return p;
    }

    public void writeManifest(File f) throws Exception {
        init();
        setJar(new Jar("dot"));
        Manifest m = calcManifest();
        FileWriter writer = new FileWriter(f);
        Map<String, String> mainMap = new TreeMap<String, String>();

        for (Map.Entry<Object, Object> entry : m.getMainAttributes().entrySet()) {
            mainMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }

        for (Map.Entry<String, String> entry : mainMap.entrySet()) {
            writer.append(entry.getKey());
            writer.append(": ");
            String value = entry.getValue();
            if (value.indexOf(',') == -1) {
                writer.append(entry.getValue());
            } else {
                Parameters p = OSGiHeader.parseHeader(value);
                boolean lineContinuation = false;
                for (Map.Entry<String, Attrs> data : p.entrySet()) {
                    if (lineContinuation) {
                        writer.append(",\r\n ");
                    }
                    // bnd might have added ~ characters if there are duplicates in 
                    // the source, so we should remove them before we output it so we
                    // get back to the original intended content.
                    String name = data.getKey();
                    int index = name.indexOf('~');
                    if (index != -1) {
                        name = name.substring(0, index);
                    }
                    writer.append(name);
                    Attrs attrbs = data.getValue();
                    for (Map.Entry<String, String> attrib : attrbs.entrySet()) {
                        writer.append("; ");
                        writer.append(attrib.getKey());
                        writer.append('=');
                        StringBuilder builder = new StringBuilder();
                        quote(builder, attrib.getValue());
                        writer.append(builder.toString());
                    }
                    lineContinuation = true;
                }
            }
            writer.append("\r\n");
        }

        writer.close();

        setJar((Jar) null);
    }

    public void setSubsystemContent(Parameters content) throws IOException {
        setProperty("Subsystem-Content", printClauses(content.asMapMap()));
    }

    public Set<Map.Entry<String, Attrs>> getFiles() {
        return getContent("-files");
    }

    public Set<Map.Entry<String, Attrs>> getJars() {
        return getContent("-jars");
    }

    public Set<Map.Entry<String, Attrs>> getFeatures() {
        return getContent("-features");
    }

    public Set<Map.Entry<String, Attrs>> getBundles() {
        return getContent("-bundles");
    }
    
    private Set<Map.Entry<String, Attrs>> getContent(String contentType) {
        String jars = getProperty(contentType, "");
        if (jars == null || jars.length() == 0) {
            return Collections.emptySet();
        }
        Parameters p = new Parameters(jars);
        return p.entrySet();
    }

    public void setAppliesTo(String name, Attrs attributes) throws IOException {
        Parameters p = new Parameters();
        p.put(name, attributes);

        setProperty("IBM-AppliesTo", printClauses(p.asMapMap()));
    }

    public void setSubsystemSymbolicName(Parameters p) throws IOException {
        setProperty(FeatureBnd.SUBSYSTEM_SYMBOLIC_NAME, printClauses(p.asMapMap()));
    }

}
