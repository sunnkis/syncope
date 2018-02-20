/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.rest.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for notifications.
 */
@Api(tags = "Notifications", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })

@Path("notifications")
public interface NotificationService extends JAXRSService {

    /**
     * Returns notification with matching key.
     *
     * @param key key of notification to be read
     * @return notification with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    NotificationTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of all notifications.
     *
     * @return list of all notifications.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<NotificationTO> list();

    /**
     * Creates a new notification.
     *
     * @param notificationTO Creates a new notification.
     * @return Response object featuring Location header of created notification
     */
    @ApiResponses(
            @ApiResponse(code = 201,
                    message = "Notification successfully created", responseHeaders = {
                @ResponseHeader(name = RESTHeaders.RESOURCE_KEY, response = String.class,
                        description = "UUID generated for the entity created")
                , @ResponseHeader(name = HttpHeaders.LOCATION, response = String.class,
                        description = "URL of the entity created") }))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull NotificationTO notificationTO);

    /**
     * Updates the notification matching the given key.
     *
     * @param notificationTO notification to be stored
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull NotificationTO notificationTO);

    /**
     * Deletes the notification matching the given key.
     *
     * @param key key for notification to be deleted
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Returns details about notification job.
     *
     * @return details about notification job
     */
    @GET
    @Path("job")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    JobTO getJob();

    /**
     * Executes an action on the notification job.
     *
     * @param action action to execute
     */
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @POST
    @Path("job")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void actionJob(@QueryParam("action") JobAction action);
}
