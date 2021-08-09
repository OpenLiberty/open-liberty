/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.parsers;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Problem;
import com.ibm.ws.product.utility.extension.ifix.xml.Resolves;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;
import com.ibm.ws.product.utility.extension.ifix.xml.Updates;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveInvalidEntryException;
import com.ibm.ws.repository.resources.writeable.IfixResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;

public class IfixParser extends ParserBase implements Parser<IfixResourceWritable> {

    private static final String APPLIES_TO = "Applies-To";
    private File _jarPayload = null;
    private File _readmePayload = null;

    /** {@inheritDoc} */
    @Override
    public IfixResourceWritable parseFileToResource(File assetFile, File metadataFile, String contentUrl) throws RepositoryException {

        ArtifactMetadata artifactMetadata = explodeArtifact(assetFile, metadataFile);

        // Throw an exception if there is no metadata and properties, we get the name and readme from it
        if (artifactMetadata == null) {
            throw new RepositoryArchiveException("Unable to find sibling metadata zip for " + assetFile.getName()
                                                 + " so do not have the required information", assetFile);
        }

        extractFiles(artifactMetadata);

        _jarPayload = assetFile;

        // Extract iFix xml file from iFix jar file
        ParserBase.ExtractedFileInformation xmlFileInfo = extractFileFromArchive(_jarPayload.getAbsolutePath(), ".*lib\\/fixes.*\\.xml");
        IFixInfo ifixInfo;
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(xmlFileInfo.getExtractedFile());
            ifixInfo = IFixInfo.fromDocument(doc);
        } catch (Exception e) {
            throw new RepositoryArchiveInvalidEntryException("Parse failure", xmlFileInfo.getSourceArchive(), xmlFileInfo.getSelectedPathFromArchive(), e);
        }

