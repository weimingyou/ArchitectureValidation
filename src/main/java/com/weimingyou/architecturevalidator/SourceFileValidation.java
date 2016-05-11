package com.weimingyou.architecturevalidator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SourceFileValidation {
	private Map<String, List<String>> disallows;
	private String srcDirectory;
	private String itself;
	private List<String> failedMessages = new ArrayList<String>();
	
	public SourceFileValidation(Map<String, List<String>> disallows, String srcDirectory, String itself) {
		this.disallows = disallows;
		this.srcDirectory = srcDirectory;
		this.itself = itself;
	}
	
	boolean validate() throws ValidationException {
		loopDirectory(this.srcDirectory, "");
		
		return this.failedMessages.isEmpty();
	}

	List<String> getFailedMessages() {
		return failedMessages;
	}
	
	private void loopDirectory(String directoryName, String currentPackage) throws ValidationException {
	    File directory = new File(directoryName);
	    
	    for (File file : directory.listFiles()) {
	        if (file.isFile() && file.getName().endsWith(".java")) {
	            checkFile(file, currentPackage);
	        } else if (file.isDirectory()) {
	        	loopDirectory(file.getAbsolutePath(), currentPackage.equals("") ? file.getName() : currentPackage + "." + file.getName());
	        }
	    }
	}

	private void checkFile(File file, String packageName) throws ValidationException {
		if (packageName.equals("")) {
			this.failedMessages.add("Put Java code at default package is discouraged.");
			return;
		}
		
		String qualifiedName = packageName + "." + file.getName().replace(".java", "");
		
		if (qualifiedName.equals(this.itself)) return;//skip itself
		
		List<String> disallowedPattern = this.disallows.get(packageName);
		
		if (null == disallowedPattern || disallowedPattern.isEmpty()) return;
		
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(file));
			
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				for (String pattern : disallowedPattern) {
					line = line.trim();
					
					if (line.equals("")) break;
					
					if (line.startsWith("//") || line.startsWith("/*")) break;
					
					if (line.contains(pattern)) {
						this.failedMessages.add(qualifiedName + " should not use " + pattern);
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new ValidationException(e.getMessage());
		} catch (IOException e) {
			throw new ValidationException(e.getMessage());
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}
}
