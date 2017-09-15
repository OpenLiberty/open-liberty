Open Liberty Version @LIBERTY_VERSION@ - (@BUILD_ID@)


I. CONTROLLING OPEN LIBERTY

The bin directory contains a server script to help control the server process.
The script supports the following actions:

 * create    -- creates a new server
 * start     -- launches the server as a background process
 * run       -- launches the server in the foreground
 * debug     -- launches the server in the foreground with JVM debug options
 * stop      -- stops a running server
 * status    -- check to see if a specified server is running.
 * package   -- packages server runtime and target server configuration/application(s) into zip archive file
 * dump      -- dump diagnostic information from the server into an archive
 * javadump  -- dump diagnostic information from the server JVM
 * list      -- list existing servers
 * version   -- displays the version of the server runtime
 * help      -- get command-line/script help, including descriptions of additional options

To use the script on Windows (the .bat extension is optional):
  > bin\server.bat create <serverName>
  > bin\server.bat start <serverName>
  > bin\server.bat help

To use the script on other platforms:
  > bin/server create <serverName>
  > bin/server start <serverName>
  > bin/server help

If a serverName is not specified, "defaultServer" will be used.

Notes:

The server requires Java 8.   To download IBM Java SDKs, visit 
http://www.ibm.com/developerworks/java/jdk/ 

The script will try to find the java executable in your environment by
looking for JAVA_HOME and JRE_HOME before falling back to the java executable
present on the path.

You may need to set one of these variables in your environment; for example,
to set JAVA_HOME:

  Set JAVA_HOME to where you have your JDK installed:
  > export JAVA_HOME=/java/jre

  If JAVA_HOME\bin is not in the PATH, add it to the path:
  > export PATH=$JAVA_HOME/bin:$PATH

By default, the server uses TCP/IP port 9080 for HTTP traffic, and port
9443 for HTTPS traffic. 

II. SERVER CONFIGURATION FILES

The server requires one configuration file: server.xml.
This file describes the configuration of the server, either through
direct assignment of values, or by including other configuration files.


III. INSTALLING AN APPLICATION

No applications are installed by default.

Before installing an application, update the server configuration file,
server.xml, to include any required features and resources. 
For more information, visit http://openliberty.io

* Adding the application as a dropin

If application-specific configuration is not required, you can place the
application (as an archive or a directory) in the monitored dropins directory.
Be sure to maintain the file extension of the archive or directory so that
the server can determine the type of the application. Do not define the
dropin applications in the server configuration.

Adding or removing an application from the dropins folder while the server
is running will be detected and updated by the server.
The server will detect changes to static or dynamically compiled resources
like JSPs as soon as they are made. 

* Adding the application to the server configuration

If application-specific configuration is required, place the application
into the apps directory and define the application in the server
configuration. To remove the application, remove the application from the
server configuration. The server can be configured to monitor changes
in configured applications, and will restart the application for you
as necessary when changes are detected.

For example:

+- wlp
|  +- usr/                          User directory (wlp.user.dir)
|  |  +- servers/
|  |  |  +- <serverName>            Server configuration directory (server.config.dir)
|  |  |  |  +- dropins/             Default application dropins directory
|  |  |  |  |  +- GuestList.war
|  |  |  |  +- apps/                Configured application directory
|  |  |  |  |  +- OrderForm.war



IV. DIRECTORY LOCATIONS AND VARIABLES

By default, the runtime uses the following directory structure:

 Directory/File                    Variable             Description
