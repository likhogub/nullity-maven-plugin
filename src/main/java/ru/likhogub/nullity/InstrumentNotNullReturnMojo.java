package ru.likhogub.nullity;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.HashSet;
import java.util.Set;

@Mojo(name = "instrument-not-null-return", defaultPhase = LifecyclePhase.COMPILE)
public class InstrumentNotNullReturnMojo extends AbstractInstrumentationMojo {

    protected final Set<Class<?>> notNullClasses = new HashSet<>();

    @Parameter(property = "notNulls", required = true, readonly = true)
    protected String[] notNulls;

    @Override
    public void execute() throws MojoExecutionException {
        prepareNotNullClasses();
        super.execute();
    }

    @Override
    byte[] transform(CtClass ctClass) throws Exception {
        int s = 0;
        for (CtMethod declaredMethod : ctClass.getDeclaredMethods()) {
            s += processCtMethod(declaredMethod);
        }
        return s == 0 ? null : ctClass.toBytecode();
    }

    private void prepareNotNullClasses() throws MojoExecutionException {
        try {
            for (String notNull : notNulls) {
                notNullClasses.add(getClass().getClassLoader().loadClass(notNull));
            }
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException(e);
        }
    }

    private int processCtMethod(CtMethod ctMethod) throws CannotCompileException {
        if (hasNotNullAnnotation(ctMethod)) {
            instrumentCtMethod(ctMethod);
            return 1;
        }
        return 0;
    }

    private boolean hasNotNullAnnotation(CtMethod ctMethod) {
        for (Class<?> notNullClass : notNullClasses) {
            if (ctMethod.hasAnnotation(notNullClass)) {
                return true;
            }
        }
        return false;
    }

    private void instrumentCtMethod(CtMethod ctMethod) throws CannotCompileException {
        String errorMessage = String.format("@NotNull method %s must not return null", ctMethod.getLongName());
        String src = String.format("if ( $_ == null) { throw new IllegalStateException(\"%s \"); }", errorMessage);
        ctMethod.insertAfter(src);
    }
}
