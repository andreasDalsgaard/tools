package sw10.spideybc.build;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.util.FileScanner;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class AnalysisEnvironmentBuilder {

	private AnalysisSpecification specification;

	private AnalysisEnvironmentBuilder(AnalysisSpecification specification) {
		this.specification = specification;
	}

	public static AnalysisEnvironment makeFromSpecification(AnalysisSpecification specification) throws IOException, 
	ClassHierarchyException, 
	IllegalArgumentException, 
	CancelException {
		AnalysisEnvironmentBuilder builder = new AnalysisEnvironmentBuilder(specification);
		return builder.makeEnvironment();
	}

	private AnalysisEnvironment makeEnvironment() throws IOException, 
	ClassHierarchyException, 
	IllegalArgumentException, 
	CancelException {
		AnalysisScope analysisScope = buildAnalysisScope();
		ClassHierarchy classHierarchy = buildClassHierarchy(analysisScope);
		
		CallGraph callGraph = buildZeroXCFAAnalysis(analysisScope, classHierarchy);

		return saveBuiltEnvironmentInSingleton(analysisScope, classHierarchy, callGraph);
	}

	private AnalysisEnvironment saveBuiltEnvironmentInSingleton(AnalysisScope scope, ClassHierarchy classHierarchy, CallGraph callGraph) {
		AnalysisEnvironment environment = AnalysisEnvironment.getAnalysisEnvironment();
		environment.setAnalysisScope(scope);
		environment.setClassHierarchy(classHierarchy);
		environment.setCallGraph(callGraph);
		return environment;
	}

	private AnalysisScope buildAnalysisScope() throws IOException {
		FileProvider fp = new FileProvider();
		AnalysisScope scope = null;
		if (specification.getJarIncludesStdLibraries()) {
			scope = AnalysisScope.createJavaAnalysisScope();
		}
		else {			
			scope = AnalysisScopeReader.makePrimordialScope(fp.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
		}

		Map<String, File> originalSourceCodeFilesByClassName = FileScanner.scan(new File(specification.getSourceFilesRootDir()));

		Module fullModuleForApplication = fp.getJarFileModule(specification.getApplicationJar(), AnalysisScopeReader.class.getClassLoader());
		Iterator<ModuleEntry> entriesInApplication = fullModuleForApplication.getEntries();
		ScopeModule applicationScope = new ScopeModule();
		ScopeModule primordialScope = new ScopeModule();

		while (entriesInApplication.hasNext()) {
			ModuleEntry entry = entriesInApplication.next();
			if (specification.getJarIncludesStdLibraries() && 
					(entry.getClassName().startsWith("java") ||
							entry.getClassName().startsWith("com") || 
							entry.getClassName().startsWith("joprt") || 
							entry.getClassName().startsWith("util/Dbg") || entry.getClassName().startsWith("util/Timer"))){				
				primordialScope.addEntry(entry);
				String className = entry.getClassName();
				File file = originalSourceCodeFilesByClassName.get(sw10.spideybc.util.Util.getClassNameOrOuterMostClassNameIfNestedClass(className));
				if(file != null)
					scope.addSourceFileToScope(scope.getLoader(AnalysisScope.APPLICATION), file, entry.getClassName() + ".java");
			}	  
			else {
				if (entry.isClassFile()) {
					applicationScope.addEntry(entry);
					String className = entry.getClassName();
					File file = originalSourceCodeFilesByClassName.get(sw10.spideybc.util.Util.getClassNameOrOuterMostClassNameIfNestedClass(className));
					if(file != null)
						scope.addSourceFileToScope(scope.getLoader(AnalysisScope.APPLICATION), file, entry.getClassName() + ".java");
				}
			}	
		}

		scope.addToScope(scope.getLoader(AnalysisScope.PRIMORDIAL), primordialScope);
		scope.addToScope(scope.getLoader(AnalysisScope.APPLICATION), applicationScope);

		return scope;
	}

	private ClassHierarchy buildClassHierarchy(AnalysisScope analysisScope) throws ClassHierarchyException {
		return ClassHierarchy.make(analysisScope);
	}

	private AnalysisOptions buildEntryPoint(AnalysisScope analysisScope, ClassHierarchy classHierarchy) {
		Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(analysisScope, classHierarchy, "L"+specification.getMainClass());
		AnalysisOptions options = new AnalysisOptions(analysisScope, entrypoints);
		options.setReflectionOptions(ReflectionOptions.NONE);
		
		return options;
	}

	private CallGraph buildZeroXCFAAnalysis(AnalysisScope analysisScope, ClassHierarchy classHierarchy) throws IllegalArgumentException, CancelException {
		AnalysisOptions analysisOptions = buildEntryPoint(analysisScope, classHierarchy);		
		
		Util.addDefaultSelectors(analysisOptions, classHierarchy);
		Util.addDefaultBypassLogic(analysisOptions, analysisScope, Util.class.getClassLoader(), classHierarchy);		
		
		AnalysisCache cache = new AnalysisCache();
		nCFABuilder builder = new nCFABuilder(3, classHierarchy, analysisOptions, cache, null, null);

		return builder.makeCallGraph(analysisOptions);
	}
}

class ScopeModule implements Module
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