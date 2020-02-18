define([], function(){

  return constants = {
    MAINTENANCE_MODE_IN_MAINTENANCE_MODE: "inMaintenanceMode",
    MAINTENANCE_MODE_NOT_IN_MAINTENANCE_MODE: "notInMaintenanceMode",
    MAINTENANCE_MODE_ALTERNATE_SERVER_UNAVAILABLE: "alternateServerUnavailable",
    MAINTENANCE_MODE_ALTERNATE_SERVER_STARTING: "alternateServerStarting",
    MAINTENANCE_MODE_FAILURE: "error",
    MAINTENANCE_MODE_NOT_FOUND: "notFound",
    MAINTENANCE_MODE_HELP_LINK: "http://www14.software.ibm.com/webapp/wsbroker/redirect?version=phil&product=was-nd-mp&topic=twlp_ui_explore_maintmode",
    MAINTENANCE_MODE_REST_HEADER_ADDITIONAL_SERVERS: "collective.additionalServers",
    MAINTENANCE_MODE_REST_HEADER_ADDITIONAL_HOSTS: "collective.additionalHosts",
    MAINTENANCE_MODE_REST_HEADER_LIST_SEPARATOR: "&",
    
    /* Runtime types */
    RUNTIME_LIBERTY: "Liberty",
    RUNTIME_NODEJS: "Node.js",
    
    /* Container types */
    CONTAINER_DOCKER: "Docker"
  };
});
