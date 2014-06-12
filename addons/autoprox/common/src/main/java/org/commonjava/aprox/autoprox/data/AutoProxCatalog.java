/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.autoprox.data;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alternative
@Named( "dont-inject-directly" )
public class AutoProxCatalog
{

    private transient final Logger logger = LoggerFactory.getLogger( getClass() );

    private final List<RuleMapping> ruleMappings;

    private boolean enabled;

    public AutoProxCatalog( final boolean enabled, final List<RuleMapping> ruleMappings )
    {
        this.ruleMappings = ruleMappings;
        this.enabled = enabled;
    }

    public AutoProxCatalog( final boolean enabled )
    {
        this.enabled = enabled;
        this.ruleMappings = Collections.emptyList();
    }

    public List<RuleMapping> getRuleMappings()
    {
        return ruleMappings;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    private AutoProxRule getRule( final String name )
    {
        for ( final RuleMapping mapping : getRuleMappings() )
        {
            logger.info( "Checking rule: '{}' for applicability to name: '{}'", mapping.getScriptName(), name );
            if ( mapping.matchesName( name ) )
            {
                final AutoProxRule rule = mapping.getRule();

                logger.info( "Using rule {} (script: {}) for new store: '{}'", rule.getClass()
                                                                                   .getSimpleName(),
                             mapping.getScriptName(), name );

                return rule;
            }
        }

        logger.info( "No AutoProx rule found for: '{}'", name );

        return null;
    }

    public RemoteRepository createRemoteRepository( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRule( name );
        try
        {
            return rule == null ? null : rule.createRemoteRepository( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, name, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public HostedRepository createHostedRepository( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRule( name );
        try
        {
            return rule == null ? null : rule.createHostedRepository( name );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public Group createGroup( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRule( name );
        try
        {
            return rule == null ? null : rule.createGroup( name );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public String getRemoteValidationUrl( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRule( name );
        try
        {
            return rule == null ? null : rule.getRemoteValidationPath();
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

    public RemoteRepository createGroupValidationRemote( final String name )
        throws AutoProxRuleException
    {
        final AutoProxRule rule = getRule( name );
        try
        {
            return rule == null ? null : rule.createGroupValidationRemote( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AutoProxRuleException( "Invalid URL genenerated for: '%s'. Reason: %s", e, name, e.getMessage() );
        }
        catch ( final Exception e )
        {
            throw new AutoProxRuleException( "Failed to create remote repository for: %s. Reason: %s", e, name,
                                             e.getMessage() );
        }
    }

}
