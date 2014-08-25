import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;


public class IntentWithPermission {
	
	static String permissions[] = new String[200];
	static int numPerm = 0;
	
	static void print(String str) {
		System.out.println(str);
	}

	static void loadPermissions() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("permissions"));
			String str;
			while ((str = in.readLine()) != null) {
				permissions[numPerm] = str;
				numPerm++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String classname;
    	String method;
        if (args.length != 1) {
        	//Default test class
        	classname = "com.android.server.DropBoxManagerService";
        	method = "void add(android.os.DropBoxManager$Entry)";
        } else {
        	String parameter = args[0];
        	
        	if (parameter.startsWith("PERMISSION:")) System.exit(0);
        	
        	int colonidx = parameter.indexOf(":");
        	classname = parameter.substring(1, colonidx);
        	method = parameter.substring(colonidx+2, parameter.length()-1);
        }
		
		// Resolve dependencies
    	Scene.v().loadBasicClasses();
    	SootClass mclass = Scene.v().loadClassAndSupport(classname);

    	mclass.setApplicationClass();
    	
    	SootMethod m = mclass.getMethod(method);
    	
    	loadPermissions();
    	try {
			Body b = m.retrieveActiveBody();
			Iterator<Unit> iter_u = b.getUnits().iterator();
			HashMap<String, List<String>> intentaction = new HashMap<String, List<String>>();
			
			while (iter_u.hasNext()) {
				Stmt u = (Stmt) iter_u.next();
				if (u instanceof DefinitionStmt) {
					DefinitionStmt d = (DefinitionStmt) u;
					if (d.getRightOp() instanceof NewExpr) {
						String newbasestr = ((NewExpr)d.getRightOp()).getBaseType().toString();
						if (newbasestr.equals("android.content.Intent")) {
                            // Found a new intent definition of form intentVar =
                            // new Intent();
                            if (!intentaction.containsKey(d.getLeftOp()
                                    .toString())) {
                                intentaction.put(d.getLeftOp().toString(),
                                        new ArrayList<String>());
                            }
                        }
                    } else if (intentaction.containsKey(d.getRightOp()
                            .toString())) {
                        // Found an intent assignment of form intentVar1 =
                        // intentVar2;
                        String alias = d.getLeftOp().toString();
                        if (!intentaction.containsKey(alias)) {
                            intentaction.put(alias, new ArrayList<String>());
                        }
                        intentaction.get(alias).addAll(intentaction.get(d.getRightOp().toString()));
                    }
                }

				if (!u.containsInvokeExpr()) continue;
				InvokeExpr i = u.getInvokeExpr();
				SootMethod invokemethod = i.getMethod();
				if (i instanceof InstanceInvokeExpr) {
					InstanceInvokeExpr ii = (InstanceInvokeExpr) i;
					if (intentaction.get(ii.getBase().toString()) != null) {
						if(invokemethod.getName().equals("<init>") 
								&& invokemethod.getParameterCount() > 0
								&& invokemethod.getParameterType(0).toString().equals("java.lang.String")) {
							// Found an action string passed to the Intent() constructor
						    intentaction.get(ii.getBase().toString()).add(ii.getArg(0).toString());
							//print (ii.getBase().toString()+":"+ii.getArg(0).toString());
						}
					} else if (invokemethod.getDeclaringClass().toString().startsWith("android.content.Context")
							&& (invokemethod.getName().equals("sendBroadcast")
									|| invokemethod.getName().equals("sendOrderedBroadcast")
									|| invokemethod.getName().equals("registerReceiver"))) {
						Iterator<ValueBox> iter_vb = u.getUseBoxes().iterator();
						boolean foundintent = false;
						boolean foundpermission = false;
						boolean isSenderPerm = invokemethod.getName().equals("registerReceiver");
						String perm = "";
						List<String> intentList = new ArrayList<String>();
						while(iter_vb.hasNext()) {
							Value v = iter_vb.next().getValue();
							
							//look for usage of stored intents
							if (intentaction.get(v.toString()) != null) {
								foundintent = true;
								intentList = intentaction.get(v.toString());
							}
							
							int j=0;
        					while(j<numPerm && !foundpermission) {
        						if (v.toString().equals("\""+permissions[j]+"\"")) {
        							foundpermission = true;
        							perm = permissions[j];
        						}
        						j++;
        					}
						}
						if (foundintent && foundpermission) {
						    for (String intent : intentList){
    							String direction = (isSenderPerm) ? "S" : "R";
    							if (intent.length()<5) {
    								print ("!!!"+intent+" "+perm+" "+direction+" "+m.toString());
    							} else {
    								intent = intent.substring(1, intent.length()-1);
    								print (intent+" "+perm+" "+direction);								
    							}
    						}
						}
					} else {
						Iterator<ValueBox> iter_vb = u.getUseBoxes().iterator();
						boolean foundintent = false;
						boolean foundpermission = false;
						String perm = "";
						List<String> intentList = new ArrayList<String>();
						while(iter_vb.hasNext()) {
							Value v = iter_vb.next().getValue();
							
							//look for usage of stored intents
							if (intentaction.get(v.toString()) != null) {
								foundintent = true;
								intentList = intentaction.get(v.toString());
							}
							
							int j=0;
        					while(j<numPerm && !foundpermission) {
        						if (v.toString().equals("\""+permissions[j]+"\"")) {
        							foundpermission = true;
        							perm = permissions[j];
        						}
        						j++;
        					}
						}
						if (foundintent && foundpermission) {
						    for (String intent: intentList){
    							print ("!!!"+intent+" "+perm+" "+invokemethod.toString());
    						}
						}
					}
				}
			}
		} catch (RuntimeException e) {
			System.err.println("ERROR:"+m.toString()+" "+e.getMessage());    			
		}
	}

}
