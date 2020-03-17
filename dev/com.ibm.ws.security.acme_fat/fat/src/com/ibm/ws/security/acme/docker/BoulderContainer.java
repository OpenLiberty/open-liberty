package com.ibm.ws.security.acme.docker;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetwork.Ipam;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

public class BoulderContainer extends CAContainer {

	public static final int HTTP_PORT = 5002;

	/** The port used to listen for incoming ACME requests. */
	public static final int LISTEN_PORT = 4001;

	/** The REST management API port. */
	public static final int MANAGEMENT_PORT = 8055;
    
    public final Network bluenet = Network.builder()
            .createNetworkCmdModifier(cmd -> {
                cmd.withDriver("bridge");
                cmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam()
                        .withDriver("default")
                        .withConfig(new com.github.dockerjava.api.model.Network.Ipam.Config()
                                .withSubnet("10.77.77.0/24")));
            })
            .build();

    public final Network rednet =  Network.builder()
            .createNetworkCmdModifier(cmd -> {
                cmd.withDriver("bridge");
                cmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam()
                        .withDriver("default")
                        .withConfig(new com.github.dockerjava.api.model.Network.Ipam.Config()
                                .withSubnet("10.88.88.0/24")));
            })
            .build();
    
    public final GenericContainer<?> bhsm = new GenericContainer<>(IMAGE)
            .withEnv("PKCS11_DAEMON_SOCKET", "tcp://0.0.0.0:5657")
            .withExposedPorts(5657)
            .withNetwork(bluenet)
            .withNetworkAliases("boulder-hsm")
            .withCommand("/usr/local/bin/pkcs11-daemon /usr/lib/softhsm/libsofthsm2.so")
            .withLogConsumer(o -> System.out.print("[HSM] " + o.getUtf8String()));
    
    public final GenericContainer<?> bmysql = new GenericContainer<>("mariadb:10.3")
            .withNetwork(bluenet)
            .withExposedPorts(3306)
            .withNetworkAliases("boulder-mysql")
            .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
            .withCommand("mysqld --bind-address=0.0.0.0 --slow-query-log --log-output=TABLE --log-queries-not-using-indexes=ON")
            .withLogConsumer(o -> System.out.print("[SQL] " + o.getUtf8String()));
    
    private static final String IMAGE = "ryanesch/acme-boulder:latest";
    
    public BoulderContainer() {
        super(IMAGE);
    }
    
    @Override
    protected void configure() {
            this.withEnv("FAKE_DNS", "172.17.0.68")
            .withEnv("PKCS11_PROXY_SOCKET", "tcp://boulder-hsm:5657")
            .withEnv("BOULDER_CONFIG_DIR", "test/config")
            .withEnv("GO111MODULE", "on")
            .withEnv("GOFLAGS", "-mod=vendor")
            .withEnv("PYTHONIOENCODING", "utf-8")
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withDns("10.77.77.77");
            })
            
            .withWorkingDirectory("/go/src/github.com/letsencrypt/boulder")
            .withCommand("/go/src/github.com/letsencrypt/boulder/test/entrypoint.sh")
            .withNetworkAliases("boulder")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("boulder"))
    		.withExposedPorts(MANAGEMENT_PORT, LISTEN_PORT)
            .withLogConsumer(o -> System.out.print("[BOL] " + o.getUtf8String()))
            .withStartupTimeout(Duration.ofMinutes(3));
    }
    									
    							
    									

    @Override
    protected void containerIsCreated(String containerId) {
        getDockerClient().connectToNetworkCmd()
                .withNetworkId(bluenet.getId())
                .withContainerId(containerId)
                .withContainerNetwork(new ContainerNetwork()
                        .withIpv4Address("10.77.77.77")
                        .withNetworkID(bluenet.getId())
                        .withIpamConfig(new Ipam().withIpv4Address("10.77.77.77"))
                        .withAliases("sa1.boulder", "ca1.boulder", "ra1.boulder", "va1.boulder", "publisher1.boulder", 
                              "ocsp-updater.boulder", "admin-revoker.boulder", "nonce1.boulder"))
                .exec();
        
        getDockerClient().connectToNetworkCmd()
                .withNetworkId(rednet.getId())
                .withContainerId(containerId)
                .withContainerNetwork(new ContainerNetwork()
                        .withIpv4Address("10.88.88.88")
                        .withNetworkID(rednet.getId())
                        .withIpamConfig(new Ipam().withIpv4Address("10.88.88.88"))
                        .withAliases("sa2.boulder", "ca2.boulder", "ra2.boulder", "va2.boulder", 
                                "publisher2.boulder", "nonce2.boulder"))
                .exec();
    }
    
    @Override
    public void start() {
        bmysql.start();
        bhsm.start();
        super.start();
    }
    
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaIntermediateCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT)
				+ "/intermediates/0";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(BoulderContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(BoulderContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}

	public byte[] getAcmeCaRootCertificate() throws Exception {
		final String METHOD_NAME = "getAcmeCaRootCertificate()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT) + "/roots/0";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(BoulderContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				byte[] result = EntityUtils.toByteArray(response.getEntity());

				Log.info(BoulderContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}
	
	/**
	 * Get the status of the certificate from the ACME CA server.
	 * 
	 * @param certificate
	 *            The certificate to check.
	 * @return The status of the certificate.
	 * @throws Exception
	 */
	public String getAcmeCertificateStatus(X509Certificate certificate) throws Exception {
		final String METHOD_NAME = "getAcmeCertificateStatus()";
		String url = "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MANAGEMENT_PORT)
				+ "/cert-status-by-serial/" + certificate.getSerialNumber().toString(16);

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {
			/*
			 * Create a GET request to the ACME CA server.
			 */
			HttpGet httpGet = new HttpGet(url);

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(BoulderContainer.class, METHOD_NAME, httpGet, response);

				StatusLine statusLine = response.getStatusLine();

				if (statusLine.getStatusCode() != 200) {
					throw new IOException(
							METHOD_NAME + ": Expected response 200, but received response: " + statusLine);
				}

				String result = EntityUtils.toString(response.getEntity());

				/*
				 * The result is in JSON, lets just parse out the status.
				 */
				Pattern p = Pattern.compile(".*\"Status\": \"(\\w+)\",.*", Pattern.DOTALL);
				Matcher m = p.matcher(result);
				if (m.find()) {
					result = m.group(1);
				} else {
					throw new Exception(
							"Certificate status response was not in expected JSON format. Response: " + result);
				}

				Log.info(BoulderContainer.class, METHOD_NAME, new String(result));
				return result;
			}
		}
	}
	@Override
	public String getAcmeDirectoryURI() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(LISTEN_PORT) + "/directory";
	}
}
