/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ClassUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.pox.dom.DomPoxMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import io.spring.guides.gs_producing_web_service.GetCountryRequest;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationIntegrationTests {

    private Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    

    @LocalServerPort
    private int port = 0;

    @Before
    public void init() throws Exception {
        marshaller.setPackagesToScan(ClassUtils.getPackageName(GetCountryRequest.class));
        marshaller.afterPropertiesSet();
    }

    @Test
    public void testSendAndReceive() throws Exception {
        Credentials credentials = new UsernamePasswordCredentials("user", "pass");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);
        CloseableHttpClient client = HttpClientBuilder.create().
        	setDefaultCredentialsProvider(credentialsProvider).build();

        HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender(
        		client);
        messageSender.setCredentials(credentials);
        messageSender.afterPropertiesSet();

        WebServiceTemplate ws = new WebServiceTemplate(marshaller);
        ws.setMessageSender(messageSender);
        ws.setMessageFactory(new DomPoxMessageFactory());
        GetCountryRequest request = new GetCountryRequest();
        request.setName("Spain");

        assertThat(ws.marshalSendAndReceive("http://localhost:"
                + port + "/ws", request)).isNotNull();
    }
}