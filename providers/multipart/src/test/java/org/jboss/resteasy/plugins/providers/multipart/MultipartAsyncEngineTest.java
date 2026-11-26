/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.plugins.providers.multipart;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that a multipart entity can be written when the client uses an
 * {@link org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine}.
 * <p>
 * See <a href="https://issues.redhat.com/browse/RESTEASY-3126">RESTEASY-3126</a>.
 * </p>
 *
 * @author <a href="mailto:dkafetzi@redhat.com">Dimitris Kafetzis</a>
 */
@RestBootstrap(MultipartAsyncEngineTest.TestResource.class)
public class MultipartAsyncEngineTest {

    private static MultipartFormDataOutput multipart() {
        final MultipartFormDataOutput multipart = new MultipartFormDataOutput();
        multipart.addFormData("key1", "val1", MediaType.TEXT_PLAIN_TYPE);
        multipart.addFormData("key2", "val2", MediaType.TEXT_PLAIN_TYPE);
        multipart.addFormData("key3", "val3", MediaType.TEXT_PLAIN_TYPE);
        return multipart;
    }

    @Test
    public void rxPostWithAsyncEngine(@RestResource @RequestPath("test/echo") final WebTarget injected) throws Exception {
        try (var client = asyncClient()) {
            final WebTarget target = client.target(injected.getUri());
            try (Response response = target.request()
                    .rx()
                    .post(Entity.entity(multipart(), MediaType.MULTIPART_FORM_DATA_TYPE))
                    .toCompletableFuture()
                    .get(10L, TimeUnit.SECONDS)) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo());
                Assertions.assertEquals("key1=val1,key2=val2,key3=val3", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void submitWithAsyncEngine(@RestResource @RequestPath("test/echo") final WebTarget injected) throws Exception {
        try (var client = asyncClient()) {
            final WebTarget target = client.target(injected.getUri());
            try (Response response = target.request()
                    .async()
                    .post(Entity.entity(multipart(), MediaType.MULTIPART_FORM_DATA_TYPE))
                    .get(10L, TimeUnit.SECONDS)) {
                Assertions.assertEquals(Response.Status.OK, response.getStatusInfo());
                Assertions.assertEquals("key1=val1,key2=val2,key3=val3", response.readEntity(String.class));
            }
        }
    }

    /**
     * Builds a client backed by whichever {@link AsyncClientHttpEngine} is the default on the class path, rather
     * than naming a concrete engine, so the test keeps exercising the async path as engine implementations change.
     */
    private static Client asyncClient() {
        final ResteasyClientBuilder builder = ((ResteasyClientBuilder) ResteasyClientBuilder.newBuilder())
                .useAsyncHttpEngine();
        Assertions.assertInstanceOf(AsyncClientHttpEngine.class, builder.getHttpEngine(),
                "This test is only meaningful with an AsyncClientHttpEngine");
        return builder.build();
    }

    @Path("/test")
    public static class TestResource {

        @POST
        @Path("/echo")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(final MultipartFormDataInput input) throws Exception {
            final StringBuilder result = new StringBuilder();
            for (Map.Entry<String, java.util.List<InputPart>> entry : input.getFormDataMap().entrySet()) {
                if (result.length() > 0) {
                    result.append(',');
                }
                result.append(entry.getKey())
                        .append('=')
                        .append(entry.getValue().get(0).getBodyAsString());
            }
            return result.toString();
        }
    }
}
