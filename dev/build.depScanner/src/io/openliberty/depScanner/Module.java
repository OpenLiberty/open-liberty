package io.openliberty.depScanner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.ComparableVersion;

public final class Module extends Jar implements Comparable<Module> {
    private static String root;
    private String groupId, artifactId, version;

    public Module(File f) {
        super(f);

        try (JarFile jar = new JarFile(f)) {
            List<JarEntry> entries = jar.stream().filter(entry -> entry.getName().endsWith(".pom")).collect(Collectors.toList());

            if (entries.size() == 1) {
                Properties props = new Properties();
                props.load(jar.getInputStream(entries.get(0)));
                groupId = props.getProperty("groupId");
                artifactId = props.getProperty(("artifactId"));
                version = props.getProperty("version");
            } else {
                File cur = f.getParentFile();
                version = cleanupVersion(cur.getName());
                artifactId = (cur = cur.getParentFile()).getName();

                cur = cur.getParentFile();

                StringBuilder groupId = new StringBuilder();

                do {
                    groupId.insert(0, cur.getName());
                    groupId.insert(0, '.');
                } while ((cur = cur.getParentFile()) != null && !cur.getAbsolutePath().equals(root));

                groupId.deleteCharAt(0);
                this.groupId = groupId.toString();
            }

            if (groupId.startsWith("com.ibm.ws.")) {
                groupId = groupId.substring(11);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // TODO compute the coordinates from the jar
    }

    private String cleanupVersion(String name) {
        String v = name;
        String[] parts = v.split("\\.");
        String prefix;
        int i;
        if (parts.length == 3) {
            prefix = parts[0] + '.' + parts[1];
            if ((parts[2].contains("was")) || (parts[2].contains("ibm") || (parts[2].contains("WAS"))))
                return prefix;
            else {
                if ((i = parts[2].indexOf("-20")) != -1)
                    return (prefix + '.' + (parts[2].substring(0, i)));
                else
                    return (prefix + '.' + parts[2]);
            }
        } else if (parts.length == 4) {
            prefix = parts[0] + '.' + parts[1] + '.' + parts[2];
            if ((parts[3].contains("was")) || (parts[3].contains("ibm") || (parts[3].contains("WAS"))))
                return prefix;
            else {
                if ((i = parts[3].indexOf("-20")) != -1)
                    return (prefix + '.' + (parts[3].substring(0, i)));
                else
                    return (prefix + '.' + parts[3]);
            }
        } else
            return v;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public static void setRoot(String path) {
        root = path;
    }

    @Override
    public int compareTo(Module o) {
        int result = groupId.compareTo(o.groupId);

        if (result == 0) {
            result = artifactId.compareTo(o.artifactId);
            if (result == 0) {
                ComparableVersion thisVersion = new ComparableVersion(version);
                ComparableVersion otherVersion = new ComparableVersion((o.version));
                result = thisVersion.compareTo(otherVersion);
            }
        }

        return result;
    }

    public String getModuleId() {
        return groupId + ":" + artifactId;
    }
}
