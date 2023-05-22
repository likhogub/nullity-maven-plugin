package ru.likhogub.nullity;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractInstrumentationMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;
    protected List<Path> paths;
    protected ClassPool classPool;

    abstract byte[] transform(CtClass ctClass) throws Exception;

    @Override
    public void execute() throws MojoExecutionException {
        classPool = getClassPool();
        paths = getPaths();
        int transformedClasses = 0;
        try {
            for (Path path : paths) {
                if (processPath(path)) {
                    transformedClasses++;
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
        getLog().info("Transformed " + transformedClasses + " classes");
    }

    protected ClassPool getClassPool() {
        return ClassPool.getDefault();
    }

    protected List<Path> getPaths() throws MojoExecutionException {
        try (Stream<Path> pathStream = Files.find(Paths.get(project.getBuild().getOutputDirectory()), Integer.MAX_VALUE, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile())) {
            return pathStream.collect(Collectors.toList());
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    protected boolean processPath(Path path) {
        CtClass ctClass;
        try (InputStream inputStream = Files.newInputStream(path)) {
            ctClass = classPool.makeClass(inputStream, false);
            if (ctClass.isFrozen()) {
                ctClass.defrost();
            }
            byte[] bytes = transform(ctClass);
            if (bytes == null) {
                return false;
            }
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                outputStream.write(ctClass.toBytecode());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