        // create asset and update with info from iFix jar file
        IfixResourceWritable resource = WritableResourceFactory.createIfix(null);
        resource.setName(getFixId(ifixInfo, xmlFileInfo));
        resource.setDisplayPolicy(DisplayPolicy.HIDDEN);
        resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);

        // create the provider info and store in local asset
        resource.setProviderName("IBM");

        // parse the jar manifest
        String appliesTo = parseManifestForAppliesTo(_jarPayload);

        // set the local extension information
        resource.setAppliesTo(appliesTo);
        resource.setProvideFix(getProvides(ifixInfo, xmlFileInfo)); // a list of fixed APARs

        // add the readme as an attachment
        resource.addAttachment(_readmePayload, AttachmentType.DOCUMENTATION);

        // Find the date with the most recent update date
        resource.setDate(getLatestDateOfAnyFile(ifixInfo, xmlFileInfo));

        // add content and upload
        addContent(resource, _jarPayload, _jarPayload.getName(), artifactMetadata, contentUrl);

        return resource;
    }

    /**
     * Override the version in ParserBase that will throw an error if NAME,
     * SHORT DESCRIPTION and LONG DESCRIPTION are note set
     */
    @Override
    protected void checkRequiredProperties(ArtifactMetadata artifact) throws RepositoryArchiveInvalidEntryException {
        // there are no required fields
    }

    /**
     * Extract the files from the zip
     *
     * @param zip
     * @throws RepositoryArchiveIOException
     * @throws RepositoryArchiveException
     * @throws RepositoryArchiveEntryNotFoundException
     */
    private void extractFiles(ArtifactMetadata artifactMetadata) throws RepositoryArchiveIOException, RepositoryArchiveEntryNotFoundException, RepositoryArchiveException {

        _readmePayload = artifactMetadata.getFileWithExtension(".txt");
        if (_readmePayload == null) {
            throw new RepositoryArchiveEntryNotFoundException("Unable to find iFix readme .txt file in archive"
                                                              + artifactMetadata.getArchive().getAbsolutePath(), artifactMetadata.getArchive(), "*.txt");
        }

    }

    /**
     * Get the ID of the iFix from the Java representation of the iFix XML
     *
     * @param iFixInfo
     * @param xmlInfo
     * @return A String containing the iFix ID
     * @throws RepositoryArchiveInvalidEntryException
     */
    private String getFixId(IFixInfo iFixInfo, ExtractedFileInformation xmlInfo) throws RepositoryArchiveInvalidEntryException {

        // check for null input
        if (null == iFixInfo) {
            throw new RepositoryArchiveInvalidEntryException("Null XML object provided", xmlInfo.getSourceArchive(), xmlInfo.getSelectedPathFromArchive());
        }

        // check the first child node is named fix
        return iFixInfo.getId();
    }

    /**
     * Get a list of the APARs fixed by this iFix
     *
     * @param iFixInfo
     * @return The list of fixed APARs
     * @throws MassiveInvalidXmlException
     */
    private List<String> getProvides(IFixInfo iFixInfo, ParserBase.ExtractedFileInformation xmlInfo) throws RepositoryArchiveInvalidEntryException {

        // check for null input
        if (null == iFixInfo) {
            throw new RepositoryArchiveInvalidEntryException("Null document provided", xmlInfo.getSourceArchive(), xmlInfo.getSelectedPathFromArchive());
        }

        Resolves resolves = iFixInfo.getResolves();
        if (null == resolves) {
            throw new RepositoryArchiveInvalidEntryException("Document does not contain a \"resolves\" node", xmlInfo.getSourceArchive(), xmlInfo.getSelectedPathFromArchive());
        }

        // Get child nodes and look for APAR ids
        List<String> retList = new ArrayList<String>();
        List<Problem> problems = resolves.getProblems();
        if (problems != null) {
            for (Problem problem : problems) {
                String displayId = problem.getDisplayId();
                if (null == displayId) {
                    throw new RepositoryArchiveInvalidEntryException("Unexpected null getting APAR id", xmlInfo.getSourceArchive(), xmlInfo.getSelectedPathFromArchive());
                }
                retList.add(displayId);
            }
        }

        return retList;
    }

    /**
     * Looks at the manifest file, extracting info and putting the info into the supplied asset
     *
     * @param jar The jar file containing the manifest
     * @param ass The asset to put meta data into
     * @return ManifestInfo
     * @throws RepositoryArchiveIOException
     */
    private String parseManifestForAppliesTo(File file) throws RepositoryArchiveIOException {

        Manifest mf = null;
        try (JarFile jar = new JarFile(file)) {
            try {
                mf = jar.getManifest();
            } catch (IOException ioe) {
                throw new RepositoryArchiveIOException("Error getting manifest from jar " + jar.getName(), new File(jar.getName()), ioe);
            }
        } catch (IOException ioe) {
            throw new RepositoryArchiveIOException("Unable to create JarFile from path " +
                                                   file, new File(file.getName()), ioe);
        }

        String appliesTo = null;

        Attributes mainattrs = mf.getMainAttributes();
        // Iterate over the main attributes in the manifest and look for the ones we
        // are interested in.
        for (Object at : mainattrs.keySet()) {
            String attribName = ((Attributes.Name) at).toString();
            String attribValue = (String) mainattrs.get(at);

            if (APPLIES_TO.equals(attribName)) {
                appliesTo = attribValue;
            }
        }

        return appliesTo;
    }

    /**
     * Get the last date that a file in the iFix was updated.
     *
     * @param ifixInfo
     * @param xmlFileInfo
     * @return
     */
    /*
     * TODO this code is pinched from
     * com.ibm.ws.repository.resolver.internal.resource.IFixResource after the cut
     * off for editing Kernel code so it is just a copy, it's probably complex
     * enough that instead of doing a copy there should be a utility method
     * somewhere. There is defect:
     *
     * 127842: Combine common code in MassiveIfixes and IFixResource
     *
     * in milestone 15 to cover this work.
     */
    private Date getLatestDateOfAnyFile(IFixInfo ifixInfo, ExtractedFileInformation xmlFileInfo) {
        List<Date> updateDates = new ArrayList<Date>();
        Updates updates = ifixInfo.getUpdates();
        if (updates != null) {
            Collection<UpdatedFile> updatedFiles = updates.getFiles();
            if (updatedFiles != null) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                for (UpdatedFile updatedFile : updatedFiles) {
                    String dateString = updatedFile.getDate();
                    if (dateString != null) {
                        try {
                            Date date = dateFormat.parse(updatedFile.getDate());
                            updateDates.add(date);
                        } catch (ParseException e) {
                            // Ignore this file
                        }
                    }
                }
            }
        }
        Collections.sort(updateDates);

        /*
         * Make sure that the iFix had at least one dated file that it updated. As all updated files have the date field set I don't it is possible to not be set but be lenient in
         * case there is an invalid XML. By setting it to new Date(0) it will mean that this IFixResource will always be sorted last so would only be selected if no other IFix
         * fixes the APARs listed for this iFix.
         */
        Date lastUpdateDate = updateDates.size() > 0 ? updateDates.get(updateDates.size() - 1) : new Date(0);
        return lastUpdateDate;
    }

}
