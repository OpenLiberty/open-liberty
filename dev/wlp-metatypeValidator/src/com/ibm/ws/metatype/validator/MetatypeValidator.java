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
package com.ibm.ws.metatype.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import com.ibm.ws.metatype.validator.xml.MetatypeAd;
import com.ibm.ws.metatype.validator.xml.MetatypeAdOption;
import com.ibm.ws.metatype.validator.xml.MetatypeBase;
import com.ibm.ws.metatype.validator.xml.MetatypeDesignate;
import com.ibm.ws.metatype.validator.xml.MetatypeObject;
import com.ibm.ws.metatype.validator.xml.MetatypeOcd;
import com.ibm.ws.metatype.validator.xml.MetatypeRoot;

public class MetatypeValidator {
    public enum ValidityState {
        NotValidated,
        Pass,
        Warning,
        Failure,
        MetatypeNotFound
    }

    private final List<Project> projects = new LinkedList<Project>();
    private final List<MetatypeOcdStats> ocdStats = new LinkedList<MetatypeOcdStats>();
    private final File directory;
    private final File outputPath;
    private final List<String> errors = new ArrayList<String>();

    /**
     * Validate all projects within the directory provided.
     *
     * @param directory directory containing the directory to all the projects to validate
     * @throws JAXBException
     */
    public MetatypeValidator(File directory, File outputPath) throws IOException, JAXBException {
        this.outputPath = outputPath;
        this.directory = directory;
        gatherMetatypeFilesAndStats();
    }

    /**
     * Iterates through and gathers the projects to validate along with scanning all the other
     * projects to gather their information for validating the projects.
     *
     * @throws IOException
     * @throws JAXBException
     */
    private void gatherMetatypeFilesAndStats() throws IOException, JAXBException {
        for (File bundle : directory.listFiles()) {
            if (bundle.isFile()) {
                analyzeBundle(bundle);
            }
        }
    }

    private static class MetatypeNamespaceFilter extends StreamReaderDelegate {

        MetatypeNamespaceFilter(XMLStreamReader xsr) {
            super(xsr);
        }

        /** {@inheritDoc} */
        @Override
        public String getNamespaceURI() {
            String ns = super.getNamespaceURI();
            if (ns != null && ns.startsWith("http://www.osgi.org/xmlns/metatype/v")) {
                return "http://www.osgi.org/xmlns/metatype/vANY";
            }
            return ns;
        }

    }

