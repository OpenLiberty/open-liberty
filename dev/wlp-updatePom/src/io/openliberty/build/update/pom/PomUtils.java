/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.function.Function;

import org.apache.maven.model.Dependency;
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
        try ( OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream) ) {
            writer.write(streamWriter, pomModel);
        }
    }

    protected static final int BUF_SIZE = 32 * 1024;
    protected static final String POM_PREFIX_PATH = "META-INF/maven/dev";

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

        boolean didRemove = false;

        Iterator<Dependency> deps = pomModel.getDependencies().iterator();
        while (deps.hasNext()) {
            Dependency dep = deps.next();
            if (isFiltered.apply(dep)) {
                deps.remove();
                didRemove = true;
            }
        }

        return (didRemove ? pomModel : null);
    }

    public static final String[] DEV_GROUPS = { "dev", "test" };

    /**
     * Tell if a dependency has a development group.
     */
    public static boolean isDevGroup(Dependency dep) {
        String groupId = dep.getGroupId();
        return "dev".equals(groupId) || "test".equals(groupId);
    }

    // TODO: Why these???
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

    // TODO: Why these?
    private static final String[] FILTERED_ARTIFACTS = { "com.ibm.crypto.CmpCrmf",
                                                         "com.ibm.crypto.ibmjceprovider",
                                                         "com.ibm.crypto.ibmkeycert",
                                                         "com.ibm.crypto.ibmpkcs",
                                                         "com.ibm.mq.commonservices",
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
