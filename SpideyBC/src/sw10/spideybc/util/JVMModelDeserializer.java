package sw10.spideybc.util;

import java.lang.reflect.Type;
import java.util.Map.Entry;

import sw10.spideybc.build.JVMModel;

import com.ibm.wala.types.TypeName;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class JVMModelDeserializer implements JsonDeserializer<JVMModel> {
	@Override
	public JVMModel deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		
		final String REFERENCE_SIZE_KEY = "ReferenceSize";
		final String ONE_UNIT_SIZE_KEY = "OneUnitSize";
		final String JVM_OBJECT_OVERHEAD_SIZE_KEY = "JvmObjectOverheadSize";
		final String PRIMORDIAL_TYPES_ARRAY_KEY = "PrimordialTypeSizes";
		final String APPLICATION_TYPES_ARRAY_KEY = "ApplicationTypeSizes";
		final String FRAME_OVERHEAD_KEY = "FrameOverhead";
		final String NATIVE_FUNCTION_MAP = "NativeFunctionMap";
		final String RECURSION_LIMIT_MAP = "RecursionLimitMap";
		JVMModel model = new JVMModel();
		
		model.referenceSize = json.getAsJsonObject().get(REFERENCE_SIZE_KEY).getAsInt();		
		model.oneUnitSize = json.getAsJsonObject().get(ONE_UNIT_SIZE_KEY).getAsInt();
		model.jvmObjectOverheadSize = json.getAsJsonObject().get(JVM_OBJECT_OVERHEAD_SIZE_KEY).getAsInt();
		model.frameOverhead = json.getAsJsonObject().get(FRAME_OVERHEAD_KEY).getAsInt();
		JsonArray nativeFuncMap = json.getAsJsonObject().get(NATIVE_FUNCTION_MAP).getAsJsonArray();
		JsonArray recursionLimitMap = json.getAsJsonObject().get(RECURSION_LIMIT_MAP).getAsJsonArray();
		
		for(JsonElement nativeFuncMapEntry : nativeFuncMap) {		
			for(Entry<String, JsonElement> e : nativeFuncMapEntry.getAsJsonObject().entrySet()) {				
				model.nativeFunctionMap.put(e.getKey(), new Integer(e.getValue().getAsInt()));  
			}			
		}
		
		for(JsonElement entry : recursionLimitMap) {		
			for(Entry<String, JsonElement> e : entry.getAsJsonObject().entrySet()) {				
				model.recursionLimitMap.put(e.getKey(), new Integer(e.getValue().getAsInt()));  
			}			
		}
		
		JsonArray primordialTypes = json.getAsJsonObject().getAsJsonArray(PRIMORDIAL_TYPES_ARRAY_KEY);
		putJsonInModel(model, primordialTypes);
		JsonArray applicationTypes = json.getAsJsonObject().getAsJsonArray(APPLICATION_TYPES_ARRAY_KEY);
		putJsonInModel(model, applicationTypes);
		
		return model;
	}
	
 	private void putJsonInModel(JVMModel model, JsonArray jsonarray){
 		for(JsonElement typeSizeEntry : jsonarray) {
			for(Entry<String, JsonElement> e : typeSizeEntry.getAsJsonObject().entrySet()) {
				model.typeSizeByTypeName.put(jsonToTypeName(e.getKey()), e.getValue().getAsInt());  
			}			
		}
 	}	

 	private TypeName jsonToTypeName(String name) { // This method transform the json syntax to the Wala syntax (java)
 		if(name.contains(".")){ // Libraries java/lang/string -> Ljava.lang.string. 
 			name = name.replace(".", "/");
 			name = "L" + name;
 		}
 		return TypeName.string2TypeName(name);
 	}
}
