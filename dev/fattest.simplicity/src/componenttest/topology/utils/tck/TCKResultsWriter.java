/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils.tck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import componenttest.topology.utils.tck.TCKResultsInfo.Type;

public class TCKResultsWriter {

    private static final String NEW_LINE = "\n";
    private static final String SPACE = " ";

    /**
     * Generate a TCK Results file in AsciiDoc format
     *
     * @param resultInfo TCK Results metadata
     */
    public static void preparePublicationFile(TCKResultsInfo resultInfo) {
        String javaMajorVersion = resultInfo.getJavaMajorVersion();
        String javaVersion = resultInfo.getJavaVersion();
        String openLibertyVersion = resultInfo.getOpenLibertyVersion();
        Type type = resultInfo.getType();
        String osVersion = resultInfo.getOsVersion();
        String specName = resultInfo.getSpecName();
        String specVersion = resultInfo.getSpecVersion();
        String rcVersion = resultInfo.getRcVersion();

        String filename = openLibertyVersion + "-" + specName.replace(" ", "-") + "-" + specVersion + "-Java" + javaMajorVersion + "-TCKResults.adoc";
        Path outputPath = Paths.get("results", filename);
        File outputFile = outputPath.toFile();
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

        String MPSpecLower = (specName.toLowerCase()).replace(" ", "-");

        String fullSpecName;
        String specURL = null;
        String tckURL = null;
        String tckSHA = null;
        if (type.equals(Type.MICROPROFILE)) {
            fullSpecName = "MicroProfile " + specName + " " + specVersion + rcVersion;
            specURL = "https://download.eclipse.org/microprofile/microprofile-" + MPSpecLower + "-" + specVersion + rcVersion + "/microprofile-" + MPSpecLower + "-spec-"
                      + specVersion + rcVersion + ".html"; //format of this URL is very inconsistent, may need to be manually updated
        } else {
            fullSpecName = "Jakarta " + specName + " " + specVersion + rcVersion;
            specURL = "https://jakarta.ee/specifications/" + specName + "/" + specVersion;
            tckURL = "https://download.eclipse.org/ee4j/"; //just a placeholder, needs to be manually updated
            tckSHA = "https://download.eclipse.org/ee4j/"; //just a placeholder, needs to be manually updated
        }
        String adocContent = getADocHeader(filename, fullSpecName, specURL, openLibertyVersion, javaMajorVersion, javaVersion, osVersion, tckURL, tckSHA);

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
                output.write(result.toString());
            }
            output.write("----");

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getADocHeader(String filename, String fullSpecName, String specURL, String openLibertyVersion, String javaMajorVersion, String javaVersion,
                                        String osVersion, String tckURL, String tckSHA) {
        StringBuilder builder = new StringBuilder();

        builder.append(":page-layout: certification ").append(NEW_LINE);
        builder.append("= TCK Results").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("As required by the https://www.eclipse.org/legal/tck.php[Eclipse Foundation Technology Compatibility Kit License], following is a summary of the TCK results for releases of ");
        builder.append(fullSpecName).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("== Open Liberty ").append(openLibertyVersion).append(" - ").append(fullSpecName).append(SPACE);
        builder.append("Certification Summary (Java ").append(javaMajorVersion).append(")").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Product Name, Version and download URL (if applicable):").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/"); //this URL isn't right for betas
        builder.append(openLibertyVersion).append("/openliberty-").append(openLibertyVersion).append(".zip");
        builder.append("[Open Liberty ").append(openLibertyVersion).append("]").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Specification Name, Version and download URL:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append(specURL);
        builder.append("[").append(fullSpecName).append("]").append(NEW_LINE);
        builder.append(NEW_LINE);

        if (tckURL != null) {
            builder.append("* TCK Version, digital SHA-256 fingerprint and download URL:").append(NEW_LINE);
            builder.append("+").append(NEW_LINE);
            builder.append(tckURL);
            builder.append("[").append(fullSpecName).append("]");

            if (tckSHA != null) {
                builder.append(SPACE);
                builder.append(tckSHA);
                builder.append("[SHA-256]");;
            }

            builder.append(NEW_LINE);
            builder.append(NEW_LINE);
        }

        builder.append("* Public URL of TCK Results Summary:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("xref:").append(filename);
        builder.append("[TCK results summary]").append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Java runtime used to run the implementation:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append("Java ").append(javaMajorVersion).append(": ").append(javaVersion).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("* Summary of the information for the certification environment, operating system, cloud, ...:").append(NEW_LINE);
        builder.append("+").append(NEW_LINE);
        builder.append(osVersion).append(NEW_LINE);
        builder.append(NEW_LINE);

        builder.append("Test results:").append(NEW_LINE);
        builder.append(NEW_LINE);
        builder.append("[source, text]").append(NEW_LINE);
        builder.append("----").append(NEW_LINE);

        return builder.toString();
    }

}
