/**
 *
 */
package io.openliberty.process;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 */
public class PostProcessBNDPom {

    private static String jarPath;
    private static String pomEntryPath;
    private static List<String> filteredGroups = Arrays.asList("org.springframework");

    /**
     * @param args
     */
    public static void main(String[] args) {

        jarPath = args[0];
        String outputDir = args[1];
        System.out.println("Reading jar: " + jarPath);
        Model pom = readJARPom(jarPath);
        removeDevDependecies(pom);
        writeTempPom(pom, outputDir);
        System.out.println("Writing pom to: " + outputDir);
        replacePomFile(outputDir + "/pom.xml", jarPath);

    }

    /**
     * @param pom
     * @return
     */
    private static void writeTempPom(Model pom, String path) {

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(new FileWriter(path + "/pom.xml"), pom);
        } catch (IOException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
    }

    /**
     * @param pom
     * @param jarPath2
     */
    private static void replacePomFile(String pomPath, String jarPath) {

        Path myFilePath = Paths.get(pomPath);

        Path zipFilePath = Paths.get(jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, null)) {
            Path fileInsideZipPath = fs.getPath(pomEntryPath);
            Files.copy(myFilePath, fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * @param pom
     */
    private static void removeDevDependecies(Model pom) {
        // TODO Auto-generated method stub

        List<Dependency> deps = pom.getDependencies();
        for (Iterator iterator = deps.iterator(); iterator.hasNext();) {
            Dependency dependency = (Dependency) iterator.next();
            if ((dependency.getGroupId().equals("dev")) || (filteredGroups.contains(dependency)))
                iterator.remove();
        }

    }

    /**
     * @param jarFile2
     */
    private static Model readJARPom(String path) {
        // TODO Auto-generated method stub

        ZipFile jar = null;
        try {
            jar = new ZipFile(path);
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        if (jar != null) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream stream = null;
                if (entry.getName().contains("pom.xml")) {
                    try {
                        pomEntryPath = entry.getName();
                        stream = jar.getInputStream(entry);
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = null;
                    try {
                        if (stream != null)
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
                    try {
                        jar.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return model;
                }

            }
        }
        try {
            jar.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
