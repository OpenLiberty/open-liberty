/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package componenttest.topology.utils.tck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class TCKResultsWriter {

    private static final String NEW_LINE = "\n";
    private static final String SPACE = " ";

    /**
     * Generate a TCK Results file in AsciiDoc format
     *
     * @param resultInfo TCK Results metadata
     */
    public static void preparePublicationFile(TCKResultsInfo resultInfo) {

        SAXParserFactory factory = null;
        SAXParser saxParser = null;
        try {
            factory = SAXParserFactory.newInstance();
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        Path outputDirectory = Paths.get("results/" + "TCK_Results_Certifications" + resultInfo.getReadableRepeatName());
        Path outputPath = Paths.get("results/" + "TCK_Results_Certifications" + resultInfo.getReadableRepeatName(), resultInfo.getFilename());

        File outputFile = outputPath.toFile();
        File outputDir = outputDirectory.toFile();

        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        String adocContent = getADocHeader(resultInfo);

        try (FileWriter output = new FileWriter(outputFile)) {
            Path junitPath = Paths.get("results", "junit");
            File junitDirectory = junitPath.toFile();
            TestSuiteXmlParser xmlParser = new TestSuiteXmlParser();
            for (final File xmlFile : junitDirectory.listFiles()) {
                if (xmlFile.isDirectory()) {
                    continue; //this shouldn't happen.
                }
                if (xmlFile.length() == 0) {
                    continue; //this probably will happen. We create an empty file then populate it after we're expecting this method to run.
                }
                saxParser.parse(xmlFile, xmlParser);
            }
            output.write(adocContent);
            for (TestSuiteResult result : xmlParser.getResults()) {
                /*
                 * Checking if the result is for the correct repeat (avoiding adding wrong repeat results to wrong TCK report repeat files)
                 * This will never be null because when calling RepeatTestFilter.getRepeatActionsAsString()
                 * if REPEAT_ACTION_STACK is empty it will return an empty string instead ("")
                 */
                if (result.toString().contains(resultInfo.getRepeat())) {
                    // Removing repeat ID from the test results
                    String newResult = result.toString().replace(resultInfo.getRepeat(), "");
                    output.write(newResult);
                }
            }
            output.write("----");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

        // Files written to the extras directory are included in the FAT output zip, but can also be uploaded to an additional location by setting the
        // 'extraFatOutputPath' property to a path on LibFS (e.g. /liberty/dev/Xo/release/BUILD_LABEL) to retain them beyond the life cycle of FAT output.
        // Note that this copying *only* occurs in CI Orchestrator environments as the code wrapping FAT execution is responsible for performing the upload to LibFS.
        // The use case here is to make .adoc files available in the build directory without having to extract them from the FAT output zip every time.
        try {
            Path extrasPath = Paths.get("extras/" + "TCK_Results_Certifications" + resultInfo.getReadableRepeatName(), resultInfo.getFilename());
            Files.createDirectories(extrasPath.getParent());
            Files.copy(outputPath, extrasPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getADocHeader(TCKResultsInfo resultInfo) {
        String timestamp = Instant.now()
                        .truncatedTo(ChronoUnit.SECONDS)
                        .toString();

        StringBuilder builder = new StringBuilder();

        builder.append(":page-layout: certification ").append(NEW_LINE);
        builder.append("= TCK Results").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("As required by the https://www.eclipse.org/legal/tck.php[Eclipse Foundation Technology Compatibility Kit License], following is a summary of the TCK results for releases of ");
        builder.append(resultInfo.getFullSpecName()).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("== Open Liberty ").append(resultInfo.getOpenLibertyVersion()).append(" - ").append(resultInfo.getFullSpecName()).append(SPACE);
        builder.append("Certification Summary (Java ").append(resultInfo.getJavaMajorVersion()).append(")").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Product Name, Version and download URL (if applicable):").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/"); //this URL isn't right for betas
        builder.append(resultInfo.getOpenLibertyVersion()).append("/openliberty-").append(resultInfo.getOpenLibertyVersion()).append(".zip");
        builder.append("[Open Liberty ").append(resultInfo.getOpenLibertyVersion()).append("]").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Specification Name, Version and download URL:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append(resultInfo.getSpecURL());
        builder.append("[").append(resultInfo.getFullSpecName()).append(" Specification]").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* (Optional) TCK Version, digital fingerprint and download URL:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append(resultInfo.getTCKURL());
        builder.append("[").append(resultInfo.getFullSpecName()).append(" TCK]").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("SHA-1: `").append(resultInfo.getSHA1()).append("`").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("SHA-256: `").append(resultInfo.getSHA256()).append("`").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Public URL of TCK Results Summary:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("xref:").append(resultInfo.getFilename());
        builder.append("[TCK results summary]").append(NEW_LINE).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Java runtime used to run the implementation:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("Java ").append(resultInfo.getJavaMajorVersion()).append(": ").append(resultInfo.getJavaVersion()).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Summary of the information for the certification environment, operating system, cloud, ...:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append(resultInfo.getOsName());
        builder.append(": ");
        builder.append(resultInfo.getOsVersion()).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("Report generated at: " + timestamp);
        builder.append(NEW_LINE);

        builder.append("Test results:").append(NEW_LINE);
        builder.append(NEW_LINE);
        builder.append("[source, text]").append(NEW_LINE);
        builder.append("----").append(NEW_LINE);

        return builder.toString();
    }

}
