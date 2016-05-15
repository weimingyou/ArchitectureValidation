package com.weimingyou.architecturevalidator;

import java.io.File;
import java.io.IOException;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public class SourcePathExtractor {
	private static final String BLANK = "";
	private static final String ECLIPSE_DEFAULT_SOURCE_OUTPUT1 = ".apt_generated/";
	private static final String ECLIPSE_DEFAULT_SOURCE_OUTPUT2 = ".apt_generated\\";
	private static final String MAVEN_DEFAULT_SOURCE_OUTPUT1 = "target\\generated-sources\\annotations\\";
	private static final String MAVEN_DEFAULT_SOURCE_OUTPUT2 = "target/generated-sources/annotations/";
	
	@SuppressWarnings("unused")
	private Types typeUtils;
	@SuppressWarnings("unused")
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	
	public SourcePathExtractor(Types typeUtils, Elements elementUtils, Filer filer, Messager messager) {
		this.typeUtils = typeUtils;
		this.elementUtils = elementUtils;
		this.filer = filer;
		this.messager = messager;
	}
	
	public String getSourcePath(Element annotatedElement) throws ValidationException {//throws IOException {
		TypeElement typeElement = (TypeElement) annotatedElement;
		String itsPackage = ((PackageElement)typeElement.getEnclosingElement()).getQualifiedName().toString();
		String fileName = typeElement.getSimpleName().toString() + ".java";		
		
		try {
			return getFromSOURCE_PATH(itsPackage, fileName);//works from command line when -sourcepath is provided.
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			return getFromCurrentPath(itsPackage, fileName);//works from command line when -sourcepath is not provided.
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			return getFromSourceOutput(itsPackage, fileName);//works from Eclipse when "Generated source directory" is set to .apt_generated by default.
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return null;
	}

	/**
	 * Command line: only works when you pass -sourcepath, if not it will throw FileNotFoundException
	 * Eclipse: throws Exception saying Unsupported Location.
	 * Maven: //TODO to test
	 * 
	 * @return sourcepath something like "/C:/my_temp/workspace/test/src/"
	 * @throws IOException
	 */
	private String getFromSOURCE_PATH(String itsPackage, String fileName) throws IOException {
		FileObject fileObject = filer.getResource(StandardLocation.SOURCE_PATH, itsPackage, fileName);
		String absolutePath = fileObject.toUri().getPath();
		String ralativePathname = itsPackage.replaceAll("\\.", "/") + "/" + fileName;
		
		return abstraceSourceRootPath(ralativePathname, absolutePath);
	}
	
	/**
	 * Command line: works when you no -sourcepath provided.
	 * Eclipse: shows wrong path where file not actually exist.
	 * Maven: //TODO to test
	 * 
	 * @return sourcepath something like "C:\my_temp\workspace\test\src\"
	 * @throws RuntimeException
	 */
	private String getFromCurrentPath(String itsPackage, String fileName) {
        String ralativePathname = itsPackage.replaceAll("\\.", "/") + "/" + fileName;
		File file = new File(ralativePathname);
        
        if (!file.exists()) throw new RuntimeException("File not exist on current path"); 
        
        String absolutePath = file.getAbsolutePath();
        
        return abstraceSourceRootPath(ralativePathname, absolutePath);
	}

	private String abstraceSourceRootPath(String ralativePathname, String absolutePath) {
		if (absolutePath.endsWith(ralativePathname))
	        return absolutePath.replace(ralativePathname, BLANK);
        else {
        	String ralativePathname2 = ralativePathname.replace('/', '\\');
        	
        	if (absolutePath.endsWith(ralativePathname2))
    	        return absolutePath.replace(ralativePathname2, BLANK);
        	else return absolutePath;
        }
	}
	
	/**
	 * Command line: //TODO to test
	 * Eclipse: works when "Generated source directory" is set to .apt_generated by default.
	 * Maven: //TODO to test
	 * 
	 * @return sourcepath something like "C:\my_temp\workspace\test\src/"
	 * @throws RuntimeException
	 */
	private String getFromSourceOutput(String itsPackage, String fileName) throws IOException {
		FileObject fileObject = filer.getResource(StandardLocation.SOURCE_OUTPUT, itsPackage, fileName);
		
		String absolutePath = fileObject.toUri().getPath();
		String ralativePathname = itsPackage.replaceAll("\\.", "/") + "/" + fileName;
		
		String sourceOutputPath = abstraceSourceRootPath(ralativePathname, absolutePath);
		itsPackage = itsPackage.replaceAll("\\.", "/");
        
		if (sourceOutputPath.endsWith(ECLIPSE_DEFAULT_SOURCE_OUTPUT1) || sourceOutputPath.endsWith(ECLIPSE_DEFAULT_SOURCE_OUTPUT2)) {
			String projectRoot = sourceOutputPath.substring(0, sourceOutputPath.length() - ECLIPSE_DEFAULT_SOURCE_OUTPUT1.length());
			
			return getSourcePathFromProjectRoot(itsPackage, fileName, projectRoot);
		} else if (sourceOutputPath.endsWith(MAVEN_DEFAULT_SOURCE_OUTPUT1) || sourceOutputPath.endsWith(MAVEN_DEFAULT_SOURCE_OUTPUT2)) {
			String projectRoot = sourceOutputPath.substring(0, sourceOutputPath.length() - MAVEN_DEFAULT_SOURCE_OUTPUT1.length());
			
			return getSourcePathFromProjectRoot(itsPackage, fileName, projectRoot);
		}

		throw new RuntimeException("File not found");
	}

	private String getSourcePathFromProjectRoot(String itsPackage, String fileName, String projectRoot) {
		File root = new File(projectRoot);
		
		for (File subDirectory : root.listFiles()) {
		    if (subDirectory.isDirectory()) {//src
		    	File file = new File(subDirectory.getAbsolutePath() + "/" + itsPackage + "/" + fileName);
		        if (file.exists()) {
		        	return subDirectory.getAbsolutePath() + "/";
		        } else {
		        	for (File sub2Directory : subDirectory.listFiles()) {
				        if (sub2Directory.isDirectory()) {//main
				        	File file2 = new File(sub2Directory.getAbsolutePath() + "/" + itsPackage + "/" + fileName);
				            if (file2.exists()) {
				            	return sub2Directory.getAbsolutePath() + "/";
				            } else {
					        	for (File sub3Directory : sub2Directory.listFiles()) {
							        if (sub3Directory.isDirectory()) {//java
							        	File file3 = new File(sub3Directory.getAbsolutePath() + "/" + itsPackage + "/" + fileName);
							            if (file3.exists()) return sub3Directory.getAbsolutePath() + "/";
							        }
							    }
					        }
				        }
				    }
		        }
		    }
		}
		
		throw new RuntimeException("File not found");
	}
	
	public void error(Element e, String msg) {
		messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
	}
}