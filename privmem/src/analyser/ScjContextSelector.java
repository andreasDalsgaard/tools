package analyser;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;


public class ScjContextSelector implements ContextSelector {
		
	private IClass missionIClass = null;
	private IClass AEHIClass = null;	
	int counter = 0;	
	private ClassHierarchy cha;
	private IClass ManagedMemoryIClass;
	private IClass MemoryAreaIClass;
	public HashMap<IClass, ScjScopeStack> classScopeMap = new HashMap<IClass, ScjScopeStack>();
	public HashSet<ScjScopeStack> scopeStacks = new HashSet<ScjScopeStack>();
	public HashMap<IMethod, ScjScopeStack> methodScopeMap = new HashMap<IMethod, ScjScopeStack>();
	public int count;		
	private IClass immortalIClass;
	private IClass safeletIClass;
	private IClass PEHIClass;
	private IClass APEHIClass;
	private IClass CyclicExecutiveIClass;
	private ScjContext immortal;
	private boolean CyclicExecutiveUsed;
	private IClass MissionSequencer;
	
	
	
	public ScjContextSelector(ClassHierarchy cha) 
	{
		this.cha = cha;
		this.missionIClass = util.getIClass("Ljavax/safetycritical/Mission", cha);
				
		if (this.missionIClass == null)
			throw new IllegalArgumentException("No mission class in ClassHierarchy");
		
		this.AEHIClass = util.getIClass("Ljavax/realtime/AbstractAsyncEventHandler", cha);
		this.ManagedMemoryIClass  = util.getIClass("Ljavax/safetycritical/ManagedMemory", cha);		
		this.MemoryAreaIClass  = util.getIClass("Ljavax/realtime/MemoryArea", cha);		
		this.immortalIClass = util.getIClass("Ljavax/realtime/ImmortalMemory", cha);
		this.safeletIClass = util.getIClass("Ljavax/safetycritical/Safelet", cha);		
		this.PEHIClass = util.getIClass("Ljavax/safetycritical/PeriodicEventHandler", cha);
		this.APEHIClass = util.getIClass("Ljavax/safetycritical/AperiodicEventHandler", cha);
		this.CyclicExecutiveIClass = util.getIClass("Ljavax/safetycritical/CyclicExecutive", cha);
		this.MissionSequencer = util.getIClass("Ljavax/safetycritical/MissionSequencer", cha);
		this.immortal = new ScjContext(null, "Ljavax/realtime/ImmortalMemory", ScjScopeType.IMMORTAL);
	}
	
	public Context getCalleeTarget(CGNode caller, CallSiteReference site,
			IMethod callee, InstanceKey[] actualParameters) 
	{	
		ScjContext calleeContext;		
	
		// Handles the first nodes that is called from the synthetic fakeRoot
		if (!(caller.getContext() instanceof ScjContext))		
		{			
			return immortal; 
		}		
		
		calleeContext = (ScjContext) caller.getContext();
				
		if (isSubclassOf(callee, this.AEHIClass) && 
				isFuncName(callee, "handleAsyncEvent")) 
		{
			calleeContext = new ScjContext(calleeContext, callee.getDeclaringClass().getName().toString(), ScjScopeType.PM);
		} else if (this.isImplemented(callee, this.safeletIClass) && isFuncName(callee, "initializeApplication")) {			
			calleeContext = this.immortal; 
		}else if (isSubclassOf(callee,this.ManagedMemoryIClass)) 
		{			
			if (isFuncName(callee, "enterPrivateMemory")) {				
				calleeContext = new ScjContext(calleeContext, getUniquePMName(), ScjScopeType.PM);
			} else if (isFuncName(callee, "executeInOuterArea")) {
				System.out.println("executeInOuterArea");
				calleeContext = new ScjContext(calleeContext.getOuterStack());
			} else if (isFuncName(callee, "executeInAreaOf")) {
				util.error("Not supported by the analysis");
			} else if (isFuncName(callee, "getCurrentManagedMemory")) {
				util.error("Not supported by new SCJ revisions");
			} 	
		} else if (isFuncName(callee, "startMission") && callee.getDeclaringClass().getName().toString().equals("Ljavax/safetycritical/JopSystem")) 
		{			
				calleeContext = new ScjContext(calleeContext, 
						caller.getMethod().getDeclaringClass().getName().toString(), ScjScopeType.MISSION);
		} else if(isFuncName(callee, "getSequencer") && this.isImplemented(callee, this.safeletIClass))
		{
			calleeContext = this.immortal;
		}
		 
		if( ScjMemoryScopeAnalysis.analyseWithoutJRE == true && isFuncName(callee, "initialize") && isSubclassOf(callee, this.missionIClass))  
		{							
			calleeContext = new ScjContext(calleeContext, 
				caller.getMethod().getDeclaringClass().getName().toString(), ScjScopeType.MISSION);			
		}
		
		this.scopeStacks.add(calleeContext.scopeStack);
		this.updateClassScopeMapping(callee,calleeContext.scopeStack);		
		this.updateMethodScope(callee, calleeContext.scopeStack);
		return calleeContext;
	}

