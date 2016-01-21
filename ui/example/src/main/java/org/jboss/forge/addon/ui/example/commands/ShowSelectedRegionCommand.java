/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.example.commands;

import java.io.PrintStream;
import java.util.Optional;

import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIRegion;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * Displays the selected {@link UIRegion} for the selected resources
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public class ShowSelectedRegionCommand implements UICommand
{

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      UIOutput output = uiContext.getProvider().getOutput();
      UISelection<Object> selection = uiContext.getSelection();
      if (selection.isEmpty())
      {
         return Results.fail("No resource selected");
      }
      else
      {
         for (Object resource : selection)
         {
            if (resource != null)
            {
               PrintStream out = output.out();
               output.info(out, "Selected Resource: " + resource);
               Optional<UIRegion> optionalRegion = selection.getSelectedRegionFor(resource);
               if (optionalRegion.isPresent())
               {
                  UIRegion region = optionalRegion.get();
                  String msg = String.format("Column: %s, Line: %s, Start: %s, End: %s", region.getColumnNumber(),
                           region.getLineNumber(), region.getStartPosition(), region.getEndPosition());
                  output.info(out, msg);
               }
               else
               {
                  output.warn(out, "No selected region found");
               }
            }
         }
         return Results.success();
      }
   }

}
