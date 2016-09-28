/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.pox.dom.DomPoxMessageFactory;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import io.spring.guides.gs_producing_web_service.GetCountryRequest;

@Configuration
class ApplicationIntegrationTestConfiguration {
	@Value("${lcm.request.keystorepath}")
	private Resource keyStore;
	@Value("${lcm.request.keystorepass}")
	private char[] keyStorePass;
	@Value("${lcm.request.keystoretype}")
	private String keyStoreType;
	@Value("${lcm.request.truststorepath}")
	private Resource trustStore;
	@Value("${lcm.request.truststorepass}")
	private char[] trustStorePass;
	@Value("${lcm.request.truststoretype}")
	private String trustStoreType;
	@Bean
	public WebServiceMessageSender messageSender(
			LayeredConnectionSocketFactory factory) throws Exception {
		Header header = new BasicHeader(HttpHeaders.ACCEPT, "application/xml");
		List<Header> defaultHeaders = Arrays.asList(header);
		CloseableHttpClient client = HttpClientBuilder.create()
			.setSSLSocketFactory(factory)
			.setDefaultHeaders(defaultHeaders)
			.build();

		HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender(
				client);
		
		// needed if used as a standalone client
		//messageSender.afterPropertiesSet(); 
		return messageSender;
	}
	@Bean
	public LayeredConnectionSocketFactory sslFactory() {
		try {
			final KeyStore keystore = KeyStore.getInstance(this.keyStoreType);
			try (InputStream readStream = this.keyStore.getInputStream()) {
				keystore.load(readStream, this.keyStorePass);
			}

			final KeyStore truststore = KeyStore.getInstance(this.trustStoreType);
			try (InputStream readStream = this.trustStore.getInputStream()) {
				truststore.load(readStream, this.trustStorePass);
			} 

			SSLContext sslContext = SSLContexts
					.custom()
					.loadTrustMaterial(truststore, null)
					.loadKeyMaterial(keystore, this.keyStorePass)
					.build();
			SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
					new DefaultHostnameVerifier()
					);
			return sslConnectionFactory;
		} catch (KeyManagementException | UnrecoverableKeyException |
				NoSuchAlgorithmException | KeyStoreException
				| CertificateException | IOException e) {
			throw new IllegalArgumentException(String.format("Problem with keystore %s or truststore %s",
					this.keyStore, this.trustStore), e);
		}
	}
}
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
	classes = {ApplicationIntegrationTestConfiguration.class,
			WebServiceConfig.class,
			Application.class
	})
public class ApplicationIntegrationTests {

	private Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
	@Autowired
	private WebServiceMessageSender messageSender;

	@LocalServerPort
	private int port = 0;

	@Before
	public void init() throws Exception {
		marshaller.setPackagesToScan(ClassUtils.getPackageName(GetCountryRequest.class));
		marshaller.afterPropertiesSet();
	}

	@Test
	public void testSendAndReceive() {
		WebServiceTemplate ws = new WebServiceTemplate(marshaller);
		ws.setMessageFactory(new DomPoxMessageFactory());
		GetCountryRequest request = new GetCountryRequest();
		request.setName("Spain");

		assertThat(ws.marshalSendAndReceive("http://localhost:"
				+ port + "/ws", request)).isNotNull();
	}
	@Test
	public void testSendAndReceiveWithCertificates() {
		WebServiceTemplate ws = new WebServiceTemplate(marshaller);
		ws.setMessageFactory(new DomPoxMessageFactory());
		ws.setMessageSender(messageSender);
		GetCountryRequest request = new GetCountryRequest();
		request.setName("Spain");

		assertThat(ws.marshalSendAndReceive("http://localhost:"
				+ port + "/ws", request)).isNotNull();
	}
}