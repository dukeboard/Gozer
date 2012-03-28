package org.sonatype.aether.impl.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.aether.RequestTrace;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.RepositoryEvent.EventType;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.MetadataResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.impl.RepositoryEventDispatcher;
import org.sonatype.aether.impl.SyncContextFactory;
import org.sonatype.aether.impl.UpdateCheck;
import org.sonatype.aether.impl.UpdateCheckManager;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.transfer.MetadataTransferException;
import org.sonatype.aether.transfer.NoRepositoryConnectorException;
import org.sonatype.aether.util.ConfigUtils;
import org.sonatype.aether.util.DefaultRequestTrace;
import org.sonatype.aether.util.concurrency.RunnableErrorForwarder;
import org.sonatype.aether.util.listener.DefaultRepositoryEvent;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.LocalMetadataRegistration;
import org.sonatype.aether.repository.LocalMetadataRequest;
import org.sonatype.aether.repository.LocalMetadataResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.spi.connector.MetadataDownload;
import org.sonatype.aether.spi.connector.RepositoryConnector;
import org.sonatype.aether.spi.locator.Service;
import org.sonatype.aether.spi.locator.ServiceLocator;
import org.sonatype.aether.spi.log.Logger;
import org.sonatype.aether.spi.log.NullLogger;

/**
 * @author Benjamin Bentmann
 */
