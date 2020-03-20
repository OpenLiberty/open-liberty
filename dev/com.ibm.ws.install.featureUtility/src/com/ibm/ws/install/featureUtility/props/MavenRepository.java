package com.ibm.ws.install.featureUtility.props;

public class MavenRepository {
    private String name;
    private String repositoryUrl;
    private String userId;
    private String password;

    public MavenRepository(String name, String repositoryUrl, String userId, String password) {
        // todo throw exception if any of these parameters are null?
        this.name = name;
        this.repositoryUrl = repositoryUrl;
        this.userId = userId;
        this.password = password;
    }

    public String getName(){
        return name;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String toString(){
        return this.repositoryUrl;
    }


}
