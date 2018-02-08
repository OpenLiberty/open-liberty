package com.ibm.ws.repository.parsers;

import java.io.File;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.writeable.ToolResourceWritable;

public class ToolParser extends ProductRelatedJarParser<ToolResourceWritable> {
    @Override
    public ResourceType getType(String contentType, File archive) {
        return ResourceType.TOOL;
    }
}
