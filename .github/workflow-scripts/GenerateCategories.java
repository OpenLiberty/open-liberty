import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class GenerateCategories {
    
    //Constants
    static final boolean DEBUG = true;
    static final String TEST_CATEGORY_DIR = ".github/test-categories/";
    static final String MODIFIED_FILES_DIFF = ".github/modified_files.diff";

    //Calculations
    static List<String> moddedFiles = getModifiedFiles();
    static final Set<String> modifiedProjects = getModifiedProjects();


    /**
     * Main method - outputs a JSON string of in the format of: 
     * <pre>
     * {
     *  "include": [
     *      { "category": "foo" },
     *      { "category": "bar" }
     *  ]
     * }
     * </pre> 
     * 
     * @param args - Provide file location of PR summary so we can parse it for mentioned categories
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        debug("Starting");

        String prSummary = getPRSummary(args);

        // Discover all categories by checking for files under .github/test-categories/
        SortedSet<String> allCategories = new TreeSet<>();
        for (File categoryFile : Paths.get(TEST_CATEGORY_DIR).toFile().listFiles()) {
            if(! categoryFile.getName().equalsIgnoreCase("QUARANTINE")) {
                allCategories.add(categoryFile.getName().toUpperCase());
            }
        }
        debug("All discovered categories are: " + allCategories);

        // Any categories that were mentioned in the PR summary should run first
        TreeMap<Integer, String> mentionedCategories = new TreeMap<>();
        for (String category : allCategories) {
            int categoryIndex = prSummary.indexOf(category);
            if (categoryIndex >= 0) {
                debug("PR summary mentioned category: " + category + " at index " + categoryIndex);
                mentionedCategories.put(categoryIndex, category);
            }
        }
        debug("Explicitly mentioned categories were: " + mentionedCategories.values());

        SortedSet<String> unmentionedCategories = new TreeSet<>(allCategories);
        unmentionedCategories.removeAll(mentionedCategories.values());

        // If this was a FAT Only change then we want to run Modified Categories + Mentioned Categories
        if (isFATOnlyChange()) {
            SortedSet<String> modifiedCategories = new TreeSet<>();
            debug("This is a FAT-only change. Will remove unrelated and unmentioned categories.");
            for (String category : unmentionedCategories) {
                List<String> categoryBuckets = getBuckets(category);
                for (String bucket : categoryBuckets) {
                    if (modifiedProjects.contains(bucket)) {
                        debug("Will run category " + category + " because it contains modified FAT bucket: " + bucket);
                        modifiedCategories.add(category);
                        break;
                    }
                }
            }

            unmentionedCategories = modifiedCategories;
            debug("Modified but unmentioned categories were: " + unmentionedCategories);
        } else {
            debug("Unmentioned categories were: " + unmentionedCategories);
        }

        List<String> finalCategories = new ArrayList<>();
        // If any FATs were modified at all, add special categories
        // that run directly modified buckets
        List<String> modifiedFATs = modifiedProjects.stream().filter(f -> f.contains("_fat")).collect(Collectors.toList());
        if (!modifiedFATs.isEmpty()) {
            debug("At least 1 FAT was modified in this PR.");

            String liteCategory = "MODIFIED_LITE_MODE";
            finalCategories.add(liteCategory);
            Path liteOut = Paths.get(TEST_CATEGORY_DIR + liteCategory);

            int partitionSize = 5;
            for (int i = 0; i < modifiedFATs.size(); i += partitionSize) {
                String fullCategory = "MODIFIED_FULL_MODE_" + (i / partitionSize);
                finalCategories.add(fullCategory);
                Path fullOut = Paths.get(TEST_CATEGORY_DIR + fullCategory);
                Files.write(liteOut, modifiedFATs.subList(i, Math.min(i + partitionSize, modifiedFATs.size())), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.write(fullOut, modifiedFATs.subList(i, Math.min(i + partitionSize, modifiedFATs.size())), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
        
        // now add mentioned categories in order
        finalCategories.addAll(mentionedCategories.values());
        // finally add all remaining categories
        finalCategories.addAll(unmentionedCategories);

        debug("Final categories are:");
        debug(finalCategories);

        //Fail build here if we generate more categories than github can handle
        if (finalCategories.size() >= 256) {
            throw new RuntimeException("We generated 256 categories or more.  GitHub Actions has a hard limit on number of categories.");
        }

        // Generate the result JSON
        String result = "{ \"include\": [";
        boolean first = true;
        for (String category : finalCategories) {
            if (first)
                first = false;
            else
                result += ", ";
            result += "{ \"category\": \"" + category + "\" } ";
        }

        result += "] }";

        debug("Result is:");
        System.out.println(result);
        debug("Done");

    }

    // ####### HELPER METHODS ##########
    private static String getPRSummary(String[] args) throws FileNotFoundException, IOException {
        debug("Inputs are: " + Arrays.toString(args));
        Path prSummaryPath = null;
        if (args != null && args.length > 0) {
            prSummaryPath = Paths.get(args[0]);
            if (!prSummaryPath.toFile().exists())
                throw new FileNotFoundException(prSummaryPath.toAbsolutePath().toString());
        }
        debug("Received PR summary path: " + prSummaryPath);
        String prSummary = prSummaryPath == null ? ""
                : Files.readAllLines(prSummaryPath).stream().collect(Collectors.joining("\n"));
        debug("PR summary text is:");
        debug("=====================");
        debug(prSummary);
        debug("=====================");

        return prSummary;
    }


    private static List<String> getBuckets(String category) throws IOException {
        Path categoryFile = Paths.get(TEST_CATEGORY_DIR + '/' + category);
        if (!categoryFile.toFile().exists()) {
            throw new FileNotFoundException(categoryFile.toAbsolutePath().toString());
        }

        return Files.readAllLines(categoryFile);
    }

    private static List<String> getModifiedFiles() {
        Path modifiedFilesPath = Paths.get(MODIFIED_FILES_DIFF);
        if (!modifiedFilesPath.toFile().exists()) {
            debug("Did not find any modified files.");
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(modifiedFilesPath).stream()
            .filter(f -> !f.isEmpty())
            .map(f -> {
                debug("Found modified file: " + f);
                return f;
            }).collect(Collectors.toList());
        } catch (IOException ioe) {
            throw new RuntimeException("Tried to access " + modifiedFilesPath + ".  It exists but was inaccessible", ioe);
        }
    }

    private static Set<String> getModifiedProjects() {
        Set<String> modifiedProjects = new HashSet<>();

        for (String modifiedFile : moddedFiles) {
            if (modifiedFile.startsWith(TEST_CATEGORY_DIR))
                continue;
            if (!modifiedFile.startsWith("dev/")) {
                debug("WARN: Found modified file outside of 'dev/' tree: " + modifiedFile);
                modifiedProjects.add("INFRA_OR_UNKNOWN");
            }
            try {
                String projectName = modifiedFile.substring(4);
                projectName = projectName.substring(0, projectName.indexOf('/'));
                modifiedProjects.add(projectName);
            } catch (StringIndexOutOfBoundsException e) {
                debug("Could not parse modifiedFile=" + modifiedFile);
            }
        }

        return modifiedProjects;
    }

    private static boolean isFATOnlyChange() throws IOException {

        if (modifiedProjects.isEmpty()) {
            debug("No modified projects found. Falling back to assuming NOT a FAT-only change.");
            return false;
        }

        debug("Found modified projects: " + modifiedProjects);

        for (String modifiedProject : modifiedProjects) {
            if (!modifiedProject.contains("_fat")) {
                debug("Found modified non-FAT project: " + modifiedProject);
                return false;
            }
        }

        return true;
    }

    private static void debug(Object msg) {
        if (!DEBUG)
            return;
        System.err.println(msg);
    }
}
