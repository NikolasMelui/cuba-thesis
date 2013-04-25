/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Eugeniy Degtyarjov
 * Created: 30.12.2009 13:03:18
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys.javacl.compiler;

import javax.tools.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;

/**
 * Compile a String or other {@link CharSequence}, returning a Java
 * {@link Class} instance that may be instantiated. This class is a Facade
 * around {@link javax.tools.JavaCompiler} for a narrower use case, but a bit easier to use.
 * <p/>
 * To compile a String containing source for a Java class which implements
 * MyInterface:
 * <p/>
 * <pre>
 * ClassLoader classLoader = MyClass.class.getClassLoader(); // optional; null is also OK
 * List&lt;Diagnostic&gt; diagnostics = new ArrayList&lt;Diagnostic&gt;(); // optional; null is also OK
 * JavaStringCompiler&lt;Object&gt; compiler = new JavaStringCompiler&lt;MyInterface&gt;(classLoader,
 *       null);
 * try {
 *    Class&lt;MyInterface&gt; newClass = compiler.compile(&quot;com.mypackage.NewClass&quot;,
 *          stringContaininSourceForNewClass, diagnostics, MyInterface);
 *    MyInterface instance = newClass.newInstance();
 *    instance.someOperation(someArgs);
 * } catch (JavaStringCompilerException e) {
 *    handle(e);
 * } catch (IllegalAccessException e) {
 *    handle(e);
 * }
 * </pre>
 * <p/>
 * The source can be in a String, {@link StringBuffer}, or your own class which
 * implements {@link CharSequence}. If you implement your own, it must be
 * thread safe (preferably, immutable.)
 *
 * @author <a href="mailto:David.Biesack@sas.com">David J. Biesack</a>
 */
public class CharSequenceCompiler<T> {
    // Compiler requires source files with a ".java" extension:
    static final String JAVA_EXTENSION = ".java";

    private final ClassLoaderImpl classLoader;

    // The compiler instance that this facade uses.
    private final JavaCompiler compiler;

    // The compiler options (such as "-target" "1.5").
    private final List<String> options;

    // collect compiler diagnostics in this instance.
    private DiagnosticCollector<JavaFileObject> diagnostics;

    // The FileManager which will store source and class "files".
    private final FileManagerImpl javaFileManager;

