package io.smallrye.graphql.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This creates an index from the classpath.
 * Based on similar class in LRA
 * (https://github.com/jbosstm/narayana/blob/master/rts/lra/lra-proxy/api/src/main/java/io/narayana/lra/client/internal/proxy/nonjaxrs/ClassPathIndexer.java)
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class IndexInitializer {

    @FFDCIgnore(IOException.class)
    public IndexView createIndex(Set<URL> urls) {
        List<IndexView> indexes = new ArrayList<>();

        // TODO: Read all jandex.idx in the classpath: 
        // something like Enumeration<URL> systemResources = ClassLoader.getSystemResources(JANDEX_IDX);

        // Check in this war
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(JANDEX_IDX)) {
            IndexReader reader = new IndexReader(stream);
            IndexView i = reader.read();
            SmallRyeGraphQLServletLogging.log.loadedIndexFrom(JANDEX_IDX);
            indexes.add(i);
        } catch (IOException ex) {
            SmallRyeGraphQLServletLogging.log.generatingIndex();
        }

        // Classes in this artifact
        IndexView i = createIndexView(urls);
        indexes.add(i);

        return merge(indexes);
    }

    public IndexView createIndex() {
        Set<URL> urls = getUrlFromClassPath();
        return createIndexView(urls);
    }

    @FFDCIgnore(IOException.class)
    private IndexView createIndexView(Set<URL> urls) {
        Indexer indexer = new Indexer();
        for (URL url : urls) {
            try {
                if (url.toString().endsWith(DOT_JAR) || url.toString().endsWith(DOT_WAR)) {
                    SmallRyeGraphQLServletLogging.log.processingFile(url.toString());
                    processJar(url.openStream(), indexer);
                } else {
                    processFolder(url, indexer);
                }
            } catch (IOException ex) {
                SmallRyeGraphQLServletLogging.log.cannotProcessFile(url.toString(), ex);
            }
        }

        return indexer.complete();
    }

    @FFDCIgnore(MalformedURLException.class)
    private Set<URL> collectURLsFromClassPath() {
        Set<URL> urls = new HashSet<>();
        for (String s : System.getProperty(JAVA_CLASS_PATH).split(System.getProperty(PATH_SEPARATOR))) {
            try {
                urls.add(Paths.get(s).toUri().toURL());
            } catch (MalformedURLException e) {
                SmallRyeGraphQLServletLogging.log.cannotCreateUrl(e);
            }
        }

        return urls;
    }

    @FFDCIgnore(URISyntaxException.class)
    private void processFolder(URL url, Indexer indexer) throws IOException {
        try {
            Path folderPath = Paths.get(url.toURI());
            if (Files.isDirectory(folderPath)) {
                try (Stream<Path> walk = Files.walk(folderPath)) {

                    List<Path> collected = walk
                            .filter(Files::isRegularFile)
                            .collect(Collectors.toList());

                    for (Path c : collected) {
                        String entryName = c.getFileName().toString();
                        processFile(entryName, Files.newInputStream(c), indexer);
                    }
                }
            } else {
                SmallRyeGraphQLServletLogging.log.ignoringUrl(url);
            }

        } catch (URISyntaxException ex) {
            SmallRyeGraphQLServletLogging.log.couldNotProcessUrl(url, ex);
        }
    }

    private void processJar(InputStream inputStream, Indexer indexer) throws IOException {

        ZipInputStream zis = new ZipInputStream(inputStream, StandardCharsets.UTF_8);
        ZipEntry ze;

        while ((ze = zis.getNextEntry()) != null) {
            String entryName = ze.getName();
            processFile(entryName, zis, indexer);
        }
    }

    private void processFile(String fileName, InputStream is, Indexer indexer) throws IOException {
        if (fileName.endsWith(DOT_CLASS)) {
            SmallRyeGraphQLServletLogging.log.processingFile(fileName);
            indexer.index(is);
        } else if (fileName.endsWith(DOT_WAR) || fileName.endsWith(DOT_JAR)) { // Liberty change - fixed in Issue 486 / PR 487
            // necessary because of the thorntail arquillian adapter
            processJar(is, indexer);
        }
    }

    private IndexView merge(Collection<IndexView> indexes) {
        return CompositeIndex.create(indexes);
    }

    private Set<URL> getUrlFromClassPath() {
        Set<URL> urls = new HashSet<>();

        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl instanceof URLClassLoader) {
            urls.addAll(Arrays.asList(((URLClassLoader) cl).getURLs()));
        } else {
            urls.addAll(collectURLsFromClassPath());
        }
        return urls;
    }

    private static final String DOT_JAR = ".jar";
    private static final String DOT_WAR = ".war";
    private static final String DOT_CLASS = ".class";
    private static final String JAVA_CLASS_PATH = "java.class.path";
    private static final String PATH_SEPARATOR = "path.separator";
    private static final String JANDEX_IDX = "META-INF/jandex.idx";
}

