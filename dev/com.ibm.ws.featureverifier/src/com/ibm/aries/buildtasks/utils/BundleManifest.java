/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BundleManifest
{
    private Manifest _man;
    private List<PackageDeclaration> _importPackage;
    private List<PackageDeclaration> _exportPackage;

    public BundleManifest(File manifestFileOrJar) throws IOException
    {
        if (manifestFileOrJar.isFile() && manifestFileOrJar.getName().endsWith(".jar")) {
            JarFile jar = null;
            try {
                jar = new JarFile(manifestFileOrJar);
                _man = jar.getManifest();
            } finally {
                try {
                    if (jar != null)
                        jar.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            _man = new Manifest(new FileInputStream(manifestFileOrJar));
        }

    }

    /**
     * Extract the packages from the manifest.
     * 
     * @param man the manifest.
     * @param declaration the attribute name the packages are in
     * @return the list of package declarations
     */
    private List<PackageDeclaration> getPackages(Manifest man, String declaration)
    {
        List<PackageDeclaration> packages = new ArrayList<PackageDeclaration>();
        String allPackages = man.getMainAttributes().getValue(declaration);

        if (allPackages != null) {

            List<String> packagesWithAttributes = new ArrayList<String>();

            String[] thePackages = allPackages.split(",");

            // we may have split up based on commas in quotes, so we undo that here.
            for (int i = 0; i < thePackages.length;) {
                String tmp = thePackages[i++].trim();
                if (unbalancedQuotes(tmp)) {
                    for (; !!!thePackages[i].contains("\"");)
                        tmp = tmp + "," + thePackages[i++].trim();

                    tmp = tmp + "," + thePackages[i++].trim();
                }

                packagesWithAttributes.add(tmp);
            }

            for (String aPackage : packagesWithAttributes) {
                String[] attributes = aPackage.split(";");
                PackageDeclaration dec = new PackageDeclaration();
                dec.setPackageName(attributes[0]);

                if ("Import-Package".equals(declaration))
                    dec.setImport(true);

                for (int i = 1; i < attributes.length; i++) {
                    String[] attribParts = attributes[i].split("=");
                    String attribValue = attribParts[1];
                    if (attribValue.startsWith("\"") && attribValue.endsWith("\""))
                        attribValue = attribValue.substring(1, attribValue.length() - 1);

                    dec.getAttributes().put(attribParts[0].trim(), attribValue.trim());
                }
                packages.add(dec);
            }
        }

        return packages;
    }

    /**
     * Returns true if the provided string contains an odd number of " characters.
     * 
     * @param tmp the string to check.
     * @return true if the number of " is odd.
     */
    private boolean unbalancedQuotes(String tmp)
    {
        int count = 0;

        int index = tmp.indexOf('"');

        while (index != -1) {
            count++;
            index = tmp.indexOf('"', index + 1);
        }

        return (count % 2) == 1;
    }

    public List<PackageDeclaration> getImportPackage()
    {
        if (_importPackage == null) {
            _importPackage = getPackages(_man, "Import-Package");
        }
        return _importPackage;
    }

    public List<PackageDeclaration> getExportPackage()
    {
        if (_exportPackage == null) {
            _exportPackage = getPackages(_man, "Export-Package");
        }
        return _exportPackage;
    }
}