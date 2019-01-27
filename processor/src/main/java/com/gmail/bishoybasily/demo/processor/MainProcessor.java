package com.gmail.bishoybasily.demo.processor;

import com.gmail.bishoybasily.demo.annotations.*;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.gmail.bishoybasily.demo.annotations.Aspect",
        "com.gmail.bishoybasily.demo.annotations.Provider"
})
public class MainProcessor extends AbstractProcessor {

    private static final String PACKAGE_NAME = "com.gmail.bishoybasily.demo.generated";

    private Predicate<Element> isClassPredicate = e -> e.getKind().equals(ElementKind.CLASS);
    private Predicate<Element> isBeanMethodPredicate = e -> e.getKind().equals(ElementKind.METHOD) && e.getAnnotation(Bean.class) != null;
    private Predicate<Element> isBeforeMethodPredicate = e -> e.getKind().equals(ElementKind.METHOD) && e.getAnnotation(Before.class) != null;
    private Predicate<Element> isAfterMethodPredicate = e -> e.getKind().equals(ElementKind.METHOD) && e.getAnnotation(After.class) != null;

    private Class<Provider> providerClass = Provider.class;
    private Class<Aspect> aspectClass = Aspect.class;

    private Filer filer;
    private Types types;
    private Elements elements;
    private Messager messager;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public final synchronized void init(ProcessingEnvironment environment) {
        this.filer = environment.getFiler();
        this.messager = environment.getMessager();
        this.elements = environment.getElementUtils();
        this.types = environment.getTypeUtils();
    }

    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        TypeSpec.Builder graphClass = TypeSpec.classBuilder("Graph")
                .addModifiers(Modifier.PUBLIC);

