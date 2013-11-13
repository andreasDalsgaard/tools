package sw10.spideybc.build;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.types.TypeName;

public class TypeSize {
	private int referenceSize;
	private int oneUnitSize;
	private int jvmObjectOverheadSize;
	private int frameOverheadsize;
	private Map<TypeName, Integer> typeSizeByTypeName;
	
	public TypeSize(){
		this.referenceSize = 0;
		this.oneUnitSize = 0;
		this.jvmObjectOverheadSize = 0;
		this.frameOverheadsize = 0;
		this.typeSizeByTypeName = new HashMap<TypeName, Integer>();
	}
	
	public void setReferenceSize(int referenceSize){
		this.referenceSize = referenceSize;
	}
	
	public int getReferenceSize(){
		return this.referenceSize;
	}
	
	public void setOneUnitSize(int oneUnitSize){
		this.oneUnitSize = oneUnitSize;
	}
	
	public int getOneUnitSize(){
		return this.oneUnitSize;
	}
	
	public void setJvmObjectOverheadSize(int jvmObjectOverheadSize){
		this.jvmObjectOverheadSize = jvmObjectOverheadSize;
	}
	
	public int getJvmObjectOverheadSize(){
		return this.jvmObjectOverheadSize;
	}
	
	public void setFrameOverheadSize(int frameOverheadsize){
		this.frameOverheadsize = frameOverheadsize;	
	}
	
	public int getFrameOverheadSize(){
		return this.frameOverheadsize;	
	}
	
	public void setTypeSize(TypeName name, Integer size){
		this.typeSizeByTypeName.put(name, size);
	}
	
	public Map<TypeName,Integer> getTypeSize(){
		return this.typeSizeByTypeName;
	}
}
