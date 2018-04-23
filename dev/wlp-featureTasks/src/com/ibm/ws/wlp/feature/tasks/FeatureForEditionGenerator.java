package com.ibm.ws.wlp.feature.tasks;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class FeatureForEditionGenerator extends Task {

    private Vector<EditionFeature> extraFeatures = new Vector<EditionFeature>();
    
    public EditionFeature createExtraFeatures() {
        EditionFeature data = new EditionFeature();
        extraFeatures.add(data);
        return data;
    }
    
    public class EditionFeature {
        private String edition;
        private Set<String> features = new HashSet<String>();
        
        public EditionFeature() {}
        
        public void setEdition(String edition) {
            this.edition = edition;
        }
        
        public String getEdition() {
            return edition;
        }
        
        public void setFeatures(String features) {
            this.features.addAll(Arrays.asList(features.split(",")));
        }

        public Set<String> getFeatures() {
            return features;
        }
    }

    private File outputFile;

    public void setDest(File file) {
        this.outputFile = file;
    }
    
   
    public void execute() {
 
        
        File f = new File(getProject().getBaseDir(), "..");
        
        File[] dirs = f.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        
        Map<String, Set<String>> featureToEditions = new HashMap<String, Set<String>>();
        
        if (dirs != null) {
            for (File dir : dirs) {
                File[] featureFiles = dir.listFiles(new FileFilter() {
                    
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && pathname.getName().endsWith(".feature");
                    }
                });
                
                if (featureFiles != null) {
                    for (File featureFile : featureFiles) {
                        Properties props = new Properties();
                        try {
                            props.load(new FileReader(featureFile));
                            if ("public".equalsIgnoreCase(props.getProperty("visibility"))) {
                                String kind = props.getProperty("kind");
                                if ("ga".equalsIgnoreCase(kind)) {
                                    String edition = props.getProperty("edition");
                                    if (edition != null) {
                                        edition = edition.toLowerCase();
                                        Set<String> features = obtain(featureToEditions, edition);
                                        features.add(props.getProperty("symbolicName"));
                                    }
                                    
                                    String selector = props.getProperty("selector");
                                    if (selector != null) {
                                        Set<String> features = obtain(featureToEditions, selector);
                                        features.add(props.getProperty("symbolicName"));
                                    }
                                } else if ("beta".equalsIgnoreCase(kind)) {
                                    Set<String> features = obtain(featureToEditions, "beta");
                                    features.add(props.getProperty("symbolicName"));
                                }
                            }
                        } catch (IOException ioe) {
                            throw new BuildException(ioe.getMessage(), ioe);
                        }
                    }
                }
            }
        }
        
        try {
            PrintStream out = new PrintStream(outputFile);
            for (Map.Entry<String, Set<String>> entry : featureToEditions.entrySet()) {
                String edition = entry.getKey();
                Set<String> features = entry.getValue();
                out.print(edition + ".features =");
                boolean first = true;
                for (String feature : features) {
                    if (!!!first) {
                        out.println(", \\");
                    }
                    first = false;
                    out.print(' ');
                    out.print(feature);
                }
                out.println();
                out.println();
            }
            out.close();
        } catch (IOException ioe) {
            throw new BuildException(ioe.getMessage(), ioe);
        }
    }
    
    private Set<String> obtain(Map<String, Set<String>> featureToEditions, String edition) {
        Set<String> features = featureToEditions.get(edition);
        if (features == null) {
            features = new TreeSet<String>();
            
            if (extraFeatures != null) {
                for (EditionFeature extraFeature : extraFeatures) {
                    if (edition.equals(extraFeature.getEdition())) {
                        features.addAll(extraFeature.getFeatures());
                    }
                }
            }
            
            featureToEditions.put(edition, features);

        }
        return features;
    }
}
