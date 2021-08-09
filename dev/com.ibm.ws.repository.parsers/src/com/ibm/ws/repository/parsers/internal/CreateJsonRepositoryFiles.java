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
package com.ibm.ws.repository.parsers.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.parsers.EsaParser;
import com.ibm.ws.repository.parsers.Parser;
import com.ibm.ws.repository.parsers.PaxParser;
import com.ibm.ws.repository.parsers.ProductParser;
import com.ibm.ws.repository.parsers.ProductZipParser;
import com.ibm.ws.repository.parsers.SamplesParser;
import com.ibm.ws.repository.parsers.ToolParser;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;

public class CreateJsonRepositoryFiles extends Task {

    private File assetFile;
    private File metadataFile;
    private String assetType;
    private ResourceType assetTypeEnumValue;
    private String outputLocation;

    @Override
    public void execute() throws BuildException {
        // trace input then create MassiveResource.Type enum
        System.out.println("CreateJsonRepositoryFiles called with following parameters");
        System.out.println("- AssetFile      = " + getAssetFile().getAbsolutePath());
        String metadataFileAbsPath = (getMetadataFile() == null) ? "null" : getMetadataFile().getAbsolutePath();
        System.out.println("- MetadataFile   = " + metadataFileAbsPath);
        System.out.println("- AssetType      = " + getAssetType());
        System.out.println("- OutputLocation = " + getOutputLocation());
        try {
            assetTypeEnumValue = ResourceType.valueOf(assetType);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            throw new BuildException(iae);
        }

        OutputStream os = null;
        Parser<? extends RepositoryResourceWritable> parser = null;

        try {
            // PRODUCTSAMPLE,OPENSOURCE,INSTALL,ADDON,FEATURE,IFIX,ADMINSCRIPT,CONFIGSNIPPET,TOOL
            switch (assetTypeEnumValue) {

                case PRODUCTSAMPLE:
                case OPENSOURCE:
                    parser = new SamplesParser();
                    break;

                case CONFIGSNIPPET:
//		      NOT YET IMPLEMENTED
//                    parser = new ConfigSnippetParser(null);
                    break;

                case ADMINSCRIPT:
//		      NOT YET IMPLEMENTED
//                   parser = new AdminScriptParser(null);
                    break;

                case TOOL:
                    parser = new ToolParser();
                    break;

                case ADDON:
                case INSTALL:
                    if (isPax(assetFile)) {
                        parser = new PaxParser();
                    } else if (isZip(assetFile)) {
                        parser = new ProductZipParser();
                    } else {
                        parser = new ProductParser();
                    }
                    break;

                case FEATURE:
                    parser = new EsaParser();
                    break;

                case IFIX:
//		      NOT YET IMPLEMENTED
//                    parser = new IfixParser(null);
                    break;

                default:
                    // we won't produce JSON for unlisted types
            }
            if (parser != null) {
                RepositoryResourceWritable resource = parser.parseFileToResource(assetFile, metadataFile, null);
                resource.updateGeneratedFields(true);
                os = getOutputStreamFromPath(getOutputLocation(), assetFile);
                resource.writeDiskRepoJSONToStream(os);
            } else {
                // Not a file we build json for
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                // Tidy up any temporary files.
                if (parser != null) {
                    parser.tidyUp();
                }
            } catch (IOException e) {
                // We will not cause the build to fail if this fails.  Just stack trace it and continue.
                e.printStackTrace();
            }
        }
    }

    private boolean isPax(File f) {
        return f.getName().toLowerCase().endsWith(".pax");
    }

    private boolean isZip(File f) {
        return f.getName().toLowerCase().endsWith(".zip");
    }

    private OutputStream getOutputStreamFromPath(String path, File ipFile) {

        path = path + "/" + ipFile.getName() + ".json";

        OutputStream os = null;
        try {
            File f = new File(path);
            System.out.println("Writing json to " + f.getAbsolutePath());
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            os = new FileOutputStream(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return os;
    }

    // ---------------------------- SETTERS AND GETTERS BELOW

    /**
     * @return the outputLocation
     */
    public String getOutputLocation() {
        return outputLocation;
    }

    /**
     * @param outputLocation
     *            the outputLocation to set
     */
    public void setOutputLocation(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    /**
     * @return the assetFile
     */
    public File getAssetFile() {
        return assetFile;
    }

    /**
     * @param assetFile
     *            the assetFile to set
     */
    public void setAssetFile(File assetFile) {
        this.assetFile = assetFile;
    }

    /**
     * @return the assetFile
     */
    public File getMetadataFile() {
        return metadataFile;
    }

    /**
     * @param metadataFile
     *            the assetFile to set
     */
    public void setMetadataFile(File metadataFile) {
        if (metadataFile.getName().equalsIgnoreCase("null")) {
            // we didn't pass in a file (just a dummy null file) so set variable to null
            this.metadataFile = null;
        } else {
            this.metadataFile = metadataFile;
        }
    }

    /**
     * @return the assetType
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * @param assetType
     *            the assetType to set
     */
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

}