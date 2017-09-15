This is a sample source project

You'll need to edit: 
* build.xml -- update the project name
* bnd.bnd   -- correctly describe your bundle (symbolic name, bVersion, services, exports)
* OSGI-INF: metatype.xml and translated metatype.properties
* delete extraneous stuff like this readme

ALL EXPORTED PACKAGES MUST BE VERSIONED: 
  version your packages by adding an @version javadoc tag to a package-info.java 
  file in the exported package, e.g. in src: com.ibm.ws.example/package-info.java

  /**
   * @version 1.0
   */
  package com.ibm.ws.example;

For more information on creating a new project: 
  http://was.pok.ibm.com/xwiki/bin/view/Liberty/Development#newProject