@Component( role = MetadataResolver.class )
public class DefaultMetadataResolver
    implements MetadataResolver, Service
{

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger logger = NullLogger.INSTANCE;

    @Requirement
    private RepositoryEventDispatcher repositoryEventDispatcher;

    @Requirement
    private UpdateCheckManager updateCheckManager;

    @Requirement
    private RemoteRepositoryManager remoteRepositoryManager;

    @Requirement
    private SyncContextFactory syncContextFactory;

    public DefaultMetadataResolver()
    {
        // enables default constructor
    }

    public DefaultMetadataResolver( Logger logger, RepositoryEventDispatcher repositoryEventDispatcher,
                                    UpdateCheckManager updateCheckManager,
                                    RemoteRepositoryManager remoteRepositoryManager,
                                    SyncContextFactory syncContextFactory )
    {
        setLogger( logger );
        setRepositoryEventDispatcher( repositoryEventDispatcher );
        setUpdateCheckManager( updateCheckManager );
        setRemoteRepositoryManager( remoteRepositoryManager );
        setSyncContextFactory( syncContextFactory );
    }

    public void initService( ServiceLocator locator )
    {
        setLogger( locator.getService( Logger.class ) );
        setRepositoryEventDispatcher( locator.getService( RepositoryEventDispatcher.class ) );
        setUpdateCheckManager( locator.getService( UpdateCheckManager.class ) );
        setRemoteRepositoryManager( locator.getService( RemoteRepositoryManager.class ) );
        setSyncContextFactory( locator.getService( SyncContextFactory.class ) );
    }

    public DefaultMetadataResolver setLogger( Logger logger )
    {
        this.logger = ( logger != null ) ? logger : NullLogger.INSTANCE;
        return this;
    }

    public DefaultMetadataResolver setRepositoryEventDispatcher( RepositoryEventDispatcher repositoryEventDispatcher )
    {
        if ( repositoryEventDispatcher == null )
        {
            throw new IllegalArgumentException( "repository event dispatcher has not been specified" );
        }
        this.repositoryEventDispatcher = repositoryEventDispatcher;
        return this;
    }

    public DefaultMetadataResolver setUpdateCheckManager( UpdateCheckManager updateCheckManager )
    {
        if ( updateCheckManager == null )
        {
            throw new IllegalArgumentException( "update check manager has not been specified" );
        }
        this.updateCheckManager = updateCheckManager;
        return this;
    }

    public DefaultMetadataResolver setRemoteRepositoryManager( RemoteRepositoryManager remoteRepositoryManager )
    {
        if ( remoteRepositoryManager == null )
        {
            throw new IllegalArgumentException( "remote repository manager has not been specified" );
        }
        this.remoteRepositoryManager = remoteRepositoryManager;
        return this;
    }

    public DefaultMetadataResolver setSyncContextFactory( SyncContextFactory syncContextFactory )
    {
        if ( syncContextFactory == null )
        {
            throw new IllegalArgumentException( "sync context factory has not been specified" );
        }
        this.syncContextFactory = syncContextFactory;
        return this;
    }

    public List<MetadataResult> resolveMetadata( RepositorySystemSession session,
                                                 Collection<? extends MetadataRequest> requests )
    {
        SyncContext syncContext = syncContextFactory.newInstance( session, false );

        try
        {
            Collection<Metadata> metadata = new ArrayList<Metadata>( requests.size() );
            for ( MetadataRequest request : requests )
            {
                metadata.add( request.getMetadata() );
            }

            syncContext.acquire( null, metadata );

            return resolve( session, requests );
        }
        finally
        {
            syncContext.release();
        }
    }

    private List<MetadataResult> resolve( RepositorySystemSession session,
                                          Collection<? extends MetadataRequest> requests )
    {
        List<MetadataResult> results = new ArrayList<MetadataResult>( requests.size() );

        List<ResolveTask> tasks = new ArrayList<ResolveTask>( requests.size() );

        Map<File, Long> localLastUpdates = new HashMap<File, Long>();

        for ( MetadataRequest request : requests )
        {
            RequestTrace trace = DefaultRequestTrace.newChild( request.getTrace(), request );

            MetadataResult result = new MetadataResult( request );
            results.add( result );

            Metadata metadata = request.getMetadata();
            RemoteRepository repository = request.getRepository();

            if ( repository == null )
            {
                LocalRepository localRepo = session.getLocalRepositoryManager().getRepository();

                metadataResolving( session, trace, metadata, localRepo );

                File localFile = getLocalFile( session, metadata );

                if ( localFile != null )
                {
                    metadata = metadata.setFile( localFile );
                    result.setMetadata( metadata );
                }
                else
                {
                    result.setException( new MetadataNotFoundException( metadata, localRepo ) );
                }

                metadataResolved( session, trace, metadata, localRepo, result.getException() );
                continue;
            }

            List<RemoteRepository> repositories = getEnabledSourceRepositories( repository, metadata.getNature() );

            if ( repositories.isEmpty() )
            {
                continue;
            }

            metadataResolving( session, trace, metadata, repository );
            LocalRepositoryManager lrm = session.getLocalRepositoryManager();
            LocalMetadataRequest localRequest =
                new LocalMetadataRequest( metadata, repository, request.getRequestContext() );
            LocalMetadataResult lrmResult = lrm.find( session, localRequest );

            File metadataFile = lrmResult.getFile();

            if ( session.isOffline() )
            {
                if ( metadataFile != null )
                {
                    metadata = metadata.setFile( metadataFile );
                    result.setMetadata( metadata );
                }
                else
                {
                    String msg =
                        "The repository system is offline but the metadata " + metadata + " from " + repository
                            + " is not available in the local repository.";
                    result.setException( new MetadataNotFoundException( metadata, repository, msg ) );
                }

                metadataResolved( session, trace, metadata, repository, result.getException() );
                continue;
            }

            Long localLastUpdate = null;
            if ( request.isFavorLocalRepository() )
            {
                File localFile = getLocalFile( session, metadata );
                localLastUpdate = localLastUpdates.get( localFile );
                if ( localLastUpdate == null )
                {
                    localLastUpdate = Long.valueOf( localFile != null ? localFile.lastModified() : 0 );
                    localLastUpdates.put( localFile, localLastUpdate );
                }
            }

            List<UpdateCheck<Metadata, MetadataTransferException>> checks =
                new ArrayList<UpdateCheck<Metadata, MetadataTransferException>>();
            Exception exception = null;
            for ( RemoteRepository repo : repositories )
            {
                UpdateCheck<Metadata, MetadataTransferException> check =
                    new UpdateCheck<Metadata, MetadataTransferException>();
                check.setLocalLastUpdated( ( localLastUpdate != null ) ? localLastUpdate.longValue() : 0 );
                check.setItem( metadata );

                // use 'main' installation file for the check (-> use requested repository)
                File checkFile =
                    new File(
                              session.getLocalRepository().getBasedir(),
                              session.getLocalRepositoryManager().getPathForRemoteMetadata( metadata, repository,
                                                                                            request.getRequestContext() ) );
                check.setFile( checkFile );
                check.setRepository( repository );
                check.setAuthoritativeRepository( repo );
                check.setPolicy( getPolicy( session, repo, metadata.getNature() ).getUpdatePolicy() );

                if ( lrmResult.isStale() )
                {
                    checks.add( check );
                }
                else
                {
                    updateCheckManager.checkMetadata( session, check );
                    if ( check.isRequired() )
                    {
                        checks.add( check );
                    }
                    else if ( exception == null )
                    {
                        exception = check.getException();
                    }
                }
            }

            if ( !checks.isEmpty() )
            {
                RepositoryPolicy policy = getPolicy( session, repository, metadata.getNature() );

                // install path may be different from lookup path
                File installFile =
                    new File(
                              session.getLocalRepository().getBasedir(),
                              session.getLocalRepositoryManager().getPathForRemoteMetadata( metadata,
                                                                                            request.getRepository(),
                                                                                            request.getRequestContext() ) );

                ResolveTask task =
                    new ResolveTask( session, trace, result, installFile, checks, policy.getChecksumPolicy() );
                tasks.add( task );
            }
            else
            {
                result.setException( exception );
                if ( metadataFile != null )
                {
                    metadata = metadata.setFile( metadataFile );
                    result.setMetadata( metadata );
                }
                metadataResolved( session, trace, metadata, repository, result.getException() );
            }
        }

        if ( !tasks.isEmpty() )
        {
            int threads = ConfigUtils.getInteger( session, 4, "aether.metadataResolver.threads" );
            Executor executor = getExecutor( Math.min( tasks.size(), threads ) );
            try
            {
                RunnableErrorForwarder errorForwarder = new RunnableErrorForwarder();

                for ( ResolveTask task : tasks )
                {
                    executor.execute( errorForwarder.wrap( task ) );
                }

                errorForwarder.await();

                for ( ResolveTask task : tasks )
                {
                    task.result.setException( task.exception );
                }
            }
            finally
            {
                shutdown( executor );
            }
            for ( ResolveTask task : tasks )
            {
                Metadata metadata = task.request.getMetadata();
                // re-lookup metadata for resolve
                LocalMetadataRequest localRequest =
                    new LocalMetadataRequest( metadata, task.request.getRepository(), task.request.getRequestContext() );
                File metadataFile = session.getLocalRepositoryManager().find( session, localRequest ).getFile();
                if ( metadataFile != null )
                {
                    metadata = metadata.setFile( metadataFile );
                    task.result.setMetadata( metadata );
                }
                if ( task.result.getException() == null )
                {
                    task.result.setUpdated( true );
                }
                metadataResolved( session, task.trace, metadata, task.request.getRepository(),
                                  task.result.getException() );
            }
        }

        return results;
    }

    private File getLocalFile( RepositorySystemSession session, Metadata metadata )
    {
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        LocalMetadataResult localResult = lrm.find( session, new LocalMetadataRequest( metadata, null, null ) );
        File localFile = localResult.getFile();
        return localFile;
    }

    private List<RemoteRepository> getEnabledSourceRepositories( RemoteRepository repository, Metadata.Nature nature )
    {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();

        if ( repository.isRepositoryManager() )
        {
            for ( RemoteRepository repo : repository.getMirroredRepositories() )
            {
                if ( isEnabled( repo, nature ) )
                {
                    repositories.add( repo );
                }
            }
        }
        else if ( isEnabled( repository, nature ) )
        {
            repositories.add( repository );
        }

        return repositories;
    }

    private boolean isEnabled( RemoteRepository repository, Metadata.Nature nature )
    {
        if ( !Metadata.Nature.SNAPSHOT.equals( nature ) && repository.getPolicy( false ).isEnabled() )
        {
            return true;
        }
        if ( !Metadata.Nature.RELEASE.equals( nature ) && repository.getPolicy( true ).isEnabled() )
        {
            return true;
        }
        return false;
    }

    private RepositoryPolicy getPolicy( RepositorySystemSession session, RemoteRepository repository,
                                        Metadata.Nature nature )
    {
        boolean releases = !Metadata.Nature.SNAPSHOT.equals( nature );
        boolean snapshots = !Metadata.Nature.RELEASE.equals( nature );
        return remoteRepositoryManager.getPolicy( session, repository, releases, snapshots );
    }

    private void metadataResolving( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                    ArtifactRepository repository )
    {
        DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_RESOLVING, session, trace );
        event.setMetadata( metadata );
        event.setRepository( repository );

        repositoryEventDispatcher.dispatch( event );
    }

    private void metadataResolved( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                   ArtifactRepository repository, Exception exception )
    {
        DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_RESOLVED, session, trace );
        event.setMetadata( metadata );
        event.setRepository( repository );
        event.setException( exception );
        event.setFile( metadata.getFile() );

        repositoryEventDispatcher.dispatch( event );
    }

    private void metadataDownloading( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                      ArtifactRepository repository )
    {
        DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_DOWNLOADING, session, trace );
        event.setMetadata( metadata );
        event.setRepository( repository );

        repositoryEventDispatcher.dispatch( event );
    }

    private void metadataDownloaded( RepositorySystemSession session, RequestTrace trace, Metadata metadata,
                                     ArtifactRepository repository, File file, Exception exception )
    {
        DefaultRepositoryEvent event = new DefaultRepositoryEvent( EventType.METADATA_DOWNLOADED, session, trace );
        event.setMetadata( metadata );
        event.setRepository( repository );
        event.setException( exception );
        event.setFile( file );

        repositoryEventDispatcher.dispatch( event );
    }

    private Executor getExecutor( int threads )
    {
        if ( threads <= 1 )
        {
            return new Executor()
            {
                public void execute( Runnable command )
                {
                    command.run();
                }
            };
        }
        else
        {
            return new ThreadPoolExecutor( threads, threads, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>() );
        }
    }

    private void shutdown( Executor executor )
    {
        if ( executor instanceof ExecutorService )
        {
            ( (ExecutorService) executor ).shutdown();
        }
    }

    class ResolveTask
        implements Runnable
    {

        final RepositorySystemSession session;

        final RequestTrace trace;

        final MetadataResult result;

        final MetadataRequest request;

        final File metadataFile;

        final String policy;

        final List<UpdateCheck<Metadata, MetadataTransferException>> checks;

        volatile MetadataTransferException exception;

        public ResolveTask( RepositorySystemSession session, RequestTrace trace, MetadataResult result,
                            File metadataFile, List<UpdateCheck<Metadata, MetadataTransferException>> checks,
                            String policy )
        {
            this.session = session;
            this.trace = trace;
            this.result = result;
            this.request = result.getRequest();
            this.metadataFile = metadataFile;
            this.policy = policy;
            this.checks = checks;
        }

        public void run()
        {
            Metadata metadata = request.getMetadata();
            RemoteRepository requestRepository = request.getRepository();

            metadataDownloading( session, trace, metadata, requestRepository );

            try
            {
                List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
                for ( UpdateCheck<Metadata, MetadataTransferException> check : checks )
                {
                    repositories.add( check.getAuthoritativeRepository() );
                }

                MetadataDownload download = new MetadataDownload();
                download.setMetadata( metadata );
                download.setRequestContext( request.getRequestContext() );
                download.setFile( metadataFile );
                download.setChecksumPolicy( RepositoryPolicy.CHECKSUM_POLICY_IGNORE );
                download.setRepositories( repositories );
                RepositoryConnector connector =
                    remoteRepositoryManager.getRepositoryConnector( session, requestRepository );
                try
                {
                    connector.get( null, Arrays.asList( download ) );
                   // System.out.println("After donwload "+download.getTrace().getData());
                    
                }
                finally
                {
                    connector.close();
                }

                exception = download.getException();

                if ( exception == null )
                {

                    List<String> contexts = Collections.singletonList( request.getRequestContext() );
                    LocalMetadataRegistration registration =
                        new LocalMetadataRegistration( metadata, requestRepository, contexts );

                    session.getLocalRepositoryManager().add( session, registration );
                }
                else if ( request.isDeleteLocalCopyIfMissing() && exception instanceof MetadataNotFoundException )
                {
                    download.getFile().delete();
                }
            }
            catch ( NoRepositoryConnectorException e )
            {
                exception = new MetadataTransferException( metadata, requestRepository, e );
            }

            for ( UpdateCheck<Metadata, MetadataTransferException> check : checks )
            {
                updateCheckManager.touchMetadata( session, check.setException( exception ) );
            }

            metadataDownloaded( session, trace, metadata, requestRepository, metadataFile, exception );
        }

    }

}
