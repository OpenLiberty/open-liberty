# Copyright IBM Corp. 2012, 2013
# The source code for this program is not published or other-
# wise divested of its trade secrets, irrespective of what has
# been deposited with the U.S. Copyright Office.

from java.lang import String
from java.lang import System
from java.util import HashMap

from javax.management import NotificationListener
from javax.management import NotificationFilter
from javax.management.remote import JMXConnector
from javax.management.remote import JMXConnectorFactory
from javax.management.remote import JMXServiceURL

from com.ibm.websphere.jmx.connector.rest import ConnectorSettings

import jarray



class BaseNotificationListener(NotificationListener):
	# The client applications can subclass BaseNotificationListener,
	# and override handleNotification() method. 
	def __init__(self):
		pass

	def handleNotification(self,notification,handback):
		# A user can override this method.
		pass



class BaseNotificationFilter(NotificationFilter):
	# The client applications can subclass BaseNotificationFilter,
	# and override isNotificationEnabled() method. 
	def __init__(self):
		pass

	def isNotificationEnabled(self,notification):
		# A user can override this method.
		return True



class JMXRESTConnector(object):
	connector = None
	mbeanConnection = None
	trustStore = None
	trustStorePassword = None
	trustStoreType = None

	def __init__(self):
		pass

	def connect(self, host, port, *args):
		if len(args)==2:
			self.connectBasic(host, port, args[0], args[1])
		else:
			self.connectAdvanced(host, port, args[0])

	def connectAdvanced(self,host,port,map):
		print "Connecting to the server..."
		System.setProperty("javax.net.ssl.trustStore", self.trustStore)
		System.setProperty("javax.net.ssl.trustStorePassword", self.trustStorePassword)
		System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
		url = JMXServiceURL("REST", host, port, "/IBMJMXConnectorREST")
		self.connector = JMXConnectorFactory.newJMXConnector(url, map)
		self.connector.connect()
		print "Successfully connected to the server " + '"' + host + ':%i"' % port

	def connectBasic(self,host,port,user,password):
		map = HashMap()
		map.put("jmx.remote.provider.pkgs", "com.ibm.ws.jmx.connector.client")
		map.put(JMXConnector.CREDENTIALS, jarray.array([user, password], String))
		map.put(ConnectorSettings.READ_TIMEOUT, 2*60*1000)
		map.put(ConnectorSettings.DISABLE_HOSTNAME_VERIFICATION, True) 
		self.connectAdvanced(host, port, map)

	def disconnect(self):
		if(self.connector==None):
			pass
		else:
			self.connector.close()
			self.connector = None
			self.mbeanConnection = None

	def getMBeanServerConnection(self):
		# This method can be called after the above connect() is executed successfully.
		self.mbeanConnection = self.connector.getMBeanServerConnection()
		return self.mbeanConnection