    private void analyzeBundle(File bundleLocation) throws IOException, JAXBException {
        Project project = new Project();
        project.name = bundleLocation.getName();
        InputStream is = null;
        Unmarshaller unmarshaller = null;
        ZipFile z = new ZipFile(bundleLocation);
        for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
            ZipEntry ze = e.nextElement();
            if (ze.getName().startsWith("OSGI-INF/metatype/") && !ze.isDirectory()) {

                HashSet<String> keys = null;
                MetatypeRoot metatype = null;
                String fileName = ze.getName();
                boolean localizationFound = true;

                if (!fileName.endsWith(".xml"))
                    continue;

                is = z.getInputStream(ze);

                try {

                    unmarshaller = JAXBContext.newInstance(MetatypeRoot.class).createUnmarshaller();
                    //for some reason the newFactory method causes a compilation error although eclipse think's it's fine.
                    XMLInputFactory xif = XMLInputFactory.newInstance();
                    XMLStreamReader xsr = xif.createXMLStreamReader(is);
                    metatype = (MetatypeRoot) unmarshaller.unmarshal(new MetatypeNamespaceFilter(xsr));
                } catch (UnmarshalException ue) {
                    errors.add("Could not unmarshal " + fileName + " in bundle " + bundleLocation + " message: " + ue.getMessage());
                    continue;
                } catch (XMLStreamException xse) {
                    errors.add("Could not read xml stream " + fileName + " in bundle " + bundleLocation + " message: " + xse.getMessage());
                    continue;
                } finally {
                    is.close();
                }

                String localization = metatype.getLocalization();
                if (localization != null) {
                    // we should have something like "OSGI-INF/l10n/metatype" where OSGI-INF/l10n is the directory and metatype is the file
                    localization = localization.trim();
                    ZipEntry localizationFile = z.getEntry(localization + ".properties");
                    if (localizationFile != null) {
                        keys = new HashSet<String>();
                        //TODO why not use Properties???
                        BufferedReader reader = new BufferedReader(new InputStreamReader(z.getInputStream(localizationFile)));
                        String line;
                        try {
                            while ((line = reader.readLine()) != null) {
                                if (!line.isEmpty() && !line.startsWith("#")) {
                                    int index = line.indexOf('=');
                                    if (index != -1)
                                        keys.add(line.substring(0, index));
                                }
                            }
                        } finally {
                            reader.close();
                        }
                    } else
                        localizationFound = false;
                }

                ocdStats.addAll(metatype.getMetatypeOcdStats());

                project.validationEntries.add(new ValidationEntry(metatype, ze.getName().substring(ze.getName().lastIndexOf('/')), keys, localizationFound));
            }
        }
        z.close();
        projects.add(project);
    }

    /**
     * Validates one or more projects located in the directory specified by
     * the constructor.
     *
     * @param validateRefs
     *
     * @return a List of validated projects
     * @throws IOException
     */
    public List<Project> validate(boolean validateRefs) throws IOException {
        for (Project project : projects) {
            for (ValidationEntry validationEntry : project.validationEntries) {
                MetatypeRoot metatype = validationEntry.parsedMetatype;
                metatype.setMetatypeFileName(validationEntry.fileName);
                metatype.setOcdStats(ocdStats);
                metatype.setNlsKeys(validationEntry.nlsKeys);
                metatype.validate(validateRefs);
                validationEntry.validity = metatype.getValidityState();

                File outputFile = new File(outputPath + "/" + project.name + "/" + validationEntry.fileName + getExtension(validationEntry.fileName));
                outputFile.getParentFile().mkdirs();
                int pass = 0;
                int fail = 0;
                if (!validationEntry.localizationFound) {
                    validationEntry.validity = ValidityState.Failure;
                    fail++;
                } else {
                    pass++;
                }

                fail += metatype.getErrorMessages().size();
                pass += metatype.getWarningMessages().size() + metatype.getInfoMessages().size();

                for (MetatypeDesignate designate : metatype.getDesignates()) {
                    fail += designate.getErrorMessages().size();
                    pass += designate.getWarningMessages().size() + designate.getInfoMessages().size();

                    for (MetatypeObject object : designate.getObjects()) {
                        fail += object.getErrorMessages().size();
                        pass += object.getWarningMessages().size() + object.getInfoMessages().size();

                        MetatypeOcd ocd = object.getMatchingOcd();

                        if (ocd != null) {
                            fail += ocd.getErrorMessages().size();
                            pass += ocd.getWarningMessages().size() + ocd.getInfoMessages().size();

                            for (MetatypeAd ad : ocd.getAds()) {
                                fail += ad.getErrorMessages().size();
                                pass += ad.getWarningMessages().size() + ad.getInfoMessages().size();

                                for (MetatypeAdOption option : ad.getOptions()) {
                                    fail += option.getErrorMessages().size();
                                    pass += option.getWarningMessages().size() + option.getInfoMessages().size();
                                }
                            }
                        }
                    }

                }

                PrintStream junit = new PrintStream(outputFile);
                junit.printf("<testsuite errors=\"%s\" failures=\"%s\" name=\"%s\" tests=\"%s\" time=\"\">%n", "0", fail, project.name, fail + pass);

                if (!validationEntry.localizationFound) {
                    junit.printf("<testcase classname=\"%s\" name=\"%s\">%n", project.name, "localizationFileNotFound");
                    junit.printf("<failure message=\"%s\"><![CDATA[%s]]></failure>%n", "Could not find file " + metatype.getLocalization(), "");
                    junit.printf("</testcase>%n");
                }

                String fileName = validationEntry.fileName;
                processMetatype(project, metatype, fileName, junit);

                for (MetatypeDesignate designate : metatype.getDesignates()) {
                    String pid = designate.getPid();
                    if (pid == null)
                        pid = designate.getFactoryPid();
                    processMetatype(project, designate, fileName + "." + pid, junit);

                    for (MetatypeObject object : designate.getObjects()) {
                        processMetatype(project, object, fileName + "." + pid, junit);

                        MetatypeOcd ocd = object.getMatchingOcd();

                        if (ocd != null) {
                            processMetatype(project, ocd, fileName + "." + pid, junit);

                            for (MetatypeAd ad : ocd.getAds()) {
                                String id = ad.getId();
                                processMetatype(project, ad, fileName + "." + pid + "." + id, junit);

                                for (MetatypeAdOption option : ad.getOptions()) {
                                    processMetatype(project, option, fileName + "." + pid + "." + id + "." + option, junit);
                                }
                            }
                        }
                    }
                }

                junit.printf("</testsuite>%n");

                junit.close();
                String failMsg = (fail > 0) ? "**FAILURE**" : "";
                System.out.println("Metatype validation " + failMsg + " for " + validationEntry.fileName + " written to " + outputFile.getAbsolutePath());
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException("xml parse errors:\n" + errors);
        }
        return projects;
    }

    /**
     * @param fileName
     * @return
     */
    private String getExtension(String fileName) {
        return fileName.endsWith(".xml") ? "" : ".xml";
    }

    private void processMetatype(Project project, MetatypeBase metatype, String context, PrintStream junit) {
        processFailures(project.name, metatype.getErrorMessages(), context, junit);
        processPasses(project.name, metatype.getWarningMessages(), context, junit);
        processPasses(project.name, metatype.getInfoMessages(), context, junit);
    }

    private void processPasses(String name, List<ValidatorMessage> passes, String context, PrintStream junit) {
        for (ValidatorMessage msg : passes) {
            junit.printf("<testcase classname=\"%s\" name=\"%s.%s.%s\">%n", name, context, msg.getMsgKey(), msg.getId());
            junit.printf("</testcase>%n");
        }
    }

    private void processFailures(String name, List<ValidatorMessage> errors, String context, PrintStream junit) {
        for (ValidatorMessage msg : errors) {
            junit.printf("<testcase classname=\"%s\" name=\"%s.%s.%s\">%n", name, context, msg.getMsgKey(), msg.getId());
            junit.printf("<failure message=\"%s\"><![CDATA[%s]]></failure>%n", msg.getMsg().replaceAll("\"", "&quot;"), msg.getMsg());
            junit.printf("</testcase>%n");
        }
    }

    /**
     * A project to validate
     */
    protected static class Project {
        public String name;
        public final List<ValidationEntry> validationEntries = new LinkedList<ValidationEntry>();
    }

    /**
     * A collection of information regarding a single metatype file, such as the NLS keys,
     * metatype file, the output file name, and validity.
     */
    protected static class ValidationEntry {
        public final MetatypeRoot parsedMetatype;
        public final String fileName;
        public ValidityState validity;
        public final HashSet<String> nlsKeys;
        public final boolean localizationFound;

        public ValidationEntry(MetatypeRoot parsedMetatype, String fileName, HashSet<String> nlsKeys, boolean localizationFound) {
            this.parsedMetatype = parsedMetatype;
            this.fileName = fileName;
            this.nlsKeys = nlsKeys != null ? nlsKeys : new HashSet<String>();
            this.localizationFound = localizationFound;
        }
    }

    /**
     * Represents a single OCD object with regard to it's name, what it's parent PID is, and
     * whether or not it supports extensions.
     */
    public static class MetatypeOcdStats {
        public String designateId;
        public String ocdId;
        public String ibmParentPid;
        public Set<String> ibmObjectClass;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("designate=\"").append(designateId).append("\" ");
            sb.append("ocd=\"").append(ocdId).append("\" ");
            if (ibmParentPid != null)
                sb.append("parentPid=\"").append(ibmParentPid).append("\"");
            if (!ibmObjectClass.isEmpty())
                sb.append("objectClass=\"").append(ibmObjectClass).append("\"");

            return sb.toString();
        }
    }
}
