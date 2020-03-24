package com.ibm.ws.install.featureUtility.props;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class FeatureUtilityRepoProperties {

    private final static String FILEPATH_EXT = "/etc/featureUtility.properties";
    private final static Set<String> DEFINED_OPTIONS= new HashSet<>(Arrays.asList("proxyHost", "proxyPort", "proxyUser", "proxyPassword", "featureLocalRepo"));
    private static Map<String, String> definedVariables = new HashMap<>();
    private static List<MavenRepository> repositoryList = new ArrayList<>();
    private static boolean didFileParse;

    static {
        File propertiesFile = new File(Utils.getInstallDir() + FILEPATH_EXT);
        didFileParse = parseProperties(propertiesFile);
    }

    public static List<MavenRepository> getMirrorRepositories(){
        return repositoryList;
    }

    public static String getProxyHost(){
        return definedVariables.get("proxyHost");
    }

    public static String getProxyPort(){
        return definedVariables.get("proxyPort");
    }

    public static String getProxyUser(){
        return definedVariables.get("proxyUser");
    }

    public static String getProxyPassword(){
        return definedVariables.get("proxyPassword"); // char array instead?
    }

    public static String getFeatureLocalRepo(){
        return definedVariables.get("featureLocalRepo");
    }

    public static boolean didLoadProperties(){
        return didFileParse;
    }

    public static File getRepoPropertiesFile(){
        return new File(Utils.getInstallDir() + FILEPATH_EXT);
    }

    public static boolean isUsingDefaultRepo(){
        return getMirrorRepositories().size() == 0;
    }

    private static boolean parseProperties(File propertiesFile) {
        Properties properties = new Properties(){
            private final HashSet<Object> keys = new LinkedHashSet<Object>();
            @Override
            public Enumeration<Object> keys() {
                return Collections.<Object>enumeration(keys);
            }

            @Override
            public Object put(Object key, Object value) {
                keys.add(key);
                return super.put(key, value);
            }
        };
        try(FileInputStream fileIn = new FileInputStream(propertiesFile)) {
            properties.load(fileIn);
        } catch (IOException e) {
            return false;
        }

        definedVariables = new HashMap<>();
        Map<String, Map<String, String>> repoMap = new LinkedHashMap<>();

        // iterate over the properties
        for(Object obj : Collections.list(properties.keys())){
            String key = obj.toString();
            String value = properties.getProperty(key);
            if(DEFINED_OPTIONS.contains(key.toString())){
                definedVariables.putIfAbsent(key.toString(), value.toString()); // only write the first proxy variables we see
            } else {
                String [] split = key.toString().split("\\.");
                if(split.length < 2){ // invalid key
                    continue;
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
        }
        // create the list of maven repositories
        repositoryList = new ArrayList<>();
        repoMap.forEach((key, value) -> {
            repositoryList.add(new MavenRepository(key, value.get("url"), value.get("user"), value.get("password")));
        });

        return true;
    }



}
