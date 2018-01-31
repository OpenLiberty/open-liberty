package com.ibm.ws.install.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.IfixResource;

// This is a mockup class for temporarily used by the Director.
public class Resolver {

    private Collection<IfixResource> ifixResources;

    public Resolver(RepositoryConnectionList loginInfo) {
        try {
            ifixResources = loginInfo.getAllIfixes();
        } catch (RepositoryException e) {
            ifixResources = new ArrayList<IfixResource>(0);
        }
    }

    public List<IfixResource> resolveFixResources(Collection<String> fixes) {
        List<IfixResource> installAssets = new ArrayList<IfixResource>();
        for (String fix : fixes) {
            for (IfixResource ifixResource : ifixResources) {
                if (ifixResource.getName().equalsIgnoreCase(fix)) {
                    if (!installAssets.contains(ifixResource))
                        installAssets.add(ifixResource);
                }
            }
        }
        return installAssets;
    }

}
