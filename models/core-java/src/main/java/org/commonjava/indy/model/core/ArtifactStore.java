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
package org.commonjava.indy.model.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import static org.commonjava.indy.model.core.PathStyle.plain;

@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = ArtifactStore.TYPE_ATTR )
@JsonSubTypes( { @Type( name = "remote", value = RemoteRepository.class ),
    @Type( name = "hosted", value = HostedRepository.class ), @Type( name = "group", value = Group.class ) } )
@ApiModel( description = "Definition of a content store on Indy, whether it proxies content from a remote server, hosts artifacts on this system, or groups other content stores.", discriminator = "type", subTypes = {
    HostedRepository.class, Group.class, RemoteRepository.class } )
public abstract class ArtifactStore
    implements Serializable
{

    public static final String PKG_TYPE_ATTR = "packageType";

    public static final String TYPE_ATTR = "type";

    public static final String KEY_ATTR = "key";

    public static final String METADATA_CHANGELOG = "changelog";

    public static final String METADATA_ORIGIN = "origin";

    public static final String TRACKING_ID = "trackingId";

    private static final long serialVersionUID = 1L;

    @ApiModelProperty( required = true, dataType = "string", value = "Serialized store key, of the form: '[hosted|group|remote]:name'" )
    private StoreKey key;

    private String description;

    private transient Map<String, Object> transientMetadata;

    private Map<String, String> metadata;

    private boolean disabled;

    @ApiModelProperty( required = false, dataType = "int", value = "Integer time in seconds which is used for repo automatically re-enable when set disable by errors, positive value means time in seconds, -1 means never disable, empty or 0 means use default timeout." )
    @JsonProperty( "disable_timeout" )
    private int disableTimeout;

    @JsonProperty( "path_style" )
    private PathStyle pathStyle;

    @JsonProperty( "path_mask_patterns" )
    private Set<String> pathMaskPatterns;

    // If true, the content in the repo will be authoritatively indexed. Transfers will be treated as missing if it is not in content-index.
    @JsonProperty("authoritative_index")
    private Boolean authoritativeIndex;

    @JsonIgnore
    private Boolean rescanInProgress = false;

    protected ArtifactStore()
    {
    }

    protected ArtifactStore( final String packageType, final StoreType type, final String name )
    {
        this.key = StoreKey.dedupe( new StoreKey( packageType, type, name ) );
    }

    public String getName()
    {
        return key.getName();
    }

    public StoreType getType()
    {
        return key.getType();
    }

    public String getPackageType()
    {
        return key.getPackageType();
    }

    public StoreKey getKey()
    {
        return key;
    }

    public abstract ArtifactStore copyOf();

    public abstract ArtifactStore copyOf( String packageType, String name );

    @Override
    public final int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( key == null ) ? 19 : key.hashCode() );
        return result;
    }

    @Override
    public final boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactStore other = (ArtifactStore) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }

    public int getDisableTimeout()
    {
        return disableTimeout;
    }

    public void setDisableTimeout( int disableTimeout )
    {
        this.disableTimeout = disableTimeout;
    }

    public void setMetadata( final Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public synchronized String setMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( metadata == null )
        {
            metadata = new HashMap<>();
        }

        return metadata.put( key, value );
    }

    public String getMetadata( final String key )
    {
        return metadata == null ? null : metadata.get( key );
    }

    public synchronized Object removeTransientMetadata( final String key )
    {
        if ( transientMetadata == null || key == null )
        {
            return null;
        }

        return transientMetadata.remove( key );
    }

    public synchronized Object setTransientMetadata( final String key, final Object value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( transientMetadata == null )
        {
            transientMetadata = new HashMap<>();
        }

        return transientMetadata.put( key, value );
    }

    public Object getTransientMetadata( final String key )
    {
        return transientMetadata == null ? null : transientMetadata.get( key );
    }

    protected void copyBase( ArtifactStore store )
    {
        store.setRescanInProgress( isRescanInProgress() );
        store.setDescription( getDescription() );
        store.setDisabled( isDisabled() );
        store.setMetadata( getMetadata() );
        store.setTransientMetadata( getTransientMetadata() );
        store.setPathStyle( getPathStyle() );
        store.setDisableTimeout( getDisableTimeout() );
        store.setPathMaskPatterns( getPathMaskPatterns() );
        store.setAuthoritativeIndex( isAuthoritativeIndex() );
    }

    protected void setTransientMetadata( Map<String, Object> transientMetadata )
    {
        this.transientMetadata = transientMetadata;
    }

    protected Map<String, Object> getTransientMetadata()
    {
        return transientMetadata;
    }

    @Override
    public String toString()
    {
        return String.format( "%s [key=%s]", getClass().getSimpleName(), key );
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

    public PathStyle getPathStyle()
    {
        return pathStyle == null ? plain : pathStyle;
    }

    public void setPathStyle( PathStyle pathStyle )
    {
        this.pathStyle = pathStyle;
    }

    public Set<String> getPathMaskPatterns()
    {
        return pathMaskPatterns;
    }

    public void setPathMaskPatterns( Set<String> pathMaskPatterns )
    {
        this.pathMaskPatterns = pathMaskPatterns;
    }

    public boolean isAuthoritativeIndex()
    {
        return authoritativeIndex == null ? Boolean.FALSE : authoritativeIndex;
    }

    public void setAuthoritativeIndex( boolean authoritativeIndex )
    {
        this.authoritativeIndex = authoritativeIndex;
    }

    public Boolean isRescanInProgress()
    {
        return rescanInProgress;
    }

    public void setRescanInProgress( Boolean rescanInProgress )
    {
        this.rescanInProgress = rescanInProgress;
    }
}
