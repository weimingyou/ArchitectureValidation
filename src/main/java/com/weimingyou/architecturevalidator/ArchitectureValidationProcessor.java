package com.weimingyou.architecturevalidator;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

@SupportedAnnotationTypes({ "com.weimingyou.architecturevalidator.ArchitectureValidation" })
public class ArchitectureValidationProcessor extends AbstractProcessor {
	private Types typeUtils;
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	
	private SourcePathExtractor sourcePathExtractor;
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
		
		this.sourcePathExtractor = new SourcePathExtractor(typeUtils, elementUtils, filer, messager);
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

				String srcDirectory = this.sourcePathExtractor.getSourcePath(annotatedElement);
				//TODO delete the following line line.
				//if (2>1) throw new ValidationException("srcDirectory = " + srcDirectory);
				
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
		}/* catch (IOException e) {
			error(element, e.getMessage());
		}*/

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
