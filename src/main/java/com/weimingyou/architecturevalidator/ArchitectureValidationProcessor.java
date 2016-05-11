package com.weimingyou.architecturevalidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes({ "com.weimingyou.architecturevalidator.ArchitectureValidation" })
public class ArchitectureValidationProcessor extends AbstractProcessor {
	@SuppressWarnings("unused")
	private Types typeUtils;
	@SuppressWarnings("unused")
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Element element = null;
		try {
			for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(ArchitectureValidation.class)) {
				element = annotatedElement;
				
				if (annotatedElement.getKind() != ElementKind.CLASS) {
					throw new ValidationException("Only class can be annotated with @%s",
							ArchitectureValidation.class.getSimpleName());
				}
				
				Map<String, List<String>> disallows = getDisallows(annotatedElement);

				String srcDirectory = getSourcePath(annotatedElement);
				if (2>1) throw new ValidationException("srcDirectory = " + srcDirectory);
				
				SourceFileValidation validation = new SourceFileValidation(disallows, srcDirectory, getQualifiedName(annotatedElement));
				
				if (validation.validate()) {
					messager.printMessage(Kind.NOTE, "SourceFileValidation succeeded.", annotatedElement);
				} else {
					throw new ValidationException("Architecture validation failed with %s", validation.getFailedMessages());
				}
		        
		        return true;
			}
		} catch (ValidationException e) {
			error(element, e.getMessage());
		} catch (IOException e) {
			error(element, e.getMessage());
		}

		return true;
	}

	private String getQualifiedName(Element annotatedElement) {
		return annotatedElement.toString();
	}

	private Map<String, List<String>> getDisallows(Element annotatedElement) throws ValidationException {
		TypeElement typeElement = (TypeElement) annotatedElement;
		ArchitectureValidation architectureValidation = typeElement.getAnnotation(ArchitectureValidation.class);

		String[] disallows = architectureValidation.disallow();
		int length = disallows.length;
		Map<String, List<String>> resultMap = new HashMap<String, List<String>>();
		
		if (length > 0) {
			for (int i = 0; i < length; i++) {
				String disallow = disallows[i];
				
				String separator = ":";
				if (disallow.indexOf(separator) < 0 || disallow.startsWith(separator) || disallow.endsWith(separator)) {
					throw new ValidationException("Format in disallow should be like :\"com.foo:com.bar.*\"");
				}
				
				String[] splits = disallow.split(separator);
				
				if (splits.length != 2) {
					throw new ValidationException("Only one : is allowed in each disallow String");
				}
				
				List<String> disallowedStuff = null;
				String toCheck = splits[0];
				
				if (resultMap.containsKey(toCheck)) {
					disallowedStuff = resultMap.get(toCheck);
				} else {
					disallowedStuff = new ArrayList<String>();
					resultMap.put(toCheck, disallowedStuff);
				}
				
				disallowedStuff.add(splits[1]);
			}
		}
		
		return resultMap;
	}

	private String getSourcePath(Element annotatedElement) throws IOException {
		TypeElement typeElement = (TypeElement) annotatedElement;
		
		String itsPackage = ((PackageElement)typeElement.getEnclosingElement()).getQualifiedName().toString();
		String simpleName = typeElement.getSimpleName().toString() + ".java";
		
		/*try {
			FileObject resource = filer.getResource(StandardLocation.SOURCE_PATH, itsPackage, simpleName);
			resource.openInputStream().close();
            return resource.toUri().getPath();
        } catch (FileNotFoundException e) {
            //messager.printMessage(Kind.ERROR, "could not read: " + e.getMessage());
            //e.printStackTrace();
        }*/
		
		itsPackage = itsPackage.replaceAll("\\.", "/");
        File file = new File(itsPackage + "/" + simpleName);
		
		//return itsPackage + ":" + simpleName;
		return file.getAbsolutePath();
		
		//return itsPackage + ":" + simpleName;
		
		/*
		String anyName = "anything";
		String blank = "";
		String elipseSourceOutput1 = ".apt_generated\\";
		String elipseSourceOutput2 = ".apt_generated/";
		String mavenSourceOutput1 = "target\\generated-sources\\annotations\\";
		String mavenSourceOutput2 = "target/generated-sources/annotations/";
		String sourcePath = "src/";
		String mavenSourcePath = "src/main/java/";*/
		
		
		
		
		
		/*JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics =
				new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends File> iter = fm.getLocation(StandardLocation.SOURCE_PATH);
		if (null == iter) return "null == iter";
		String output = "";
		for (File f : iter) {
			output += f.getAbsolutePath();
		}
		if (2>1) return output;*/
		
		/*FileObject fileObject = filer.getResource(StandardLocation.SOURCE_OUTPUT, blank,anyName);//SOURCE_PATH not working
		String rootPath = fileObject.toUri().getPath().replace(anyName, blank);//file:/C:/my_temp/workspace/test/src/anything
		rootPath = rootPath.substring(0, rootPath.length() - 1);
		
		if (rootPath.endsWith(elipseSourceOutput1)) {
			rootPath = rootPath.replace(elipseSourceOutput1, blank);
			rootPath += sourcePath;
		} else if (rootPath.endsWith(elipseSourceOutput2)) {
			rootPath = rootPath.replace(elipseSourceOutput2, blank);
			rootPath += sourcePath;
		} else if (rootPath.endsWith(mavenSourceOutput1)) {
			rootPath = rootPath.replace(mavenSourceOutput1, blank);
			rootPath += mavenSourcePath;
		} else if (rootPath.endsWith(mavenSourceOutput2)) {
			rootPath = rootPath.replace(mavenSourceOutput2, blank);
			rootPath += mavenSourcePath;
		}
		
		return rootPath;*/
	}

	/**
	 * Prints an error message
	 *
	 * @param e
	 *            The element which has caused the error. Can be null
	 * @param msg
	 *            The error message
	 */
	public void error(Element e, String msg) {
		messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
	}

}
