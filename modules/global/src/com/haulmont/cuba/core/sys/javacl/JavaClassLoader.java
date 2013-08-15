/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Eugeniy Degtyarjov
 * Created: 28.12.2009 17:30:25
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys.javacl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.TimeSource;
import com.haulmont.cuba.core.sys.javacl.compiler.CharSequenceCompiler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.perf4j.log4j.Log4JStopWatch;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ManagedBean("cuba_JavaClassLoader")
public class JavaClassLoader extends URLClassLoader {
    private static final String JAVA_CLASSPATH = System.getProperty("java.class.path");
    private static final String PATH_SEPARATOR = System.getProperty("path.separator");
    private static final String JAR_EXT = ".jar";

    private static Log log = LogFactory.getLog(JavaClassLoader.class);

    protected final String cubaClassPath;
    protected final String classPath;

    protected final String rootDir;

    protected final Map<String, TimestampClass> compiled = new ConcurrentHashMap<String, TimestampClass>();
    protected final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();

    protected final ProxyClassLoader proxyClassLoader;
    protected final SourceProvider sourceProvider;

    @Inject
    private TimeSource timeSource;

    @Inject
    public JavaClassLoader(Configuration configuration) {
        super(new URL[0], Thread.currentThread().getContextClassLoader());

        this.proxyClassLoader = new ProxyClassLoader(Thread.currentThread().getContextClassLoader(), compiled);
        GlobalConfig config = configuration.getConfig(GlobalConfig.class);
        this.rootDir = config.getConfDir() + "/";
        this.cubaClassPath = config.getCubaClasspathDirectories();
        this.classPath = buildClasspath();
        this.sourceProvider = new SourceProvider(rootDir);
    }

    //Please use this constructor only in tests
    JavaClassLoader(ClassLoader parent, String rootDir, String cubaClassPath) {
        super(new URL[0], parent);

        Preconditions.checkNotNull(rootDir);
        Preconditions.checkNotNull(cubaClassPath);

        this.proxyClassLoader = new ProxyClassLoader(parent, compiled);
        this.rootDir = rootDir;
        this.cubaClassPath = cubaClassPath;
        this.classPath = buildClasspath();
        this.sourceProvider = new SourceProvider(rootDir);
    }

    public void clearCache() {
        compiled.clear();
    }

