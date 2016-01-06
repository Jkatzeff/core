/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.projects.stacks;

import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectType;

/**
 * This is a {@link StackFacet} implementation placeholder
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class NullStackFacet extends AbstractFacet<Project> implements StackFacet
{
   @Override
   public boolean install()
   {
      return true;
   }

   @Override
   public boolean isInstalled()
   {
      return false;
   }

   @Override
   public Stack getStack()
   {
      return NullStack.INSTANCE;
   }

   @Override
   public boolean supports(ProjectType projectType)
   {
      return true;
   }

   @Override
   public int priority()
   {
      return Integer.MIN_VALUE;
   }

}
