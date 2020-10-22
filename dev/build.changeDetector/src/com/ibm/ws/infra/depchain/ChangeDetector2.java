package com.ibm.ws.infra.depchain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.stream.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class ChangeDetector2 {

    public static enum FileType {
        PRODUCT,
        PRODUCT_FEATURE,
        UNIT_BVT_TEST,
        FAT_TEST,
        INFRA,
        UNKNOWN
    }

    // Inputs
    private final String WLP_DIR;
    private final String REPO_ROOT;
    private final String FAT_FEATURE_DEPS;
    private static String OUTPUT_FILE;
    private final String ACCESS_TOKEN;
    private final String FAT_NAME;

    // Some example OpenLiberty PRs to test with...
    // 4931 --> unittest-only change
    // 4771 --> FAT-only change
    // 4956 --> FAT-infra change
    // 4947 --> feature-file only change (JPA and JAX-RS)
    // 4942 --> low-level product code change
    // 5333 --> adds a new feature

    public static final boolean CACHE_DIFF = false;
    public final boolean LOCAL_ONLY;
    public final boolean DEBUG;

    private final FeatureCollection knownFeatures;

    private Map<String, Set<String>> fatToFeatureMap;

    private static final Set<String> RUN_ALL = Collections.singleton("all");

    public static void main(String stringArgs[]) throws Exception {
        try {
            System.out.println("### BEGIN ChangeDetector");
            MainArgs args = new MainArgs(stringArgs);
            ChangeDetector2 changeDetector = new ChangeDetector2(args);

            Set<String> modifiedFiles = args.getGitDiff() != null ? //
                            changeDetector.getModifiedFilesFromDiff(args.getGitDiff()) : //
                            changeDetector.getModifiedFilesFromUrl(args.getUrl());

            boolean shouldRun = changeDetector.shouldFatRun(modifiedFiles);
            System.out.println("Should fat run? " + shouldRun);
            if (!shouldRun) {
                // Only write the output file if the FAT should NOT run
                // The Gradle build will use the presence of this file to mean that the FAT should be skipped
                BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter(new File(OUTPUT_FILE)));
                outputFileWriter.write("" + shouldRun);

                outputFileWriter.flush();
                outputFileWriter.close();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public ChangeDetector2(MainArgs args) {
        WLP_DIR = args.wlpDir();
        REPO_ROOT = args.getRepoRoot();
        LOCAL_ONLY = args.isLocalOnly();
        DEBUG = args.isDebug();
        FAT_FEATURE_DEPS = args.getDepsFile();
        OUTPUT_FILE = args.getOutputFile();
        ACCESS_TOKEN = args.getAccessToken();
        FAT_NAME = args.getFatName();
        knownFeatures = new FeatureCollection(WLP_DIR, args.getRepoRoot());
        if (DEBUG)
            System.out.println(knownFeatures.toString());
    }

    public boolean shouldFatRun(Set<String> modifiedFiles) throws Exception {

        if (modifiedFiles == null || modifiedFiles.isEmpty()) {
            System.out.println("No modified files detected. Will run all FATs because this is not a normal PR.");
            return true;
        }

        Set<String> fatsToRun = new TreeSet<String>();

        Set<String> modifiedBundles = new HashSet<>();
        Set<String> modifiedFeatures = new HashSet<>();

        // Sort out the modified files
        for (String f : modifiedFiles) {
            FileType fType = getFileType(f);
            switch (fType) {
                case UNIT_BVT_TEST:
                    break;
                case FAT_TEST:
                    if (getProjectName(f).equalsIgnoreCase(FAT_NAME)) {
                        fatsToRun.add(getProjectName(f));
                    }
                    break;
                case PRODUCT:
                    modifiedBundles.add(getBundleName(f));
                    break;
                case PRODUCT_FEATURE:
                    modifiedFeatures.add(f.substring(f.lastIndexOf('/') + 1, f.lastIndexOf('.')));
                    break;
                case INFRA:
                    System.out.println("Run everything because we found an infra file: " + f);
                    return runEverything();
                case UNKNOWN:
                default:
                    System.out.println("Run everything because we found an unknown file: " + f);
                    return runEverything();
            }
        }

        if (modifiedBundles.isEmpty() && modifiedFeatures.isEmpty()) {
            System.out.println("No product or infra code has been modified.");
            return fatsToRun.size() > 0;
        }

        System.out.println("Modified FATs:");
        for (String fat : fatsToRun)
            System.out.println("  " + fat);
        System.out.println("Modified bundles:");
        for (String bundle : modifiedBundles)
            System.out.println("  " + bundle);
        System.out.println("Modified feature manifests: ");
        for (String manifest : modifiedFeatures)
            System.out.println("  " + manifest);

        Set<String> effectedFeatures = getEffectedFeatures(modifiedBundles, modifiedFeatures);
        System.out.println("Computed features impacted:");
        for (String effectedFeature : effectedFeatures)
            System.out.println("  " + effectedFeature);

        fatsToRun.addAll(getFATsTestingFeatures(effectedFeatures));
        if (fatsToRun.contains("all"))
            return runEverything();
        return fatsToRun.size() > 0;
    }

    public Set<String> getEffectedFeatures(Set<String> modifiedBundles, Set<String> modifiedFeatures) {
        Set<String> effectedFeatures = new HashSet<>();
        for (String bundle : modifiedBundles)
            knownFeatures.addFeaturesUsingBundle(bundle, effectedFeatures);
        Set<String> aggregateModifiedFeatures = new HashSet<>(modifiedFeatures);
        aggregateModifiedFeatures.addAll(effectedFeatures);
        for (String feature : aggregateModifiedFeatures)
            knownFeatures.addFeaturesUsingFeature(feature, effectedFeatures);
        return effectedFeatures;
    }

    public Set<String> getFATsTestingFeatures(Set<String> effectedFeatures) {
        Set<String> fatsToRun = new TreeSet<>();

//        {
//            "feature-deps": [
//                "servlet-4.0",
//                "servlet-5.0",
//                "servlet-3.1",
//                "componenttest-1.0"
//            ]
//        }

        // Read fat-metadata.json and create Feature->Bucket and Bucket->Feature mappings
        Map<String, Set<String>> featureToBucketMap = new HashMap<>();
        Map<String, Set<String>> bucketToFeatureMap = new HashMap<>();
        try {
            JsonParser parser = Json.createParser(new FileInputStream(FAT_FEATURE_DEPS));
            parser.next();
            JsonObject fatMap = parser.getObject();
            JsonArray fatsForFeature = fatMap.getJsonArray("feature-deps");
            for (JsonString testedFeatureJson : fatsForFeature.getValuesAs(JsonString.class)) {
                String testedFeature = testedFeatureJson.getString();
                Set<String> fats = featureToBucketMap.get(testedFeature);
                if (fats == null)
                    featureToBucketMap.put(testedFeature, fats = new HashSet<String>());
                fats.add("current");
                Set<String> features = bucketToFeatureMap.get("current");
                if (features == null)
                    bucketToFeatureMap.put("current", features = new HashSet<String>());
                features.add(testedFeature);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return RUN_ALL;
        }

        // Ensure all effected features are known
        for (String effectedFeature : effectedFeatures) {
            if (knownFeatures.get(effectedFeature) == null) {
                System.out.println("Found effected feature " + effectedFeature
                                   + " that was not known to any Liberty feature! Falling back to running everything.");
                return RUN_ALL;
            }
        }

        // Cache this info to speed up unit tests...
        if (fatToFeatureMap == null) {
            fatToFeatureMap = new HashMap<>();
            for (Entry<String, Set<String>> fatToFeatures : bucketToFeatureMap.entrySet()) {
                Set<String> fatEnabledFeatures = new HashSet<>();
                for (String featureShortName : fatToFeatures.getValue()) {
                    Feature f = knownFeatures.getPublic(featureShortName);
                    if (f != null)
                        fatEnabledFeatures.add(f.getSymbolicName());
                }
                Set<String> allEnabledFeatures = knownFeatures.getEnabledFeatures(fatEnabledFeatures);
                fatToFeatureMap.put(fatToFeatures.getKey(), allEnabledFeatures);
            }
        }

        // Determine what buckets enable the effected auto-features
        Set<String> effectedAutoFeatures = knownFeatures.filterAutoOnly(effectedFeatures);
        for (Entry<String, Set<String>> fatToFeatures : fatToFeatureMap.entrySet()) {
            for (String effectedAutoFeature : effectedAutoFeatures) {
                Feature autoFeature = knownFeatures.get(effectedAutoFeature);
                if (autoFeature.isCapabilitySatisfied(fatToFeatures.getValue()))
                    fatsToRun.add(fatToFeatures.getKey());
            }
        }

        // Determine what buckets enable the effected features
        for (String effectedPublicFeature : knownFeatures.filterPublicOnly(effectedFeatures)) {
            Set<String> fatsTestingFeature = featureToBucketMap.get(effectedPublicFeature.toLowerCase());
            if (fatsTestingFeature == null) {
                continue;
            }
            System.out.println("Feature " + effectedPublicFeature + " is tested by FATs: " + fatsTestingFeature);
            fatsToRun.addAll(fatsTestingFeature);
        }

        if (fatsToRun.isEmpty()) {
            //System.out.println("Did not find any FATs testing these changes. Falling back to running everything.");
            return Collections.emptySet();
        }

        // If we are going to run a specific set of FATs, add the ALL_FEATURES buckets at the last second
        fatsToRun.addAll(featureToBucketMap.getOrDefault("all_features", Collections.emptySet()));

        return fatsToRun;
    }

    private Set<String> getModifiedFilesFromDiff(String gitDiffSpec) throws IOException, InterruptedException {
        System.out.println("Parsing modified files using command: git diff --name-only " + gitDiffSpec);
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--name-only", gitDiffSpec);
        if (REPO_ROOT != null)
            pb.directory(Paths.get(REPO_ROOT).toFile());
        Process gitDiff = pb.start();
        if (!gitDiff.waitFor(30, TimeUnit.SECONDS))
            throw new IllegalStateException("Timed out waiting for git diff command to complete");
        BufferedReader reader = new BufferedReader(new InputStreamReader(gitDiff.getInputStream()));
        Set<String> modifiedFiles = new HashSet<>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            modifiedFiles.add(line);
        }

        System.out.println("Found modified files:");
        for (String f : modifiedFiles)
            System.out.println("  " + f);

        return modifiedFiles;
    }

    @Deprecated
    public Set<String> getModifiedFilesFromUrl(String prURL) {
        System.out.println("Parsing modified files for PR: " + prURL);

        // Convert: https://github.com/OpenLiberty/open-liberty/pull/5333
        // to:      https://github.com/OpenLiberty/open-liberty/pull/4947.diff
        // to:      https://github.ibm.com/api/v3/repos/was-liberty/WS-CD-Open/pulls/13986/files
        String prNumber = prURL.substring(prURL.lastIndexOf("/"));

        // Decide which GitHub we need to make an API call to and choose the appropriate URL/access key
        String apiURL = prURL.contains("github.ibm") ? //
                        "https://github.ibm.com/api/v3/repos/was-liberty/WS-CD-Open/pulls" + prNumber + "/files" : //
                        "https://api.github.com/repos/OpenLiberty/open-liberty/pulls" + prNumber + "/files";

        Set<String> modifiedFiles = new HashSet<>();

        // Required to run on older IBM JDK 8 versions
        System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");

        HttpClient client = HttpClientBuilder.create().build();
        boolean readNextPage = true;
        try {
            for (int i = 1; i < 4 && readNextPage; i++) {
                // GitHub API can only return 100 modified files at a time, up to a max of 300 files so request the results in chunks
                Set<String> pageResults = readModifiedFilesFromURL(client, apiURL + "?per_page=100&page=" + i);
                modifiedFiles.addAll(pageResults);
                if (pageResults.size() < 100 || pageResults.contains("ERROR"))
                    readNextPage = false;
            }
        } catch (Exception e) {
            modifiedFiles.add("ERROR");
            e.printStackTrace();
            return modifiedFiles;
        }

        System.out.println("Found modified files:");
        for (String f : modifiedFiles)
            System.out.println("  " + f);

        // Fail in error scenarios of either no files being modified, or the maximum number of files being modified.
        // With the maximum scenario, there is a very small edge case of someone changing exactly 300 files, but it is most likely that
        // a PR that reaches three full pages has more than 300 files changed.
        if (modifiedFiles.isEmpty())
            throw new IllegalStateException("No modified files!  Something must have gone wrong reading this URL: " + apiURL);

        if (modifiedFiles.size() >= 300)
            throw new IllegalStateException("Too many modified files to use this utility: " + apiURL);

        return modifiedFiles;
    }

    private Set<String> readModifiedFilesFromURL(HttpClient client, String url) throws Exception {
        Set<String> modifiedFiles = new HashSet<>();

        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("Authorization", "token " + ACCESS_TOKEN);

        HttpResponse firstResponse = client.execute(getRequest);
        String responseStr = EntityUtils.toString(firstResponse.getEntity());

        // Dump the JSON array output of the API call into an object to iteration on
        JsonReader outputJsonReader = Json.createReader(new StringReader(responseStr));
        JsonArray outputJsonArray = null;
        try {
            outputJsonArray = outputJsonReader.readArray();
        } catch (Exception e) {
            // This usually happens if we had a bad auth token
            System.out.println(responseStr);
            throw e;
        }
        outputJsonReader.close();

        // Parse through each JSON element looking for the name of the file that was changed
        for (int index = 0; index < outputJsonArray.size(); index++) {
            JsonObject currentJson = outputJsonArray.getJsonObject(index);
            String changedFile = currentJson.getString("filename");
            modifiedFiles.add(changedFile);
        }

        return modifiedFiles;
    }

    private boolean runEverything() {
        return true;
    }

    private static String getProjectName(String file) {
        if (file == null || !file.contains("dev/"))
            return null;
        String afterDev = file.substring(4);
        if (!afterDev.contains("/"))
            return null; // a root file such as 'dev/build.gradle'
        return afterDev.substring(0, afterDev.indexOf('/'));
    }

    /**
     * Normally the bundle name is simply the name of the project folder, but there are some aggregate
     * project folders for API bundles that do not follow this pattern.
     *
     * @param file a modified file
     * @return the bundle name of a given file change.
     */
    private static String getBundleName(String file) {
        String projectName = getProjectName(file);
        if (isApiParentProject(projectName) && file.endsWith(".bnd")) {
            String fileName = file.substring(file.lastIndexOf('/') + 1, file.length() - 4);
            return projectName + '.' + fileName;
        }
        return projectName;
    }

    static FileType getFileType(String fPath) {
        if (fPath == null)
            return FileType.UNKNOWN;

        if (fPath.contains("dependabot"))
            return FileType.UNIT_BVT_TEST;

        if (!fPath.startsWith("dev/"))
            return FileType.UNKNOWN;

        String projectName = getProjectName(fPath);
        if (projectName == null)
            return FileType.INFRA;

        if (projectName.contains("_test") ||
            projectName.contains("_bvt") ||
            projectName.startsWith("build.changeDetector") ||
            fPath.contains("/test/") ||
            fPath.endsWith(".maven") ||
            fPath.endsWith("/.gitignore") ||
            fPath.matches(".*\\/l10n/[^\\/]*\\.properties"))
            return FileType.UNIT_BVT_TEST;
        if (isFATCommonProject(projectName))
            return FileType.INFRA;
        if (projectName.contains("_fat") ||
            projectName.contains("_zfat"))
            return FileType.FAT_TEST;
        if (projectName.contains("_tools"))
            return FileType.INFRA;
        // Consider any project that does not have an IBM WebSphere prefix to be infra
        if (!projectName.startsWith("com.ibm.ws.") &&
            !projectName.startsWith("com.ibm.wsspi.") &&
            !projectName.startsWith("com.ibm.websphere.") &&
            !projectName.startsWith("io.openliberty."))
            return FileType.INFRA;
        if (fPath.endsWith(".feature") ||
            fPath.endsWith(".mf"))
            return FileType.PRODUCT_FEATURE;

        return FileType.PRODUCT;
    }

    private static boolean isApiParentProject(String projectName) {
        String[] apiProjects = {
                                 "com.ibm.websphere.appserver.api",
                                 "com.ibm.websphere.appserver.spi",
                                 "com.ibm.websphere.javaee",
                                 "com.ibm.websphere.org.eclipse.microprofile",
                                 "io.openliberty.jakarta",
                                 "io.openliberty.org.eclipse.microprofile"
        };
        for (String apiProject : apiProjects)
            if (projectName.equalsIgnoreCase(apiProject))
                return true;
        return false;
    }

    private static boolean isFATCommonProject(String projectName) {
        // The following use a name pattern that matches '*_fat' which normally indicates
        // a FAT, but are actually INFRA projects (i.e. other FATs depend on them)
        // com.ibm.ws.security.csiv2_fat.common
        // com.ibm.ws.security.oauth.oidc_fat.common
        // com.ibm.ws.security.jwt_fat.common
        // com.ibm.ws.security.saml.sso_fat.common
        // com.ibm.ws.security_fat.common.tooling
        // com.ibm.ws.security.openidconnect.server_fat
        // com.ibm.ws.collective.security_fat.common
        // com.ibm.ws.security.audit_fat.common.tooling
        // com.ibm.ws.wssecurity_fat
        return projectName.contains("_fat.common") ||
               projectName.equals("com.ibm.ws.security.openidconnect.server_fat") ||
               projectName.equals("com.ibm.ws.wssecurity_fat");
    }

}
