package org.sonatype.aether.impl.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RepositoryEvent.EventType;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeployResult;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.impl.Deployer;
import org.sonatype.aether.impl.MetadataGenerator;
import org.sonatype.aether.impl.MetadataGeneratorFactory;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.impl.RepositoryEventDispatcher;
import org.sonatype.aether.impl.SyncContextFactory;
import org.sonatype.aether.impl.UpdateCheck;
import org.sonatype.aether.impl.UpdateCheckManager;
import org.sonatype.aether.metadata.MergeableMetadata;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.spi.connector.ArtifactUpload;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.MetadataUpload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.connector.Transfer;
import org.sonatype.aether.spi.io.FileProcessor;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;
import org.sonatype.aether.transfer.ArtifactTransferException;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.transfer.MetadataTransferException;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.aether.util.DefaultRequestTrace;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;

/**
 * @author Benjamin Bentmann
 */
@Component( role = Deployer.class )
public class DefaultDeployer
    implements Deployer, Service
{

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private FileProcessor fileProcessor;

    @Requirement
    private RepositoryEventDispatcher repositoryEventDispatcher;

    @Requirement
    private RemoteRepositoryManager remoteRepositoryManager;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    @Requirement( role = MetadataGeneratorFactory.class )
    private List<MetadataGeneratorFactory> metadataFactories = new ArrayList<MetadataGeneratorFactory>();

    @Requirement
    private SyncContextFactory syncContextFactory;

    public DefaultDeployer()
    {
        // enables default constructor
    }

    public DefaultDeployer( Logger logger, FileProcessor fileProcessor,
                            RepositoryEventDispatcher repositoryEventDispatcher,
                            RemoteRepositoryManager remoteRepositoryManager, UpdateCheckManager updateCheckManager,
                            List<MetadataGeneratorFactory> metadataFactories, SyncContextFactory syncContextFactory )
    {
        setLogger( logger );
        setFileProcessor( fileProcessor );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
        setRemoteRepositoryManager( remoteRepositoryManager );
        setUpdateCheckManager( updateCheckManager );
        setMetadataFactories( metadataFactories );
        setSyncContextFactory( syncContextFactory );
    }

    public void initService( ServiceLocator locator )
    {
        setLogger( locator.getService( Logger.class ) );
        setFileProcessor( locator.getService( FileProcessor.class ) );
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
        setRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
        setUpdateCheckManager( locator.getService( UpdateCheckManager.class ) );
        setMetadataFactories( locator.getServices( MetadataGeneratorFactory.class ) );
        setSyncContextFactory( locator.getService( SyncContextFactory.class ) );
    }

    public DefaultDeployer setLogger( Logger logger )
    {
        this.logger = ( logger != null ) ? logger : NullLogger.INSTANCE;
        return this;
    }

    public DefaultDeployer setFileProcessor( FileProcessor fileProcessor )
    {
        if ( fileProcessor == null )
        {
            throw new IllegalArgumentException( "file processor has not been specified" );
        }
        this.fileProcessor = fileProcessor;
        return this;
    }

    public DefaultDeployer setRepositoryEventDispatcher( RepositoryEventDispatcher repositoryEventDispatcher )
    {
        if ( repositoryEventDispatcher == null )
        {
            throw new IllegalArgumentException( "repository event dispatcher has not been specified" );
        }
        this.repositoryEventDispatcher = repositoryEventDispatcher;
        return this;
    }

    public DefaultDeployer setRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        if ( remoteRepositoryManager == null )
        {
            throw new IllegalArgumentException( "remote repository manager has not been specified" );
        }
        this.remoteRepositoryManager = remoteRepositoryManager;
        return this;
    }

    public DefaultDeployer setUpdateCheckManager( UpdateCheckManager updateCheckManager )
    {
        if ( updateCheckManager == null )
        {
            throw new IllegalArgumentException( "update check manager has not been specified" );
        }
        this.updateCheckManager = updateCheckManager;
        return this;
    }

    public DefaultDeployer addMetadataGeneratorFactory( MetadataGeneratorFactory factory )
    {
        if ( factory == null )
        {
            throw new IllegalArgumentException( "metadata generator factory has not been specified" );
        }
        metadataFactories.add( factory );
        return this;
    }

    public DefaultDeployer setMetadataFactories( List<MetadataGeneratorFactory> metadataFactories )
    {
        if ( metadataFactories == null )
        {
            this.metadataFactories = new ArrayList<MetadataGeneratorFactory>();
        }
        else
        {
            this.metadataFactories = metadataFactories;
        }
        return this;
    }

    public DefaultDeployer setSyncContextFactory( SyncContextFactory syncContextFactory )
    {
        if ( syncContextFactory == null )
        {
            throw new IllegalArgumentException( "sync context factory has not been specified" );
        }
        this.syncContextFactory = syncContextFactory;
        return this;
    }

    public DeployResult deploy( RepositorySystemSession session, DeployRequest request )
        throws DeploymentException
    {
        if ( session.isOffline() )
        {
            throw new DeploymentException( "The repository system is in offline mode, deployment impossible" );
        }

        SyncContext syncContext = syncContextFactory.newInstance( session, false );

        try
        {
            return deploy( syncContext, session, request );
        }
        finally
        {
            syncContext.release();
        }
    }

    private DeployResult deploy( SyncContext syncContext, RepositorySystemSession session, DeployRequest request )
        throws DeploymentException
    {
        DeployResult result = new DeployResult( request );

        RequestTrace trace = DefaultRequestTrace.newChild( request.getTrace(), request );

        RemoteRepository repository = request.getRepository();

        RepositoryConnector connector;
        try
        {
            connector = remoteRepositoryManager.getRepositoryConnector( session, repository );
        }
        catch ( NoRepositoryConnectorException e )
        {
            throw new DeploymentException( "Failed to deploy artifacts/metadata: " + e.getMessage(), e );
        }

        List<MetadataGenerator> generators = getMetadataGenerators( session, request );

        try
        {
            List<ArtifactUpload> artifactUploads = new ArrayList<ArtifactUpload>();
            List<MetadataUpload> metadataUploads = new ArrayList<MetadataUpload>();
            IdentityHashMap<Metadata, Object> processedMetadata = new IdentityHashMap<Metadata, Object>();

            EventCatapult catapult = new EventCatapult( session, trace, repository, repositoryEventDispatcher );

            List<Artifact> artifacts = new ArrayList<Artifact>( request.getArtifacts() );

            List<Metadata> metadatas = Utils.prepareMetadata( generators, artifacts );

            syncContext.acquire( artifacts, Utils.combine( request.getMetadata(), metadatas ) );

            for ( Metadata metadata : metadatas )
            {
                upload( metadataUploads, session, metadata, repository, connector, catapult );
                processedMetadata.put( metadata, null );
            }

            for ( int i = 0; i < artifacts.size(); i++ )
            {
                Artifact artifact = artifacts.get( i );

                for ( MetadataGenerator generator : generators )
                {
                    artifact = generator.transformArtifact( artifact );
                }

                artifacts.set( i, artifact );

                artifactUploads.add( new ArtifactUploadEx( artifact, artifact.getFile(), catapult ) );
            }

            connector.put( artifactUploads, null );

            for ( ArtifactUpload upload : artifactUploads )
            {
                if ( upload.getException() != null )
                {
                    throw new DeploymentException( "Failed to deploy artifacts: " + upload.getException().getMessage(),
                                                   upload.getException() );
                }
                result.addArtifact( upload.getArtifact() );
            }

            metadatas = Utils.finishMetadata( generators, artifacts );

            syncContext.acquire( null, metadatas );

            for ( Metadata metadata : metadatas )
            {
                upload( metadataUploads, session, metadata, repository, connector, catapult );
                processedMetadata.put( metadata, null );
            }

            for ( Metadata metadata : request.getMetadata() )
            {
                if ( !processedMetadata.containsKey( metadata ) )
                {
                    upload( metadataUploads, session, metadata, repository, connector, catapult );
                    processedMetadata.put( metadata, null );
                }
            }

            connector.put( null, metadataUploads );

            for ( MetadataUpload upload : metadataUploads )
            {
                if ( upload.getException() != null )
                {
                    throw new DeploymentException( "Failed to deploy metadata: " + upload.getException().getMessage(),
                                                   upload.getException() );
                }
                result.addMetadata( upload.getMetadata() );
            }
        }
        finally
        {
            connector.close();
        }

        return result;
    }

    private List<MetadataGenerator> getMetadataGenerators( RepositorySystemSession session, DeployRequest request )
    {
        List<MetadataGeneratorFactory> factories = Utils.sortMetadataGeneratorFactories( this.metadataFactories );

        List<MetadataGenerator> generators = new ArrayList<MetadataGenerator>();

        for ( MetadataGeneratorFactory factory : factories )
        {
            MetadataGenerator generator = factory.newInstance( session, request );
            if ( generator != null )
            {
                generators.add( generator );
            }
        }

        return generators;
    }

    private void upload( Collection<MetadataUpload> metadataUploads, RepositorySystemSession session,
                         Metadata metadata, RemoteRepository repository, RepositoryConnector connector,
                         EventCatapult catapult )
        throws DeploymentException
    {
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        File basedir = lrm.getRepository().getBasedir();

        File dstFile = new File( basedir, lrm.getPathForRemoteMetadata( metadata, repository, "" ) );

        if ( metadata instanceof MergeableMetadata )
        {
            if ( !( (MergeableMetadata) metadata ).isMerged() )
            {
                {
                    DefaultRepositoryEvent event =
                        new DefaultRepositoryEvent( EventType.METADATA_RESOLVING, session, catapult.getTrace() );
                    event.setMetadata( metadata );
                    event.setRepository( repository );
                    repositoryEventDispatcher.dispatch( event );

                    event = new DefaultRepositoryEvent( EventType.METADATA_DOWNLOADING, session, catapult.getTrace() );
                    event.setMetadata( metadata );
                    event.setRepository( repository );
                    repositoryEventDispatcher.dispatch( event );
                }

                RepositoryPolicy policy = getPolicy( session, repository, metadata.getNature() );
                MetadataDownload download = new MetadataDownload();
                download.setMetadata( metadata );
                download.setFile( dstFile );
                download.setChecksumPolicy( policy.getChecksumPolicy() );
                connector.get( null, Arrays.asList( download ) );

                Exception error = download.getException();

                if ( error instanceof MetadataNotFoundException )
                {
                    dstFile.delete();
                }

                {
                    DefaultRepositoryEvent event =
                        new DefaultRepositoryEvent( EventType.METADATA_DOWNLOADED, session, catapult.getTrace() );
                    event.setMetadata( metadata );
                    event.setRepository( repository );
                    event.setException( error );
                    event.setFile( dstFile );
                    repositoryEventDispatcher.dispatch( event );

                    event = new DefaultRepositoryEvent( EventType.METADATA_RESOLVED, session, catapult.getTrace() );
                    event.setMetadata( metadata );
                    event.setRepository( repository );
                    event.setException( error );
                    event.setFile( dstFile );
                    repositoryEventDispatcher.dispatch( event );
                }

                if ( error != null && !( error instanceof MetadataNotFoundException ) )
                {
                    throw new DeploymentException( "Failed to retrieve remote metadata " + metadata + ": "
                        + error.getMessage(), error );
                }
            }

            try
            {
                ( (MergeableMetadata) metadata ).merge( dstFile, dstFile );
            }
            catch ( RepositoryException e )
            {
                throw new DeploymentException( "Failed to update metadata " + metadata + ": " + e.getMessage(), e );
            }
        }
        else
        {
            if ( metadata.getFile() == null )
            {
                throw new DeploymentException( "Failed to update metadata " + metadata + ": No file attached." );
            }
            try
            {
                fileProcessor.copy( metadata.getFile(), dstFile, null );
            }
            catch ( IOException e )
            {
                throw new DeploymentException( "Failed to update metadata " + metadata + ": " + e.getMessage(), e );
            }
        }

        UpdateCheck<Metadata, MetadataTransferException> check = new UpdateCheck<Metadata, MetadataTransferException>();
        check.setItem( metadata );
        check.setFile( dstFile );
        check.setRepository( repository );
        check.setAuthoritativeRepository( repository );
        updateCheckManager.touchMetadata( session, check );

        metadataUploads.add( new MetadataUploadEx( metadata, dstFile, catapult ) );
    }

    private RepositoryPolicy getPolicy( RepositorySystemSession session, RemoteRepository repository,
                                        Metadata.Nature nature )
    {
        boolean releases = !Metadata.Nature.SNAPSHOT.equals( nature );
        boolean snapshots = !Metadata.Nature.RELEASE.equals( nature );
        return remoteRepositoryManager.getPolicy( session, repository, releases, snapshots );
    }

    static class EventCatapult
    {

        private final RepositorySystemSession session;

        private final RequestTrace trace;

        private final RemoteRepository repository;

        private final RepositoryEventDispatcher dispatcher;

        public EventCatapult( RepositorySystemSession session, RequestTrace trace, RemoteRepository repository,
                              RepositoryEventDispatcher dispatcher )
        {
            this.session = session;
            this.trace = trace;
            this.repository = repository;
            this.dispatcher = dispatcher;
        }

        public RequestTrace getTrace()
        {
            return trace;
        }

        public void artifactDeploying( Artifact artifact, File file )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.ARTIFACT_DEPLOYING, session, trace );
            event.setArtifact( artifact );
            event.setRepository( repository );
            event.setFile( file );

            dispatcher.dispatch( event );
        }

        public void artifactDeployed( Artifact artifact, File file, ArtifactTransferException exception )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.ARTIFACT_DEPLOYED, session, trace );
            event.setArtifact( artifact );
            event.setRepository( repository );
            event.setFile( file );
            event.setException( exception );

            dispatcher.dispatch( event );
        }

        public void metadataDeploying( Metadata metadata, File file )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_DEPLOYING, session, trace );
            event.setMetadata( metadata );
            event.setRepository( repository );
            event.setFile( file );

            dispatcher.dispatch( event );
        }

        public void metadataDeployed( Metadata metadata, File file, Exception exception )
        {
            DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_DEPLOYED, session, trace );
            event.setMetadata( metadata );
            event.setRepository( repository );
            event.setFile( file );
            event.setException( exception );

            dispatcher.dispatch( event );
        }

    }

    static class ArtifactUploadEx
        extends ArtifactUpload
    {

        private final EventCatapult catapult;

        public ArtifactUploadEx( Artifact artifact, File file, EventCatapult catapult )
        {
            super( artifact, file );
            this.catapult = catapult;
        }

        @Override
        public Transfer setState( State state )
        {
            super.setState( state );

            if ( State.ACTIVE.equals( state ) )
            {
                catapult.artifactDeploying( getArtifact(), getFile() );
            }
            else if ( State.DONE.equals( state ) )
            {
                catapult.artifactDeployed( getArtifact(), getFile(), getException() );
            }

            return this;
        }

    }

    static class MetadataUploadEx
        extends MetadataUpload
    {

        private final EventCatapult catapult;

        public MetadataUploadEx( Metadata metadata, File file, EventCatapult catapult )
        {
            super( metadata, file );
            this.catapult = catapult;
        }

        @Override
        public Transfer setState( State state )
        {
            super.setState( state );

            if ( State.ACTIVE.equals( state ) )
            {
                catapult.metadataDeploying( getMetadata(), getFile() );
            }
            else if ( State.DONE.equals( state ) )
            {
                catapult.metadataDeployed( getMetadata(), getFile(), getException() );
            }

            return this;
        }

    }

}
