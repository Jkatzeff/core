/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.javaee;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectType;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.stacks.Stack;
import org.jboss.forge.furnace.util.Sets;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;

/**
 * Implementation for the {@link JavaEE7Facet}
 *
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 */
@FacetConstraint(DependencyFacet.class)
public class JavaEE7FacetImpl extends AbstractJavaEEFacet implements JavaEE7Facet
{
   @Inject
   public JavaEE7FacetImpl(DependencyInstaller installer)
   {
      super(installer);
   }

   @Override
   public String getSpecName()
   {
      return "Java EE";
   }

   @Override
   public Version getSpecVersion()
   {
      return SingleVersion.valueOf("7");
   }

   @Override
   protected Map<Dependency, List<Dependency>> getRequiredDependencyOptions()
   {
      Map<Dependency, List<Dependency>> result = new LinkedHashMap<>();
      result.put(JAVAEE7, Arrays.asList(JAVAEE7));
      return result;
   }

   @Override
   protected Iterable<Dependency> getRequiredManagedDependenciesFor(Dependency dependency)
   {
      return Collections.emptySet();
   }

   @Override
   public Stack getStack()
   {
      return JavaEE7Facet.STACK;
   }

   @Override
   public boolean supports(ProjectType projectType)
   {
      Set<Class<? extends ProjectFacet>> facets = Sets.toSet(projectType.getRequiredFacets());
      return facets.contains(JavaSourceFacet.class);
   }

   @Override
   public int priority()
   {
      return 1;
   }

   @Override
   public boolean uninstall()
   {
      DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);
      facet.removeDependency(JAVAEE7);
      facet.removeManagedDependency(JAVAEE7);
      return true;
   }
}