------------------------------    -------------------  -----------------------
  wlp /                            wlp.install.dir      Root of WebSphere Liberty installation
  +- bin/                                               Liberty scripts
  +- clients/                                           Liberty client and thin client libraries
  +- dev/                                               Root for developer resources (APIs, SPIs, specifications, and tools)
  +- etc/                                               User customizations (optional: settings apply to all servers)
  |  +- server.env                                      Default server script environment variables (optional)
  |  +- jvm.options                                     Default JVM options (optional)
  +- lib/                                               Platform runtime (internal)
  +- templates/                                         Runtime customization templates and examples
  |  +- servers/                                        Server configuration templates
  +- usr/                          wlp.user.dir         User directory
  |  +- shared/
  |  |  +- jvm.options                                  Server JVM options (optional: merged with server specific jvm.options)
  |  |  +- server.env                                   Server script envrionment variables (optional: merged with wlp/etc/server.env)
  |  |  +- apps/                   shared.app.dir       Shared applications
  |  |  +- config/                 shared.config.dir    Shared server configuration files
  |  |  +- resources/              shared.resource.dir  Shared resource definitions: adapters, data sources
  |  +- servers/
  |  |  +- <serverName>         ** server.config.dir    Server configuration directory
  |  |  |                          wlp.server.name      The name of the server is the directory name
  |  |  |  +- bootstrap.properties                      Server bootstrap properties (optional)
  |  |  |  +- jvm.options                               Server JVM options (optional: replaces wlp/etc/jvm.options)
  |  |  |  +- server.env                                Server script environment variables (optional: merged with wlp/usr/shared/server.env)
  |  |  |  +- server.xml                                Server configuration file (required)
  |  |  |  +- apps/                                     Server's configured applications directory
  |  |  |  |  +- <application.type>                     Application directory or archive
  |  |  |  +- dropins                                   Server's default application dropins directory
  |  |  |  |  +- <application.type>                     Application directory or archive
  |  |  +- <serverName>         ** server.output.dir    Server output directory: logs and workarea
  |  |  |  +- logs                                      Server's logs directory (including FFDC logs)
  |  |  |  |  +- state                                  Files that contain information about the runtime state, such as JMX addresses
  |  |  |  +- workarea                                  Server's workarea directory


** Note: The individual server directory appears in the above list twice. Read-only 
configuration data may be stored separately from the read-write space required by a
running server. See Section V.

The variables associated with each directory (where present) can be used to
specify file locations when configuring the server in server.xml. For example, you
can specify a shared application location as "${shared.app.dir}/myShared.war".

Tip: To ensure configuration portability, use the most specific variable that is
appropriate, and avoid relying on the relationship between resources; for
example, the install location, ${wlp.install.dir}, might not be the parent of
the custom configuration directory, ${wlp.user.dir}, in some configurations.
Likewise, the server directory itself might be split in half; use ${server.config.dir}
to reference server-specific configuration (applications), and ${server.output.dir}
to describe artifacts generated by the server (log files).


V. CUSTOMIZING THE SERVER ENVIRONMENT

The server management script uses certain environment variables to support
the placement of product binaries and shared resources in read-only file
systems.

The ${wlp.install.dir} shown in Section IV has an inferred location; the
installation directory is always set to the parent of the directory containing
the launch script.

The WLP_USER_DIR environment variable can be used to specify an alternate
location for ${wlp.user.dir}. If this is specified, the runtime will look for
shared resources and server definitions in the specified directory.
${server.config.dir} is equivalent to ${wlp.user.dir}/servers/serverName.

The WLP_OUTPUT_DIR environment variable can be used to specify an alternate
location for server generated output (logs, the server's workarea, generated
files, etc). If this environment variable is specified, ${server.output.dir}
is set to the equivalent of WLP_OUTPUT_DIR/serverName. If not,
${server.output.dir} is the same as ${server.config.dir}.


1. Specifying environment variables with server.env

Use server.env files at the install and server levels to specify environment
variables such as JAVA_HOME, WLP_USER_DIR, and WLP_OUTPUT_DIR. These files
support only KEY=value pairs. Key values should not contain any white space
characters. All characters in the value are considered to be literal, including
back-slashes and leading and trailing white space. Shell and variable expansion 
are not supported.

The server management script will search for server.env files in a few locations:
${wlp.install.dir}/etc/, ${wlp.user.dir}/shared/ and ${server.config.dir}/. 
All these files are read in the order specified above. If the same property
is specified in multiple files then values in later files will take precedence.
It should be noted that WLP_USER_DIR is special in that it can only be specified
in the etc/server.env since it dictates the locations of the other server.env files.

2. Customizing JVM options

Use jvm.options files at the runtime and server levels to specify additional
server startup options, for example, -X arguments. The options will be applied
when the start, run, and debug actions are invoked through the server management
script. Be sure to specify only one option per line.

The server management script will search for jvm.options files in multiple locations.
It will look for jvm.options in: ${wlp.usr.dir}/shared/, 
${server.config.dir}/configDropins/defaults, ${server.config.dir}/,
${server.config.dir}/configDropins/overrides/ it will read all these files and
then append the options in order. If none of those files exist then the
${wlp.install.dir}/etc/jvm.options will be used.


3. Bootstrap properties

Bootstrap properties affect the configuration and initialization of the
server's OSGi framework and platform core. These properties can be specified
through system properties or in a bootstrap.properties file located in the
server directory.

Note: Variables in bootstrap.properties are not exported as system properties;
however, the variables are available for use in server.xml.