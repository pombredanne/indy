/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.vertx.admin;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.bind.vertx.util.RequestUtils;
import org.commonjava.aprox.core.dto.repl.ReplicationDTO;
import org.commonjava.aprox.core.rest.ReplicationController;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.web.json.ser.JsonSerializer;
import org.vertx.java.core.http.HttpServerRequest;

//@Path( "/admin/replicate" )
public class ReplicationResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ReplicationController controller;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Routes( { @Route( path = "/admin/replicate", method = Method.POST, contentType = ApplicationContent.application_json ) } )
    public void replicate( final HttpServerRequest req )
    {
        final ReplicationDTO dto = RequestUtils.fromRequestBody( req, serializer, ReplicationDTO.class );
        try
        {
            final Set<StoreKey> replicated = controller.replicate( dto );

            final Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put( "replicationCount", replicated.size() );
            params.put( "items", replicated );
            formatOkResponseWithEntity( req, serializer.toString( params ) );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Replication failed: %s", e, e.getMessage() );
            formatResponse( e, req.response() );
        }
    }

}