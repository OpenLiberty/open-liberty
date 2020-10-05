/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.infra.depchain;

import java.io.File;
import java.net.URL;

public class MainArgs {

    private String url;
    private String wlpDir;
    private String repoRoot;
    private String depsFile;
    private boolean isLocalOnly = false;
    private boolean isDebug = false;
    private String outputFile;
    private String accessToken;
    private String gitDiff;
    private String fatName;

    public MainArgs(String args[]) {
        if (args.length == 0) {
            helpAndExplode("More than 0 arguments required.");
        }

        for (int i = 0; i < args.length; i++) {
            if ("--url".equals(args[i]) && (i + 1 < args.length)) {
                url = args[++i];
            } else if ("--wlp".equals(args[i]) && (i + 1) < args.length) {
                wlpDir = args[++i];
            } else if ("--deps".equals(args[i]) && (i + 1) < args.length) {
                depsFile = args[++i];
            } else if ("--debug".equals(args[i])) {
                isDebug = true;
            } else if ("--local".equals(args[i])) {
                isLocalOnly = true;
            } else if ("--output".equals(args[i]) && (i + 1) < args.length) {
                outputFile = args[++i];
            } else if ("--token".equals(args[i]) && (i + 1) < args.length) {
                accessToken = args[++i];
            } else if ("--repo-root".equals(args[i]) && (i + 1) < args.length) {
                repoRoot = args[++i];
            } else if ("--git-diff".equals(args[i]) && (i + 1) < args.length) {
                gitDiff = args[++i];
            } else if ("--fat-name".equals(args[i]) && (i + 1) < args.length) {
                fatName = args[++i];
            } else {
                helpAndExplode("Unknown argument: " + args[i]);
            }
        }

        // Set default values
        if (wlpDir == null) {
            String home = System.getProperty("user.dir").replace("\\", "/");
            if (home.contains("/WS-CD-Open/dev/")) {
                wlpDir = home.substring(0, home.indexOf("/WS-CD-Open/dev/")) + "/WS-CD-Open/dev/build.image/wlp";
            }
        }

        // Validate arguments
        if ((url == null && !isLocalOnly) && gitDiff == null)
            helpAndExplode("Must specify a pull request URL via --url <url> or a git diff using --git-diff <diffSpec>");
        if (url != null && gitDiff != null)
            helpAndExplode("Cannot specify --url and --git-diff. Specify one or the other.");
        if (url != null && !isLocalOnly) {
            try {
                new URL(url);
            } catch (Exception e) {
                helpAndExplode("Specified pull request URL was not a valid URL: " + e.getMessage());
            }
            helpAndExplode("Must specify an access token to authenticate with --token <github token>");
        }

        if (wlpDir == null)
            helpAndExplode("Must specify a Liberty install directory with --wlp <path>");
        if (!new File(wlpDir).exists())
            helpAndExplode("Specified a nonexistant Liberty install directory: " + wlpDir);
        if (!new File(wlpDir + "/bin/server").exists())
            helpAndExplode("Specified Liberty install directory did not seem to be a valid Liberty install: " + wlpDir);

        if (depsFile == null)
            helpAndExplode("Must specify a feature dependencies JSON file with --deps <path>");
        if (!new File(depsFile).exists())
            helpAndExplode("Specified a nonexistant feature dependencies JSON file: " + depsFile);

        // If OL/CL root is specified, make sure it exists
        if (repoRoot != null && !new File(repoRoot).exists())
            helpAndExplode("Did not find specified repository root dir at: " + repoRoot);
    }

    public boolean isLocalOnly() {
        return isLocalOnly;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public String getUrl() {
        return url;
    }

    public String wlpDir() {
        return wlpDir;
    }

    public String getRepoRoot() {
        return repoRoot;
    }

    public String getDepsFile() {
        return depsFile;
    }

    public String getOutputFile() {
        if (outputFile == null)
            return System.getProperty("java.io.tmpdir") + "tmpOutput";

        return outputFile;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getGitDiff() {
        return gitDiff;
    }

    public String getFatName() {
        return fatName;
    }

    private void helpAndExplode(String msg) {
        String helpText = "\n" +
                          "Usage: java -jar changeDetector.jar --url <pr url> --deps <path> [--token <github token> | --git-diff <sha1..sha2> ] " +
                          "[--wlp <path>] [--output <path>]\n\n" +
                          "where options include:\n" +
                          "  --url      GitHub or GitHubEnterprise pull request URL\n" +
                          "  --wlp      Path to a Liberty install directory\n" +
                          "  --deps     Path to a FAT feature dependencies JSON file\n" +
                          "  --token    GitHub or GitHubEnterprise access token\n" +
                          "  --git-diff A 'git diff' spec in the format sha1..sha2\n" +
                          "  --output   Path to output file\n";
        System.out.println(helpText);
        throw new IllegalArgumentException(msg);
    }

}
