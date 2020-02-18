Building a Default Admin Center Tool
===============================

A default Admin Center tool is a feature and WAB that is delivered in the Liberty runtime,
and doesn't need a user to install it into the runtime.

When creating a new default tool, you should:

1) Copy this project and rename it.
- Update build.xml project name

2) Rename and update the references to ExampleTool with the new tool's name
- bnd.bnd
- resources/WEB-INF/web.xml

3) Update the bnd.bnd file to configure the bundle symbolic name for the tool bundle,
   as well as version, context root, and other UI headers that are needed.

4) Rename and update the publish/features/*.mf. This file needs to be updated with
   the bundle(s) that are created within the new project. You should leave the visibility
   of the feature as private, unless you have a need to change this. Also you need to
   configure the other UI headers that map screenshots, icons, tool descriptions etc. 

5) Update lib/releaseProfile.js as you update JavaScript

6) Update the feature's icons
- publish/features/<featureSymbolicName>/OSGI-INF

7) Add an entry to the ant_build.js/publish/servers/*/apps/ for loose config
- Copy and modify devExampleTool.war.xml to a new location

For now, ignore 'screenshots'
