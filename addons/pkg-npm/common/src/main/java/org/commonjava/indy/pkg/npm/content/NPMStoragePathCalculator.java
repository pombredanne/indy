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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.content.StoragePathCalculator;
import org.commonjava.indy.model.core.StoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;
import static org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger.METADATA_NAME;
import static org.commonjava.maven.galley.util.PathUtils.normalize;

/**
 * We know that NPM requests to package-level resources, such as just "jquery" actually return package metadata, as
 * you would find in a package.json file. Also, we know that NPM stores packages as binary tarballs UNDER A SUBPATH of
 * the path used to access this metadata. Caching both requires us to map the actual storage location of metadata to
 * a different subpath, so they both share a package-root directory and don't collide on the filesystem
 * (as concrete file vs directory).
 *
 * This implementation looks for metadata paths when the package type is NPM, and maps them to a package.json filename
 * under the package name, so we have a compatible file name for storage.
 *
 * NOTE: This does NOT affect the remote HTTP request path used to talk to upstream servers like npmjs.org.
 */
@ApplicationScoped
@Named
public class NPMStoragePathCalculator
        implements StoragePathCalculator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String calculateStoragePath( final StoreKey key, final String path )
    {
        if ( PKG_TYPE_NPM.equals( key.getPackageType() ) )
        {
            // This is considering the single path for npm standard like "/jquery"
            final boolean isSinglePath = path.split( "/" ).length < 2;
            // This is considering the scoped path for npm standard like "/@type/jquery"
            final boolean isScopedPath = path.startsWith( "@" ) && path.split( "/" ).length < 3;
            if ( isSinglePath || isScopedPath )
            {
                logger.debug( "Modifying target path: {}, appending '{}'", path, METADATA_NAME );
                return normalize( path, METADATA_NAME );
            }
        }

        return path;
    }
}
