/**
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.database.tools.generate;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hibernate.cfg.JDBCMetaDataConfiguration;
import org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.ReverseEngineeringSettings;
import org.hibernate.cfg.reveng.ReverseEngineeringStrategy;
import org.hibernate.cfg.reveng.SchemaSelection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2x.ArtifactCollector;
import org.hibernate.tool.hbm2x.POJOExporter;
import org.hibernate.tool.hbm2x.pojo.ComponentPOJOClass;
import org.hibernate.tool.hbm2x.pojo.EntityPOJOClass;
import org.hibernate.tool.hbm2x.pojo.POJOClass;
import org.jboss.forge.addon.database.tools.util.HibernateToolsHelper;
import org.jboss.forge.addon.database.tools.util.JDBCUtils;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UIValidationContext;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.Lists;

/**
 * In this step, the user can choose the database tables
 */
public class DatabaseTableSelectionStep implements UIWizardStep
{
   private static final String LAST_USED_CONNECTION_PROPERTIES = "LastUsedConnectionProperties";

   private UISelectOne<String> databaseCatalog;
   private UISelectOne<String> databaseSchema;
   private UISelectMany<String> databaseTables;

   private volatile Set<String> catalogValueChoices;
   private volatile Set<String> schemaValueChoices;
   private volatile Set<String> tableValueChoices;

   private Throwable exception;

   private final GenerateEntitiesCommandDescriptor descriptor;

   private static final Logger logger = Logger.getLogger(DatabaseTableSelectionStep.class.getName());

