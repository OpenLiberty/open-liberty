<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>WS-AT Test Application</title>
</head>
<body>
<h1>Welcome to WS-AT Test Application from <%=request.getLocalAddr() %>:<%=request.getLocalPort() %></h1><h3> Made by Jordan 2015-09-23</h3> <br>
<h2><font color="green">URL Example: </font></h2>
<a href="http://localhost:9080/wsatAppWithoutAssertion/ClientServlet" target="_blank">http://localhost:9080/wsatAppWithoutAssertion/ClientServlet</a>?server1p=commit:http://localhost:9081&server2l=commit:http://localhost:9082
 <br> <br>
|---------------------------------------- README ----------------------------------------| <br>
| server1p=commit:http://localhost:9081<br>
| server2l=rollback:http://localhost:9082<br>
| client=commit<br>
| Server URL example: [server1=commit:http://localhost:9081, won't request if null]<br>
| Server Operation type: [d: DIspatch, l:local wsdl, p or other: client proxy]
| Server Operation example: [commit, rollback, transcommit, cleandb, countdb, listdb or nested-commit-server2-9082 for sayHelloToOther method, NULL for sayHello method] <br>
| Client Operation example: [commit, rollback, exception, setrollbackonly or cleandb, countdb, listdb] <br>
| Without Client Transaction: [withouttrans=true] <br>
|---------------------------------------- ENDEND ----------------------------------------| <br>
<br><br>
<h2><font color="green">Database Setup:</font></h2>
<b><font color="blue">No need to do it by yourself, can be done in ResultServlet?server=server1&method=init</font></b> <br>
Download Derby database from: <a href="http://db.apache.org/derby/derby_downloads.html" target="_blank">Derby Download</a> <br>
Open a command line and start ij.bat. <br>
<b>Client Side:</b><br>
connect 'jdbc:derby:WsatDatabase<font color="red">0</font>;create=true';<br>
create table wsatTable0(<br>
id varchar(20) primary key not null,<br>
value varchar(60)<br>
);<br>
select * from wsatTable0;<br>
<br>
<b>Server1 Side:</b><br>
connect 'jdbc:derby:WsatDatabase<font color="red">1</font>;create=true';<br>
create table wsatTable1(<br>
id varchar(20) primary key not null,<br>
value varchar(60)<br>
);<br>
select * from wsatTable1;<br>
<br>
<b>Server2 Side:</b><br>
connect 'jdbc:derby:WsatDatabase<font color="red">2</font>;create=true';<br>
create table wsatTable2(<br>
id varchar(20) primary key not null,<br>
value varchar(60)<br>
);<br>
select * from wsatTable2;<br>
<br>
<b>ServerX Side:</b><br>
...
<br><br>
<h2><font color="green">Server.xml Example:</font></h2>
&lt;server description=&quot;new server&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;!-- Enable features --&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;featureManager&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;feature&gt;jsp-2.2&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;feature&gt;localConnector-1.0&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;feature&gt;servlet-3.1&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;feature&gt;wsAtomicTransaction-1.2&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    	&lt;feature&gt;jndi-1.0&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;		&lt;feature&gt;jdbc-4.0&lt;/feature&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;	&lt;/featureManager&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;	<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;!-- To access this server from a remote client add a host attribute to the following element, e.g. host=&quot;*&quot; --&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;httpEndpoint httpPort=&quot;9083&quot; httpsPort=&quot;9443&quot; id=&quot;defaultHttpEndpoint host=&quot;*&quot;/&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;applicationMonitor updateTrigger=&quot;mbean&quot;/&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;	<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;webApplication id=&quot;wsatAppWithoutAssertion&quot; location=&quot;wsatAppWithoutAssertion.war&quot; name=&quot;wsatAppWithoutAssertion&quot;/&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;jdbcDriver id=&quot;derbyDriver&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;library name=&quot;derbyLib&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;            &lt;fileset id=&quot;derbyJar&quot; dir=&quot;C:\Jordan\WS-AT\derby\lib&quot;&gt;&lt;/fileset&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;        &lt;/library&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;/jdbcDriver&gt;<br><br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;!-- For client datasource configuration --&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;dataSource id=&quot;derbyConnection1&quot; jndiName=&quot;jdbc/wsatDataSource1&quot; jdbcDriverRef=&quot;derbyDriver&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    	&lt;properties.derby.embedded createDatabase=&quot;create&quot; databaseName=&quot;C:\Jordan\WS-AT\derby\bin\WsatDatabase1&quot;&gt;&lt;/properties.derby.embedded&gt;<br><br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;!-- For server datasource configuration --&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;/dataSource&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;dataSource id=&quot;derbyConnection2&quot; jndiName=&quot;jdbc/wsatDataSource2&quot; jdbcDriverRef=&quot;derbyDriver&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;    	&lt;properties.derby.embedded createDatabase=&quot;create&quot; databaseName=&quot;C:\Jordan\WS-AT\derby\bin\WsatDatabase2&quot;&gt;&lt;/properties.derby.embedded&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;/dataSource&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;    &lt;!-- logging traceSpecification=&quot;*=info: com.ibm.ws.*=all&quot; / --&gt;<br>
&lt;/server&gt;<br>
<br><br>
<h2><font color="green">Useful link:</font></h2>
<b>Database can be initialized by using:</b> <br>
<a href="http://localhost:9081/wsatAppWithoutAssertion/ResultServlet?server=server1&method=init" target"_blank">http://localhost:9081/wsatAppWithoutAssertion/ResultServlet?server=server1&method=init</a><br><br>
<b>Commit all transaction:</b> <br>
<a href="http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1=commit:http://localhost:9081&server2=commit:http://localhost:9082" target"_blank">http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1p=commit:http://localhost:9081&server2p=commit:http://localhost:9082</a><br><br>
<b>Other transaction:</b> <br>
<a href="http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1=rollback:http://localhost:9081&server2=exception:http://localhost:9082" target"_blank">http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1p=rollback:http://localhost:9081&server2p=exception:http://localhost:9082</a><br><br>
<b>Without client transaction:</b> <br>
<a href="http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1=countdb:http://localhost:9081&server2=countdb:http://localhost:9082&withouttrans=true" target"_blank">http://localhost:9080/wsatAppWithoutAssertion/ClientServlet?server1=countdb:http://localhost:9081&server2=countdb:http://localhost:9082&withouttrans=true</a><br><br>
<b>Get db count:</b> <br>
<a href="http://localhost:9081/wsatAppWithoutAssertion/ResultServlet?server=server1" target"_blank">http://localhost:9081/wsatAppWithoutAssertion/ResultServlet?server=server1</a><br>
<br><br>
<h2><font color="green">Other Information:</font></h2>
Please check if enable WS-AT in WSDL, such as:<br>
&nbsp;&nbsp;&nbsp;&nbsp;&lt;wsp:Policy wsu:Id=&quot;TransactedPolicy&quot; &gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;wsat:ATAssertion/&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&lt;/wsp:Policy&gt;<br>
...<br>
&nbsp;&nbsp;&nbsp;&nbsp;&lt;wsdl:binding name=&quot;HelloImplServiceSoapBinding&quot; type=&quot;tns:Hello&quot;&gt;<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;wsp:PolicyReference URI=&quot;#TransactedPolicy&quot; wsdl:required=&quot;true&quot; /&gt;<br>
...<br>
</body>
</html>
