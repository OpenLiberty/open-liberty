import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /*
     *
     * Generate a JSON string in the format of:
     //@formatter:off
     {
       "include": [
         { "category": "foo" },
         { "category": "bar" }
       ]
     }
     //@formatter:on
     */

    static final boolean DEBUG = false;

    static final String TEST_CATEGORY_DIR = ".github/test-categories";
    static final String MODIFIED_FILES_DIFF = ".github/modified_files.diff";

    public static void main(String args[]) throws Exception {

        debug("Starting");

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

        Set<String> modifiedProjects = getModifiedProjects();
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
        if (getModifiedProjects().stream().anyMatch(proj -> proj.contains("_fat"))) {
            debug("At least 1 FAT was modified in this PR.");
            finalCategories.add("MODIFIED_LITE_MODE");
            finalCategories.add("MODIFIED_FULL_MODE");
        }
        // now add mentioned categories in order
        finalCategories.addAll(mentionedCategories.values());
        // finally add all remaining categories
        finalCategories.addAll(unmentionedCategories);

        debug("Final categories are:");
        debug(finalCategories);

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

    private static List<String> getBuckets(String category) throws IOException {
        Path categoryFile = Paths.get(TEST_CATEGORY_DIR + '/' + category);
        if (!categoryFile.toFile().exists()) {
            throw new FileNotFoundException(categoryFile.toAbsolutePath().toString());
        }

        return Files.readAllLines(categoryFile);
    }

    private static List<String> moddedFiles = null;

    private static List<String> getModifiedFiles() throws IOException {
        if (moddedFiles != null)
            return moddedFiles;
        Path modifiedFilesPath = Paths.get(MODIFIED_FILES_DIFF);
        if (!modifiedFilesPath.toFile().exists()) {
            debug("Did not find any modified files.");
            return Collections.emptyList();
        }

        return moddedFiles = Files.readAllLines(modifiedFilesPath).stream()//
                .filter(f -> !f.isEmpty())//
                .map(f -> {
                    debug("Found modified file: " + f);
                    return f;
                }).collect(Collectors.toList());
    }

    private static Set<String> getModifiedProjects() throws IOException {
        Set<String> modifiedProjects = new HashSet<>();

        for (String modifiedFile : getModifiedFiles()) {
            if (modifiedFile.startsWith(TEST_CATEGORY_DIR))
                continue;
            if (!modifiedFile.startsWith("dev/")) {
                debug("WARN: Found modified file outside of 'dev/' tree.");
                modifiedProjects.add("INFRA_OR_UNKNOWN");
            }
            String projectName = modifiedFile.substring(4);
            projectName = projectName.substring(0, projectName.indexOf('/'));
            modifiedProjects.add(projectName);
        }

        return modifiedProjects;
    }

    private static boolean isFATOnlyChange() throws IOException {
        Set<String> modifiedProjects = getModifiedProjects();

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
        System.out.println(msg);
    }

}