   public DatabaseTableSelectionStep(GenerateEntitiesCommandDescriptor descriptor)
   {
      this.descriptor = descriptor;
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.forCommand(getClass()).name("Database Table Selection")
               .description("Select the database tables for which you want to generate entities");
   }

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      UIContext context = builder.getUIContext();
      if (databaseCatalog == null)
      {
         InputComponentFactory factory = builder.getInputComponentFactory();

         databaseCatalog = factory.createSelectOne("databaseCatalog", String.class)
                  .setLabel("Database Catalog")
                  .setDescription("The database catalog for which to generate entities.")
                  .setDefaultValue(() -> {
                     Iterator<String> it = databaseCatalog.getValueChoices().iterator();
                     return it.hasNext() ? it.next() : null;
                  })
                  .setValueChoices(() -> catalogValueChoices);

         databaseCatalog.addValueChangeListener((event) -> updateValueChoices(context, event));

         databaseSchema = factory.createSelectOne("databaseSchema", String.class)
                  .setLabel("Database Schema")
                  .setDescription("The database schema for which to generate entities.")
                  .setDefaultValue(() -> {
                     Iterator<String> it = databaseSchema.getValueChoices().iterator();
                     return it.hasNext() ? it.next() : null;
                  })
                  .setValueChoices(() -> schemaValueChoices);

         databaseSchema.addValueChangeListener((event) -> updateValueChoices(context, event));

         databaseTables = factory.createSelectMany("databaseTables", String.class)
                  .setLabel("Database Tables")
                  .setDescription("The database tables for which to generate entities. Use '*' to select all tables")
                  .setValueChoices(() -> tableValueChoices);
      }
      Database database = updateValueChoices(context, null);
      if (database != null)
      {
         if (database.isCatalogSet())
         {
            databaseCatalog.setValue(database.getCatalog());
         }
         if (database.isSchemaSet())
         {
            databaseSchema.setValue(database.getSchema());
         }
      }
      builder.add(databaseCatalog).add(databaseSchema).add(databaseTables);
   }

   private boolean connectionInfoHasChanged(UIContext context)
   {
      Map<Object, Object> attributeMap = context.getAttributeMap();
      Properties currentConnectionProperties = (Properties) attributeMap.get(LAST_USED_CONNECTION_PROPERTIES);
      return !Objects.equals(descriptor.getConnectionProperties(), currentConnectionProperties);
   }

   @Override
   public void validate(UIValidationContext context)
   {
      UIContext uiContext = context.getUIContext();
      updateValueChoices(uiContext, null);
      if (exception != null)
      {
         if (exception instanceof UnknownHostException)
         {
            context.addValidationError(databaseTables, "Unknown host: " + exception.getMessage());
         }
         else
         {
            String message = exception.getMessage();
            if (message == null)
            {
               message = String.format("%s during validation. Check logs for more information",
                        exception.getClass().getName());
            }
            context.addValidationError(databaseTables, exception.getMessage());
         }
      }
      else
      {
         List<String> list = Lists.toList(databaseTables.getValue());
         if (list == null || list.isEmpty())
         {
            context.addValidationError(databaseTables, "At least one database table must be specified");
         }
      }
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      Collection<String> entities = exportSelectedEntities(descriptor);
      return Results.success(entities.size() + " entities were generated in " + descriptor.getTargetPackage());
   }

   private Collection<String> exportSelectedEntities(GenerateEntitiesCommandDescriptor descriptor) throws Exception
   {
      String catalog = databaseCatalog.getValue();
      String schema = databaseSchema.getValue();
      Collection<String> selectedTableNames = Lists.toList(databaseTables.getValue());
      JavaSourceFacet java = descriptor.getSelectedProject().getFacet(JavaSourceFacet.class);
      JDBCMetaDataConfiguration jmdc = new JDBCMetaDataConfiguration();
      jmdc.setProperties(descriptor.getConnectionProperties());
      jmdc.setReverseEngineeringStrategy(
               createReverseEngineeringStrategy(descriptor, catalog, schema, selectedTableNames));
      HibernateToolsHelper.buildMappings(descriptor.getUrls(), descriptor.getDriverClass(), jmdc);
      POJOExporter pj = new POJOExporter(jmdc, java.getSourceDirectory().getUnderlyingResourceObject())
      {
         @Override
         protected void exportPOJO(Map<String, Object> additionalContext, POJOClass element)
         {
            if (isSelected(selectedTableNames, element))
            {
               super.exportPOJO(additionalContext, element);
            }
         }
      };
      Properties pojoProperties = new Properties();
      pojoProperties.setProperty("jdk5", "true");
      pojoProperties.setProperty("ejb3", "true");
      pj.setProperties(pojoProperties);
      pj.setArtifactCollector(new ArtifactCollector());
      pj.start();
      return selectedTableNames;
   }

   private ReverseEngineeringStrategy createReverseEngineeringStrategy(GenerateEntitiesCommandDescriptor descriptor,
            String catalog, String schema, Collection<String> selectedTableNames)
   {
      ReverseEngineeringStrategy strategy = new DefaultReverseEngineeringStrategy()
      {
         @Override
         public List<org.hibernate.cfg.reveng.SchemaSelection> getSchemaSelections()
         {
            return selectedTableNames
                     .stream()
                     .map((table) -> new SchemaSelection(catalog, schema, table))
                     .collect(Collectors.toList());
         }
      };

      ReverseEngineeringSettings revengsettings = new ReverseEngineeringSettings(strategy)
               .setDefaultPackageName(descriptor.getTargetPackage())
               .setDetectManyToMany(true)
               .setDetectOneToOne(true)
               .setDetectOptimisticLock(true);
      strategy.setSettings(revengsettings);
      return strategy;
   }

   private boolean isSelected(Collection<String> selection, POJOClass element)
   {
      boolean result = false;
      if (element instanceof ComponentPOJOClass)
      {
         ComponentPOJOClass cpc = (ComponentPOJOClass) element;
         Iterator<?> iterator = cpc.getAllPropertiesIterator();
         result = true;
         while (iterator.hasNext())
         {
            Object object = iterator.next();
            if (object instanceof Property)
            {
               Property property = (Property) object;
               String tableName = property.getValue().getTable().getName();
               if (!selection.contains(tableName))
               {
                  result = false;
                  break;
               }
            }
         }
      }
      else if (element instanceof EntityPOJOClass)
      {
         EntityPOJOClass epc = (EntityPOJOClass) element;
         Object object = epc.getDecoratedObject();
         if (object instanceof PersistentClass)
         {
            PersistentClass pc = (PersistentClass) object;
            Table table = pc.getTable();
            if (selection.contains(table.getName()))
            {
               result = true;
            }
         }
      }
      return result;
   }

   private synchronized Database getDatabase(UIContext context)
   {
      Map<Object, Object> attributeMap = context.getAttributeMap();
      Database database = (Database) attributeMap.get(Database.class.getName());
      if (database == null || connectionInfoHasChanged(context))
      {
         try
         {
            database = JDBCUtils.getDatabaseInfo(descriptor);
            exception = null;
            attributeMap.put(Database.class.getName(), database);
            attributeMap.put(LAST_USED_CONNECTION_PROPERTIES, descriptor.getConnectionProperties());
         }
         catch (Exception e)
         {
            attributeMap.remove(Database.class.getName(), database);
            attributeMap.remove(LAST_USED_CONNECTION_PROPERTIES, descriptor.getConnectionProperties());
            logger.log(Level.SEVERE, "Error while fetching the DB info", exception);
            exception = e;
         }
      }
      return database;
   }

   private Database updateValueChoices(UIContext context, ValueChangeEvent event)
   {
      Database database = getDatabase(context);
      List<DatabaseTable> tables = new ArrayList<>();
      if (database != null)
      {
         tables.addAll(database.getTables());
      }
      // Update Catalogs
      catalogValueChoices = tables
               .stream()
               .map(DatabaseTable::getCatalog)
               .filter(Objects::nonNull)
               .collect(Collectors.toCollection(TreeSet::new));
      final String catalog = (event != null && event.getSource() == databaseCatalog) ? (String) event.getNewValue()
               : databaseCatalog.getValue();
      // Update schemas
      schemaValueChoices = tables
               .stream()
               .filter((item) -> Objects.equals(item.getCatalog(), catalog))
               .map(DatabaseTable::getSchema)
               .filter(Objects::nonNull)
               .collect(Collectors.toCollection(TreeSet::new));
      final String schema = (event != null && event.getSource() == databaseSchema) ? (String) event.getNewValue()
               : databaseSchema.getValue();
      // Update tables
      tableValueChoices = tables
               .stream()
               .filter(item -> Objects.equals(item.getCatalog(), catalog))
               .filter(item -> Objects.equals(item.getSchema(), schema))
               .map(DatabaseTable::getName)
               .filter(Objects::nonNull)
               .collect(Collectors.toCollection(TreeSet::new));
      return database;
   }
}
