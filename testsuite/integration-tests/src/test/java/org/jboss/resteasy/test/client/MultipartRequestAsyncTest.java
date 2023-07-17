package org.jboss.resteasy.test.client;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import junit.framework.Assert;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpAsyncClient4Engine;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.test.client.resource.MultipartRequestResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(Arquillian.class)
@RunAsClient
public class MultipartRequestAsyncTest {

    @ArquillianResource
    private URI uri;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = TestUtil.prepareArchive(MultipartRequestAsyncTest.class.getSimpleName());
        return TestUtil.finishContainerPrepare(war, null, MultipartRequestResource.class);
    }

    @Test
    public void testDoesNotWorkWithAsyncEngine() throws Exception {

        // fill out a query param and execute a get request
        ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
        try (CloseableHttpAsyncClient apacheClient = HttpAsyncClients.createDefault()) {
            apacheClient.start();
            ClientHttpEngine engine = new ApacheHttpAsyncClient4Engine(apacheClient, false);
            builder.httpEngine(engine);
            Client client = builder.build();

            MultipartFormDataOutput multipart = new MultipartFormDataOutput();
            multipart.addFormData("key1", "val1", MediaType.TEXT_PLAIN_TYPE);
            multipart.addFormData("key2", "val2", MediaType.TEXT_PLAIN_TYPE);
            multipart.addFormData("key3", "val3", MediaType.TEXT_PLAIN_TYPE);
            Entity<?> entity = Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA_TYPE);
            WebTarget target = client.target(uri+"/multipart");
            try {
                // execute in background
                org.jboss.resteasy.core.ResteasyContext.getContextData(jakarta.ws.rs.ext.Providers.class);
                final AtomicReference<Throwable> value = new AtomicReference<>();
                final CountDownLatch latch = new CountDownLatch(1);
                target.request().rx() //
                        .post(entity) //
                        .handle((response, error) -> {
                            if (error != null) {
                                error.printStackTrace();
                            }
                            value.set(error);
                            latch.countDown();
                            return null;
                        });

                // await for callback to wake us up
                latch.await(10, TimeUnit.SECONDS);
                Assert.assertNull(value.get());
            }
            finally {
                client.close();
            }
        }
    }

    @Test
    public void testDoesWorkWithSyncEngine() throws Exception {
        // fill out a query param and execute a get request
        ResteasyClientBuilder builder = (ResteasyClientBuilder)ClientBuilder.newBuilder();
        ClientHttpEngine engine = new ApacheHttpClient43Engine();
        builder.httpEngine(engine);
        Client client = builder.build();

        MultipartFormDataOutput multipart = new MultipartFormDataOutput();
        multipart.addFormData("key1", "val1", MediaType.TEXT_PLAIN_TYPE);
        multipart.addFormData("key2", "val2", MediaType.TEXT_PLAIN_TYPE);
        multipart.addFormData("key3", "val3", MediaType.TEXT_PLAIN_TYPE);
        Entity<?> entity = Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA_TYPE);
        WebTarget target = client.target(uri+"/multipart");
        try {
            // execute in background
            org.jboss.resteasy.core.ResteasyContext.getContextData(jakarta.ws.rs.ext.Providers.class);
            final AtomicReference<Throwable> value = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);
            target.request().rx() //
                    .post(entity) //
                    .handle((response, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                        }
                        value.set(error);
                        latch.countDown();
                        return null;
                    });

            // await for callback to wake us up
            latch.await(10, TimeUnit.SECONDS);
            Assert.assertNull(value.get());
        }
        finally {
            client.close();
        }
    }
}
