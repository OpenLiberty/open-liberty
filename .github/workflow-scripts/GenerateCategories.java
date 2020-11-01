import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        for (File categoryFile : Paths.get(".github/test-categories").toFile().listFiles()) {
            allCategories.add(categoryFile.getName().toUpperCase());
        }
        debug("All discovered categories are: " + allCategories);

        // Any categories that were mentioned in the PR summary should run first
        TreeMap<Integer, String> menteiondCategories = new TreeMap<>();
        for (String category : allCategories) {
            int categoryIndex = prSummary.indexOf(category);
            if (categoryIndex >= 0) {
                debug("PR summary mentioned category: " + category + " at index " + categoryIndex);
                menteiondCategories.put(categoryIndex, category);
            }
        }
        debug("Explicitly mentioned categories were: " + menteiondCategories.values());

        List<String> finalCategories = new ArrayList<>();
        // special categories that run directly modified buckets
        finalCategories.add("MODIFIED_LITE_MODE");
        finalCategories.add("MODIFIED_FULL_MODE");
        // now add mentioned categories in order
        finalCategories.addAll(menteiondCategories.values());
        // finally add all remaining categories
        SortedSet<String> unmentionedCategories = new TreeSet<>(allCategories);
        unmentionedCategories.removeAll(menteiondCategories.values());
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

    private static void debug(Object msg) {
        if (!DEBUG)
            return;
        System.out.println(msg);
    }

}
