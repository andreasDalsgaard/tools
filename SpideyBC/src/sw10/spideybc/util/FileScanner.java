package sw10.spideybc.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import sw10.spideybc.program.AnalysisSpecification;

public class FileScanner {
	
	private static Map<String, File> mapping = new HashMap<String, File>(); 

	public static Map<String, File> scan(File root) {
		return scan(root, "");
	}
	
	private static Map<String, File> scan(File root, String accumPath) {
		File[] files = root.listFiles();
		for(File file : files) {
			String filename = file.getName();
			if(filename.endsWith(".java")) {
				mapping.put(accumPath + filename.substring(0, filename.indexOf('.')), file);
			} else if(file.isDirectory()) {
				scan(file, accumPath + file.getName() + "/");
			}
		}
		return mapping;
	}
	
	/* Relative path from source files root directory
	 * 
	 * Args: 	exampleFile.java
	 * Returns: examplePackage/exampleFile.java 
	 */
	public static String getRelativePath(String fileName) {	
	  
		fileName = fileName.substring(0, fileName.indexOf('.'));
		String fullPath = "";
		for(String path : mapping.keySet()) {
			if(path.endsWith(fileName)) {
				fullPath = path + ".java";
				break;
			}
		}
		return fullPath;
	}
	
	/* Full path on file system
	 * 
	 * Args:	exampleFile.java
	 * Returns: /Users/../exampleFile.java 
	 */
	public static String getFullPath(String fileName) {
		String path = AnalysisSpecification.getAnalysisSpecification().getSourceFilesRootDir();		
		return path + File.separatorChar + getRelativePath(fileName);
	}
}

