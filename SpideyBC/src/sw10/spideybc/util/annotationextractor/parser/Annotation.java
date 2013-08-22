package sw10.spideybc.util.annotationextractor.parser;

public class Annotation {
		
	private final AnnotationType annoType;
	private final String annoValue;
	
	public static enum AnnotationType {
		LOOPBOUND,
		LENGTH
	}
	
	public Annotation(AnnotationType annoType, String annoValue) {
		this.annoType = annoType;
		this.annoValue = annoValue;
	}
	
	public AnnotationType getAnnotationType() {
		return this.annoType;
	}
	public String getAnnotationValue() {
		return this.annoValue;
	}
	
}
