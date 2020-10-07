FROM gcavalcante8808/krb5-server

COPY docker-entrypoint.sh /
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/docker-entrypoint.sh"]
