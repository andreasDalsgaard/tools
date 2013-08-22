package sw10.spideybc.reports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import sw10.spideybc.analysis.AnalysisResults;
import sw10.spideybc.analysis.CostResultMemory;
import sw10.spideybc.analysis.ICostResult;
import sw10.spideybc.build.JVMModel;
import sw10.spideybc.program.AnalysisSpecification;
import sw10.spideybc.util.OutputPrinter;
import sw10.spideybc.util.RunConfiguration;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.viz.DotUtil;
import com.ibm.wala.viz.NodeDecorator;

public class ReportGenerator {

	/* Auxiliary fields */
	private AnalysisSpecification specification;
	private AnalysisResults analysisResults;
	private ReportDataToJSONConverter converter;
	private ReportData reportData;
	private JVMModel jvmModel;
	private RunConfiguration runConfiguration;

	/* Directory and file titles */
	private final String OUTPUT = "output";
	private final String RESOURCES = "resources";
	private final String DT = "dt";
	private final String PDF = "pdf";
	private final String HTML = "html";
	private final String JS = "js";
	private final String INDEX_HTML = "index.html";
	private final String STATIC = "static";
	private final String DYNAMIC = "dynamic";

	/* Directory paths (Absolute paths) */
	private String OUTPUT_DIR;
	private String RESOURCES_DIR;
	private String STATIC_DIR;
	private String DYNAMIC_DIR;
	private String DT_DIR;
	private String PDF_DIR;
	private String HTML_DIR;
	private String JS_DIR;
	
	/* Directory paths (Relative paths from output directory) */
	private String STATIC_DIR_RELATIVE;
	private String DYNAMIC_DIR_RELATIVE;
	
	/* Helper fields */
	private Map<CGNode, String> guidByCGNode;
	private String webDir;

	public ReportGenerator() {
		this.specification = AnalysisSpecification.getAnalysisSpecification();
		this.analysisResults = AnalysisResults.getAnalysisResults();
		this.converter = new ReportDataToJSONConverter();
		this.guidByCGNode = converter.getCreatedGuidsForCGNodes();
		this.reportData = analysisResults.getReportData();

		this.jvmModel = specification.getJvmModel();
		this.runConfiguration = specification.getRunConfiguration();
		String outputDir = specification.getOutputDir();

		this.OUTPUT_DIR = outputDir;
		this.RESOURCES_DIR = outputDir + File.separatorChar + RESOURCES;

		if(runConfiguration == RunConfiguration.DEPLOY) {
			this.STATIC_DIR = RESOURCES_DIR + File.separatorChar + STATIC;
			this.DYNAMIC_DIR = RESOURCES_DIR + File.separatorChar + DYNAMIC;
			this.STATIC_DIR_RELATIVE = RESOURCES + File.separatorChar + STATIC;
			this.DYNAMIC_DIR_RELATIVE = RESOURCES + File.separatorChar + DYNAMIC;
			
			this.DT_DIR = DYNAMIC_DIR + File.separatorChar + DT;
			this.PDF_DIR = DYNAMIC_DIR + File.separatorChar + PDF;
			this.HTML_DIR = DYNAMIC_DIR + File.separatorChar + HTML;
			this.JS_DIR = DYNAMIC_DIR + File.separatorChar + JS;
		} else {
			this.DT_DIR = RESOURCES_DIR + File.separatorChar + DT;
			this.PDF_DIR = RESOURCES_DIR + File.separatorChar + PDF;
			this.HTML_DIR =  RESOURCES_DIR + File.separatorChar + HTML;
			this.JS_DIR = RESOURCES_DIR + File.separatorChar + JS;
		}
	}

