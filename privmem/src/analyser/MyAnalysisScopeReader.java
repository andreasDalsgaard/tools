package analyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.Plugin;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ide.plugin.CorePlugin;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

class MyModule implements Module
{
	List<ModuleEntry> entries = new ArrayList<ModuleEntry>();
	
	void addEntry(ModuleEntry entry)
	{
		this.entries.add(entry);
	}
	
	@Override
	public Iterator<ModuleEntry> getEntries() {
		return entries.iterator();
	}
	
}

public class MyAnalysisScopeReader extends AnalysisScopeReader {
	
	public static AnalysisScope makeJavaBinaryAnalysisScope(String classPath, String scjJar, File exclusionsFile) throws IOException {
	    return makeJavaBinaryAnalysisScope(classPath, scjJar, exclusionsFile, CorePlugin.getDefault());
	  }

	  /**
	   * @param classPath class path to analyze, delimited by File.pathSeparator
	   * @param exclusionsFile file holding class hierarchy exclusions. may be null
	   * @throws IOException 
	   * @throws IllegalStateException if there are problems reading wala properties
	   */
	  public static AnalysisScope makeJavaBinaryAnalysisScope(String application, String primordial, File exclusionsFile, Plugin plugIn) throws IOException {	    
	    AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();	    
	    FileProvider fp = new FileProvider();
	    
	    if (primordial != null)
	    {
	    	addClassPathToScope(primordial, scope, scope.getLoader(AnalysisScope.PRIMORDIAL));
	    	
	    	if (new File(application).exists()) {
	    		Module appMixed = fp.getJarFileModule(application, AnalysisScopeReader.class.getClassLoader());	    
			    Iterator<ModuleEntry> myit = appMixed.getEntries();
			   		    
			    MyModule app = new MyModule();
			    
			    while (myit.hasNext())
			    {
			    	ModuleEntry entry = myit.next();
			    	if (!entry.getClassName().startsWith("java") && 
			    			!entry.getClassName().startsWith("com") && 
			    			!entry.getClassName().startsWith("joprt") && 
			    			!entry.getClassName().startsWith("util/Dbg") && 
			    			!entry.getClassName().startsWith("util/Timer") ){			      		
			    		if (entry.isClassFile()) 
			    			app.addEntry(entry);			    			
			    	}
			    	
			    }
			    		    			    
			    scope.addToScope(scope.getLoader(AnalysisScope.APPLICATION), app);
	    	} else {
	    		util.error("File not found "+application);
	    	}
	    	
	    } else 
	    {
		    Module appMixed = fp.getJarFileModule(application, AnalysisScopeReader.class.getClassLoader());	    
		    Iterator<ModuleEntry> myit = appMixed.getEntries();
		   		    
		    MyModule app = new MyModule(), prim = new MyModule();
		    
		    while (myit.hasNext())
		    {
		    	ModuleEntry entry = myit.next();
		    	if (entry.getClassName().startsWith("java") || 
		    			entry.getClassName().startsWith("com") || 
		    			entry.getClassName().startsWith("joprt") || 
		    			entry.getClassName().startsWith("util/Dbg") || entry.getClassName().startsWith("util/Timer") ){
		      		prim.addEntry(entry);
		    	}		  
		    	else
		    	{
		    		if (entry.isClassFile())
		    			app.addEntry(entry);			    			    		
		    	}
		    	
		    }
		    		    
		    scope.addToScope(scope.getLoader(AnalysisScope.PRIMORDIAL), prim);
		    scope.addToScope(scope.getLoader(AnalysisScope.APPLICATION), app);		    
	    }
	    
	    return scope;
	  }
}
