/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wlp.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * This ANT task will create the sample content for the download.xml file and wasdevGeneration.xml.
 */
public class CreateSampleXml extends CreateDownloadXml {

    private String readmeSourceFilePath;
    private String builtReadmeFilePath;
    private String assetDownloadUrl;

    private File sourceReadmeFile;
    private File builtReadmeFile;
    private String propertiesFilePath;
    private String sourceFilePath;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wlp.repository.CreateDownloadXml#execute()
     */
    @Override
    public void execute() throws BuildException {
        /*
         * A couple of the sample properties used in the download XML come from the Readme.html file and need the HTML elements stripped out before we can write the XML using the
         * supertype
         */
        String sampleName = null;
        String sampleAbstract = null;
        String readmeContent = null;

        // Parse the source readme file for the abstract, as it exists only in here
        try {
            Document doc = Jsoup.parse(sourceReadmeFile, "UTF-8");
            //Short abstract description is used by the tools (via download XML - "description"), and WASDev via wasdev.xml.
            Element abstractElement = getRequiredElementById(doc, "ibm-wasdev-sample-abstract-content");
            sampleAbstract = abstractElement.text();
            this.setDescription(abstractElement.text());

        } catch (IOException e) {
            // Can't read the readme so can't produce the XML
            throw new BuildException("Unable to load the Readme.html from " + sourceReadmeFile.getAbsolutePath() + " due to IOException: " + e.getMessage(), e);
        }

        // Parse the built readme file for everything else
        try {
            Document doc = Jsoup.parse(builtReadmeFile, "UTF-8");
            Element nameElement = getRequiredElementById(doc, "ibm-wasdev-sample-name-title");
            sampleName = nameElement.text();
            this.setName(sampleName);

            // Several nodes aren't needed for the WASDev page
            removeElementIfExistsById(doc, "ibm-wasdev-sample-name-title");
            removeElementIfExistsById(doc, "ibm-wasdev-sample-desc-title");
            removeElementIfExistsById(doc, "ibm-wasdev-sample-external-deps-title");
            removeElementIfExistsById(doc, "ibm-wasdev-sample-external-deps-content");
            removeElementIfExistsById(doc, "ibm-wasdev-sample-abstract-content");
            removeElementIfExistsById(doc, "ibm-title-support-info");
            removeElementIfExistsById(doc, "ibm-support-info");
            removeElementIfExistsById(doc, "ibmLicenseFlowing");

            Element readmeElement = getRequiredElementById(doc, "ibm-wasdev-sample-readme-content");

            // If the source is supplied append it to the description
            if (this.sourceFilePath != null) {
                File sourceFile = new File(this.sourceFilePath);
                if (sourceFile.exists()) {
                    BufferedReader br = null;
                    try {
                        br = new BufferedReader(new FileReader(sourceFile));
                        Element sourceNode = new Element(Tag.valueOf("code"), "");
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            // Add nice spacing in the HTML, first tabs to spaces
                            line = line.replaceAll("\\t", "    ");
                            while (line.startsWith(" ")) {
                                line = line.substring(1);
                                sourceNode.append("&nbsp;");
                            }
                            sourceNode.appendText(line);
                            sourceNode.append("<br/>");
                        }
                        readmeElement.appendChild(sourceNode);
                    } finally {
                        if (br != null) {
                            br.close();
                        }
                    }
                }
            }
            readmeContent = readmeElement.html();
        } catch (IOException e) {
            // Can't read the readme so can't produce the XML
            throw new BuildException("Unable to load the Readme.html from " + builtReadmeFile.getAbsolutePath() + " due to IOException: " + e.getMessage(), e);
        }

        // All processing done (everything is set by ANT) so let the supertype generate the XML
        super.execute();

        // We also need to generate a properties file and a description.html with content used to populate Massive
        // First the properties file
        File propertiesFile = new File(this.propertiesFilePath);
        Properties assetInfoProps = new Properties();
        if (propertiesFile.exists()) {
            // Append to an existing props
            FileInputStream in = null;
            try {
                in = new FileInputStream(propertiesFile);
                assetInfoProps.load(in);
            } catch (FileNotFoundException e) {
                throw new BuildException(e);
            } catch (IOException e) {
                throw new BuildException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new BuildException(e);
                    }
                }
            }
        }
        assetInfoProps.put("name", sampleName);
        assetInfoProps.put("shortDescription", sampleAbstract);
        assetInfoProps.put("downloadURL", assetDownloadUrl);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(propertiesFile);
            assetInfoProps.store(out, null);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new BuildException(e);
                }
            }
        }

        // In the same directory as the properties file, write the description.html file
        File descriptionFile = new File(propertiesFile.getParentFile(), "description.html");
        Writer descriptionWriter = null;
        try {
            descriptionWriter = new OutputStreamWriter(new FileOutputStream(descriptionFile), StandardCharsets.UTF_8);
            descriptionWriter.write(readmeContent);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            if (descriptionWriter != null) {
                try {
                    descriptionWriter.close();
                } catch (IOException e) {
                    throw new BuildException(e);
                }
            }
        }
    }

    /**
     * @param readmeSourceFilePath the readmeSourceFilePath to set
     */
    public void setReadmeSourceFile(String readmeSourceFilePath) {
        this.readmeSourceFilePath = readmeSourceFilePath;
        sourceReadmeFile = new File(this.readmeSourceFilePath);
        if (!sourceReadmeFile.exists()) {
            throw new BuildException("The source readme file " + sourceReadmeFile.getAbsolutePath() + " does not exist");
        }
    }

    /**
     * @param builtReadmeFilePath the builtReadmeFilePath to set
     */
    public void setBuiltReadmeFile(String builtReadmeFilePath) {
        this.builtReadmeFilePath = builtReadmeFilePath;
        builtReadmeFile = new File(this.builtReadmeFilePath);
        if (!builtReadmeFile.exists()) {
            throw new BuildException("The source readme file " + builtReadmeFile.getAbsolutePath() + " does not exist");
        }
    }

    /**
     * @param assetDownloadUrl the assetDownloadUrl to set
     */
    public void setAssetDownloadUrl(String assetDownloadUrl) {
        this.assetDownloadUrl = assetDownloadUrl;
    }

    private boolean removeElementIfExistsById(Document doc, String id) {
        Element externalDepsContentElement = doc.getElementById(id);
        if (externalDepsContentElement != null) {
            externalDepsContentElement.remove();
            return true;
        } else {
            return false;
        }
    }

    private Element getRequiredElementById(Document doc, String id) throws BuildException {
        Element foundElement = doc.getElementById(id);
        if (foundElement == null) {
            throw new BuildException("Could not locate required element with id " + id + " in readme html document!");
        }
        return foundElement;
    }

    /**
     * @param propertiesFilePath the propertiesFilePath to set
     */
    public void setPropertiesFilePath(String propertiesFilePath) {
        this.propertiesFilePath = propertiesFilePath;
    }

    /**
     * Sets the optional source file. A path to a file whose contents will be appended to the long description in a <code> block.
     * 
     * @param sourceFile
     */
    public void setSourceFile(String sourceFile) {
        this.sourceFilePath = sourceFile;
    }

}
