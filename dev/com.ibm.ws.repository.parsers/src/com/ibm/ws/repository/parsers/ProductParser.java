package com.ibm.ws.repository.parsers;

import java.io.File;
import java.security.InvalidParameterException;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;

public class ProductParser extends ProductRelatedJarParser<ProductResourceWritable> {

//    TODO
//    public ProductParser(RepositoryConnection loginInfoResource) {
//        super(loginInfoResource);
//    }

    @Override
    public ResourceType getType(String contentType, File archive) throws InvalidParameterException {
        ResourceType type = null;
        if ("install".equalsIgnoreCase(contentType)) {
            type = ResourceType.INSTALL;
        } else if ("addon".equalsIgnoreCase(contentType)) {
            type = ResourceType.ADDON;
        } else {
            throw new InvalidParameterException(
                            "The content type of the archive file " + archive + " is "
                                            + contentType
                                            + " but only \"install\" and \"addon\" are valid");
        }
        return type;
    }

}
