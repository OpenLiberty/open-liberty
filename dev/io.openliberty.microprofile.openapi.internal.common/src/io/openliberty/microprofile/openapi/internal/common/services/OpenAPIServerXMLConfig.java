package io.openliberty.microprofile.openapi.internal.common.services;

import java.util.List;
import java.util.Optional;

/**
 * An Object that contains the mpOpenAPI config from server.xml
 */
public interface OpenAPIServerXMLConfig {

    /**
     * Retrieve a list of string representing the included modules.
     * Valid entries are in the format applicationName and/or
     * applicationName/moduleName.
     *
     * @return A list of included modules or an empty optional if there are none.
     */
    Optional<List<String>> getIncludedAppsAndModules();

    /**
     * Retrieve a list of string representing the excluded modules.
     * Valid entries are in the format applicationName and/or
     * applicationName/moduleName.
     *
     * @return A list of excluded modules or an empty optional if there are none.
     */
    Optional<List<String>> getExcludedAppsAndModules();

    /**
     * If the only includes statement in server.xml is "all", "first", or "none" this is a special keyword that changes the mode
     * of OpenApi. This method will retrieve the config mode if it was set, otherwise it returns an empty optional.
     *
     * @return And Optional<String> containing the config mode if one was explicitly set, otherwise an empty optional
     */
    Optional<ConfigMode> getConfigMode();

    /**
     * Was any config for mpOpenAPI found in the server.xml, this is equivalent to calling isPresent on all preceding methods
     * and returning true if any are true.
     *
     * @return true if we found server.xml config, otherwise false;
     */
    boolean wasAnyConfigFound();

    public enum ConfigMode {
        First,
        All,
        None
    }
}
