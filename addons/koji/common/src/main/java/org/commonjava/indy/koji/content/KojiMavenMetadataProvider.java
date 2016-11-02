package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiArchiveInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.koji.conf.IndyKojiConfig;
import org.commonjava.indy.koji.inject.KojiMavenVersionMetadataCache;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.group.MavenMetadataProvider;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.StoreType.group;

/**
 * Created by jdcasey on 11/1/16.
 */
@ApplicationScoped
public class KojiMavenMetadataProvider
        implements MavenMetadataProvider
{

    private static final java.lang.String LAST_UPDATED_FORMAT = "yyyyMMddHHmmss";

    @Inject
    @KojiMavenVersionMetadataCache
    private CacheHandle<ProjectRef, Metadata> versionMetadata;

    @Inject
    private KojiClient kojiClient;

    @Inject
    private IndyKojiConfig kojiConfig;

    private Map<ProjectRef, ReentrantLock> versionMetadataLocks = new HashMap<>();

    public Metadata getMetadata( StoreKey targetKey, String path )
            throws IndyWorkflowException
    {
        if ( group != targetKey.getType() )
        {
            return null;
        }

        if ( !kojiConfig.isEnabled() )
        {
            return null;
        }

        if ( !kojiConfig.isEnabledFor( targetKey.getName() ) )
        {
            return null;
        }

        File mdFile = new File( path );
        File artifactDir = mdFile.getParentFile();
        File groupDir = artifactDir == null ? null : artifactDir.getParentFile();

        if ( artifactDir == null || groupDir == null )
        {
            return null;
        }

        String groupId = groupDir.getPath().replace( File.separatorChar, '.' );
        String artifactId = artifactDir.getName();

        ProjectRef ga = null;
        try
        {
            ga = new SimpleProjectRef( groupId, artifactId );
        }
        catch ( InvalidRefException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.debug( "Not a valid Maven GA: {}:{}. Skipping Koji metadata retrieval.", groupId, artifactId );
        }

        if ( ga == null )
        {
            return null;
        }

        ReentrantLock lock;
        synchronized ( versionMetadataLocks )
        {
            lock = versionMetadataLocks.get( ga );
            if ( lock == null )
            {
                lock = new ReentrantLock();
                versionMetadataLocks.put( ga, lock );
            }
        }

        try
        {
            boolean locked = lock.tryLock( kojiConfig.getLockTimeoutSeconds(), TimeUnit.SECONDS );
            if ( !locked )
            {
                throw new IndyWorkflowException(
                        "Failed to acquire Koji GA version metadata lock on: %s in %d seconds.", ga,
                        kojiConfig.getLockTimeoutSeconds() );
            }

            Metadata metadata = versionMetadata.get( ga );
            ProjectRef ref = ga;
            if ( metadata == null )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );

                try
                {
                    metadata = kojiClient.withKojiSession( ( session ) -> {

                        List<KojiArchiveInfo> archives = kojiClient.listArchivesMatching( ref, session );

                        List<SingleVersion> versions = new ArrayList<>();
                        for ( KojiArchiveInfo archive : archives )
                        {
                            if ( !archive.getFilename().endsWith( ".pom" ) )
                            {
                                continue;
                            }

                            logger.debug( "Checking for builds/tags of: {}", archive );
                            List<KojiTagInfo> tags = kojiClient.listTags( archive.getBuildId(), session );

                            for ( KojiTagInfo tag : tags )
                            {
                                if ( kojiConfig.isTagAllowed( tag.getName() ) )
                                {
                                    try
                                    {
                                        versions.add( new SingleVersion( archive.getVersion() ) );
                                    }
                                    catch ( InvalidVersionSpecificationException e )
                                    {
                                        logger.warn( String.format(
                                                "Encountered invalid version: %s for archive: %s. Reason: %s",
                                                archive.getVersion(), archive.getArchiveId(), e.getMessage() ), e );
                                    }
                                }
                            }
                        }

                        if ( versions.isEmpty() )
                        {
                            return null;
                        }

                        Collections.sort( versions );

                        Metadata md = new Metadata();
                        md.setGroupId( ref.getGroupId() );
                        md.setArtifactId( ref.getArtifactId() );

                        Versioning versioning = new Versioning();
                        versioning.setRelease( versions.get( versions.size() - 1 ).renderStandard() );
                        versioning.setLatest( versions.get( versions.size() - 1 ).renderStandard() );
                        versioning.setVersions(
                                versions.stream().map( ( v ) -> v.renderStandard() ).collect( Collectors.toList() ) );

                        Date lastUpdated = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ).getTime();
                        versioning.setLastUpdated( new SimpleDateFormat( LAST_UPDATED_FORMAT ).format( lastUpdated ) );

                        md.setVersioning( versioning );

                        return md;
                    } );
                }
                catch ( KojiClientException e )
                {
                    throw new IndyWorkflowException(
                            "Failed to retrieve version metadata for: %s from Koji. Reason: %s", e, ga,
                            e.getMessage() );
                }

                Metadata md = metadata;

                // FIXME: Need a way to listen for cache expiration and re-request this?
                versionMetadata.execute( ( cache ) -> cache.getAdvancedCache()
                                                           .put( ref, md, kojiConfig.getMetadataTimeoutSeconds(),
                                                                 TimeUnit.SECONDS ) );

                if ( metadata != null )
                {
                    versionMetadata.put( ga, metadata );
                }
            }

            return metadata;
        }
        catch ( InterruptedException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.warn( "Interrupted waiting for Koji GA version metadata lock on target: {}", ga );
        }
        finally
        {
            lock.unlock();
        }

        return null;
    }
}
