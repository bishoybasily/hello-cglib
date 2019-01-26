package com.gmail.bishoybasily.demo.processor;

import com.gmail.bishoybasily.demo.annotations.Aspect;
import com.gmail.bishoybasily.demo.annotations.Bean;
import com.gmail.bishoybasily.demo.annotations.Provider;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "com.gmail.bishoybasily.demo.annotations.Aspect",
        "com.gmail.bishoybasily.demo.annotations.After",
        "com.gmail.bishoybasily.demo.annotations.Before",
        "com.gmail.bishoybasily.demo.annotations.Provider",
        "com.gmail.bishoybasily.demo.annotations.Bean"
})
public class MainProcessor extends AbstractProcessor {

    private static final String PACKAGE_NAME = "com.gmail.bishoybasily.demo.generated";
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

        roundEnv.getElementsAnnotatedWith(Provider.class)
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.CLASS))
                .map(TypeElement.class::cast)
                .forEach(new Consumer<TypeElement>() {
                    @Override
                    public void accept(TypeElement clasElem) {

                        String providerName = clasElem.getSimpleName().toString();
                        String providerFieldName = lowerFirstLetter(providerName);
                        TypeName providerType = ClassName.get(clasElem.asType());

                        // graph fields (configurations)
                        FieldSpec.Builder providerField = FieldSpec.builder(providerType, providerFieldName)
                                .addModifiers(Modifier.PRIVATE);
                        graphClass.addField(providerField.build());

                        MethodSpec.Builder providerFunction = MethodSpec.methodBuilder(providerFieldName)
                                .addModifiers(Modifier.PUBLIC)
                                .addStatement("if ( this." + providerFieldName + " == null ) this." + providerFieldName + " = new " + providerName + "()")
                                .addStatement("return this." + providerFieldName)
                                .returns(providerType);
                        graphClass.addMethod(providerFunction.build());

                        clasElem.getEnclosedElements()
                                .stream()
                                .filter(e -> e.getKind().equals(ElementKind.METHOD) && e.getAnnotation(Bean.class) != null)
                                .map(ExecutableElement.class::cast)
                                .forEach(new Consumer<ExecutableElement>() {
                                    @Override
                                    public void accept(ExecutableElement methElem) {

                                        String beanName = methElem.getSimpleName().toString();
                                        String beanFieldName = lowerFirstLetter(beanName);
                                        TypeName beanType = ClassName.get(methElem.getReturnType());

                                        FieldSpec.Builder beanField = FieldSpec.builder(beanType, beanFieldName)
                                                .addModifiers(Modifier.PRIVATE);
                                        graphClass.addField(beanField.build());
//
                                        MethodSpec.Builder beanFunction = MethodSpec.methodBuilder(beanFieldName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addStatement("return " + providerFieldName + "()." + beanName + "()")
                                                .returns(beanType);
                                        graphClass.addMethod(beanFunction.build());

                                    }

                                });

                    }
                });

        List<TypeElement> aspects = roundEnv.getElementsAnnotatedWith(Aspect.class)
                .stream()
                .filter(e -> e.getKind().equals(ElementKind.CLASS))
                .map(TypeElement.class::cast)
                .collect(Collectors.toList());

        flush(PACKAGE_NAME, graphClass.build());

        return false;
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

}
