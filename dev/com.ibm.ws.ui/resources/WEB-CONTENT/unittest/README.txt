The UI unit tests are a strange bunch. It's not like any other test project in the Liberty build.

Here are some fundamental things you need to understand, so that you can run and write unit tests for the UI.

1) The UI unit tests use the DOH (Dojo Objective Harness) framework. For an essential primer in understanding DOH and before you do 
anything else, please read "Unit testing Web 2.0 applications using the Dojo Objective Harness" 
(http://www.ibm.com/developerworks/web/library/wa-aj-doh/index.html), an excellent article that explains DOH very well.

2) Since we need to run the unit tests in a web browser, they need to run as a part of the FAT project. The reason behind this is 
due to the Liberty build infrastructure: only certain build machines have Firefox and other browsers installed.

3) The actual JavaScript test files are located in this project, com.ibm.ws.ui_test, and they must end with "Test.js". For 
example, myWebAppTest.js.

4) The product .js files ultimately must be copied over the build.image\wlp\usr\servers\com.ibm.ws.ui.utWebServer\apps\ui_unittest.war 
directory, since DOH requires being run in a web server.

Since there was no product JavaScript code at the time of implementing the example unit test (demo/doh/tests/*), I just placed the 
example product code in the same file structure as the tests. This won't be the case for our real JavaScript files. Eventually, the Ant 
script (build-test.xml) will have to be modified in order to copy these .js files to the appropriate place in the com.ibm.ws.ui_fat.ui 
project, and from there copied to the installed server.

5) For code coverage measuring, the product .js files must also be instrumented with code generated by JSCover. See the instrumentProductCode
Ant target in build-test.xml.

6) To run the unit test (which will also run the FATs), do the following:
 
- In your RTC's Ant view, add this project's build-test.xml file
- Invoke the buildandrun target

You should not see any build failures. However, the FAT JUnit results (autoFVT\results\junit\TEST-com.ibm.ws.ui.fat.FATSuite.xml) is not a real 
marker of the unit tests success or failures. There is a junit.xml file generated under autoFVT, which shows the unit test results and that you 
can read with the JUnit view.

TODO: We will need to somehow incorporate the data from autoFVT\junit.xml to be reflected in the FAT's build results, so that the developer 
can see at a glance her unit test results, instead of having to manually open the junit.xml file.

7) The unit tests also create a autoFVT\cobertura-coverage.xml file. This is the code coverage analysis generated by JSCover. Also TODO is to 
configure the Sonar server to convert this XML into a readable, HTML format that developers can view this in a web brower.
