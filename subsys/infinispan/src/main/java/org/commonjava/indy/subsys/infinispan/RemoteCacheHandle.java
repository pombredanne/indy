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
package org.commonjava.indy.subsys.infinispan;

import org.commonjava.indy.metrics.IndyMetricsManager;
import org.infinispan.client.hotrod.RemoteCache;

import static com.codahale.metrics.MetricRegistry.name;

public class RemoteCacheHandle<K,V> extends BasicCacheHandle<K, V>
{

    public RemoteCacheHandle( String named, RemoteCache<K, V> cache, IndyMetricsManager metricsManager, String metricPrefix )
    {
        super( named, cache, metricsManager, metricPrefix );
    }

    public RemoteCacheHandle( String named, RemoteCache<K, V> cache )
    {
        this( named, cache, null, null );
    }

    @Override
    protected String getMetricName( String opName )
    {
        return name( getMetricPrefix(), cache.getName(), "remote", opName );
    }

}
