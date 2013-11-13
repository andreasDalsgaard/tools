package sw10.spideybc.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.simple.parser.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import sw10.spideybc.build.TypeSize;

public class ModelBuilder {
	public void foo(String file) {
		TypeSize typesize = new TypeSize();
		JSONParser parser = new JSONParser();
		
		try {
			Object obj = parser.parse(new FileReader(file));
			JSONObject jsonObject = (JSONObject) obj;
			
			typesize.setReferenceSize((Integer) jsonObject.get("ReferenceSize"));
			typesize.setOneUnitSize((Integer) jsonObject.get("OneUnitSize"));
			typesize.setFrameOverheadSize((Integer) jsonObject.get("FrameOverhead"));
			typesize.setJvmObjectOverheadSize((Integer) jsonObject.get("JvmObjectOverheadSize"));
			
			JSONArray primordialtypesize = (JSONArray) jsonObject.get("PrimordialTypeSizes");
			JSONArray applicationtypesize = (JSONArray) jsonObject.get("ApplicationTypeSizes");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
