FROM ibmcom/cloudant-developer:2.0.1

#Contains vulnerable log4j library for features not needed for testing
RUN rm -rf /opt/cloudant/search/
RUN rm -rf /opt/cloudant/etc/log4j.properties
