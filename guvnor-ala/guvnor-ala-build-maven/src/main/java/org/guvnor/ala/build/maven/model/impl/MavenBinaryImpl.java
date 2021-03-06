/*
 * Copyright 2016 JBoss, by Red Hat, Inc
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

package org.guvnor.ala.build.maven.model.impl;

import org.guvnor.ala.build.Project;
import org.guvnor.ala.build.maven.model.MavenBinary;
import org.guvnor.ala.config.CloneableConfig;
import org.uberfire.java.nio.file.Path;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public class MavenBinaryImpl implements MavenBinary,
                                        CloneableConfig<MavenBinary> {

    private Path path;

    private String name;

    public MavenBinaryImpl(final Path path, final String name) {
        this.path = checkNotNull( "path", path );
        this.name = checkNotNull( "name", name );
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MavenBinary asNewClone( final MavenBinary source ) {
        return new MavenBinaryImpl( source.getPath(), source.getName() );
    }
}