    /**
     * Construct a new instance which delegates to the named class loader.
     *
     * @param loader  the application ClassLoader. The compiler will look through to
     *                this // class loader for dependent classes
     * @param options The compiler options (such as "-target" "1.5"). See the usage
     *                for javac
     * @throws IllegalStateException if the Java compiler cannot be loaded.
     */
    public CharSequenceCompiler(ClassLoader loader, Iterable<String> options) {
        compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. "
                    + "Check that your class path includes tools.jar");
        }
        classLoader = new ClassLoaderImpl(loader);
        diagnostics = new DiagnosticCollector<JavaFileObject>();
        final JavaFileManager fileManager = compiler.getStandardFileManager(diagnostics,
                null, null);
        // create our FileManager which chains to the default file manager
        // and our ClassLoader
        javaFileManager = new FileManagerImpl(fileManager, classLoader);
        this.options = new ArrayList<String>();
        if (options != null) { // make a save copy of input options
            for (String option : options) {
                this.options.add(option);
            }
        }
    }

    /**
     * Compile Java source in <var>javaSource</name> and return the resulting
     * class.
     * <p/>
     * Thread safety: this method is thread safe if the <var>javaSource</var>
     * and <var>diagnosticsList</var> are isolated to this thread.
     *
     * @param qualifiedClassName The fully qualified class name.
     * @param javaSource         Complete java source, including a package statement and a class,
     *                           interface, or annotation declaration.
     * @param diagnosticsList    Any diagnostics generated by compiling the source are added to
     *                           this collector.
     * @param types              zero or more Class objects representing classes or interfaces
     *                           that the resulting class must be assignable (castable) to.
     * @return a Class which is generated by compiling the source
     * @throws CharSequenceCompilerException
     *                            if the source cannot be compiled - for example, if it contains
     *                            syntax or semantic errors or if dependent classes cannot be
     *                            found.
     * @throws ClassCastException if the generated class is not assignable to all the optional
     *                            <var>types</var>.
     */
    public synchronized Class<T> compile(final String qualifiedClassName,
                                         final CharSequence javaSource,
                                         final DiagnosticCollector<JavaFileObject> diagnosticsList,
                                         final Class<?>... types) throws CharSequenceCompilerException,
            ClassCastException {
        if (diagnosticsList != null)
            diagnostics = diagnosticsList;
        else
            diagnostics = new DiagnosticCollector<JavaFileObject>();
        Map<String, CharSequence> classes = new HashMap<String, CharSequence>(1);
        classes.put(qualifiedClassName, javaSource);
        Map<String, Class<T>> compiled = compile(classes, diagnosticsList);
        Class<T> newClass = compiled.get(qualifiedClassName);
        return castable(newClass, types);
    }

    /**
     * Compile multiple Java source strings and return a Map containing the
     * resulting classes.
     * <p/>
     * Thread safety: this method is thread safe if the <var>classes</var> and
     * <var>diagnosticsList</var> are isolated to this thread.
     *
     * @param classes         A Map whose keys are qualified class names and whose values are
     *                        the Java source strings containing the definition of the class.
     *                        A map value may be null, indicating that compiled class is
     *                        expected, although no source exists for it (it may be a
     *                        non-public class contained in one of the other strings.)
     * @param diagnosticsList Any diagnostics generated by compiling the source are added to
     *                        this list.
     * @return A mapping of qualified class names to their corresponding classes.
     *         The map has the same keys as the input <var>classes</var>; the
     *         values are the corresponding Class objects.
     * @throws CharSequenceCompilerException if the source cannot be compiled
     */
    public synchronized Map<String, Class<T>> compile(
            final Map<String, CharSequence> classes,
            final DiagnosticCollector<JavaFileObject> diagnosticsList)
            throws CharSequenceCompilerException {
        List<JavaFileObject> sources = new ArrayList<JavaFileObject>();
        for (Map.Entry<String, CharSequence> entry : classes.entrySet()) {
            String qualifiedClassName = entry.getKey();
            CharSequence javaSource = entry.getValue();
            if (javaSource != null) {
                final int dotPos = qualifiedClassName.lastIndexOf('.');
                final String className = dotPos == -1 ? qualifiedClassName
                        : qualifiedClassName.substring(dotPos + 1);
                final String packageName = dotPos == -1 ? "" : qualifiedClassName
                        .substring(0, dotPos);
                final JavaFileObjectImpl source = new JavaFileObjectImpl(className,
                        javaSource);
                sources.add(source);
                // Store the source file in the FileManager via package/class
                // name.
                // For source files, we add a .java extension
                javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, packageName,
                        className + JAVA_EXTENSION, source);
            }
        }
        // Get a CompliationTask from the compiler and compile the sources
        final JavaCompiler.CompilationTask task = compiler.getTask(null, javaFileManager, diagnostics,
                options, null, sources);
        final Boolean result = task.call();
        if (result == null || !result.booleanValue()) {
            String cause = "\n";
            for (Diagnostic d : diagnostics.getDiagnostics()) {
                cause += d + " ";

            }
            throw new CharSequenceCompilerException("Compilation failed. Causes: " + cause, classes
                    .keySet(), diagnostics);
        }
        try {
            // For each class name in the input map, get its compiled
            // class and put it in the output map
            Map<String, Class<T>> compiled = new HashMap<String, Class<T>>();
            for (String qualifiedClassName : classes.keySet()) {
                final Class<T> newClass = loadClass(qualifiedClassName);
                compiled.put(qualifiedClassName, newClass);
            }
            return compiled;
        } catch (ClassNotFoundException e) {
            throw new CharSequenceCompilerException(classes.keySet(), e, diagnostics);
        } catch (IllegalArgumentException e) {
            throw new CharSequenceCompilerException(classes.keySet(), e, diagnostics);
        } catch (SecurityException e) {
            throw new CharSequenceCompilerException(classes.keySet(), e, diagnostics);
        }
    }

    /**
     * Load a class that was generated by this instance or accessible from its
     * parent class loader. Use this method if you need access to additional
     * classes compiled by
     * {@link #compile(String, CharSequence, javax.tools.DiagnosticCollector, Class...) compile()},
     * for example if the primary class contained nested classes or additional
     * non-public classes.
     *
     * @param qualifiedClassName the name of the compiled class you wish to load
     * @return a Class instance named by <var>qualifiedClassName</var>
     * @throws ClassNotFoundException if no such class is found.
     */
    @SuppressWarnings("unchecked")
    public Class<T> loadClass(final String qualifiedClassName)
            throws ClassNotFoundException {
        return (Class<T>) classLoader.loadClass(qualifiedClassName);
    }

    /**
     * Check that the <var>newClass</var> is a subtype of all the type
     * parameters and throw a ClassCastException if not.
     *
     * @param types zero of more classes or interfaces that the <var>newClass</var>
     *              must be castable to.
     * @return <var>newClass</var> if it is castable to all the types
     * @throws ClassCastException if <var>newClass</var> is not castable to all the types.
     */
    private Class<T> castable(Class<T> newClass, Class<?>... types)
            throws ClassCastException {
        for (Class<?> type : types)
            if (!type.isAssignableFrom(newClass)) {
                throw new ClassCastException(type.getName());
            }
        return newClass;
    }

    /**
     * COnverts a String to a URI.
     *
     * @param name a file name
     * @return a URI
     */
    static URI toURI(String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return This compiler's class loader.
     */
    public ClassLoader getClassLoader() {
        return javaFileManager.getClassLoader();
    }
}

