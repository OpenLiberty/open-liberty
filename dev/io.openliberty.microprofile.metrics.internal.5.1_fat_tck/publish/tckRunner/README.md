# mpMetricsTckRunner

To run MicroProfile Metrics TCK:
1. Modify `tck/src/test/resoures/arquillian.xml` line 19 property `wlpHome` to point to your local Liberty image
2. Run `mvn test -Dtest.url=https://localhost:9443 -Dtest.user=<userName> -Dtest.pwd=<userPassword>`
