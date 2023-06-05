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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import io.openliberty.build.update.Update;
import io.openliberty.build.update.UpdateFactory;
import io.openliberty.build.update.UpdateFile;
import io.openliberty.build.update.UpdateRunner;
import io.openliberty.build.update.util.FileUtils;
import io.openliberty.build.update.util.Logger;

/**
 * Update the dependencies of the first "pom.xml" entry of a single
 * target archive.
 *
 * Only select dependencies are removed, per {@link PomUtils#isDevGroup(Dependency)},
 * {@link PomUtils#filterGroup(Dependency)} and {@link PomUtils#filterArtifact(Dependency)}.
 *
 * If no dependencies were removed, the target archive is not updated.
 *
 * If any dependencies were removed, rewrite the target file with
 * newly serialized pom data which has the dependences taken out
 *
 * The update to the target archive is done by writing an updated
 * archive to a temporary location, then moving the new archive
 * in place of the original file.
 *
 * See:
 * https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#What_is_a_POM
 */
public class UpdatePomJar extends UpdateFile {
    public static UpdateFactory createUpdateFactory() {
        return new UpdateFactory() {
            @Override
            public Class<? extends Update> getUpdateClass() {
                return UpdatePomJar.class;
            }

            @Override
            public UpdatePomJar createUpdate(File targetFile, File tmpDir, Logger logger, boolean failOnError) throws Exception {
                return new UpdatePomJar(targetFile, tmpDir, logger, failOnError);
            }
        };
    }

    public static void main(String[] args) {
        try {
            @SuppressWarnings("unused")
            int processed = UpdateRunner.run(createUpdateFactory(), args);
        } catch (Exception e) {
            System.out.println("Failure");
            e.printStackTrace();
        }
    }

    public UpdatePomJar(File jarFile, File tmpDir) {
        super(jarFile, tmpDir);
    }

    public UpdatePomJar(File jarFile, File tmpDir, boolean failOnError) {
        super(jarFile, tmpDir, failOnError);
    }

    public UpdatePomJar(File jarFile, File tmpDir, Logger logger) {
        super(jarFile, tmpDir, logger);
    }

    public UpdatePomJar(File jarFile, File tmpDir, Logger logger, boolean failOnError) {
        super(jarFile, tmpDir, logger, failOnError);
    }

    // POM data ...

    /**
     * This is the name of the "pom.xml" archive entry. This usually
     * includes path information.
     *
     * This must be recorded so to relocate the POM entry if the target
     * archive must be updated.
     */
    private String pomPath;

    protected void setPomPath(String pomPath) {
        this.pomPath = pomPath;
    }

    public String getPomPath() {
        return pomPath;
    }

    //

    /**
     * Main API:Read the POM from the target. Update the POM's dependencies.
     * If any dependencies were updated, rewrite the target with the POM updated
     * with the updated dependencies.
     *
     * Do nothing if the dependencies were not updated.
     *
     * @throws Exception Thrown if processing fails.
     */
    @Override
    public int basicRun() throws Exception {
        String m = "basicRun";

        logMark();
        Model pomModel = readPomFromTarget();
        if (pomModel == null) {
            logTime(m, "No POM");
            return 0;
        }

        Model updatedPomModel = PomUtils.updateDependecies(pomModel);

        if (updatedPomModel == null) {
            logTime(m, "No updates to POM");
            return 0;
        }

        // logTime(m, "Updating POM");
        replacePom(PomUtils.writePom(updatedPomModel));
        logTime(m, "Updated POM");

        return 1;
    }

    /**
     * Read a POM from the target archive.
     *
     * Locate and read the first entry named "pom.xml", possibly with
     * a path prefix (e.g., "META-INF/pom.xml").
     *
     * Invoke {@link #setPomPath(String, Model)} if the POM was located
     * and read.
     *
     * @return True or false telling if the POM was located.
     *
     * @throws Exception Thrown if processing failed.
     */
    protected Model readPomFromTarget() throws Exception {
        try (ZipFile zipFile = new ZipFile(getTargetFile())) {
            String jarName = getTargetFile().getName();
            if (jarName.indexOf("_") != -1)
                jarName = jarName.substring(0, jarName.indexOf("_"));
            else
                jarName = jarName.substring(0, jarName.indexOf(".jar"));
            ZipEntry pomEntry = zipFile.getEntry(PomUtils.POM_PREFIX_PATH + jarName + "/pom.xml");
            if (pomEntry == null) {
                log("readPomFromTarget", "Pom not found at: " + (PomUtils.POM_PREFIX_PATH + jarName + "/pom.xml"));
                //pom not found in usual location - search jar for any maven pom

                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while ((pomEntry == null) && entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entryName.equals("pom.xml") || entryName.endsWith("/pom.xml")) {
                        pomEntry = entry;
                    }
                }
                //If still no pom found return null
                if (pomEntry == null)
                    return null;
            }
            Model pomModel;
            try (InputStream entryStream = zipFile.getInputStream(pomEntry)) {
                pomModel = PomUtils.readPom(entryStream);
                setPomPath(pomEntry.getName());
            }
            return pomModel;
        }

    }

    /**
     * Perform POM replacement on the target.
     *
     * Copy the target, replacing the POM with the updated POM, which has
     * changed dependencies. Replace the target with the updated target.
     *
     * @param pomInput A stream containing updated POM content.
     *
     * @throws IOException Thrown if processing fails.
     */
    protected void replacePom(InputStream pomInput) throws IOException {
        String usePomPath = getPomPath();

        File tmpDir = FileUtils.ensure(getTmpDir());

        File inputFile = getTargetFile();
        File outputFile = File.createTempFile(inputFile.getName(), null, tmpDir);

        // Replacing the POM requires a rewrite of the target archive.
        // This is done by doing a streaming copy of the target archive.
        // The POM entry is replaced with the updated POM data.
        //
        // After writing the updated target archive, move it back to replace
        // the initial target archive.

        byte[] transferBuffer = new byte[32 * 1024];
        FileUtils.copyReplacing(inputFile, outputFile, usePomPath, pomInput, transferBuffer);
        // The target is now the original input
        Path inputPath = Paths.get(outputFile.getPath());
        Path outputPath = Paths.get(inputFile.getPath());
        //Now overwrite the origin target file
        Files.move(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
    }
}