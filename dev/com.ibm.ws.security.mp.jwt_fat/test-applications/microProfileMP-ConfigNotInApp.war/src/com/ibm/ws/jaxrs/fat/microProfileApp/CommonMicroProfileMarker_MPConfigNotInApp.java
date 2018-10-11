package com.ibm.ws.jaxrs.fat.microProfileApp;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * A JAX-RS application marked as requiring MP-JWT authentication
 *
 * (There should only be one LoginConfig annotation per module, or processing
 * will be indeterminate.)
 */
@ApplicationPath("rest")
public class CommonMicroProfileMarker_MPConfigNotInApp extends Application {

}
