This is the root project for the Liberty Admin Center.

This project contains both the Java server-side source as well as the hosted
JavaScript code.

===========================
Notes on the URL structure
===========================

URL Naming Scheme
-----------------
The IBM API root for all REST resources is /ibm/api. This Liberty Admin Center
uses the namespace /ibm/api/adminCenter for all of its REST APIs. This namespace
is further sub-divided into versioned API implementations.

Resource Naming Scheme
----------------------
xyzRoot  - The root of the REST resource tree for that particular name space.
           A 'root' resource does not have methods or resources of its own,
           but points to child resources which have those methods.
xyzAPI   - The API of a particular resource type. This follows the general API
           design of using http VERB + resource NOUN to construct a semantic
           URL request
xyzUtils - A collection of utility methods. This does NOT map to the 'resource'
           concept and therefore violates the general REST API design of using
           http VERB + resource NOUN to construct a semantic URL request.

Package Naming Scheme
---------------------
The package name maps directly to the URL scheme (as can be seen below). The
root of the REST implementation is the 'com.ibm.ws.ui.internal.rest' package.

URL to Class Mapping
--------------------
https://localhost:9443/ibm/api/adminCenter/             - com.ibm.ws.ui.internal.rest.APIRoot 
https://localhost:9443/ibm/api/adminCenter/v1/          - com.ibm.ws.ui.internal.rest.v1.V1Root
https://localhost:9443/ibm/api/adminCenter/v1/catalog   - com.ibm.ws.ui.internal.rest.v1.CatalogAPI
https://localhost:9443/ibm/api/adminCenter/v1/toolbox   - com.ibm.ws.ui.internal.rest.v1.ToolboxAPI
https://localhost:9443/ibm/api/adminCenter/v1/utils     - com.ibm.ws.ui.internal.rest.v1.utils.UtilsRoot
https://localhost:9443/ibm/api/adminCenter/v1/utils/url - com.ibm.ws.ui.internal.rest.v1.utils.URLUtils
