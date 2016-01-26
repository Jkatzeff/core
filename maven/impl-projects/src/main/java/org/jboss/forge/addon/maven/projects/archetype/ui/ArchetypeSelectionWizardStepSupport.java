/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.forge.addon.maven.projects.archetype.ui;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyRepository;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.projects.archetype.ArchetypeHelper;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.container.simple.lifecycle.SimpleContainer;

import java.io.File;

/**
 * Base class for a wizard step which on execution creates an archetype
 */
public abstract class ArchetypeSelectionWizardStepSupport extends AbstractUICommand implements UIWizardStep {
   @Override
   public NavigationResult next(UINavigationContext context) throws Exception
   {
      return null;
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      Project project = (Project) uiContext.getAttributeMap().get(Project.class);
      String coordinate = getArchetypeGroupId() + ":" + getArchetypeArtifactId() + ":"
               + getArchetypeVersion();
      DependencyQueryBuilder depQuery = DependencyQueryBuilder.create(coordinate);
      String repository = getArchetypeRepository();
      if (repository != null)
      {
         depQuery.setRepositories(new DependencyRepository("archetype", repository));
      }
      DependencyResolver resolver = SimpleContainer.getServices(ArchetypeSelectionWizardStepSupport.class.getClassLoader(), DependencyResolver.class)
               .get();
      Dependency resolvedArtifact = resolver.resolveArtifact(depQuery);
      FileResource<?> artifact = resolvedArtifact.getArtifact();
      MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
      File fileRoot = project.getRoot().reify(DirectoryResource.class).getUnderlyingResourceObject();
      ArchetypeHelper archetypeHelper = new ArchetypeHelper(artifact.getResourceInputStream(), fileRoot,
               metadataFacet.getProjectGroupName(), metadataFacet.getProjectName(), metadataFacet.getProjectVersion());
      JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
      archetypeHelper.setPackageName(facet.getBasePackage());
      archetypeHelper.execute();
      return Results.success();
   }

   protected abstract String getArchetypeRepository();

   protected abstract String getArchetypeVersion();

   protected abstract String getArchetypeArtifactId();

   protected abstract String getArchetypeGroupId();
}
