package org.commonjava.aprox.depgraph.rest.render;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.commonjava.tensor.agg.AggregationUtils.collectProjectReferences;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.commonjava.aprox.depgraph.util.RequestAdvisor;
import org.commonjava.maven.atlas.common.ref.ProjectRef;
import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.common.ref.VersionlessArtifactRef;
import org.commonjava.maven.atlas.common.version.CompoundVersionSpec;
import org.commonjava.maven.atlas.common.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.common.version.VersionSpec;
import org.commonjava.maven.atlas.effective.EProjectGraph;
import org.commonjava.maven.atlas.effective.EProjectWeb;
import org.commonjava.maven.atlas.effective.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.effective.rel.DependencyRelationship;
import org.commonjava.maven.atlas.effective.rel.ProjectRelationship;
import org.commonjava.maven.atlas.effective.traverse.FilteringTraversal;
import org.commonjava.maven.atlas.spi.GraphDriverException;
import org.commonjava.tensor.agg.AggregatorConfig;
import org.commonjava.tensor.agg.GraphAggregator;
import org.commonjava.tensor.agg.ProjectRefCollection;
import org.commonjava.tensor.data.CartoDataException;
import org.commonjava.tensor.data.CartoDataManager;
import org.commonjava.tensor.discover.DiscoverySourceManager;
import org.commonjava.tensor.event.TensorEventFunnel;
import org.commonjava.tensor.inject.TensorData;
import org.commonjava.tensor.io.AggregatorConfigUtils;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@Path( "/depgraph/graph/render" )
@RequestScoped
public class GraphRenderingResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CartoDataManager data;

    @Inject
    private GraphAggregator aggregator;

    //    @Inject
    //    private ProjectRelationshipDiscoverer discoverer;

    @Inject
    private TensorEventFunnel funnel;

    @Inject
    @TensorData
    private JsonSerializer serializer;

    @Inject
    private DiscoverySourceManager sourceFactory;

    @Inject
    private RequestAdvisor requestAdvisor;

    @Path( "/bom/{g}/{a}/{v}" )
    @POST
    public Response bomFor( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                            @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        AggregatorConfig config = null;
        try
        {
            config = AggregatorConfigUtils.read( request.getInputStream() );
            final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );

            final EProjectWeb web = data.getProjectWeb( filter, config.getRoots() );

            if ( web == null )
            {
                return response;
            }

            final Map<ProjectRef, ProjectRefCollection> projects = collectProjectReferences( web );

            final Model model = new Model();
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );
            model.setPackaging( "pom" );
            model.setName( artifactId + ":: Bill of Materials" );
            model.setDescription( "Generated by Tensor Dependency Grapher at " + new Date() );

            final DependencyManagement dm = new DependencyManagement();
            model.setDependencyManagement( dm );

            for ( final Map.Entry<ProjectRef, ProjectRefCollection> entry : projects.entrySet() )
            {
                final ProjectRef r = entry.getKey();
                final ProjectRefCollection prc = entry.getValue();

                final VersionSpec spec = generateVersionSpec( prc.getVersionRefs() );
                final Set<VersionlessArtifactRef> arts = prc.getVersionlessArtifactRefs();
                if ( arts == null )
                {
                    continue;
                }

                for ( final VersionlessArtifactRef artifact : arts )
                {
                    final Dependency d = new Dependency();

                    d.setGroupId( r.getGroupId() );
                    d.setArtifactId( r.getArtifactId() );
                    d.setVersion( spec.renderStandard() );
                    if ( !"jar".equals( artifact.getType() ) )
                    {
                        d.setType( artifact.getType() );
                    }

                    if ( artifact.getClassifier() != null )
                    {
                        d.setClassifier( artifact.getClassifier() );
                    }

                    dm.addDependency( d );
                }
            }

            final StringWriter writer = new StringWriter();
            new MavenXpp3Writer().write( writer, model );

            final String out = writer.toString();
            response = Response.ok( out )
                               .build();

        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read list of GAVs from POST body: %s", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to retrieve web for: %s. Reason: %s", e, config, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    private VersionSpec generateVersionSpec( final Set<ProjectVersionRef> refs )
    {
        final List<VersionSpec> versions = new ArrayList<VersionSpec>();
        for ( final ProjectVersionRef ref : refs )
        {
            final VersionSpec spec = ref.getVersionSpec();
            versions.add( spec );
        }

        Collections.sort( versions );

        if ( versions.size() == 1 )
        {
            return versions.get( 0 );
        }

        return new CompoundVersionSpec( null, versions );
    }

    @Path( "/dotfile/{g}/{a}/{v}" )
    @Produces( "text/x-graphviz" )
    @GET
    public Response dotfile( @PathParam( "g" ) final String groupId, @PathParam( "a" ) final String artifactId,
                             @PathParam( "v" ) final String version, @Context final HttpServletRequest request )
    {
        Response response = Response.status( NO_CONTENT )
                                    .build();

        //        final DiscoveryConfig discovery = createDiscoveryConfig( request, null, sourceFactory );
        final ProjectRelationshipFilter filter = requestAdvisor.createRelationshipFilter( request );
        try
        {
            final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
            //            ref = discoverer.resolveSpecificVersion( ref, discovery );

            final EProjectGraph graph = data.getProjectGraph( ref );

            if ( graph != null )
            {
                final FilteringTraversal t = new FilteringTraversal( filter, true );
                graph.traverse( t );

                final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>( t.getCapturedProjects( true ) );
                final List<ProjectRelationship<?>> rels = t.getCapturedRelationships();

                final Map<ProjectVersionRef, String> aliases = new HashMap<ProjectVersionRef, String>();

                final StringBuilder sb = new StringBuilder();
                sb.append( "digraph " )
                  .append( cleanDotName( groupId ) )
                  .append( '_' )
                  .append( cleanDotName( artifactId ) )
                  .append( '_' )
                  .append( cleanDotName( version ) )
                  .append( " {" );

                sb.append( "\nsize=\"300,20\"; resolution=72;\n" );

                for ( final ProjectVersionRef r : refs )
                {
                    final String aliasBase = cleanDotName( r.toString() );

                    String alias = aliasBase;
                    int idx = 2;
                    while ( aliases.containsValue( alias ) )
                    {
                        alias = aliasBase + idx++;
                    }

                    aliases.put( r, alias );

                    sb.append( "\n" )
                      .append( alias )
                      .append( " [label=\"" )
                      .append( r )
                      .append( "\"];" );
                }

                sb.append( "\n" );

                for ( final ProjectRelationship<?> rel : rels )
                {
                    final String da = aliases.get( rel.getDeclaring() );
                    final String ta = aliases.get( rel.getTarget()
                                                      .asProjectVersionRef() );

                    sb.append( "\n" )
                      .append( da )
                      .append( " -> " )
                      .append( ta );

                    appendRelationshipInfo( rel, sb );
                    sb.append( ";" );
                }

                sb.append( "\n\n}\n" );

                response = Response.ok( sb.toString() )
                                   .build();
            }
            else
            {
                logger.error( "Cannot find graph: %s:%s:%s", groupId, artifactId, version );
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }

        }
        catch ( final CartoDataException e )
        {
            logger.error( "Failed to lookup project graph for: %s:%s:%s. Reason: %s", e, groupId, artifactId, version,
                          e.getMessage() );

            response = Response.serverError()
                               .build();
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( "Invalid version in request: '%s'. Reason: %s", e, version, e.getMessage() );
            response = Response.status( BAD_REQUEST )
                               .entity( "Invalid version: '" + version + "'" )
                               .build();
        }
        catch ( final GraphDriverException e )
        {
            logger.error( "Failed to generate dotfile for project graph: %s:%s:%s. Reason: %s", e, groupId, artifactId,
                          version, e.getMessage() );

            response = Response.serverError()
                               .build();
        }

        return response;
    }

    private String cleanDotName( final String src )
    {
        return src.replace( ':', '_' )
                  .replace( '.', '_' )
                  .replace( '-', '_' );
    }

    @SuppressWarnings( "incomplete-switch" )
    private void appendRelationshipInfo( final ProjectRelationship<?> rel, final StringBuilder sb )
    {
        sb.append( " [type=\"" )
          .append( rel.getType()
                      .name() )
          .append( "\"" );
        switch ( rel.getType() )
        {
            case DEPENDENCY:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                sb.append( " scope=\"" )
                  .append( ( (DependencyRelationship) rel ).getScope()
                                                           .realName() )
                  .append( "\"" );
                break;
            }
            case PLUGIN:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                break;
            }
            case PLUGIN_DEP:
            {
                sb.append( " managed=\"" )
                  .append( ( (DependencyRelationship) rel ).isManaged() )
                  .append( "\"" );
                break;
            }
        }
        sb.append( "]" );
    }

}