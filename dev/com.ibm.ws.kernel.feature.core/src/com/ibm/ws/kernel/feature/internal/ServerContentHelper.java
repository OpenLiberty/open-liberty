/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.internal.subsystem.KernelFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.launch.service.ServerContent;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 *
 */
public class ServerContentHelper {
    private static final TraceComponent tc = Tr.register(ServerContentHelper.class);

    private final BundleContext bundleContext;
    private final FeatureManager featureResolver;
    private final WsLocationAdmin locationService;

    static boolean isServerContentRequest(BundleContext bc) {
        String contentRequest = bc.getProperty(ServerContent.REQUEST_SERVER_CONTENT_PROPERTY);

        //check the property is set, and has the expected content..
        //otherwise this is a no-op.
        if (contentRequest == null || !contentRequest.equals("1.0.0")) {
            return false;
        }

        return true;
    }

    ServerContentHelper(BundleContext bundleContext, FeatureManager featureResolver, WsLocationAdmin locationAdmin) {
        this.bundleContext = bundleContext;
        this.featureResolver = featureResolver;
        this.locationService = locationAdmin;
    }

    /**
     * Builds a list of server content and publishes a service to let the launcher obtain them..
     * <p>
     * <em>ONLY</em> takes effect if the Launcher has set the magic server content property
     */
    void processServerContentRequest() {

        File installRoot = new File(locationService.resolveString("${wlp.install.dir}"));

        final Set<String> absPathsForLibertyContent = new HashSet<String>();
        final Map<String, List<String>> specificPlatformPathsByPlatform = new HashMap<String, List<String>>();
        Set<String> discoveredFeatures = new HashSet<String>();
        Collection<FeatureResource> fixes = new ArrayList<FeatureResource>();

        // We need ALL Kernel features (even the ones that aren't active in the system)
        // as well as all installed features.
        List<ProvisioningFeatureDefinition> allFeatureDefs = new ArrayList<ProvisioningFeatureDefinition>();
        allFeatureDefs.addAll(KernelFeatureDefinitionImpl.getAllKernelFeatures(bundleContext, locationService));
        allFeatureDefs.addAll(featureResolver.getInstalledFeatureDefinitions());

        //keep a list of product extensions being used in the server
        List<String> extensionRepos = new ArrayList<String>();

        for (ProvisioningFeatureDefinition fd : allFeatureDefs) {

            BundleRepositoryHolder brh = BundleRepositoryRegistry.getRepositoryHolder(fd.getBundleRepositoryType());
            ContentBasedLocalBundleRepository br = brh.getBundleRepository();
            Collection<FeatureResource> frs = fd.getConstituents(null);

            /*
             * lib/fixes processing needs to happen here, to filter the xml down to just the current in use set.
             * made slightly more complex, because the locations tracked for minify are absolute, so we have to identify and trim installroot
             * to identify lib/fixes/* content, which we process afterward
             */
            File libFixes = new File(installRoot, "lib/fixes");

            for (FeatureResource fr : frs) {
                switch (fr.getType()) {
                    case FEATURE_TYPE: {
                        //type = feature, no paths to worry about, we are processing the list of all installed features
                        //                which will include nested features.
                        discoveredFeatures.add(fr.getSymbolicName());
                        break;
                    }

                    case BUNDLE_TYPE:
                    case JAR_TYPE:
                    case BOOT_JAR_TYPE: {
                        //type = bundle, need to query the bundle repository for this feature def, to find which
                        //               files are being used.
                        //type = jar, basically same as type=bundle.. for minify purposes.
                        //type = boot.jar, basically same as type=bundle.. for minify purposes.
                        File b = br.selectBundle(fr.getLocation(), fr.getSymbolicName(), fr.getVersionRange());
                        File bb = br.selectBaseBundle(fr.getLocation(), fr.getSymbolicName(), fr.getVersionRange());
                        //b & bb will be the same bundle if the base was the selected.. 
                        if (b != null && bb != null) {
                            boolean pathsSame = b.getAbsolutePath().equals(bb.getAbsolutePath());
                            absPathsForLibertyContent.add(b.getAbsolutePath());
                            if (!pathsSame) {
                                absPathsForLibertyContent.add(bb.getAbsolutePath());
                            }
                            List<String> osList = fr.getOsList();
                            if (osList != null) {
                                for (String os : osList) {
                                    List<String> pathsForOs = specificPlatformPathsByPlatform.get(os);
                                    if (pathsForOs == null) {
                                        pathsForOs = new ArrayList<String>();
                                        specificPlatformPathsByPlatform.put(os, pathsForOs);
                                    }
                                    pathsForOs.add(b.getAbsolutePath());
                                    if (!pathsSame) {
                                        pathsForOs.add(bb.getAbsolutePath());
                                    }
                                }
                            }
                        } else {
                            //b & bb were null, we have no match for this resource.. 
                            //maybe the resource was minfiied out?
                            List<String> osList = fr.getOsList();
                            if (osList != null) {
                                //ignore the missing resource, as the file is tagged for specific os's,
                                //and will be reported missing based on the os filters later.
                                //(if the user has no os filter set, they meant 'all currently supported by the install')
                                //(so this can still be ignored).
                            } else {
                                //use match string for error message, it provides best chance for being helpful.
                                Tr.warning(tc, "ERROR_MISSING_FEATURE_RESOURCE", new Object[] { fd.getFeatureName(), fr.getMatchString() });
                            }
                        }
                        break;
                    }

                    case FILE_TYPE: {
                        //file uses loc as a relative path from install root.
                        String locString = fr.getLocation();
                        if (locString != null) {
                            String[] locs;
                            if (locString.contains(",")) {
                                locs = locString.split(",");
                            } else {
                                locs = new String[] { locString };
                            }

                            for (String loc : locs) {
                                File test = new File(loc);
                                if (!test.isAbsolute()) {
                                    test = new File(brh.getInstallDir(), loc);
                                }
                                loc = test.getAbsolutePath();
                                //filter fixes content into the fixes set, all other content is remembered.
                                if (loc.startsWith(libFixes.getAbsolutePath() + File.separator)) {
                                    fixes.add(fr);
                                } else {
                                    absPathsForLibertyContent.add(loc);
                                }

                                List<String> osList = fr.getOsList();
                                if (osList != null) {
                                    for (String os : osList) {
                                        List<String> pathsForOs = specificPlatformPathsByPlatform.get(os);
                                        if (pathsForOs == null) {
                                            pathsForOs = new ArrayList<String>();
                                            specificPlatformPathsByPlatform.put(os, pathsForOs);
                                        }
                                        pathsForOs.add(loc);
                                    }
                                }
                            }
                        } else {
                            //a file type without a loc is bad, means misuse of the type.
                            Tr.warning(tc, "ERROR_UNKNOWN_FEATURE_RESOURCE_TYPE", new Object[] { fd.getFeatureName(), fr.getSymbolicName(), "file" });
                        }
                        break;
                    }

                    case UNKNOWN: {
                        //if its not jar,bundle,feature, or file.. then something is wrong.
                        Tr.warning(tc, "ERROR_UNKNOWN_FEATURE_RESOURCE_TYPE", new Object[] { fd.getFeatureName(),
                                                                                            fr.getSymbolicName(),
                                                                                            fr.getRawType() });
                        //we assume that other types will use the location field as something useful.
                        String loc = fr.getLocation();
                        if (loc != null) {
                            File test = new File(loc);
                            if (!test.isAbsolute()) {
                                test = new File(installRoot, loc);
                            }
                            absPathsForLibertyContent.add(test.getAbsolutePath());

                            List<String> osList = fr.getOsList();
                            if (osList != null) {
                                for (String os : osList) {
                                    List<String> pathsForOs = specificPlatformPathsByPlatform.get(os);
                                    if (pathsForOs == null) {
                                        pathsForOs = new ArrayList<String>();
                                        specificPlatformPathsByPlatform.put(os, pathsForOs);
                                    }
                                    pathsForOs.add(test.getAbsolutePath());
                                }
                            }
                        }
                        break;
                    }
                    default:
                        break;
                }
            }
            //add in (all) the NLS files for this featuredef.. if any..
            for (File nls : fd.getLocalizationFiles()) {
                if (nls.exists()) {
                    absPathsForLibertyContent.add(nls.getAbsolutePath());
                }
            }

            //add in the manifest for the feature itself .. 
            File featureFile = fd.getFeatureDefinitionFile();
            if (featureFile != null) {
                absPathsForLibertyContent.add(featureFile.getAbsolutePath());
            }
            File checksumFile = fd.getFeatureChecksumFile();
            if (checksumFile != null) {
                absPathsForLibertyContent.add(checksumFile.getAbsolutePath());
            }

            String repoType = fd.getBundleRepositoryType();
            //work out if this content is a product extension and if it is include its properties file
            if (repoType != null && !ExtensionConstants.CORE_EXTENSION.equals(repoType)
                && !ExtensionConstants.USER_EXTENSION.equals(repoType)) {
                //not core or user, must be a product extension
                extensionRepos.add(repoType);
            }

            // Add in any Icon files
            File featureDefinitionFile = fd.getFeatureDefinitionFile();
            if (featureDefinitionFile != null) {
                File featureDefinitionParent = featureDefinitionFile.getParentFile();
                Collection<String> expectedIcons = fd.getIcons();
                if (featureDefinitionParent != null && expectedIcons != null) {
                    File iconsFolder = new File(featureDefinitionParent, "icons/" + fd.getSymbolicName());
                    scanFolderForIcons(absPathsForLibertyContent, iconsFolder.getAbsolutePath(), iconsFolder, expectedIcons);
                }
            }

        }

        //if there are any extensions in use we need to bring their wlp/etc/extensions/*.properties file along
        for (String extensionRepo : extensionRepos) {
            WsResource extensionProperties = locationService.getRuntimeResource("etc/extensions/" + extensionRepo + ".properties");
            absPathsForLibertyContent.add(locationService.resolveResource(extensionProperties.toRepositoryPath()).asFile().getAbsolutePath());
        }

        //this odd looking bit of code will take the current list of files identified,
        //and if any are jars, will look in them to see if they have manifest classpaths
        //if so, the jars referenced by the manifest classpaths are added to the manifestJars
        //argument.
        //We have to start by passing both args as the current set of content (but using different set instances).
        Set<String> manifestJars = new HashSet<String>();
        manifestJars.addAll(absPathsForLibertyContent);
        gatherManifestClasspathJars(absPathsForLibertyContent, manifestJars);
        manifestJars.removeAll(absPathsForLibertyContent);

        //now manifestJars contains any jars that we didn't already know about, discovered via manifests.
        absPathsForLibertyContent.addAll(manifestJars);

        //process the fixes set of fix xml's, and add the required ones to the set of files to keep.
        addRequiredFixFiles(installRoot, absPathsForLibertyContent, fixes);

        final String OS_REMOVE_CHAR = "-";

        //publish the service that will be used by the launcher to read back the data.
        final Dictionary<String, Object> d = new Hashtable<String, Object>();
        final String[] pathArray = absPathsForLibertyContent.toArray(new String[] {});

        bundleContext.registerService(ServerContent.class, new ServerContent() {
            @Override
            public String[] getServerContentPaths(String osRequest) throws IOException {
                if (osRequest == null || osRequest.isEmpty() || "all".equals(osRequest))
                    return pathArray;
                else {
                    //filter paths as required.. then return them..                                       
                    String[] osNames = osRequest.split(",");
                    boolean add = true;

                    //check if this is an add, or a remove.
                    //if both are specified, remove wins ;p
                    for (String osName : osNames) {
                        if (osName.startsWith(OS_REMOVE_CHAR)) {
                            add = false;
                        }
                    }

                    if (add) {
                        //someone has named an os (or os's) they want
                        //so we remove everything, and add back just those os's.
                        for (Map.Entry<String, List<String>> me : specificPlatformPathsByPlatform.entrySet()) {
                            absPathsForLibertyContent.removeAll(me.getValue());
                        }
                        for (String osName : osNames) {
                            if (!osName.startsWith(OS_REMOVE_CHAR)) {
                                List<String> osPaths = specificPlatformPathsByPlatform.get(osName);
                                if (osPaths != null && !osPaths.isEmpty()) {
                                    absPathsForLibertyContent.addAll(osPaths);

                                    //verify the requested paths File.exists .. otherwise 
                                    //this could be an error ?
                                    for (String osPath : osPaths) {
                                        if (!FileUtils.fileExists(new File(osPath))) {
                                            throw new FileNotFoundException(osPath);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        //user request 'all except this platform' type filter..
                        for (String osName : osNames) {
                            if (osName.startsWith(OS_REMOVE_CHAR)) {
                                osName = osName.substring(1);
                                List<String> osPaths = specificPlatformPathsByPlatform.get(osName);
                                if (osPaths != null && !osPaths.isEmpty()) {
                                    absPathsForLibertyContent.removeAll(osPaths);
                                    //in this kind of filter, we don't need to test at all.
                                }
                            } else {
                                List<String> osPaths = specificPlatformPathsByPlatform.get(osName);
                                if (osPaths != null && !osPaths.isEmpty()) {
                                    //verify the requested paths File.exists .. otherwise 
                                    //this could be an error.
                                    for (String osPath : osPaths) {
                                        if (!FileUtils.fileExists(new File(osPath))) {
                                            throw new FileNotFoundException(osPath);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //return newly filtered data
                    return absPathsForLibertyContent.toArray(new String[] {});
                }
            }
        }, d);

        //we're done here.. let liberty proceed to declare itself 'started' =)
    }

    /**
     * 
     * Scan a folder and any subfolders for icons, and compare them to the list of expected icons
     * 
     * @param iconPaths the collection where found icons will go. Cannot be null
     * @param baseFolder the folder from where we will start searching. Used when we recurse to maintain a root
     * @param iconsFolder the current folder being scanned.
     * @param icons the collection of expected icons. Cannot be null
     */
    private void scanFolderForIcons(Collection<String> iconPaths, String baseFolder, File iconsFolder, Collection<String> icons) {
        if (iconsFolder != null && iconsFolder.exists() && baseFolder != null) {
            for (File fileInIconFolder : iconsFolder.listFiles()) {
                if (fileInIconFolder.isDirectory()) {
                    // This is a directory, so scan it for more files
                    scanFolderForIcons(iconPaths, baseFolder, fileInIconFolder, icons);
                } else {
                    // This is a file. If its in the list of icons then add it
                    String absPath = fileInIconFolder.getAbsolutePath();

                    for (String iconAbsPath : icons) {
                        // get the base part of the folder so we can always check we exist relative to some other folder
                        // for consistency we assume file separators will only be forward slash, but when we store the
                        // full path it should resolve to the system default
                        String relativePath = absPath.replaceAll("\\\\", "/").substring(baseFolder.length() + 1);
                        if (relativePath.equals(iconAbsPath)) {
                            iconPaths.add(absPath);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void gatherManifestClasspathJars(Set<String> sources, Set<String> found) {
        Set<String> foundFromHere = new HashSet<String>();
        for (String s : sources) {
            File test = new File(s);
            File parent = test.getParentFile();
            if (test.isFile() && s.endsWith(".jar")) {
                JarFile jar = null;
                try {
                    jar = new JarFile(test);
                    String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
                    if (cp != null) {
                        String jars[] = cp.split(" ");
                        for (String j : jars) {
                            File mfJar = new File(parent, j);
                            if (mfJar.isFile()) {
                                String abs = mfJar.getCanonicalPath();
                                if (!found.contains(abs)) {
                                    foundFromHere.add(mfJar.getCanonicalPath());
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    //means we were unable to open the jar to read its manifest 
                    //which means we may be missing some jars.. also, means the jar
                    //is broken.
                    Tr.warning(tc, "ERROR_OPENING_JAR_FOR_CLASSPATH", new Object[] { test.getAbsolutePath(), e });
                    //continue, to find other jars.
                } finally {
                    tryToClose(jar);
                }
            }
        }
        if (!foundFromHere.isEmpty()) {
            Set<String> newFound = new HashSet<String>();
            newFound.addAll(sources);
            newFound.addAll(foundFromHere);
            gatherManifestClasspathJars(foundFromHere, newFound);
            newFound.removeAll(sources);
            found.addAll(newFound);
        }
    }

    /**
     * Take the set of FeatureResources representing lib/fixes content
     * and add the ones required for the current server to the set of absolute paths.
     * 
     * @param installRoot required to process absolute paths to files in the set
     * @param absPathsForLibertyContent set to add paths to keep to, expected to already contain all paths planned to be kept other than fix xmls.
     * @param fixes collection of FeatureResources
     */
    private void addRequiredFixFiles(File installRoot, Set<String> absPathsForLibertyContent, Collection<FeatureResource> fixes) {
        //Filter the fix xmls, and add the ones required.
        //has to be last thing done to absPath set, as it uses the total set of files being kept so far to determine which
        //xmls to keep.
        for (FeatureResource fixFile : fixes) {
            //if fixFile isnt xml.. add anyways.. (if we don't understand it, we better keep it)
            //if fixFile IS xml, then we need to make sense of it.
            /*
             * <fix>
             * <updates>
             * <file hash="sha1hash" id="path under installroot"/>
             * </updates>
             * </fix>
             * 
             * if any file in updates exists in our current set of paths-to-keep
             * AND said file has the same SHA1
             * THEN keep xml.
             * else (if no files exist, or all that did had different sha1's)
             * discard xml.
             */
            String fixFileLoc = fixFile.getLocation();
            //convert fix loc back to absolute.. featureResources use relative to wlp root.
            File f = new File(installRoot, fixFileLoc);
            String absFixFileLoc = f.getAbsolutePath();
            if (fixFileLoc.regionMatches(true, fixFileLoc.length() - 4, ".xml", 0, 4)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Evaluating fix xml " + fixFileLoc);
                }
                try {
                    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document d = db.parse(f);
                    NodeList fl = d.getChildNodes();
                    for (int x = 0; x < fl.getLength(); x++) {
                        Node fn = fl.item(x);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "n.getNodeName(" + x + ")" + fn.getNodeName() + " element?" + (fn instanceof Element));
                        }
                        if ((fn instanceof Element) && "fix".equals(fn.getNodeName())) {
                            NodeList nl = fn.getChildNodes();
                            for (int i = 0; i < nl.getLength(); i++) {
                                Node n = nl.item(i);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "n.getNodeName(" + i + ")" + n.getNodeName() + " element?" + (n instanceof Element));
                                }
                                if ((n instanceof Element) && "updates".equals(n.getNodeName())) {
                                    NodeList files = n.getChildNodes();
                                    for (int j = 0; j < files.getLength(); j++) {
                                        Node o = files.item(j);
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            Tr.debug(tc, "o.getNodeName(" + j + ")" + o.getNodeName() + " element?" + (o instanceof Element));
                                        }
                                        if ((o instanceof Element) && "file".equals(o.getNodeName())) {
                                            Element eo = (Element) o;
                                            String oName = eo.getAttribute("id");
                                            String oHash = eo.getAttribute("hash");

                                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                Tr.debug(tc, "eo.getNodeName()" + eo.getNodeName() + " oName:" + oName + " oHash:" + oHash);
                                            }

                                            File testFile = new File(installRoot, oName);
                                            if (FileUtils.fileExists(testFile)) {
                                                String tHash = HashUtils.getFileMD5String(testFile);
                                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                    Tr.debug(tc, "tHash:" + tHash);
                                                }
                                                if (oHash.equals(tHash)) {
                                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                                        Tr.debug(tc, "Keeping fix xml due to hash match" + fixFileLoc);
                                                    }
                                                    absPathsForLibertyContent.add(absFixFileLoc);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (ParserConfigurationException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to parse file " + fixFileLoc + " due to parser config error, file will be kept regardless");
                    }
                    //error? add the file anyways.. 
                    absPathsForLibertyContent.add(absFixFileLoc);
                } catch (SAXException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to parse file " + fixFileLoc + " due to sax error, file will be kept regardless");
                    }
                    //error? add the file anyways.. 
                    absPathsForLibertyContent.add(absFixFileLoc);
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to parse file " + fixFileLoc + " due to io error, file will be kept regardless");
                    }
                    //error? add the file anyways.. 
                    absPathsForLibertyContent.add(absFixFileLoc);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to parse file " + fixFileLoc + " not xml, file will be kept regardless");
                }
                //not an xml file? add the file anyways.. 
                absPathsForLibertyContent.add(absFixFileLoc);
            }
        }
    }

    @FFDCIgnore(IOException.class)
    private static final void tryToClose(JarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (IOException ioe) {
                // ignore. Used in finally.
            }
        }
    }
}
