/*
 * Copyright (c) 2013 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.bali.db.QueryRunner;
import com.haulmont.bali.db.ResultSetHandler;
import com.haulmont.cuba.core.global.DbDialect;
import com.haulmont.cuba.core.global.MssqlDbDialect;
import com.haulmont.cuba.core.sys.persistence.DbmsType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrMatcher;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author artamonov
 * @version $Id$
 */
public class DbUpdaterEngine implements DbUpdater {

    private static final String SQL_EXTENSION = "sql";
    private static final String SQL_COMMENT_PREFIX = "--";
    private static final String SQL_DELIMITER = "^";

    private static final Log log = LogFactory.getLog(DbUpdaterEngine.class);

    protected DataSource dataSource;
    protected File dbDir;
    protected DbDialect dbDialect;

    protected boolean changelogTableExists = false;

    // register handlers for script files
    protected final Map<String, FileHandler> extensionHandlers = new HashMap<>();

    {
        extensionHandlers.put(SQL_EXTENSION, new FileHandler() {
            @Override
            public void run(File file) {
                executeSqlScript(file);
            }
        });
    }

    public DbUpdaterEngine(DataSource dataSource, File dbDir, DbDialect dbDialect) {
        this.dataSource = dataSource;
        this.dbDir = dbDir;
        this.dbDialect = dbDialect;
    }

