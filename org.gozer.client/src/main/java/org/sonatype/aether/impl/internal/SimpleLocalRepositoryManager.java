package org.sonatype.aether.impl.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalArtifactRegistration;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;
import org.sonatype.aether.repository.LocalMetadataRegistration;
import org.sonatype.aether.repository.LocalMetadataRequest;
import org.sonatype.aether.repository.LocalMetadataResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.log.Logger;

/**
 * A local repository manager that realizes the classical Maven 2.0 local repository.
 * 
 * @author Benjamin Bentmann
 */
public class SimpleLocalRepositoryManager
    implements LocalRepositoryManager
{

    private final LocalRepository repository;

    public SimpleLocalRepositoryManager( File basedir )
    {
        this( basedir, "simple" );
    }

    public SimpleLocalRepositoryManager( String basedir )
    {
        this( ( basedir != null ) ? new File( basedir ) : null, "simple" );
    }

    SimpleLocalRepositoryManager( File basedir, String type )
    {
        if ( basedir == null )
        {
            throw new IllegalArgumentException( "base directory has not been specified" );
        }
        repository = new LocalRepository( basedir.getAbsoluteFile(), type );
    }

    public SimpleLocalRepositoryManager setLogger( Logger logger )
    {
        return this;
    }

    public LocalRepository getRepository()
    {
        return repository;
    }

    String getPathForArtifact( Artifact artifact, boolean local )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '/' );

        path.append( artifact.getBaseVersion() ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '-' );
        if ( local )
        {
            path.append( artifact.getBaseVersion() );
        }
        else
        {
            path.append( artifact.getVersion() );
        }

        if ( artifact.getClassifier().length() > 0 )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        if ( artifact.getExtension().length() > 0 )
        {
            path.append( '.' ).append( artifact.getExtension() );
        }

        return path.toString();
    }

    public String getPathForLocalArtifact( Artifact artifact )
    {
        return getPathForArtifact( artifact, true );
    }

    public String getPathForRemoteArtifact( Artifact artifact, RemoteRepository repository, String context )
    {
        return getPathForArtifact( artifact, false );
    }

    public String getPathForLocalMetadata( Metadata metadata )
    {
        return getPath( metadata, "local" );
    }

    public String getPathForRemoteMetadata( Metadata metadata, RemoteRepository repository, String context )
    {
        return getPath( metadata, getRepositoryKey( repository, context ) );
    }

    String getRepositoryKey( RemoteRepository repository, String context )
    {
        String key;

        if ( repository.isRepositoryManager() )
        {
            // repository serves dynamic contents, take request parameters into account for key

            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( repository.getId() );

            buffer.append( '-' );

            SortedSet<String> subKeys = new TreeSet<String>();
            for ( RemoteRepository mirroredRepo : repository.getMirroredRepositories() )
            {
                subKeys.add( mirroredRepo.getId() );
            }

            SimpleDigest digest = new SimpleDigest();
            digest.update( context );
            for ( String subKey : subKeys )
            {
                digest.update( subKey );
            }
            buffer.append( digest.digest() );

            key = buffer.toString();
        }
        else
        {
            // repository serves static contents, its id is sufficient as key

            key = repository.getId();
        }

        return key;
    }

    private String getPath( Metadata metadata, String repositoryKey )
    {
        StringBuilder path = new StringBuilder( 128 );

        if ( metadata.getGroupId().length() > 0 )
        {
            path.append( metadata.getGroupId().replace( '.', '/' ) ).append( '/' );

            if ( metadata.getArtifactId().length() > 0 )
            {
                path.append( metadata.getArtifactId() ).append( '/' );

                if ( metadata.getVersion().length() > 0 )
                {
                    path.append( metadata.getVersion() ).append( '/' );
                }
            }
        }

        path.append( insertRepositoryKey( metadata.getType(), repositoryKey ) );

        return path.toString();
    }

    private String insertRepositoryKey( String filename, String repositoryKey )
    {
        String result;
        int idx = filename.indexOf( '.' );
        if ( idx < 0 )
        {
            result = filename + '-' + repositoryKey;
        }
        else
        {
            result = filename.substring( 0, idx ) + '-' + repositoryKey + filename.substring( idx );
        }
        return result;
    }

    public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
    {
        String path = getPathForArtifact( request.getArtifact(), false );
        File file = new File( getRepository().getBasedir(), path );

        LocalArtifactResult result = new LocalArtifactResult( request );
        if ( file.isFile() )
        {
            result.setFile( file );
            result.setAvailable( true );
        }

        return result;
    }

    public void add( RepositorySystemSession session, LocalArtifactRegistration request )
    {
        // noop
    }

    @Override
    public String toString()
    {
        return String.valueOf( getRepository() );
    }

    public LocalMetadataResult find( RepositorySystemSession session, LocalMetadataRequest request )
    {
        LocalMetadataResult result = new LocalMetadataResult( request );

        String path;

        Metadata metadata = request.getMetadata();
        String context = request.getContext();
        RemoteRepository remote = request.getRepository();

        if ( remote != null )
        {
            path = getPathForRemoteMetadata( metadata, remote, context );
        }
        else
        {
            path = getPathForLocalMetadata( metadata );
        }

        File file = new File( getRepository().getBasedir(), path );
        if ( file.isFile() )
        {
            result.setFile( file );
        }

        return result;
    }

    public void add( RepositorySystemSession session, LocalMetadataRegistration request )
    {
        // noop
    }

}
