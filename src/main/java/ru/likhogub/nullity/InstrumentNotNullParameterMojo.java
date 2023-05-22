package ru.likhogub.nullity;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Arrays;

@Mojo(name = "instrument-not-null-parameter", defaultPhase = LifecyclePhase.COMPILE)
public class InstrumentNotNullParameterMojo extends AbstractInstrumentationMojo {

    @Parameter(property = "notNulls", required = true, readonly = true)
    protected String[] notNulls;

    @Override
    byte[] transform(CtClass ctClass) throws Exception {
        int s = 0;
        for (CtMethod declaredMethod : ctClass.getDeclaredMethods()) {
            s += processCtMethod(declaredMethod);
        }
        return s == 0 ? null : ctClass.toBytecode();
    }

    private int processCtMethod(CtMethod ctMethod) throws CannotCompileException, NotFoundException {
        Object[][] parameterAnnotations = ctMethod.getAvailableParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (hasNotNullAnnotation(parameterAnnotations[i])) {
                CtClass parameterType = ctMethod.getParameterTypes()[i];
                instrumentCtMethod(ctMethod, parameterType, i);
                return 1;
            }
        }
        return 0;
    }

    private boolean hasNotNullAnnotation(Object[] parameterAnnotations) {
        for (Object parameterAnnotation : parameterAnnotations) {
            String annotationClassName = parameterAnnotation.toString().substring(1);
            if (Arrays.asList(notNulls).contains(annotationClassName)) {
                return true;
            }
        }
        return false;
    }

    private void instrumentCtMethod(CtMethod ctMethod, CtClass parameterCtClass, int parameterIdx) throws CannotCompileException {
        String errorMessage = String.format("Argument for @Nonnull parameter %s[%d] of %s must not be null", parameterCtClass.getName(), parameterIdx, ctMethod.getLongName());
        String src = String.format("if ( $%d == null) { throw new IllegalArgumentException(\"%s\"); }", parameterIdx + 1, errorMessage);
        ctMethod.insertBefore(src);
    }
}
