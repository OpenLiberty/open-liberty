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

public class TCKResultsWriter {

    public static void preparePublicationFile(TCKResultsInfo resultInfo, String[] specParts) {
        String javaMajorVersion = resultInfo.getJavaMajorVersion();
        String javaVersion = resultInfo.getJavaVersion();
        String openLibertyVersion = resultInfo.getOpenLibertyVersion();
        String type = resultInfo.getType();
        String osVersion = resultInfo.getOsVersion();
        String specName = resultInfo.getSpecName();
        String specVersion = resultInfo.getSpecVersion();

        Path outputPath = Paths.get("results", openLibertyVersion + "-Java" + javaMajorVersion + "-" + specName + "-" + specVersion + "-TCKResults.adoc");
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
        TestSuiteXmlParser xmlParser = new TestSuiteXmlParser();

        try (FileWriter output = new FileWriter(outputFile)) {
            Path junitPath = Paths.get("results", "junit");
            File junitDirectory = junitPath.toFile();
            for (final File xmlFile : junitDirectory.listFiles()) {
                if (xmlFile.isDirectory()) {
                    continue; //this shouldn't happen.
                }
                if (xmlFile.length() == 0) {
                    continue; //this probably will happen. We create an empty file then populate it after we're expecting this method to run.
                }
                saxParser.parse(xmlFile, xmlParser);
            }

            String adocContent = "";
            String MPversion = "";

            String MPSpecLower = (specName.toLowerCase()).replace(" ", "-");
            String rcVersion = specParts[2];
            String[] documentStart = { ":page-layout: certification \n= TCK Results\n\n",
                                       "As required by the https://www.eclipse.org/legal/tck.php[Eclipse Foundation Technology Compatibility Kit License], following is a summary of the TCK results for releases of ",
                                       type //MicroProfile or Jakarta EE
            };
            for (String part : documentStart) {
                adocContent += part;
            }
            if (type.equals("MicroProfile")) {
                String[] documentMain = { " ", specName, " ", specVersion,
                                          ".\n\n== Open Liberty ", openLibertyVersion, " - MicroProfile ", specName, " ", specVersion,
                                          " Certification Summary (Java ", javaMajorVersion, ")\n\n",
                                          "* Product Name, Version and download URL (if applicable):\n",
                                          "+\nhttps://repo1.maven.org/maven2/io/openliberty/openliberty-runtime/",
                                          openLibertyVersion, "/openliberty-runtime-", openLibertyVersion, ".zip[Open Liberty ", openLibertyVersion, "]\n",
                                          "* Specification Name, Version and download URL:\n+\n",
                                          "link:https://download.eclipse.org/microprofile/microprofile-", MPSpecLower, "-", specVersion, rcVersion,
                                          "/microprofile-", MPSpecLower, "-spec-", specVersion, rcVersion, ".html[MicroProfile ", specName, " ", specVersion,
                                          rcVersion, "]\n\n* Public URL of TCK Results Summary:\n+\n", "link:", openLibertyVersion, "-Java", javaMajorVersion,
                                          "-TCKResults.html[TCK results summary]\n\n",
                                          "* Java runtime used to run the implementation:\n+\nJava ", javaMajorVersion, ": ", javaVersion,
                                          "\n\n* Summary of the information for the certification environment, operating system, cloud, ...:\n+\n", "Java ", javaMajorVersion, ": ",
                                          osVersion, "\n\nTest results:\n\n[source, text]\n----\n" };
                for (String part : documentMain) {
                    adocContent += part;
                }
            } else {
                String[] documentMain = { ".\n\n== Open Liberty ", openLibertyVersion, " ", type, " ", specName, " Certification Summary (Java ", javaMajorVersion, ")",
                                          "\n\n* Product Name, Version and download URL (if applicable):",
                                          "\n+\nhttps://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/", openLibertyVersion, ".zip[Open Liberty ",
                                          openLibertyVersion,
                                          "]\n\n",
                                          "* Specification Name, Version and download URL:\n+\n",
                                          "link:https://jakarta.ee/specifications/", specName, "/", specVersion, "[Jakarta EE ", specName, " ", specVersion, "]\n\n",
                                          "* TCK Version, digital SHA-256 fingerprint and download URL:\n+\n",
                                          "https://download.eclipse.org/jakartaee/", ".zip[Jakarta EE]\n",
                                          "SHA-256: ``\n\n",
                                          "* Public URL of TCK Results Summary:\n+\n",
                                          "link:", openLibertyVersion, "-Java", javaMajorVersion, "-TCKResults.html[TCK results summary]\n\n",
                                          "* Any Additional Specification Certification Requirements:\n\n",
                                          "+\nJakarta Contexts and Dependency Injection\n",
                                          "link:https://download.eclipse.org/jakartaee/cdi/", "SHA-256:\n``\n",
                                          "+\nJakarta Bean Validation\n",
                                          "link:https://download.eclipse.org/jakartaee/bean-validation/", "SHA-256:\n``\n",
                                          "+\nDependency Injection\n",
                                          "link:https://download.eclipse.org/jakartaee/dependency-injection/", "SHA-256:\n``\n",
                                          "+\nDebugging\n",
                                          "link:https://download.eclipse.org/jakartaee/debugging/", "SHA-256:\n``\n",
                                          "* Java runtime used to run the implementation:\n+\n", javaVersion,
                                          "\n\n* Summary of the information for the certification environment, operating system, cloud, ...:\n+\n",
                                          osVersion,
                                          "\n\nTest results:\n\n[source, text]\n----\n" };
                for (String part : documentMain) {
                    adocContent += part;
                }
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
}