	public void Generate(ArrayList<ReportEntry> reportEntries) {
		createOutputDirectories();

		/* Template engine initialization */
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();

		Template indexTemplate = ve.getTemplate("templates/index.vm");
		Template codeTemplate = ve.getTemplate("templates/code.vm");

		VelocityContext ctxIndex = new VelocityContext();
		VelocityContext ctxCode = new VelocityContext();

		try {
			webDir = new File(".").getCanonicalPath() + "/web/";
			if(runConfiguration == RunConfiguration.DEPLOY) {
				/* Copy web resources to static output folder */
				copyFolder(new File(webDir), new File(STATIC_DIR));
			}

			GenerateCSSIncludesForIndexFile(ctxIndex);
			GenerateJSIncludesForIndexFile(ctxIndex);
			
			GenerateCSSIncludesForCodeFile(ctxCode);
			GenerateJSIncludesForCodeFile(ctxCode);

			/* Page generation (or really just page tabs) */
			GenerateSummary(ctxIndex, reportEntries);
			GenerateCallGraph(codeTemplate, ctxIndex, ctxCode);
			GenerateDetails(indexTemplate, ctxIndex, reportEntries);
			
		} catch (IOException e) {
			OutputPrinter.printError("Could not generate reports. " + e.getStackTrace());
		}
	}

	private void GenerateCSSIncludesForIndexFile(Context ctxIndex) {
		if(runConfiguration == RunConfiguration.DEPLOY) {	
			ctxIndex.put("bootstrapCSS", STATIC_DIR_RELATIVE + "/bootstrap/css/bootstrap.css");
			ctxIndex.put("fancyboxCSS", STATIC_DIR_RELATIVE + "/fancyapps-fancyBox-0ffc358/source/jquery.fancybox.css");
			ctxIndex.put("syntaxCSS", STATIC_DIR_RELATIVE + "/syntaxhighlighter_3.0.83/styles/shCoreDefault.css");
			ctxIndex.put("stylesCSS", STATIC_DIR_RELATIVE + "/styles.css");
		} else {
			ctxIndex.put("bootstrapCSS", webDir + "bootstrap/css/bootstrap.css");
			ctxIndex.put("fancyboxCSS", webDir + "fancyapps-fancyBox-0ffc358/source/jquery.fancybox.css");
			ctxIndex.put("syntaxCSS", webDir + "syntaxhighlighter_3.0.83/styles/shCoreDefault.css");
			ctxIndex.put("stylesCSS", webDir + "styles.css");
		}
	}

	private void GenerateJSIncludesForIndexFile(Context ctxIndex) {
		if(runConfiguration == RunConfiguration.DEPLOY) {
			ctxIndex.put("syntaxcoreJS", STATIC_DIR_RELATIVE + "/syntaxhighlighter_3.0.83/scripts/shCore.js");
			ctxIndex.put("syntaxbrushJS", STATIC_DIR_RELATIVE + "/syntaxhighlighter_3.0.83/scripts/shBrushJava.js");
			ctxIndex.put("fancyboxJS", STATIC_DIR_RELATIVE + "/fancyapps-fancyBox-0ffc358/source/jquery.fancybox.pack.js");
			ctxIndex.put("bootstrapJS", STATIC_DIR_RELATIVE + "/bootstrap/js/bootstrap.js");
			ctxIndex.put("smoothScrollJS", STATIC_DIR_RELATIVE + "/smoothscroll.js");
			ctxIndex.put("graphDataset", DYNAMIC_DIR_RELATIVE  + File.separatorChar + JS + "/graph.js");
			ctxIndex.put("graphJS", STATIC_DIR_RELATIVE + "/graph.js");
			ctxIndex.put("scriptsJS", STATIC_DIR_RELATIVE + "/scripts.js");
		} else {
			ctxIndex.put("syntaxcoreJS", webDir + "syntaxhighlighter_3.0.83/scripts/shCore.js");
			ctxIndex.put("syntaxbrushJS", webDir + "syntaxhighlighter_3.0.83/scripts/shBrushJava.js");
			ctxIndex.put("fancyboxJS", webDir + "fancyapps-fancyBox-0ffc358/source/jquery.fancybox.pack.js");
			ctxIndex.put("bootstrapJS", webDir + "bootstrap/js/bootstrap.js");
			ctxIndex.put("smoothScrollJS", webDir + "smoothscroll.js");
			ctxIndex.put("graphDataset", JS_DIR + File.separatorChar + "graph.js");
			ctxIndex.put("graphJS", webDir + "graph.js");
			ctxIndex.put("scriptsJS", webDir + "scripts.js");
		}
	}

