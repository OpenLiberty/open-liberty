package com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import com.ibm.ws.microprofile.openapi.impl.jaxrs2.DefaultParameterExtension;

public class OpenAPIExtensions {
    //private static Logger LOGGER = LoggerFactory.getLogger(OpenAPIExtensions.class);

    private static List<OpenAPIExtension> extensions = null;

    public static List<OpenAPIExtension> getExtensions() {
        return extensions;
    }

    public static void setExtensions(List<OpenAPIExtension> ext) {
        extensions = ext;
    }

    public static Iterator<OpenAPIExtension> chain() {
        return extensions.iterator();
    }

    static {
        extensions = new ArrayList<>();
        ServiceLoader<OpenAPIExtension> loader = ServiceLoader.load(OpenAPIExtension.class);
        for (OpenAPIExtension ext : loader) {
            //LOGGER.debug("adding extension " + ext);
            extensions.add(ext);
        }
        extensions.add(new DefaultParameterExtension());
    }
}