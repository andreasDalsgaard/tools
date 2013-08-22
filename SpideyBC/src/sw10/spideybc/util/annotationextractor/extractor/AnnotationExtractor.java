package sw10.spideybc.util.annotationextractor.extractor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.util.Util;
import sw10.spideybc.util.annotationextractor.file.FileFinder;
import sw10.spideybc.util.annotationextractor.parser.Annotation;
import sw10.spideybc.util.annotationextractor.parser.Parser;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

public class AnnotationExtractor {

	private static AnnotationExtractor singletonObject;
	
	private AnalysisSpecification specification;
	private Map<String, Map<Integer, Annotation>> cachedAnnotations;

	private AnnotationExtractor() {
		this.cachedAnnotations = new HashMap<String, Map<Integer,Annotation>>();
		this.specification = AnalysisSpecification.getAnalysisSpecification();
	}
	
	public static synchronized AnnotationExtractor getAnnotationExtractor() {
		if (singletonObject == null) {
			singletonObject = new AnnotationExtractor();
		}
		return singletonObject;
	}
	
	private Map<Integer, Annotation> retrieveAnnotations(String path, String file) throws IOException {
		String fileKey = path + file;
		if (cachedAnnotations.containsKey(fileKey)) {
			return cachedAnnotations.get(fileKey);
		}
		
		FileFinder fileFinder = new FileFinder(path);
		Parser parser = new Parser();
		BufferedReader fileReader = null;

		try {
			fileReader = fileFinder.find(file);
		}catch(FileNotFoundException e) {
			return null;
		} catch(NullPointerException e) {
			return null;
		}
		
		Map<Integer, Annotation> annotations = parser.GetAnnotations(fileReader);
		cachedAnnotations.put(fileKey, annotations);
		
		return annotations;
	}
	
	public Map<Integer, Annotation> getAnnotations(IMethod method) {
		String packageName = method.getDeclaringClass().getName().toString();
		packageName = Util.getClassNameOrOuterMostClassNameIfNestedClass(packageName);
		packageName = (packageName.contains("/") ? packageName.substring(1, packageName.lastIndexOf('/')) : "");

		String path = specification.getSourceFilesRootDir() + '/';
		path = (packageName.isEmpty() ? path : path + packageName + '/');

		String sourceFileName = method.getDeclaringClass().getSourceFileName();
		Map<Integer, Annotation> annotationsForMethod = null;
		try {
			annotationsForMethod = retrieveAnnotations(path, sourceFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return annotationsForMethod;
	}
}


