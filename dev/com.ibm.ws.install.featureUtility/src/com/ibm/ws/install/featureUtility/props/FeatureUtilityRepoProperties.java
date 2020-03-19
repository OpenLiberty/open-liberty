package com.ibm.ws.install.featureUtility.props;

import com.ibm.ws.kernel.boot.cmdline.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class FeatureUtilityRepoProperties {

    private final static String FILEPATH_EXT = "/etc/featureUtility.repo.properties";
    private final static Set<String> DEFINED_OPTIONS= new HashSet<>(Arrays.asList("proxyHost", "proxyPort", "proxyUser", "proxyPassword", "featureLocalRepo"));
    private static Map<String, String> definedVariables;
    private static List<MavenRepository> repositoryList;

    static {
        File propertiesFile = new File(Utils.getInstallDir() + FILEPATH_EXT);
        try {
            parseProperties(propertiesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<MavenRepository> getMirrorRepositories(){
        return repositoryList;
    }

    public String getProxyHost(){
        return definedVariables.get("proxyHost");
    }

    public String getProxyPort(){
        return definedVariables.get("proxyPort");
    }

    public String getProxyUser(){
        return definedVariables.get("proxyUser");

    }

    public String getProxyPassword(){
        return definedVariables.get("proxyPassword");
    }

    public String getFeatureLocalRepo(){
        return definedVariables.get("featureLocalRepo");
    }

    private static void parseProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(propertiesFile));

        definedVariables = new HashMap<>();
        Map<String, Map<String, String>> repoMap = new LinkedHashMap<>();

        // iterate over the properties
        properties.forEach((key, value) -> {
            if(DEFINED_OPTIONS.contains(key.toString())){
                definedVariables.putIfAbsent(key.toString(), value.toString()); // only write the first proxy variables we see
            } else {
                String [] split = key.toString().split("\\.");
                if(split.length == 0){ // invalid key
                    return;
                }

                String repoName = split[0];
                String option = split[split.length - 1]; // incase there are periods in the key, cant use [1]
                if(repoMap.containsKey(repoName)){
                    repoMap.get(repoName).put(option, value.toString());
                } else {
                    HashMap<String, String> individualMap = new HashMap<>();
                    individualMap.put(option, value.toString());
                    repoMap.put(repoName, individualMap);
                }
            }
        });
        // create the list of maven repositories
        repositoryList = new ArrayList<>();
        repoMap.forEach((key, value) -> {
            repositoryList.add(new MavenRepository(key, value.get("url"), value.get("user"), value.get("password")));
        });
    }


}
