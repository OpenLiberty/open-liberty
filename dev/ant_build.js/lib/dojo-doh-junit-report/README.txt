DOJO-JUNIT plugin downloaded from github:
https://github.com/manekinekko/dojo-doh-junit-report

Dojo license (http://dojotoolkit.org/license)


How I got this to work:
1. Download source from above

2. I modified the runner-junit to no longer be a runner, but rather a plugin.
   The new plugin file is called 'doh-junit.js' and lives both here as well as
   in dojo_1.9.3/util/doh/plugins

3. Update runner.html with a span:
<span id="xml-report"></span>

4. Invoke tests with the new doh-junit plugin:
https://localhost:9443/devAdminCenter/util/doh/runner.html?test=unittest/all&dohPlugins=util/doh/plugin/doh-junit
