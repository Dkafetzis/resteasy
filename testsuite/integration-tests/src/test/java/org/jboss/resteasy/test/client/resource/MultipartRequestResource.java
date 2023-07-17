package org.jboss.resteasy.test.client.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/multipart")
public class MultipartRequestResource {

    @POST
    @Produces("application/json")
    @Consumes("multipart/form-data")
    public void testPost() {}
}
