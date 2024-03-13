/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.build.update.pom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

/**
 * Low level POM utilities. These utilities are used to
 * read and write POM data, and to modify the dependencies
 * of POM data.
 *
 * This utility encodes the specification of development dependency
 * group IDs which are to be removed, encodes the specification of
 * dependency group IDs which are to be removed, and encodes the
 * specification of dependency artifact IDs which are to be removed.
 *
 * TODO: Why these particular dependency group IDs and artifact IDS?
 */
public class PomUtils {
    // Low level POM utilities ...

    public static Model readPom(InputStream inputStream) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read(inputStream);
    }

    public static void writePom(Model pomModel, OutputStream outputStream) throws Exception {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try (OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream)) {
            writer.write(streamWriter, pomModel);
        }
    }

    protected static final int BUF_SIZE = 32 * 1024;
    protected static final String POM_PREFIX_PATH = "META-INF/maven/dev/";

    public static ByteArrayInputStream writePom(Model pomModel) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUF_SIZE);
        writePom(pomModel, outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    //

    public static Model updateDependecies(Model pomModel) {
        Function<Dependency, Boolean> filter = (Dependency dep) -> (isDevGroup(dep) || filterGroup(dep) || filterArtifact(dep));
        return updateDependencies(pomModel, filter);
    }

    public static Model updateDependencies(Model pomModel, Function<Dependency, Boolean> isFiltered) {
        String m = "updateDependencies";

        boolean didChange = false;

        Iterator<Dependency> deps = pomModel.getDependencies().iterator();
        while (deps.hasNext()) {
            Dependency dep = deps.next();
            if (isFiltered.apply(dep)) {
                deps.remove();
                didChange = true;
            } else if (hasExcludes(dep)) {
                addTransitiveExcludes(dep);
                didChange = true;
            }
        }

        return (didChange ? pomModel : null);
    }

    /**
     * @param dep
     * @return
     */
    private static boolean hasExcludes(Dependency dep) {

        String depString = dep.getGroupId() + ':' + dep.getArtifactId() + ':' + dep.getVersion();

        if (transitiveExcludes.containsKey(depString))
            return true;

        return false;
    }

    public static final String[] DEV_GROUPS = { "dev", "test" };

    /**
     * Tell if a dependency has a development group.
     */
    public static boolean isDevGroup(Dependency dep) {
        String groupId = dep.getGroupId();
        return "dev".equals(groupId) || "test".equals(groupId);
    }

    public static void addTransitiveExcludes(Dependency dep) {

        List exclusions = dep.getExclusions();
        String depString = dep.getGroupId() + ':' + dep.getArtifactId() + ':' + dep.getVersion();
        String artifactPath = transitiveExcludes.get(depString);
        String exclusion[] = artifactPath.split(":");
        Exclusion ex = new Exclusion();
        ex.setGroupId(exclusion[0]);
        ex.setArtifactId(exclusion[1]);
        exclusions.add(ex);
    }

    //We will add a pom excludes fragment in each of these dependency sections - Proven these are NOT shipped
    static Map<String, String> transitiveExcludes = new HashMap<String, String>() {
        {
            put("org.apache.openwebbeans:openwebbeans-ee-common:1.1.6", "javassist:javassist");
            put("org.apache.openwebbeans:openwebbeans-impl:1.1.6", "javassist:javassist");
            put("org.apache.openwebbeans:openwebbeans-jsf:1.1.6", "javassist:javassist");
            put("org.apache.openwebbeans:openwebbeans-web:1.1.6", "javassist:javassist");
            put("org.apache.openwebbeans:openwebbeans-ee:1.1.6", "javassist:javassist");
            put("org.apache.openwebbeans:openwebbeans-ejb:1.1.6", "javassist:javassist");
            put("javax.resource:javax.resource-api:1.7", "javax.transaction:javax.transaction-api");
            put("org.jboss.weld:weld-osgi-bundle:2.4.8.Final", "org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec");
            put("org.jboss.weld:weld-osgi-bundle:3.1.9.Final", "org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec");
            put("org.jboss.weld:weld-osgi-bundle:5.1.1.SP1", "org.jboss.logging:jboss-logging-processor");
            put("org.jboss.weld.se:weld-se-core:5.1.1.SP1", "org.jboss.logging:jboss-logging-processor");
            put("org.apache.cxf:cxf-rt-rs-service-description:3.3.0", "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec");
            put("org.apache.cxf:cxf-rt-rs-sse:3.3.0", "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec");
        }
    };

    //We know we are not including these packages in our shipped jar's
    public static final String[] FILTERED_GROUPS = { "org.springframework",
                                                     "org.springframework.boot",
                                                     "com.ibm.ws.common.encoder" };

    /**
     * Tell if a dependency is to be filtered based on the dependency's group ID.
     */
    public static boolean filterGroup(Dependency dep) {
        String groupId = dep.getGroupId();
        for (String filteredGroup : FILTERED_GROUPS) {
            if (filteredGroup.equals(groupId)) {
                return true;
            }
        }
        return false;
    }

    // Filter known IBM built binaries - not containing any OSS licenses
    private static final String[] FILTERED_ARTIFACTS = { "com.ibm.crypto.CmpCrmf",
                                                         "com.ibm.crypto.ibmjceprovider",
                                                         "com.ibm.crypto.ibmkeycert",
                                                         "com.ibm.crypto.ibmpkcs",
                                                         "com.ibm.mq.commonservices",
                                                         "com.ibm.ws.ras.instrument",
                                                         "com.ibm.ws.eba.blueprint.extensions.interceptors",
                                                         "com.ibm.ws.eba.blueprint.transform",
                                                         "com.ibm.ws.eba.jpa.container.annotations",
                                                         "com.ibm.ws.kernel.feature",
                                                         "com.ibm.ws.kernel.service",
                                                         "com.ibm.ws.prereq.rxa",
                                                         "dhbcore",
                                                         "java.ibmjgssprovider",
                                                         "wmq_java" };

    /**
     * Tell if a dependency is to be filtered based on its artifact.
     */
    public static boolean filterArtifact(Dependency dep) {
        String artifactId = dep.getArtifactId();
        for (String filteredArtifact : FILTERED_ARTIFACTS) {
            if (filteredArtifact.equals(artifactId)) {
                return true;
            }
        }
        return false;
    }
}