	private void GenerateJSIncludesForCodeFile(Context ctxCode) {
		if(runConfiguration == RunConfiguration.DEPLOY) {
			ctxCode.put("syntaxcoreJS", "../../"  + STATIC + "/syntaxhighlighter_3.0.83/scripts/shCore.js");
			ctxCode.put("syntaxbrushJS", "../../" + STATIC + "/syntaxhighlighter_3.0.83/scripts/shBrushJava.js");
		} else {
			ctxCode.put("syntaxcoreJS", webDir + "syntaxhighlighter_3.0.83/scripts/shCore.js");
			ctxCode.put("syntaxbrushJS", webDir + "syntaxhighlighter_3.0.83/scripts/shBrushJava.js");
		}
	}
	
	private void GenerateCSSIncludesForCodeFile(Context ctxCode) {
		if(runConfiguration == RunConfiguration.DEPLOY) {
			ctxCode.put("syntaxCSS", "../../" + STATIC + "/syntaxhighlighter_3.0.83/styles/shCoreDefault.css");
		} else {
			ctxCode.put("syntaxCSS", webDir + "syntaxhighlighter_3.0.83/styles/shCoreDefault.css");
		}
	}

	private void GenerateSummary(VelocityContext ctxIndex, ArrayList<ReportEntry> reportEntries) {
		ctxIndex.put("Mainclass", specification.getMainClass());
		ctxIndex.put("Application", specification.getApplicationJar());
		ctxIndex.put("JarIncludesStdLibraries", specification.getJarIncludesStdLibraries());
		ctxIndex.put("JVMModel", specification.getJvmModelString());
		ctxIndex.put("OutputDir", specification.getOutputDir());
		ctxIndex.put("SourceFilesRootDir", specification.getSourceFilesRootDir());
		ctxIndex.put("Analysis", specification.getTypeOfAnalysisPerformed());
		if (specification.getEntryPointSignatures() != null)
			ctxIndex.put("Methods", specification.getEntryPointSignatures().length);
		else
			ctxIndex.put("Methods", 1);

		StringBuilder rows = new StringBuilder();
		StringBuilder row = null;
		for(ReportEntry reportEntry : reportEntries) {
			for(Entry<CGNode, ICostResult> entry : reportEntry.getEntries().entrySet()) {
				CostResultMemory memCost = (CostResultMemory)entry.getValue();
				row = new StringBuilder();
				row.append(
						"<tr class=\"warning\">" +
						"<td>" + entry.getKey().getMethod().getSignature() + "</td>" +
						"<td>" + memCost.getCostScalar() + "</td>" +
						"<td>" + memCost.getAccumStackCostInBytes() + "</td>"	
				);
			}
			rows.append(row);
		}

		ctxIndex.put("analyzedMethodRow", rows);
	}

	private void GenerateCallGraph(Template codeTemplate, VelocityContext ctxIndex, VelocityContext ctxCode) {
		CallGraphNodeModel model = converter.createCallGraphJson();

		/* Create json.js file */
		StringBuilder jsContent = new StringBuilder();
		String json = converter.convertToJSONString(model);
		jsContent.append("var dataset = '" + json + "'");
		writeJSONJSFile(jsContent, JS_DIR + File.separatorChar + "graph.js");

		StringBuilder anchors = new StringBuilder();

		CallGraphNodeModel[] entryModels = model.children;
		for(CallGraphNodeModel entryModel : entryModels) {
			instantiateCodeFileFromTemplate(entryModel.node, entryModel, codeTemplate, ctxCode, anchors);
		}

		ctxIndex.put("codeAnchors", anchors.toString());
	}

