/**
 *
 */
package io.openliberty.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 */
public class PostProcessBNDPom {

    private static List<Dependency> primaryPomDepList;

    private static List<Gav> lost;
    private static List<Gav> shippedAndFound;

    private static GavCalculator calc;
    private static String jarPath;

    /**
     * @param args
     */
    public static void main(String[] args) {

        jarPath = args[0];
        Model pom = readJARPom(jarPath);
        removeDevDependecies(pom);
        replacePomFile(pom,jarPath);

        

    }

    /**
     * @param jarFile2
     */
    private static Model readJARPom(String path) {
        // TODO Auto-generated method stub

        ZipFile jar = new ZipFile(path);
        if (jar != null) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().contains("pom.xml")) {
                    InputStream stream = jar.getInputStream(entry);
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = null;
                    try {
                        model = reader.read(stream);
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return model;
            }

        }

    }

    /**
     *
     */
    private static void removeNotShippedFromLost() {
        for (Dependency dep : primaryPomDepList) {
            for (Gav gav : lost) {
                if (dep.getGroupId().equals(gav.getGroupId()) &&
                    dep.getArtifactId().equals(gav.getArtifactId()) &&
                    dep.getVersion().equals(gav.getVersion()))
                    shippedAndFound.add(gav);
            }
        }

        System.out.println("From " + jsonFile + " found " + shippedAndFound.size() + " artifacts shipped");
        for (Gav gav : shippedAndFound) {

            System.out.println(gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion());
        }

    }

    /**
     * @param jsonFile
     */
    private static void readJSON(String jsonFile) {
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = null;
        try {
            jsonArray = (JSONArray) parser.parse(new FileReader(jsonFile));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (Object o : jsonArray) {
            JSONObject artifact = (JSONObject) o;

            String artifactPath = (String) artifact.get("path");
            Gav gav = calc.pathToGav(artifactPath);
            if (gav != null) {
                //System.out.println(gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion());
                lost.add(gav);
            } else
                System.out.println("Problem parsing GAV: " + artifactPath);
        }

    }

    /**
     * @param pomDir
     */
    private static void readInPoms(String pomFolder) {

        File folder = new File(pomFolder);
        File[] children = folder.listFiles();

        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (!child.isDirectory())
                continue;

            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = null;
            try {
                model = reader.read(new FileReader(child.getName() + "/pom.xml"));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            List<Dependency> list = model.getDependencies();
            primaryPomDepList.addAll(list);

        }
//        int i = 1;
//        for (Dependency o : primaryPomDepList) {
//
//            System.out.println(i + " " + o);
//            i++;
//        }

    }

}
