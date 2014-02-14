package sw10.spideybc.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import sw10.spideybc.util.JVMModelDeserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.wala.types.TypeName;

public class JVMModel {
	public int referenceSize;
	public int oneUnitSize;
	public Map<TypeName, Integer> typeSizeByTypeName;
	public int jvmObjectOverheadSize;
	public int frameOverhead;
	public Map<String, Integer> nativeFunctionMap;
	public Map<String, Integer> recursionLimitMap;
	
	public JVMModel() {
		this.typeSizeByTypeName = new HashMap<TypeName, Integer>();
		this.nativeFunctionMap = new HashMap<String, Integer>();
		this.recursionLimitMap = new HashMap<String, Integer>();
	}
	
	public int getSizeofType(TypeName type) {
		if (this.typeSizeByTypeName.containsKey(type)) {
			return this.typeSizeByTypeName.get(type);
		}
		else
		{
			throw new NoSuchElementException();
		}
	}
	
	public void addType(TypeName typeName, int size)
	{
		typeSizeByTypeName.put(typeName, size);
	}
	
	public static JVMModel makeFromJson(String file) {
		String line;
		StringBuilder json = new StringBuilder();
		BufferedReader reader = null;
		
		try {
			File jsonFile = new File(file);
			reader = new BufferedReader(new FileReader(jsonFile));
			while((line = reader.readLine()) != null) {
				json.append(line);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Could not find model file, " + file + ", when trying to make JVMModel. " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(reader != null)
					reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(JVMModel.class, new JVMModelDeserializer());
		Gson gson = gsonBuilder.create();
		JVMModel model = gson.fromJson(json.toString(), JVMModel.class);
		
		return model;
	}
}