	private void instantiateCodeFileFromTemplate(CGNode cgNode, CallGraphNodeModel model, Template codeTemplate, 
			VelocityContext ctxCode, StringBuilder anchors) {
		CostResultMemory cost = (CostResultMemory)analysisResults.getResultsForNode(cgNode);
		List<CGNode> refNodes = cost.getWorstCaseReferencedMethods();

		JavaFileData data = reportData.getJavaFileDataForCGNode(cgNode);

		VelocityContext ctxCodeClone = (VelocityContext)ctxCode.clone();
		if(data != null) {
			StringBuilder lines = new StringBuilder();
			Iterator<Integer> linesIterator = data.getLineNumbersForCGNode(cgNode).iterator();
			while(linesIterator.hasNext()) {
				lines.append(linesIterator.next());
				if(linesIterator.hasNext())
					lines.append(", ");
			}

			BufferedReader fileJavaReader = null;
			try {
				fileJavaReader = new BufferedReader(new FileReader(data.getSourceFile()));
				StringBuilder code = new StringBuilder();
				code.append("<pre class=\"brush: java; highlight: [" + lines + "]\">\n&nbsp;");

				String line;
				while ((line = fileJavaReader.readLine()) != null) {
					line = line.replace("<", "&#60;");
					line = line.replace(">", "&#62;");
					code.append(line + "\n");
				}
				code.append("</pre>");
				code.append("<script>SyntaxHighlighter.all()</script>");

				/* Add code tag <pre>...</pre> */
				ctxCode.put("code", code.toString());

				/* Write code template to html file */
				String path = HTML_DIR + File.separatorChar + guidByCGNode.get(cgNode) + ".html";
				writeTemplateToFile(codeTemplate, ctxCode, path);  

			} catch (FileNotFoundException e) {
				OutputPrinter.printError("Could not find file. " + e.getStackTrace());
			} catch(IOException e) {
				OutputPrinter.printError("Could not write to file. " + e.getStackTrace());
			} finally {
				try {
					fileJavaReader.close();
				} catch (IOException e) {
					OutputPrinter.printError("Could not close filereader. " + e.getStackTrace());
				}
			}

			String href;
			if(runConfiguration == RunConfiguration.DEPLOY) {
				href = DYNAMIC_DIR_RELATIVE + File.separatorChar + HTML + File.separatorChar + model.guid + ".html";
			} else {
				href = HTML_DIR + File.separatorChar + model.guid + ".html";
			}
			
			anchors.append("<a id=\"anchor-" + model.guid + "\" data-fancybox-type=\"iframe\" class=\"codeViewer\" href=\"" + href + "\" />");
		}

		int index = 0;
		CallGraphNodeModel children[] = model.children;
		for(CGNode refNode : refNodes) {			
			instantiateCodeFileFromTemplate(refNode, children[index], codeTemplate, ctxCodeClone, anchors);
			index++;
		}
	}

	private void writeTemplateToFile(Template template, Context ctx, String fullPath) throws IOException {
		StringWriter writer = new StringWriter();
		template.merge(ctx, writer);
		String filecontent = writer.toString();

		File htmlFile = new File(fullPath);
		if(!htmlFile.exists()){
			htmlFile.createNewFile();
		}

		FileWriter fw = new FileWriter(htmlFile);
		fw.write(filecontent);
		fw.close();
	}