/**
 * A JavaFileObject which contains either the source text or the compiler
 * generated class. This class is used in two cases.
 * <ol>
 * <li>This instance uses it to store the source which is passed to the
 * compiler. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, CharSequence)}
 * constructor.
 * <li>The Java compiler also creates instances (indirectly through the
 * FileManagerImplFileManager) when it wants to create a JavaFileObject for the
 * .class output. This uses the
 * {@link JavaFileObjectImpl#JavaFileObjectImpl(String, javax.tools.JavaFileObject.Kind)}
 * constructor.
 * </ol>
 * This class does not attempt to reuse instances (there does not seem to be a
 * need, as it would require adding a Map for the purpose, and this would also
 * prevent garbage collection of class byte code.)
 */
final class JavaFileObjectImpl extends SimpleJavaFileObject {
    // If kind == CLASS, this stores byte code from openOutputStream
    private ByteArrayOutputStream byteCode;

    // if kind == SOURCE, this contains the source text
    private final CharSequence source;

    Class definedClass;

    /**
     * Construct a new instance which stores source
     *
     * @param baseName the base name
     * @param source   the source code
     */
    JavaFileObjectImpl(final String baseName, final CharSequence source) {
        super(CharSequenceCompiler.toURI(baseName + CharSequenceCompiler.JAVA_EXTENSION),
                Kind.SOURCE);
        this.source = source;
    }

    /**
     * Construct a new instance
     *
     * @param name the file name
     * @param kind the kind of file
     */
    JavaFileObjectImpl(final String name, final Kind kind) {
        super(CharSequenceCompiler.toURI(name), kind);
        source = null;
    }

    /**
     * Return the source code content
     *
     * @see javax.tools.SimpleJavaFileObject#getCharContent(boolean)
     */
    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors)
            throws UnsupportedOperationException {
        if (source == null)
            throw new UnsupportedOperationException("getCharContent()");
        return source;
    }

    /**
     * Return an input stream for reading the byte code
     *
     * @see javax.tools.SimpleJavaFileObject#openInputStream()
     */
    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(getByteCode());
    }

    /**
     * Return an output stream for writing the bytecode
     *
     * @see javax.tools.SimpleJavaFileObject#openOutputStream()
     */
    @Override
    public OutputStream openOutputStream() {
        byteCode = new ByteArrayOutputStream();
        return byteCode;
    }

    /**
     * @return the byte code generated by the compiler
     */
    byte[] getByteCode() {
        return byteCode.toByteArray();
    }
}
