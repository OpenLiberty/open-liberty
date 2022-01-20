FROM ibmcom/db2:11.5.7.0

#Contains vulnerable log4j library for features not needed for general testing
RUN rm -rf /opt/ibm/db2/V11.5/federation
RUN rm -rf /opt/ibm/db2/V11.5/db2tss
# Currently tagged in DockerHub as: kyleaure/db2:1.0