    protected DbUpdaterEngine() {
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DbDialect getDbDialect() {
        return dbDialect;
    }

    protected void createChangelogTable() {
        QueryRunner runner = new QueryRunner(getDataSource());
        try {
            runner.update("create table SYS_DB_CHANGELOG(" +
                    "SCRIPT_NAME varchar(300) not null primary key, " +
                    "CREATE_TS " + (getDbDialect() instanceof MssqlDbDialect ? "datetime" : "timestamp") + " default current_timestamp, " +
                    "IS_INIT integer default 0)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<File> getScriptsByExtension(Collection files, final URI scriptDirUri, final String extension) {
        Collection scriptsCollection = CollectionUtils.select(files, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                File file = (File) object;
                return extension.equals(FilenameUtils.getExtension(file.getName()));
            }
        });
        // noinspection unchecked
        List<File> scripts = new ArrayList<File>(scriptsCollection);
        Collections.sort(scripts, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                URI f1Uri = scriptDirUri.relativize(f1.toURI());
                URI f2Uri = scriptDirUri.relativize(f2.toURI());

                return f1Uri.getPath().compareTo(f2Uri.getPath());
            }
        });
        return scripts;
    }

    protected List<File> getUpdateScripts() {
        List<File> databaseScripts = new ArrayList<>();
        List<File> additionalScripts = new ArrayList<>();

        if (dbDir.exists()) {
            String[] moduleDirs = dbDir.list();
            Arrays.sort(moduleDirs);
            for (String moduleDirName : moduleDirs) {
                File moduleDir = new File(dbDir, moduleDirName);
                File initDir = new File(moduleDir, "update");
                File scriptDir = new File(initDir, DbmsType.getCurrent().getId());
                if (scriptDir.exists()) {
                    Collection list = FileUtils.listFiles(scriptDir, null, true);
                    URI scriptDirUri = scriptDir.toURI();

                    List<File> sqlFiles = getScriptsByExtension(list, scriptDirUri, SQL_EXTENSION);

                    for (String ext : extensionHandlers.keySet()) {
                        if (!ext.equals(SQL_EXTENSION)) {
                            List<File> additionalFiles = getScriptsByExtension(list, scriptDirUri, ext);
                            additionalScripts.addAll(additionalFiles);
                        }
                    }

                    databaseScripts.addAll(sqlFiles);
                }
            }
        }
        databaseScripts.addAll(additionalScripts);
        return databaseScripts;
    }

    protected String getScriptName(File file) {
        try {
            String path = file.getCanonicalPath();
            String dir = dbDir.getCanonicalPath();
            return path.substring(dir.length() + 1).replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateDatabase() {
        if (dbInitialized())
            doUpdate();
        else
            doInit();
    }

    @Override
    public List<String> findUpdateDatabaseScripts() throws DBNotInitializedException {
        List<String> list = new ArrayList<>();
        if (dbInitialized()) {
            if (!changelogTableExists) {
                throw new DBNotInitializedException(
                        "Unable to determine required updates because SYS_DB_CHANGELOG table doesn't exist");
            } else {
                List<File> files = getUpdateScripts();
                Set<String> scripts = getExecutedScripts();
                for (File file : files) {
                    String name = getScriptName(file);
                    if (!scripts.contains(name)) {
                        list.add(name);
                    }
                }
            }
        } else {
            throw new DBNotInitializedException(
                    "Unable to determine required updates because SEC_USER table doesn't exist");
        }
        return list;
    }

    protected boolean dbInitialized() {
        Connection connection = null;
        try {
            connection = getDataSource().getConnection();
            DatabaseMetaData dbMetaData = connection.getMetaData();
            ResultSet tables = dbMetaData.getTables(null, null, null, null);
            boolean found = false;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if ("SYS_DB_CHANGELOG".equalsIgnoreCase(tableName)) {
                    changelogTableExists = true;
                }
                if ("SEC_USER".equalsIgnoreCase(tableName)) {
                    found = true;
                }
            }
            return found;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null)
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
        }
    }

    protected void doInit() {
        log.info("Initializing database");

        createChangelogTable();

        List<File> initFiles = getInitScripts();
        for (File file : initFiles) {
            executeScript(file);
            markScript(getScriptName(file), true);
        }

        prepareScripts();

        log.info("Database initialized");
    }

    protected void doUpdate() {
        log.info("Updating database...");

        if (!changelogTableExists) {
            log.info("Changelog table not found, creating it and mark all scripts as executed");

            createChangelogTable();

            List<File> initFiles = getInitScripts();
            for (File file : initFiles) {
                markScript(getScriptName(file), true);
            }

            prepareScripts();

            return;
        }

        List<File> files = getUpdateScripts();
        Set<String> scripts = getExecutedScripts();
        for (File file : files) {
            String name = getScriptName(file);
            if (!scripts.contains(name)) {
                executeScript(file);
                markScript(name, false);
            }
        }
        log.info("Database is up-to-date");
    }

    /**
     * Mark all SQL updates scripts as evaluated
     * Try to execute Groovy scripts
     */
    protected void prepareScripts() {
        List<File> updateFiles = getUpdateScripts();
        for (File file : updateFiles)
            markScript(getScriptName(file), true);
    }

    protected Set<String> getExecutedScripts() {
        QueryRunner runner = new QueryRunner(getDataSource());
        try {
            Set<String> scripts = runner.query("select SCRIPT_NAME from SYS_DB_CHANGELOG",
                    new ResultSetHandler<Set<String>>() {
                        @Override
                        public Set<String> handle(ResultSet rs) throws SQLException {
                            Set<String> rows = new HashSet<>();
                            while (rs.next()) {
                                rows.add(rs.getString(1));
                            }
                            return rows;
                        }
                    });
            return scripts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void markScript(String name, boolean init) {
        QueryRunner runner = new QueryRunner(getDataSource());
        try {
            runner.update("insert into SYS_DB_CHANGELOG (SCRIPT_NAME, IS_INIT) values (?, ?)",
                    new Object[]{name, init ? 1 : 0}
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void executeSqlScript(File file) {
        String script;
        try {
            script = FileUtils.readFileToString(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StrTokenizer tokenizer = new StrTokenizer(script,
                StrMatcher.charSetMatcher(SQL_DELIMITER),
                StrMatcher.singleQuoteMatcher()
        );
        QueryRunner runner = new QueryRunner(getDataSource());
        while (tokenizer.hasNext()) {
            String sql = tokenizer.nextToken();
            try {
                if (isLikelySelect(sql)) {
                    runner.query(sql, new ResultSetHandler<Object>() {
                        @Override
                        public Object handle(ResultSet rs) throws SQLException {
                            return null;
                        }
                    });
                } else {
                    runner.update(sql);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // If first keyword is not SELECT then its probably not a select query.
    protected boolean isLikelySelect(String sql) {
        String[] lines = sql.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith(SQL_COMMENT_PREFIX)) {
                return line.toLowerCase().startsWith("select");
            }
        }
        return false;
    }

    protected void executeScript(File file) {
        log.info("Executing script " + file.getPath());
        String filename = file.getName();
        String extension = FilenameUtils.getExtension(filename);
        if (StringUtils.isNotEmpty(extension)) {
            if (extensionHandlers.containsKey(extension)) {
                FileHandler handler = extensionHandlers.get(extension);
                handler.run(file);
            } else
                log.warn("Update script ignored, file handler for extension not found:" +
                        file.getName());
        } else
            log.warn("Update script ignored, file extension undefined:" + file.getName());
    }

    protected List<File> getInitScripts() {
        List<File> files = new ArrayList<>();
        if (dbDir.exists()) {
            String[] moduleDirs = dbDir.list();
            Arrays.sort(moduleDirs);
            for (String moduleDirName : moduleDirs) {
                File moduleDir = new File(dbDir, moduleDirName);
                File initDir = new File(moduleDir, "init");
                File scriptDir = new File(initDir, DbmsType.getCurrent().getId());
                if (scriptDir.exists()) {
                    File[] scriptFiles = scriptDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith("create-db.sql");
                        }
                    });
                    Arrays.sort(scriptFiles);
                    files.addAll(Arrays.asList(scriptFiles));
                }
            }
        }
        return files;
    }

    // File extension handler
    public interface FileHandler {
        void run(File file);
    }
}