	public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
		return EmptyIntSet.instance;
	}
	
	public boolean isSubclassOf(IMethod callee, IClass parent)
	{
		if (parent != null)			
			return this.cha.isSubclassOf(callee.getDeclaringClass(), parent);
		return false;		
	}
	
	public boolean isImplemented(IMethod callee, IClass parent)
	{
		if (parent != null)					
			return this.cha.implementsInterface(callee.getDeclaringClass(), parent);
		return false;		
	}
	
	
	public boolean isFuncName(IMethod callee, String str)
	{
		return callee.getName().toString().equals(str);
	}
	
	public String getUniquePMName()
	{
		return UUID.randomUUID().toString();
	}
	
	private void updateClassScopeMapping(IMethod callee, ScjScopeStack scopeStack)
	{
		if (isSubclassOf(callee, this.CyclicExecutiveIClass))
		{
			updateClassScope(callee.getDeclaringClass(),immortal.scopeStack);			
			this.CyclicExecutiveUsed = true;		
		} else if(isImplemented(callee, this.safeletIClass)) {		
			updateClassScope(callee.getDeclaringClass(),immortal.scopeStack);
		} else if(isSubclassOf(callee, this.missionIClass)) { 		//Mission 		
			updateClassScope(callee.getDeclaringClass(), scopeStack);
		} else if(isSubclassOf(callee, this.PEHIClass) || isSubclassOf(callee, this.APEHIClass)) {		//EventHandlers
			ScjScopeStack ss = new ScjScopeStack();			
			ss.add(scopeStack.get(0));
			ss.add(scopeStack.get(1));	
			updateClassScope(callee.getDeclaringClass(), ss);			
		}
	}
	
	private void updateClassScope(IClass type, ScjScopeStack ss1)
	{
		ScjScope scjScope = ss1.getLast();
		
		if (this.classScopeMap.containsKey(type))
		{
			ScjScopeStack ss2 = this.classScopeMap.get(type);			
			ss2.add(scjScope);
			this.classScopeMap.put(type, ss2);	
		} else {
			ScjScopeStack scopestack = new ScjScopeStack();	
			scopestack.add(ss1.getLast());
			this.classScopeMap.put(type, scopestack);
		}
	}
	
	private void updateMethodScope(IMethod method, ScjScopeStack ss1)
	{
		ScjScope scjScope = ss1.getLast();

		//Filter out constructors, cleanUp methods
		if (method.getName().toString().equals("cleanUp") && isSubclassOf(method, this.missionIClass) || 
				method.getName().toString().equals("<init>"))
			return;
		
		if (this.methodScopeMap.containsKey(method))
		{
			ScjScopeStack ss2 = this.methodScopeMap.get(method);			
			
			if (ss2.size() != 1 || ss2.getLast() == scjScope)
			{ 
				this.methodScopeMap.remove(method);
				ss2.add(scjScope);
				this.methodScopeMap.put(method, ss2);								
			}
			
		} else {
			ScjScopeStack scopestack = new ScjScopeStack();	
			scopestack.add(ss1.getLast());
			this.methodScopeMap.put(method, scopestack);
		}
	}
	
	
}
