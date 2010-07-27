package joist;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import joist.codegen.Codegen;
import joist.codegen.CodegenConfig;
import joist.domain.util.ConnectionSettings;
import joist.domain.util.MySqlC3p0Factory;
import joist.migrations.DatabaseBootstrapper;
import joist.migrations.Migrater;
import joist.migrations.MigraterConfig;
import joist.migrations.PermissionFixer;
import joist.util.Inflector;

public abstract class AbstractJoistCli {

    public ConnectionSettings dbAppUserSettings;
    public ConnectionSettings dbAppSaSettings;
    public ConnectionSettings dbSystemSettings;
    public CodegenConfig codegenConfig = new CodegenConfig();
    public MigraterConfig migraterConfig = new MigraterConfig();
    private final Map<ConnectionSettings, DataSource> dss = new HashMap<ConnectionSettings, DataSource>();

    public AbstractJoistCli(String projectName) {
        this.dbAppUserSettings = ConnectionSettings.forApp(Inflector.underscore(projectName));
        this.dbAppSaSettings = ConnectionSettings.forSa(Inflector.underscore(projectName));

        this.dbSystemSettings = ConnectionSettings.forSa(Inflector.underscore(projectName));
        this.dbSystemSettings.databaseName = "mysql";
        this.dbSystemSettings.password = this.dbAppSaSettings.password;

        this.migraterConfig.setProjectNameForDefaults(projectName);
        this.codegenConfig.setProjectNameForDefaults(projectName);
        if (".".equals(this.dbAppSaSettings.password)) {
            throw new RuntimeException("You need to set db.sa.password either on the command line or in build.properties.");
        }
    }

    public void cycle() {
        this.createDatabase();
        this.migrateDatabase();
        this.fixPermissions();
        this.codegen();
    }

    public void createDatabase() {
        new DatabaseBootstrapper(//
            this.getDataSourceForSystemTableAsSaUser(),
            this.getDataSourceForAppTableAsSaUser(),
            this.dbAppUserSettings).dropAndCreate();
    }

    public void migrateDatabase() {
        new Migrater(this.dbAppUserSettings, this.getDataSourceForAppTableAsSaUser(), this.migraterConfig).migrate();
    }

    public void fixPermissions() {
        PermissionFixer pf = new PermissionFixer(this.dbAppUserSettings, this.getDataSourceForAppTableAsSaUser());
        pf.setOwnerOfAllTablesTo(this.dbAppSaSettings.user);
        // pf.setOwnerOfAllSequencesTo(this.dbAppSaSettings.user);
        pf.grantAllOnAllTablesTo(this.dbAppUserSettings.user);
        // pf.grantAllOnAllSequencesTo(this.dbAppUserSettings.user);
    }

    public void codegen() {
        new Codegen(this.dbAppUserSettings, this.getDataSourceForAppTableAsSaUser(), this.codegenConfig).generate();
    }

    private DataSource getDataSourceForAppTableAsSaUser() {
        return this.getCachedDatasource(this.dbAppSaSettings);
    }

    private DataSource getDataSourceForSystemTableAsSaUser() {
        return this.getCachedDatasource(this.dbSystemSettings);
    }

    private DataSource getCachedDatasource(ConnectionSettings settings) {
        if (!this.dss.containsKey(settings)) {
            DataSource ds = new MySqlC3p0Factory(settings).create();
            this.dss.put(settings, ds);
        }
        return this.dss.get(settings);
    }

}