	private void writeJSONJSFile(StringBuilder content, String fullPath) {
		FileWriter fw = null;
		try {
			File jsonFile = new File(fullPath);
			if(!jsonFile.exists())
				jsonFile.createNewFile();

			fw = new FileWriter(jsonFile);
			fw.write(content.toString());
		} catch(IOException e) {
			OutputPrinter.printError("Could not write file, " + fullPath + ". " + e.getStackTrace());
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				OutputPrinter.printError("Could not close filewriter. " + e.getStackTrace());
			}
		}
	}

	private void GenerateDetails(Template tmpIndex, VelocityContext ctxIndex, ArrayList<ReportEntry> reportEntries) throws IOException {
		BufferedReader fileJavaReader;

		StringBuilder sidemenuAllocations = new StringBuilder();
		StringBuilder sidemenuJVMStack = new StringBuilder();
		StringBuilder code = new StringBuilder();
		StringBuilder jvmStack = new StringBuilder();

		StringBuilder lines;

		for(ReportEntry reportEntry : reportEntries) {
			String javaFile = reportEntry.getSource();
			for(Entry<CGNode, ICostResult> entry : reportEntry.getEntries().entrySet()) {
				CGNode cgNode = entry.getKey();
				ICostResult cost = entry.getValue();
				Set<Integer> lineNumbers = reportEntry.getLineNumbers(cgNode);
				IMethod method = cgNode.getMethod();				

				CostResultMemory memCost = (CostResultMemory)cost;
				String guid = guidByCGNode.get(cgNode);

				/* Control-Flow Graph */
				try {
					GenerateCFG(cgNode.getIR().getControlFlowGraph(), guid);
				}catch(WalaException e) {
					System.err.println("Could not generate report. " + e.getMessage());
				}

				/* JVMStack side menu */
				sidemenuJVMStack.append("<li><a title=\"" + method.getSignature() + "\" id=\"methodjvm-" + guid + "\" href=\"#\"><i class=\"icon-home icon-black\"></i>" + method.getSignature() + "</a></li>\n");
				sidemenuJVMStack.append("<ul class=\"nav nav-list\">");
				sidemenuJVMStack.append("<li><i class=\"icon-certificate icon-black\"></i>Cost: " + memCost.getAccumStackCostInBytes() + " bytes</li>\n");
				sidemenuJVMStack.append("</ul>");

				/* Allocations side menu */
				sidemenuAllocations.append("<li><a title=\"" + method.getSignature() + "\" id=\"method-" + guid + "\" href=\"#\"><i class=\"icon-home icon-black\"></i>" + method.getSignature() + "</a></li>\n");

				/* Allocations side menu --> sub-menu  */
				sidemenuAllocations.append("<ul class=\"nav nav-list\">");
				sidemenuAllocations.append("<li><i class=\"icon-certificate icon-black\"></i>Cost: " + cost.getCostScalar() + " bytes</li>\n");
				
				String href;
				if(runConfiguration == RunConfiguration.DEPLOY) {
					href = DYNAMIC_DIR_RELATIVE + File.separatorChar + PDF + File.separatorChar + guid + ".pdf";
				} else {
					href = PDF_DIR + File.separatorChar + guid + ".pdf";
				}
				
				sidemenuAllocations.append("<li><a data-fancybox-type=\"iframe\" class=\"cfgViewer\" href=\"" + href + "\"><i class=\"icon-refresh icon-black\"></i>Control-Flow Graph</a></li>\n");
				href = guid;
				sidemenuAllocations.append("<li><a id=\"details-" + guid + "\" href=\"#\"><i class=\"icon-search icon-black\"></i>Details</a></li>\n");
				sidemenuAllocations.append("<li><a id=\"referencedMethods-" + guid + "\" href=\"#\"><i class=\"icon-align-justify icon-black\"></i>Referenced Methods</a></li>\n");
				sidemenuAllocations.append("<ul id=\"methodrefsub-" + guid + "\" class=\"nav nav-list\" style=\"display:none;\">");

				for(CGNode refCGNode : memCost.worstcaseReferencesMethods) {
					IMethod refMethod = refCGNode.getMethod();
					String refMethodSignature = refMethod.getSignature();
					if(refMethodSignature.contains("<")) {
						refMethodSignature = refMethodSignature.replace("<", "&lt;");
						refMethodSignature = refMethodSignature.replace(">", "&gt;");
					}


					String refCGNodeGuid = guidByCGNode.get(refCGNode);

					sidemenuAllocations.append("<li><a id=\"methodrefsubentry-" + refCGNodeGuid + "\" href=\"#" + refCGNodeGuid + "\" class=\"smoothScroll\"><i class=\"icon-arrow-right icon-black\"></i>" + refMethodSignature + "</a></li>\n");
				}	

				sidemenuAllocations.append("</ul>\n");
				sidemenuAllocations.append("</ul>\n");

				lines = new StringBuilder();
				Iterator<Integer> linesIterator = lineNumbers.iterator();
				while(linesIterator.hasNext()) {
					lines.append(linesIterator.next());
					if(linesIterator.hasNext())
						lines.append(", ");
				}

				fileJavaReader = new BufferedReader(new FileReader(javaFile));

				/* Analyzed method code content viewer */
				code.append("<div id=\"code-" + guid + "\">\n");
				code.append("<pre class=\"brush: java; highlight: [" + lines + "]\">\n&nbsp;");

				String line;
				while ((line = fileJavaReader.readLine()) != null) {
					line = line.replace("<", "&#60;");
					line = line.replace(">", "&#62;");
					code.append(line + "\n");
				}
				code.append("</pre>");
				code.append("</div>");

				/* JVM Stack content viewer */
				ArrayList<CGNode> callStack = AnalysisResults.getAnalysisResults().getWorstCaseStackTraceFromNode(cgNode);
				jvmStack.append("<div id=\"stack-" + guid + "\" style=\"display:none; width:80%;\">");
				for(CGNode stackElement : callStack) {
					String stackGuid = java.util.UUID.randomUUID().toString();
					CostResultMemory stackElementCost = (CostResultMemory)AnalysisResults.getAnalysisResults().getResultsForNode(stackElement);
					int locals = stackElementCost.getMaxLocals();
					int stack = stackElementCost.getMaxStackHeight();

					StringBuilder content = new StringBuilder();
					content.append("<small>Max Locals:       " + locals + "</small><br/>");
					content.append("<small>Max Stack height: " + stack + "</small>");

					jvmStack.append("<a style=\"text-decoration:none; color:#000000\" data-html=\"true\" data-trigger=\"manual\" id=\"stackelement-" + stackGuid + "\" rel=\"popover\" data-content=\"" + content + "\" data-original-title=\"Accumulated: " + stackElementCost.getAccumStackCost() + "\">");
					jvmStack.append("<div style=\"height:90px; margin:0px; text-align:center;\" class=\"well\"><div style=\"margin-top:35px;\">" + stackElement.getMethod().getSignature() + "</div></div>");
					jvmStack.append("</a>");
				}
				jvmStack.append("</div>");

				/* Allocations details viewer */
				code.append("<div class=\"well\" id=\"det-" + guid + "\" style=\"display:none; position:relative;\">\n");
				code.append("<div class=\"methodsignaturebox\">" + method.getSignature() + "</div>");
				code.append("<span class=\"label label-info topRight\">Cost: " + memCost.getCostScalar() + "</span>");

				if(memCost.countByTypename.size() > 0) {
					/* Allocations table (self) */
					code.append("<br/><br/><div class=\"desc\">Allocation table for the method itself</div>");
					code.append("<table class=\"table table-striped table-bordered table-hover\">");
					code.append("<tbody>");
					code.append("<tr>");
					code.append("<td width=\"60%\"><b>Typename</b></td>");
					code.append("<td width=\"20%\"><b>Count</b></td>");
					code.append("<td width=\"20%\"><b>Cost</b></td>");
					code.append("</tr>");

					for(Entry<TypeName, Integer> countByTypename : memCost.countByTypename.entrySet()) {
						code.append("<tr>");
						TypeName typeName = countByTypename.getKey();
						code.append("<td>" + typeName + "</td>");
						int count = countByTypename.getValue();
						code.append("<td>" + count + "</td>");

						int typeSize;

						if(memCost.arraySizeByTypeName.containsKey(typeName)) {
							typeSize = memCost.arraySizeByTypeName.get(typeName) * jvmModel.getSizeofType(typeName);
						} else {
							typeSize = jvmModel.getSizeofType(typeName);
						}
						int totalCost = count*typeSize;

						code.append("<td>" + totalCost + "</td>");
						code.append("</tr>");
					}
					code.append("</tbody>");
					code.append("</table>");
				}

				if(memCost.aggregatedCountByTypename.size() > 0) {
					/* Allocations table (aggr) */
					code.append("<div class=\"desc\">Aggregrated allocation table for all referenced methods and the method itself</div>");
					code.append("<table class=\"table table-striped table-bordered table-hover\">");
					code.append("<tbody>");
					code.append("<tr>");
					code.append("<td width=\"60%\"><b>Typename</b></td>");
					code.append("<td width=\"20%\"><b>Count</b></td>");
					code.append("<td width=\"20%\"><b>Cost</b></td>");
					code.append("</tr>");
					for(Entry<TypeName, Integer> countByTypename : memCost.aggregatedCountByTypename.entrySet()) {
						code.append("<tr>");
						TypeName typeName = countByTypename.getKey();
						code.append("<td>" + typeName + "</td>");
						int count = countByTypename.getValue();
						if (count != 1) {count++;};
						code.append("<td>" + count + "</td>");

						int typeSize;

						if(memCost.aggregatedArraySizeByTypeName.containsKey(typeName)) {							
							typeSize = memCost.aggregatedArraySizeByTypeName.get(typeName) * jvmModel.getSizeofType(typeName);
						} else {
							typeSize = count * jvmModel.getSizeofType(typeName);
						}

						code.append("<td>" + typeSize + "</td>");
						code.append("</tr>");
					}
					code.append("</tbody>");
					code.append("</table>");
				}

				code.append("</div>");

				/* Referenced Methods content viewer */
				code.append("<div id=\"ref-" + guid + "\" style=\"display:none;\">\n");
				for(CGNode refCGNode : memCost.worstcaseReferencesMethods) {	
					IMethod refMethod = refCGNode.getMethod();
					String refMethodSignature = refMethod.getSignature();
					if(refMethodSignature.contains("<")) {
						refMethodSignature = refMethodSignature.replace("<", "&lt;");
						refMethodSignature = refMethodSignature.replace(">", "&gt;");
					}

					code.append("<a name=\"" + guidByCGNode.get(refCGNode) + "\"></a>");
					code.append("<div class=\"well\" style=\"position:relative;\">");
					code.append("<div class=\"methodsignaturebox\">" + refMethodSignature + "</div>");
					CostResultMemory refCGNodeCost = (CostResultMemory)analysisResults.getResultsForNode(refCGNode);
					code.append("<span class=\"label label-info topRight\">Cost: " + refCGNodeCost.getCostScalar() + "</span>");

					if(refCGNodeCost.aggregatedCountByTypename.size() > 0) {
						/* Allocations table (aggr) */
						code.append("<br/><div class=\"desc\">Aggregrated allocation table for all referenced methods and the method itself</div>");
						code.append("<table class=\"table table-striped table-bordered table-hover\">");
						code.append("<tbody>");
						code.append("<tr>");
						code.append("<td width=\"60%\"><b>Typename</b></td>");
						code.append("<td width=\"20%\"><b>Count</b></td>");
						code.append("<td width=\"20%\"><b>Cost</b></td>");
						code.append("</tr>");
						for(Entry<TypeName, Integer> countByTypename : refCGNodeCost.aggregatedCountByTypename.entrySet()) {
							code.append("<tr>");
							TypeName typeName = countByTypename.getKey();
							code.append("<td>" + typeName + "</td>");
							int count = countByTypename.getValue();
							code.append("<td>" + count + "</td>");

							int typeSize;

							if(refCGNodeCost.aggregatedArraySizeByTypeName.containsKey(typeName)) {
								typeSize = memCost.aggregatedArraySizeByTypeName.get(typeName) * jvmModel.getSizeofType(typeName);
							} else {
								typeSize = count * jvmModel.getSizeofType(typeName);
							}

							code.append("<td>" + typeSize + "</td>");
							code.append("</tr>");
						}
						code.append("</tbody>");
						code.append("</table>");
					}
					code.append("</tbody>");
					code.append("</table>");
					code.append("</div>");
				}
				code.append("</div>");
			}
		}


		ctxIndex.put("sidemenuAllocations", sidemenuAllocations.toString());
		ctxIndex.put("sidemenuJVMStack", sidemenuJVMStack.toString());
		ctxIndex.put("code", code.toString());
		ctxIndex.put("JVMStack", jvmStack.toString());

		try {
			String path = OUTPUT_DIR + File.separatorChar + INDEX_HTML;
			writeTemplateToFile(tmpIndex, ctxIndex, path);
		} catch(IOException e) {
			System.err.println("Could not generate output file from template, index.vm. " + e.getStackTrace());
		}
	}

	private void GenerateCFG(SSACFG cfg, String guid) throws WalaException{
		Properties wp = WalaProperties.loadProperties();
		wp.putAll(WalaExamplesProperties.loadProperties());

		String psFile = PDF_DIR + File.separatorChar + guid + ".pdf";	
		String dotFile = DT_DIR + File.separatorChar + guid + ".dt";
		String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);

		final HashMap<BasicBlock, String> labelMap = HashMapFactory.make();
		for (Iterator<ISSABasicBlock> iteratorBasicBlock = cfg.iterator(); iteratorBasicBlock.hasNext();) {
			SSACFG.BasicBlock basicBlock = (SSACFG.BasicBlock) iteratorBasicBlock.next();

			StringBuilder label = new StringBuilder();
			

			if(basicBlock.isEntryBlock()) {
				label.append(basicBlock.toString());
				label.append("(entry)");				
			} else if(basicBlock.isExitBlock()) {
				label.append(basicBlock.toString());
				label.append("(exit)");
			} else {
				label.append("BB"+basicBlock.getNumber()+"   ");
				Iterator<SSAInstruction> iteratorInstruction = basicBlock.iterator();
				while(iteratorInstruction.hasNext()) {
					SSAInstruction inst = iteratorInstruction.next();
					label.append(inst.toString() + " ");
				}
			}
			labelMap.put(basicBlock, label.toString());
		}
		NodeDecorator labels = new NodeDecorator() {
			public String getLabel(Object o) {
				return labelMap.get(o);
			}
		};
		DotUtil.dotify(cfg, labels, dotFile, psFile, dotExe);
	}

	private void createOutputDirectories() {
		File outputDir = new File(OUTPUT_DIR);		
		if(outputDir.exists()) {
			deleteFilesInDirectory(outputDir);
		}
		
		outputDir.mkdir();
		
		try {
			new File(RESOURCES_DIR).mkdir();

			if(runConfiguration == RunConfiguration.DEPLOY) {
				new File(STATIC_DIR).mkdir();
				new File(DYNAMIC_DIR).mkdir();
			}

			new File(DT_DIR).mkdir();
			new File(PDF_DIR).mkdir();
			new File(HTML_DIR).mkdir();
			new File(JS_DIR).mkdir();		
		} catch (SecurityException e) {
			OutputPrinter.printError("Could not create output directories. " + e.getStackTrace());
		}	
	}

	/*
	 * Recursively deletes all files and folders in the supplied directory
	 */
	private void deleteFilesInDirectory(File folder) {
		File[] files = folder.listFiles();
		if(files != null) {
			for(File file : files) {
				if(file.isDirectory()) {
					deleteFilesInDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		if(!folder.getName().equals(OUTPUT)) {
			folder.delete();
		}
	}

	public void copyFolder(File src, File dest) throws IOException {
		if(src.isDirectory()){
			if(!dest.exists()){
				dest.mkdir();
			}
			
			String files[] = src.list();

			for (String file : files) {
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				copyFolder(srcFile,destFile);
			}

		}else{
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 

			byte[] buffer = new byte[1024];

			int length;
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
		}
	}
}
