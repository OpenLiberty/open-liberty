/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json;

import java.io.IOException;
import java.io.File;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Class to emit build information about the JSON4J jar from the manifest data.
 */
public class Version
{
    private static String DEFAULT = "UNKNOWN";

    private String buildDate   = DEFAULT;

    private String specTitle   = DEFAULT;

    private String specVersion = DEFAULT;
    
    private String specVendor  = DEFAULT;

    private String implTitle   = DEFAULT;
    
    private String implVersion = DEFAULT;

    private String implVendor  = DEFAULT;

    private String file        = DEFAULT;

    public static void main(String[] args)
    {
        try
        {
            Version vs = new Version();
            System.out.println("");
            System.out.println("MANIFEST information for jar: [" + vs.getFile() + "]");
            System.out.println("");
            System.out.println("Specification:");
            System.out.println("\tTitle: [" + vs.getSpecificationTitle() + "]");
            System.out.println("\tVersion: [" + vs.getSpecificationVersion() + "]");
            System.out.println("\tVendor: [" + vs.getSpecificationVendor() + "]");
            System.out.println("");
            System.out.println("Implementation:");
            System.out.println("\tTitle: [" + vs.getImplementationTitle() + "]");
            System.out.println("\tVersion: [" + vs.getImplementationVersion() + "]");
            System.out.println("\tVendor: [" + vs.getImplementationVendor() + "]");
            System.out.println("");
            System.out.println("Build date: [" + vs.getBuildDate() + "]");
        }
        catch (Exception ex)
        {
            System.out.println("Fatal error: " + ex.toString());
            ex.printStackTrace();
        }
    }

    public Version()
    {
        try
        {
            Class cls = this.getClass();
            ProtectionDomain pd = cls.getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL jar = cs.getLocation();
            String fName = jar.getFile();
            if (fName != null)
            {
                File file = new File(fName);
                this.file = file.getAbsolutePath();
                JarFile jf = new JarFile(fName);
                Manifest mf = jf.getManifest();
                jf.close();
                
                if (mf != null)
                {
                    Attributes attrs = mf.getMainAttributes();

                    /**
                     * Load all the manifest data available.
                     */
                    if (attrs.getValue("Build-Date") != null)
                    {
                        this.buildDate = attrs.getValue("Build-Date");
                    }
                    if (attrs.getValue(Attributes.Name.SPECIFICATION_TITLE) != null)
                    {
                        this.specTitle = attrs.getValue(Attributes.Name.SPECIFICATION_TITLE);
                    }
                    if (attrs.getValue(Attributes.Name.SPECIFICATION_VERSION) != null)
                    {
                        this.specVersion = attrs.getValue(Attributes.Name.SPECIFICATION_VERSION);
                    }
                    if (attrs.getValue(Attributes.Name.SPECIFICATION_VENDOR) != null)
                    {
                        this.specVendor = attrs.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                    }
                    if (attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE) != null)
                    {
                        this.implTitle = attrs.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                    }
                    if (attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION) != null)
                    {
                        this.implVersion = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    }
                    if (attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR) != null)
                    {
                        this.implVendor = attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("Fatal error: " + ex.toString());
            ex.printStackTrace();
        }
    }

    /**
     * Method to obtain the build date as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getBuildDate()
    {
        return this.buildDate;
    }
    
    /**
     * Method to obtain the specification version as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getSpecificationVersion()
    {
        return this.specVersion;
    }
    
    /**
     * Method to obtain the specification title as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getSpecificationTitle()
    {
        return this.specTitle;
    }
    
    /**
     * Method to obtain the specification vendor as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getSpecificationVendor()
    {
        return this.specVendor;
    }

    /**
     * Method to obtain the implementation version as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getImplementationVersion()
    {
        return this.implVersion;
    }

    /**
     * Method to obtain the implementation title as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getImplementationTitle()
    {
        return this.implTitle;
    }

    /**
     * Method to obtain the implementation vendor as determined from the jar manifest.
     * @returns The build date as a string.
     */
    public String getImplementationVendor()
    {
        return this.implVendor;
    }

    /**
     * Method to obtain filename that was examined
     * @returns The file name as a string
     */
    public String getFile()
    {
        return this.file;
    }
}