        FieldSpec.Builder instanceField = FieldSpec.builder(CLASS("Graph"), "instance")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);
        graphClass.addField(instanceField.build());

        MethodSpec.Builder graphConstructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        graphClass.addMethod(graphConstructor.build());

        MethodSpec.Builder graphGetInstanceFunction = MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("if (instance == null) { synchronized (Graph.class) { if (instance == null) { instance = new Graph(); } } } return instance")
                .returns(CLASS("Graph"));
        graphClass.addMethod(graphGetInstanceFunction.build());

        Stream
                .concat(
                        getTypeElementStream(roundEnv, isClassPredicate, providerClass),
                        getTypeElementStream(roundEnv, isClassPredicate, aspectClass)
                )
                .forEach(typeElement -> {

                    String name = typeElement.getSimpleName().toString();
                    String fieldName = lowerFirstLetter(name);
                    TypeName type = ClassName.get(typeElement.asType());

                    defineField(fieldName, type, graphClass);
                    defineFieldInitializerConstructorMethod(name, fieldName, type, graphClass);

                });

        getTypeElementStream(roundEnv, isClassPredicate, providerClass)
                .forEach(typeElement -> {

                    String providerName = typeElement.getSimpleName().toString();
                    String providerFieldName = lowerFirstLetter(providerName);
                    TypeName providerType = ClassName.get(typeElement.asType());

                    getExecutableElementStream(typeElement, isBeanMethodPredicate)
                            .forEach(new Consumer<ExecutableElement>() {
                                @Override
                                public void accept(ExecutableElement methElem) {

                                    String beanName = methElem.getSimpleName().toString();
                                    String beanFieldName = lowerFirstLetter(beanName);
                                    String beanClassName = upperFirstLetter(beanName);
                                    TypeName beanType = ClassName.get(methElem.getReturnType());

                                    defineField(beanFieldName, beanType, graphClass);

                                    Stream<Advice> beforeStream = getTypeElementStream(roundEnv, isClassPredicate, aspectClass)
                                            .flatMap(typeElement -> {
                                                return getExecutableElementStream(typeElement, isBeforeMethodPredicate)
                                                        .map(executableElement -> new Advice(Type.BEFORE, executableElement.getAnnotation(Before.class).value(), typeElement, executableElement));
                                            });

                                    Stream<Advice> afterStream = getTypeElementStream(roundEnv, isClassPredicate, aspectClass)
                                            .flatMap(typeElement -> {
                                                return getExecutableElementStream(typeElement, isAfterMethodPredicate)
                                                        .map(executableElement -> new Advice(Type.AFTER, executableElement.getAnnotation(After.class).value(), typeElement, executableElement));
                                            });

                                    Map<String, Set<Advice>> advicesMap = new HashMap<>();

                                    Stream.concat(beforeStream, afterStream).forEach(new Consumer<Advice>() {
                                        @Override
                                        public void accept(Advice advice) {
                                            Set<Advice> adviceList = advicesMap.getOrDefault(advice.value, new HashSet<>());
                                            adviceList.add(advice);
                                            advicesMap.put(advice.value, adviceList);

                                        }
                                    });

                                    if (advicesMap.isEmpty()) {

                                        defineFieldInitializerGetterMethod(beanName, providerFieldName, beanType, graphClass);

                                    } else {


                                        StringBuffer buffer = new StringBuffer();

                                        buffer.append("if ( this." + beanFieldName + " == null ) {").append("\n");
                                        buffer.append("    net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();").append("\n");
                                        buffer.append("    enhancer.setSuperclass(" + beanClassName + ".class);").append("\n");
                                        buffer.append("    enhancer.setCallback(new net.sf.cglib.proxy.MethodInterceptor() {").append("\n");
                                        buffer.append("        @Override").append("\n");
                                        buffer.append("        public Object intercept(Object o, java.lang.reflect.Method method, Object[] objects, net.sf.cglib.proxy.MethodProxy methodProxy) throws Throwable {").append("\n");

                                        advicesMap.entrySet().forEach(new Consumer<Map.Entry<String, Set<Advice>>>() {
                                            @Override
                                            public void accept(Map.Entry<String, Set<Advice>> entry) {

                                                buffer.append("            if (method.toString().equals(\"" + entry.getValue().iterator().next().value + "\")) {").append("\n");

                                                entry.getValue().stream().filter(a -> a.type.equals(Type.BEFORE)).forEach(advice -> {

                                                    String typeName = lowerFirstLetter(advice.typeElement.getSimpleName().toString());
                                                    String executableName = lowerFirstLetter(advice.executableElement.getSimpleName().toString());
                                                    String statement = "                " + typeName + "()." + executableName + "();";
                                                    buffer.append(statement).append("\n");

                                                });

                                                buffer.append("                Object result = methodProxy.invokeSuper(o, objects);").append("\n");

                                                entry.getValue().stream().filter(a -> a.type.equals(Type.AFTER)).forEach(advice -> {

                                                    String typeName = lowerFirstLetter(advice.typeElement.getSimpleName().toString());
                                                    String executableName = lowerFirstLetter(advice.executableElement.getSimpleName().toString());
                                                    String statement = "                " + typeName + "()." + executableName + "();";
                                                    buffer.append(statement).append("\n");

                                                });

                                                buffer.append("                return result;").append("\n");
                                                buffer.append("            }").append("\n");


                                            }
                                        });

                                        buffer.append("            return methodProxy.invokeSuper(o, objects);").append("\n");
                                        buffer.append("        }").append("\n");
                                        buffer.append("    });").append("\n");
                                        buffer.append("    this." + beanFieldName + " = (" + beanClassName + ") enhancer.create();").append("\n");
                                        buffer.append("}").append("\n");

                                        MethodSpec.Builder beanFunction = MethodSpec.methodBuilder(beanFieldName)
                                                .addModifiers(Modifier.PUBLIC)

                                                .addCode(buffer.toString())

                                                .addStatement("return this." + beanFieldName)

                                                .returns(beanType);
                                        graphClass.addMethod(beanFunction.build());

                                    }


                                }

                            });

                });

        flush(PACKAGE_NAME, graphClass.build());

        return false;
    }

    private void defineField(String providerFieldName, TypeName providerType, TypeSpec.Builder graphClass) {
        FieldSpec.Builder providerField = FieldSpec.builder(providerType, providerFieldName)
                .addModifiers(Modifier.PRIVATE);
        graphClass.addField(providerField.build());
    }

    private void defineFieldInitializerConstructorMethod(String name, String fieldName, TypeName type, TypeSpec.Builder graphClass) {
        MethodSpec.Builder providerFunction = MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("if ( this." + fieldName + " == null ) this." + fieldName + " = new " + name + "()")
                .addStatement("return this." + fieldName)
                .returns(type);
        graphClass.addMethod(providerFunction.build());
    }

    private void defineFieldInitializerGetterMethod(String fieldName, String providerName, TypeName type, TypeSpec.Builder graphClass) {
        MethodSpec.Builder providerFunction = MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("if ( this." + providerName + " == null ) this." + providerName + "()." + fieldName + "()")
                .addStatement("return this." + fieldName)
                .returns(type);
        graphClass.addMethod(providerFunction.build());
    }

    private Stream<ExecutableElement> getExecutableElementStream(TypeElement clasElem, Predicate<Element> isBeanMethodPredicate) {
        return clasElem.getEnclosedElements()
                .stream()
                .filter(isBeanMethodPredicate)
                .map(ExecutableElement.class::cast);
    }

    private Stream<TypeElement> getTypeElementStream(RoundEnvironment roundEnv, Predicate<Element> isClassPredicate, Class<? extends Annotation> aspectClass) {
        return roundEnv.getElementsAnnotatedWith(aspectClass)
                .stream()
                .filter(isClassPredicate)
                .map(TypeElement.class::cast);
    }

    private String upperFirstLetter(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private String lowerFirstLetter(String input) {
        return input.substring(0, 1).toLowerCase() + input.substring(1);
    }

    private void flush(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TypeName CLASS(TypeMirror typeMirror) {
        return ClassName.get(typeMirror);
    }

    public TypeName CLASS(String pkg, String nam) {
        return ClassName.get(pkg, nam);
    }

    public TypeName CLASS(Class<?> cls) {
        return CLASS(cls.getPackage().getName(), cls.getSimpleName());
    }

    public TypeName CLASS(String name) {
        return ClassName.bestGuess(name);
    }

    enum Type {
        BEFORE,
        AFTER
    }

    @Data
    @AllArgsConstructor
    @EqualsAndHashCode(of = {"type", "value"})
    class Advice {

        private Type type;
        private String value;
        private TypeElement typeElement;
        private ExecutableElement executableElement;

    }

}

