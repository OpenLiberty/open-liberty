FROM gcavalcante8808/krb5-server

COPY docker-entrypoint.sh /
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/docker-entrypoint.sh"]

# Currently tagged in DockerHub as: kyleaure/krb5-server:1.0