    public Class loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Log4JStopWatch loadingWatch = new Log4JStopWatch("LoadClass");
        try {
            lock(className);
            Class clazz;

            if (!sourceProvider.getSourceFile(className).exists()) {
                clazz = super.loadClass(className, resolve);
                return clazz;
            }

            CompilationScope compilationScope = new CompilationScope(this, className);
            if (!compilationScope.compilationNeeded()) {
                return getTimestampClass(className).clazz;
            }

            String src;
            try {
                src = sourceProvider.getSourceString(className);
            } catch (IOException e) {
                throw new ClassNotFoundException("Could not load java sources for class " + className);
            }

            try {
                log.debug("Compiling " + className);
                final DiagnosticCollector<JavaFileObject> errs = new DiagnosticCollector<>();


                SourcesAndDependencies sourcesAndDependencies = new SourcesAndDependencies(rootDir);
                sourcesAndDependencies.putSource(className, src);
                sourcesAndDependencies.collectDependencies(className);

                Map<String, CharSequence> sourcesForCompilation = collectSourcesForCompilation(className, sourcesAndDependencies.sources);

                Map<String, Class> compiledClasses = createCompiler().compile(sourcesForCompilation, errs);

                Map<String, TimestampClass> compiledTimestampClasses = convertCompiledClassesAndDependencies(compiledClasses, sourcesAndDependencies.dependencies);

                compiled.putAll(compiledTimestampClasses);

                clazz = compiledClasses.get(className);
                return clazz;
            } catch (Exception e) {
                proxyClassLoader.restoreRemoved();
                throw new RuntimeException(e);
            } finally {
                proxyClassLoader.cleanupRemoved();
            }
        } finally {
            unlock(className);
            loadingWatch.stop();
        }
    }

    @Override
    public URL findResource(String name) {
        if (name.startsWith("/"))
            name = name.substring(1);
        File file = new File(rootDir, name);
        if (file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else
            return null;
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        if (resource != null)
            return resource;
        else
            return super.getResource(name);
    }

    protected Date getCurrentTimestamp() {
        return timeSource.currentTimestamp();
    }

    TimestampClass getTimestampClass(String name) {
        return compiled.get(name);
    }

    private Map<String, TimestampClass> convertCompiledClassesAndDependencies(Map<String, Class> compiledClasses, Multimap<String, String> dependecies) {
        Map<String, TimestampClass> compiledTimestampClasses = new HashMap<>();
        for (String currentClassName : compiledClasses.keySet()) {
            Class currentClass = compiledClasses.get(currentClassName);
            Collection<String> dependentClasses = dependecies.get(currentClassName);
            compiledTimestampClasses.put(currentClassName, new TimestampClass(currentClass, getCurrentTimestamp(), dependentClasses, new HashSet<String>()));
        }

        for (Map.Entry<String, TimestampClass> entry : compiledTimestampClasses.entrySet()) {
            for (String dependencyClassName : entry.getValue().dependencies) {
                TimestampClass dependencyClass = compiledTimestampClasses.get(dependencyClassName);
                if (dependencyClass != null) {
                    dependencyClass.dependent.add(entry.getKey());
                }
            }
        }
        return compiledTimestampClasses;
    }

    /**
     * Decides what to compile using CompilationScope (hierarchical search)
     * Find all classes dependent from those we are going to compile and add them to compilation as well
     */
    private Map<String, CharSequence> collectSourcesForCompilation(String rootClassName, Map<String, CharSequence> sourcesToCompilation) throws ClassNotFoundException, IOException {
        Map<String, CharSequence> dependentSources = new HashMap<String, CharSequence>();

        proxyClassLoader.removeFromCache(rootClassName);
        collectDependent(rootClassName, dependentSources);
        for (String dependencyClassName : sourcesToCompilation.keySet()) {
            CompilationScope dependencyCompilationScope = new CompilationScope(this, dependencyClassName);
            if (dependencyCompilationScope.compilationNeeded()) {
                collectDependent(dependencyClassName, dependentSources);
            }
        }
        sourcesToCompilation.putAll(dependentSources);
        return sourcesToCompilation;
    }

    private void collectDependent(String dependencyClassName, Map<String, CharSequence> dependentSources) throws IOException {
        TimestampClass removedClass = proxyClassLoader.removeFromCache(dependencyClassName);
        if (removedClass != null) {
            for (String dependentName : removedClass.dependent) {
                dependentSources.put(dependentName, sourceProvider.getSourceString(dependentName));
                collectDependent(dependentName, dependentSources);
            }
        }
    }

    private CharSequenceCompiler createCompiler() {
        return new CharSequenceCompiler(
                proxyClassLoader,
                Arrays.asList("-classpath", classPath, "-g")
        );
    }

    private void unlock(String name) {
        locks.get(name).unlock();
    }

    private void lock(String name) {//not sure it's right, but we can not use synchronization here
        locks.putIfAbsent(name, new ReentrantLock());
        locks.get(name).lock();
    }

    private String buildClasspath() {
        StringBuilder classpathBuilder = new StringBuilder(JAVA_CLASSPATH).append(PATH_SEPARATOR);

        if (cubaClassPath != null) {
            String[] directories = cubaClassPath.split(";");
            for (String directoryPath : directories) {
                if (StringUtils.isNotBlank(directoryPath)) {
                    classpathBuilder.append(directoryPath).append(PATH_SEPARATOR);
                    File directory = new File(directoryPath);
                    File[] directoryFiles = directory.listFiles();
                    if (directoryFiles != null) {
                        for (File file : directoryFiles) {
                            if (file.getName().endsWith(JAR_EXT)) {
                                classpathBuilder.append(file.getAbsolutePath()).append(PATH_SEPARATOR);
                            }
                        }
                    }
                }
            }
        }
        return classpathBuilder.toString();
    }
}