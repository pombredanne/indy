/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.conf;

import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.ConfigurationSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

@SectionName( ConfigurationSectionListener.DEFAULT_SECTION )
@ApplicationScoped
public class DefaultIndyConfiguration
    implements IndyConfiguration, IndyConfigInfo, SystemPropertyProvider
{

    public static final int DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS = 300;

    public static final int DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS = 10800; // 3 hours

    public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30; // 5 seconds (previous) is crazy on most normal networks

    public static final int DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS = 1800; // 30 minutes

    public static final int DEFAULT_NFC_EXPIRATION_SWEEP_MINUTES = 30;

    public static final int DEFAULT_NFC_MAX_RESULT_SET_SIZE = 10000; // 10,000

    public static final Boolean DEFAULT_ALLOW_REMOTE_LIST_DOWNLOAD = false;

    public static final int DEFAULT_REMOTE_METADATA_TIMEOUT_SECONDS = 86400;

    public static final int DEFAULT_FORKJOINPOOL_COMMON_PARALLELISM = 48;

    private Integer passthroughTimeoutSeconds;

    private Integer notFoundCacheTimeoutSeconds;

    private Integer requestTimeoutSeconds;

    private Integer storeDisableTimeoutSeconds;

    private Integer nfcExpirationSweepMinutes;

    private Integer nfcMaxResultSetSize;

    private Integer remoteMetadataTimeoutSeconds;

    private String mdcHeaders;

    private Integer forkJoinPoolCommonParallelism;

    private Boolean allowRemoteListDownload;

    private Boolean clusterEnabled;

    private String nodeId;

    private Boolean sslRequired;
    
    private List<String> remoteNoSSLHosts;

    public DefaultIndyConfiguration()
    {
    }

    @Override
    public String getNodeId()
    {
        return nodeId == null ? getDefaultNodeId() : nodeId;
    }

    private String getDefaultNodeId()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        String nodeId = System.getenv( "HOSTNAME" );
        if ( isBlank( nodeId ) )
        {
            nodeId = System.getenv( "HOST" );
        }

        if ( isBlank( nodeId ) )
        {
            // Windows uses %COMPUTERNAME% instead of hostanme, apparently
            nodeId = System.getenv( "COMPUTERNAME" );
        }

        if ( isBlank( nodeId ) )
        {
            logger.warn( "No nodeId found! Using 'localhost'." );
            nodeId = "localhost";
        }

        return nodeId;
    }

    @ConfigName( IndyConfiguration.PROP_NODE_ID )
    public void setNodeId( String nodeId )
    {
        this.nodeId = nodeId;
    }

    @Override
    public int getPassthroughTimeoutSeconds()
    {
        return passthroughTimeoutSeconds == null ? DEFAULT_PASSTHROUGH_TIMEOUT_SECONDS : passthroughTimeoutSeconds;
    }

    @ConfigName( "passthrough.timeout" )
    public void setPassthroughTimeoutSeconds( final int seconds )
    {
        passthroughTimeoutSeconds = seconds;
    }

    @ConfigName( "nfc.timeout" )
    public void setNotFoundCacheTimeoutSeconds( final int seconds )
    {
        notFoundCacheTimeoutSeconds = seconds;
    }

    @ConfigName( "mdc.headers" )
    public void setMDCHeaders( final String headers )
    {
        mdcHeaders = headers;
    }

    @Override
    public String getMdcHeaders()
    {
        return mdcHeaders;
    }

    @Override
    public int getNotFoundCacheTimeoutSeconds()
    {
        return notFoundCacheTimeoutSeconds == null ? DEFAULT_NOT_FOUND_CACHE_TIMEOUT_SECONDS : notFoundCacheTimeoutSeconds;
    }

    @Override
    public int getRequestTimeoutSeconds()
    {
        return requestTimeoutSeconds == null ? DEFAULT_REQUEST_TIMEOUT_SECONDS : requestTimeoutSeconds;
    }

    @ConfigName( "request.timeout" )
    public void setRequestTimeoutSeconds( final Integer requestTimeoutSeconds )
    {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    @Override
    public int getStoreDisableTimeoutSeconds()
    {
        return storeDisableTimeoutSeconds == null ? DEFAULT_STORE_DISABLE_TIMEOUT_SECONDS : storeDisableTimeoutSeconds;
    }

    @ConfigName( "nfc.sweep.minutes" )
    public void setDefaultNfcExpirationSweepMinutes( final int minutes )
    {
        this.nfcExpirationSweepMinutes = minutes;
    }

    @ConfigName( "nfc.maxresultsetsize" )
    public void setDefaultNfcMaxResultSetSize( final int size )
    {
        this.nfcMaxResultSetSize = size;
    }

    @Override
    public int getNfcExpirationSweepMinutes()
    {
        return nfcExpirationSweepMinutes == null ? DEFAULT_NFC_EXPIRATION_SWEEP_MINUTES : nfcExpirationSweepMinutes;
    }

    @Override
    public int getNfcMaxResultSetSize()
    {
        return nfcMaxResultSetSize == null ? DEFAULT_NFC_MAX_RESULT_SET_SIZE : nfcMaxResultSetSize;
    }

    @Override
    public File getIndyHomeDir()
    {
        return getSyspropDir( IndyConfigFactory.INDY_HOME_PROP );
    }

    @Override
    public File getIndyConfDir()
    {
        return getSyspropDir( IndyConfigFactory.CONFIG_DIR_PROP );
    }

    @Override
    public Boolean isAllowRemoteListDownload()
    {
        return allowRemoteListDownload == null ? DEFAULT_ALLOW_REMOTE_LIST_DOWNLOAD : allowRemoteListDownload;
    }

    @ConfigName( "remote.list.download.enabled" )
    public void setAllowRemoteListDownload( Boolean allowRemoteListDownload )
    {
        this.allowRemoteListDownload = allowRemoteListDownload;
    }

    private File getSyspropDir( final String property )
    {
        String dir = System.getProperty( property );
        return isEmpty(dir) ? null : new File( dir );
    }

    @ConfigName( "store.disable.timeout" )
    public void setStoreDisableTimeoutSeconds( final Integer storeDisableTimeoutSeconds )
    {
        this.storeDisableTimeoutSeconds = storeDisableTimeoutSeconds;
    }

    @Override
    public int getRemoteMetadataTimeoutSeconds()
    {
        return remoteMetadataTimeoutSeconds == null ?
                DEFAULT_REMOTE_METADATA_TIMEOUT_SECONDS :
                remoteMetadataTimeoutSeconds;
    }

    @Override
    public int getForkJoinPoolCommonParallelism()
    {
        return forkJoinPoolCommonParallelism == null ? DEFAULT_FORKJOINPOOL_COMMON_PARALLELISM : forkJoinPoolCommonParallelism;
    }

    @Override
    public boolean isClusterEnabled()
    {
        return clusterEnabled == null ? false : clusterEnabled;
    }

    @ConfigName( "cluster.enabled" )
    public void setClusterEnabled( Boolean clusterEnabled )
    {
        this.clusterEnabled = clusterEnabled;
    }

    @ConfigName( "forkjoinpool.common.parallelism" )
    public void setForkJoinPoolCommonParallelism( Integer forkJoinPoolCommonParallelism )
    {
        this.forkJoinPoolCommonParallelism = forkJoinPoolCommonParallelism;
    }

    @ConfigName( "remote.metadata.timeout" )
    public void setRemoteMetadataTimeoutSeconds( Integer remoteMetadataTimeoutSeconds )
    {
        this.remoteMetadataTimeoutSeconds = remoteMetadataTimeoutSeconds;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return IndyConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-main.conf" );
    }

    @Override
    public Properties getSystemPropertyAdditions()
    {
        Properties props = new Properties();
        props.setProperty( PROP_NODE_ID, getNodeId() );

        return props;
    }

    @ConfigName(value = "remote.ssl.required")
    public void setSslRequired(Boolean sslEnabled)
    {
        this.sslRequired = sslEnabled;
    }

    @Override
    public boolean isSSLRequired()
    {
        return this.sslRequired == null ? false : this.sslRequired;
    }
    
    @ConfigName(value = "remote.nossl.hosts")
    public void setRemoteNoSSLHosts(String hosts)
    {
        String[] arrayNSSLHosts = hosts.split(",");
        this.remoteNoSSLHosts = new ArrayList<>();
        this.remoteNoSSLHosts.addAll(Arrays.asList(arrayNSSLHosts));
    }

    @Override
    public List<String> getRemoteNoSSLHosts()
    {
        return this.remoteNoSSLHosts;
    }
}
