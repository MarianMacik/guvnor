/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.guvnor.common.services.builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.guvnor.common.services.project.builder.events.InvalidateDMOProjectCacheEvent;
import org.guvnor.common.services.project.model.Project;
import org.guvnor.common.services.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.vfs.Path;
import org.uberfire.rpc.SessionInfo;
import org.uberfire.workbench.events.ResourceAddedEvent;
import org.uberfire.workbench.events.ResourceBatchChangesEvent;
import org.uberfire.workbench.events.ResourceChange;
import org.uberfire.workbench.events.ResourceChangeType;
import org.uberfire.workbench.events.ResourceDeletedEvent;
import org.uberfire.workbench.events.ResourceUpdatedEvent;

/**
 * Server side component that observes for the different resource add/delete/update events related to
 * a given project and that causes the ProjectDataModelOracle to be invalidated. Typically .java, .class and pom.xml
 * files. When such a resource is modified an InvalidateDMOProjectCacheEvent event is fired.
 */
@ApplicationScoped
public class ResourceChangeObserver {

    private static final Logger logger = LoggerFactory.getLogger( ResourceChangeObserver.class );

    @Inject
    private ProjectService projectService;

    @Inject
    private ResourceChangeIncrementalBuilder incrementalBuilder;

    @Inject
    private Event<InvalidateDMOProjectCacheEvent> invalidateDMOProjectCacheEvent;

    public void processResourceAdd( @Observes final ResourceAddedEvent resourceAddedEvent ) {
        processResourceChange( resourceAddedEvent.getSessionInfo(),
                               resourceAddedEvent.getPath(),
                               ResourceChangeType.ADD );
        incrementalBuilder.addResource( resourceAddedEvent.getPath() );
    }

    public void processResourceDelete( @Observes final ResourceDeletedEvent resourceDeletedEvent ) {
        processResourceChange( resourceDeletedEvent.getSessionInfo(),
                               resourceDeletedEvent.getPath(),
                               ResourceChangeType.DELETE );
        incrementalBuilder.deleteResource( resourceDeletedEvent.getPath() );
    }

    public void processResourceUpdate( @Observes final ResourceUpdatedEvent resourceUpdatedEvent ) {
        processResourceChange( resourceUpdatedEvent.getSessionInfo(),
                               resourceUpdatedEvent.getPath(),
                               ResourceChangeType.UPDATE );
        incrementalBuilder.updateResource( resourceUpdatedEvent.getPath() );
    }

    public void processBatchChanges( @Observes final ResourceBatchChangesEvent resourceBatchChangesEvent ) {
        final Map<Path, Collection<ResourceChange>> batchChanges = resourceBatchChangesEvent.getBatch();
        final Map<String, Boolean> notifiedProjects = new HashMap<String, Boolean>();

        if ( batchChanges == null ) {
            //un expected case
            logger.warn( "No batchChanges was present for the given resourceBatchChangesEvent: " + resourceBatchChangesEvent );
        }

        //All the changes must be processed, we don't have warranties that all the changes belongs to the same project.
        for ( final Map.Entry<Path, Collection<ResourceChange>> pathCollectionEntry : batchChanges.entrySet() ) {
            for ( ResourceChange change : pathCollectionEntry.getValue() ) {
                processResourceChange( resourceBatchChangesEvent.getSessionInfo(),
                                       pathCollectionEntry.getKey(),
                                       change.getType(),
                                       notifiedProjects );
            }
        }
        incrementalBuilder.batchResourceChanges( resourceBatchChangesEvent.getBatch() );
    }

    private void processResourceChange( final SessionInfo sessionInfo,
                                        final Path path,
                                        final ResourceChangeType changeType ) {
        processResourceChange( sessionInfo,
                               path,
                               changeType,
                               new HashMap<String, Boolean>() );
    }

    private void processResourceChange( final SessionInfo sessionInfo,
                                        final Path path,
                                        final ResourceChangeType changeType,
                                        final Map<String, Boolean> notifiedProjects ) {
        //Only process Project resources
        final Project project = projectService.resolveProject( path );
        if ( project == null ) {
            return;
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Processing resource change for sessionInfo: " + sessionInfo
                                  + ", project: " + project
                                  + ", path: " + path
                                  + ", changeType: " + changeType );
        }

        if ( !notifiedProjects.containsKey( project.getRootPath().toURI() ) && isObservableResource( path ) ) {
            invalidateDMOProjectCacheEvent.fire( new InvalidateDMOProjectCacheEvent( sessionInfo,
                                                                                     project,
                                                                                     path ) );
            notifiedProjects.put( project.getRootPath().toURI(),
                                  Boolean.TRUE );
        }

    }

    private boolean isObservableResource( Path path ) {
        return path != null && ( path.getFileName().endsWith( ".java" )
                || path.getFileName().endsWith( ".class" )
                || path.getFileName().equals( "pom.xml" )
                || path.getFileName().equals( "kmodule.xml" )
                || path.getFileName().endsWith( ".drl" ) );
    }

}
