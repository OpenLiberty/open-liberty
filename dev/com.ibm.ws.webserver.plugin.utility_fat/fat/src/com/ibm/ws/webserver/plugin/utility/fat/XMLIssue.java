package com.ibm.ws.webserver.plugin.utility.fat;

public class XMLIssue {
    String path;
    String problem;

    public XMLIssue(String path, String problem) {
        this.path = path;
        this.problem = problem;
    }

    public String getPath() {return path;}
    public String getProblem() {return problem;}
}
