package gov.nih.nlm.mor.auditmap.definitionalFeaturesAnalysis.bossAndAIsubdivision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import gov.nih.nlm.mor.util.RestUrl;
import gov.nih.nlm.mor.auditmap.utilitaries.OntologyClassManagement;
import gov.nih.nlm.mor.util.ExcelReport;
import gov.nih.nlm.mor.util.ExponentialBackoff;

public class ExportFile {
//	private static OWLOntology Ontology;
//
//    private static OWLReasoner Elkreasoner;
//    private static OWLOntologyManager manager;
	private static RestUrl restUrl = new RestUrl();
	private static ExponentialBackoff exponentialBackoff = new ExponentialBackoff();
	
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		String path ="AuditRxNorm2SNOMED.owl";
		File OntologyPath = new File ("/git/MapRxNormToSnomed/Audit/Livrable/File/OWLFile/"+path);
//		
////			
//		OntologyClassManagement classMana = new OntologyClassManagement(OntologyPath);
//		  Elkreasoner = classMana.getElkreasoner();
//		  manager=classMana.getManager();
//		 Ontology=classMana.getOntology();
//		  PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
//		  OWLDataFactory  factory = manager.getOWLDataFactory();
//	        
//	       OWLClass co=factory.getOWLClass("204890",pm);
//	       Set<String> errr= new HashSet<String>();
//	       errr.add("Rx762830");
		//getRxCodesFile(co);
	      // createdFileSUM_VAL(errr, "Boss");
		
		

	}
	
	
	public static void createdFileSUM_VAL(OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,  String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
           	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
       		 OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
						String errorTag= vv.getLiteral();
						if(error.equals("SUM")) {
						if(errorTag.startsWith("SUM")) {
							String[] errorT = errorTag.toLowerCase().split(" with | on | unit | and ");
							String relatedSnom=errorT[1].trim();
							String levelerro=errorT[2].trim();
							String setRx="Rx"+errorT[3].trim();
							//String[] rx=setRx.split(" ");
//							for(int i=0; i<errorT.length; i++) {
//								//rxNormIngredientBoss.add(rx[i].trim());
//							System.out.println(errorTag+" i "+i+" errorT " +errorT[i]);
//							}
							String setSNOMED=errorT[4].trim();
							OWLClass relat= factory.getOWLClass(relatedSnom,pm);
							String labelSonmedv="";
							for(OWLAnnotation eop:EntitySearcher.getAnnotationObjects(relat, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				   				//label
				   				OWLAnnotationValue jop =eop.annotationValue();
				   				OWLLiteral vvop= (OWLLiteral) jop;
				   				labelSonmedv= vvop.getLiteral();
				   				//label=a.toString();
				   			}
							String aaa=a.getIRI().getShortForm()+";"+label+";"+relatedSnom+";"+labelSonmedv+";"+setRx+";"+setSNOMED+";"+levelerro+"\n";
							resultatFinal.add(aaa);

						}
           	}
						if(error.equals("VAL")) {
						if(errorTag.startsWith("VAL")) {
							String[] errorT = errorTag.toLowerCase().split(" with | on | value | and ");
							String relatedSnom=errorT[1].trim();
							String levelerro=errorT[2].trim();
							String setRx="Rx"+errorT[3].trim();
							//String[] rx=setRx.split(" ");
//							for(int i=0; i<errorT.length; i++) {
//								//rxNormIngredientBoss.add(rx[i].trim());
//							System.out.println(errorTag+" i "+i+" errorT " +errorT[i]);
//							}
							String setSNOMED=errorT[4].trim();
							OWLClass relat= factory.getOWLClass(relatedSnom,pm);
							String labelSonmedv="";
							for(OWLAnnotation eop:EntitySearcher.getAnnotationObjects(relat, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				   				//label
				   				OWLAnnotationValue jop =eop.annotationValue();
				   				OWLLiteral vvop= (OWLLiteral) jop;
				   				labelSonmedv= vvop.getLiteral();
				   				//label=a.toString();
				   			}
							String aaa=a.getIRI().getShortForm()+";"+label+";"+relatedSnom+";"+labelSonmedv+";"+setRx+";"+setSNOMED+";"+levelerro+"\n";
							resultatFinal.add(aaa);

						
						}
						}
						
						
						
           	}
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD;SCTID_CD;Label_CD;ID_Rx;IDSN;level\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	
	public static void createdFileDFE(OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,  String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
           	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
       		 OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
						String errorTag= vv.getLiteral();
						
						if(errorTag.startsWith("DFE")) {
							String[] errorT = errorTag.split("with | on | and ");
							String relatedSnom=errorT[1].trim();
							String setRx="Rx"+errorT[2].trim();
							//String[] rx=setRx.split(" ");
//							for(int i=0; i<errorT.length; i++) {
//								//rxNormIngredientBoss.add(rx[i].trim());
//							System.out.println(errorTag+" i "+i+" errorT " +errorT[i]);
//							}
							String setSNOMED=errorT[3].trim();
							OWLClass relat= factory.getOWLClass(relatedSnom,pm);
							String labelSonmedv="";
							for(OWLAnnotation eop:EntitySearcher.getAnnotationObjects(relat, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
				   				//label
				   				OWLAnnotationValue jop =eop.annotationValue();
				   				OWLLiteral vvop= (OWLLiteral) jop;
				   				labelSonmedv= vvop.getLiteral();
				   				//label=a.toString();
				   			}
							String aaa=a.getIRI().getShortForm()+";"+label+";"+relatedSnom+";"+labelSonmedv+";"+setRx+";"+setSNOMED+"\n";
							resultatFinal.add(aaa);

						}
						
						
						
						
           	}
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD;SCTID;Label_CD;Rxcui_DF;SCTID_PDF_UOP\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	
	public static String RepartitionSemanticError( OWLDataFactory  factory, OWLOntology Ontology, Set<String> RxNorms, String errorType) throws FileNotFoundException {
	
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		  Set<String> result = new HashSet<String>();
		 for(String RxNorm: RxNorms) {
			 OWLClass Rx=factory.getOWLClass(RxNorm,pm);
		    String labelRx="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(Rx, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						labelRx= vv.getLiteral();
           	}
           	result.add(RxNorm.trim()+";"+labelRx+"\n");
           	 
		 }
		 String resu="RxCui;Label\n";
		 for(String aaaa:result ) {
			 resu=resu+aaaa;
		 }

  	       try (PrintWriter out = new PrintWriter(errorType+".csv")) {
  	     	    out.println(resu);
  	     	}
  	       
  	       return resu;
	}
	
	public static String onlyAsserted( OWLDataFactory  factory, OWLOntology Ontology, String RxNorm, String SNOMED) throws FileNotFoundException {
		String result="";
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
		    OWLClass Rx=factory.getOWLClass(RxNorm,pm);
		    String labelRx="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(Rx, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						labelRx= vv.getLiteral();
           	}
           	
		    OWLClass SNO=factory.getOWLClass(SNOMED,pm);
		    String labelSNo="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(SNO, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   				labelSNo= vv.getLiteral();
           	}
           	 result=RxNorm.trim()+";"+labelRx+";"+SNOMED.trim()+";"+labelSNo;
		
		return result;
	}
	public static void createdFileBOS_SAI( OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,  String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
    
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
           	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
       		 OWLAnnotationValue j =d.annotationValue();
				OWLLiteral vv= (OWLLiteral) j;
						String errorTag= vv.getLiteral();
						
						if(errorTag.startsWith("MOD-"+error)) {
							String[] errorT = errorTag.split(":|modification relation with");
							String setRx=errorT[1].trim();
							//String[] rx=setRx.split(" ");
//							for(int i=0; i<errorT.length; i++) {
//								//rxNormIngredientBoss.add(rx[i].trim());
//							System.out.println(errorTag+" i "+i+" errorT " +errorT[i]);
//							}
							String setSNOMED=errorT[2].trim();
							String aaa=a.getIRI().getShortForm()+";"+label+";"+setRx+";"+setSNOMED+";MOD\n";
							resultatFinal.add(aaa);
							//System.out.println(errorTag+" setSNOMED "+setSNOMED +" setRx  "+setRx);
							//String[] snom=setSNOMED.split(" ");
//							for(int u=0; u<snom.length; u++) {
//								SNOMEDsubstBoss.add(snom[u].trim());
//							}
						}
						if(errorTag.startsWith("NOMOD-"+error)) {
							String[] errorT = errorTag.split(":|with");
							String setRx=errorT[1].trim();
							//String[] rx=setRx.split(" ");
//							for(int i=0; i<errorT.length; i++) {
//								//rxNormIngredientBoss.add(rx[i].trim());
//							System.out.println(errorTag+" i "+i+" errorT " +errorT[i]);
//							}
							String setSNOMED=errorT[2].trim();
							String aaa=a.getIRI().getShortForm()+";"+label+";"+setRx+";"+setSNOMED+";NOMOD\n";
							resultatFinal.add(aaa);
							//System.out.println(errorTag+" setSNOMED "+setSNOMED +" setRx  "+setRx);
							//String[] snom=setSNOMED.split(" ");
//							for(int u=0; u<snom.length; u++) {
//								SNOMEDsubstBoss.add(snom[u].trim());
//							}
						}
						
						
						
           	}
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD;RxIngredient;SCT_substance;Tag\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	
	
	public static void createdFileERR(OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
           	Set<String> etape= new HashSet<String>();
        	for(OWLAnnotation d:EntitySearcher.getAnnotationObjects(a, Ontology, prop).collect(Collectors.toSet())) {
          		 OWLAnnotationValue j =d.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						String errorTag= vv.getLiteral();
   						
   						if(errorTag.startsWith("ERR")) {
   							String[] errorT = errorTag.split("-with-");
   							String relatedSnom=errorT[1].trim();
   							
   							
   								OWLClass relat= factory.getOWLClass(relatedSnom,pm);
   							String labelSonmedv="";
   							for(OWLAnnotation eop:EntitySearcher.getAnnotationObjects(relat, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				   				//label
   				   				OWLAnnotationValue jop =eop.annotationValue();
   				   				OWLLiteral vvop= (OWLLiteral) jop;
   				   				labelSonmedv= vvop.getLiteral();
   				   				//label=a.toString();
   				   			}
   							String aaa=a.getIRI().getShortForm()+";"+label+";"+relatedSnom+";"+labelSonmedv+";";
   							etape.add(aaa);

   						}
   						
   						
   						
   						
              	}
           	
       	 Set<String> doseForms = getRxCodesFile(a);
       	 for(String df: doseForms) {
       		 for(String et:etape) {
       		 String aaa=et+df+"\n";
       		resultatFinal.add(aaa);
       		 }
       	 }
       	 
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD;SCTID;Label_CD;RxCui_DF;Label_DF\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	public static void createdFileSPE(OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
        //OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
       	 Set<String> doseForms = getRxCodesFile(a);
       	 for(String df: doseForms) {
       		 String aaa=a.getIRI().getShortForm()+";"+label+";"+df+"\n";
       		resultatFinal.add(aaa);
       	 }
       	 
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD;RxCui_DF;Label_DF\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	public static void createdFileMAI_ENM_SSU(OWLDataFactory  factory, OWLOntology Ontology, Set<String> listToCreate,String error) throws FileNotFoundException {
		Set<OWLClass> RxNirMedicinalProducts = new HashSet<OWLClass>();
		 
		 PrefixManager pm = new DefaultPrefixManager("http://snomed.info/id/");
       // OWLAnnotationProperty prop=factory.getOWLAnnotationProperty("ErrorsTag",pm);
        for(String bo:listToCreate ) {
        	OWLClass MedicinalProduct=factory.getOWLClass(bo.trim(),pm);
        	RxNirMedicinalProducts.add(MedicinalProduct);
        }
        
        Set<String> resultatFinal = new HashSet<String>();
       
        RxNirMedicinalProducts.forEach(a->{
        	String label="";
           	for(OWLAnnotation e:EntitySearcher.getAnnotationObjects(a, Ontology, factory.getRDFSLabel()).collect(Collectors.toSet())) {
   				//label
   				OWLAnnotationValue j =e.annotationValue();
   				OWLLiteral vv= (OWLLiteral) j;
   						label= vv.getLiteral();
   				//label=a.toString();
   			}
     
       		 String aaa=a.getIRI().getShortForm()+";"+label+"\n";
       		resultatFinal.add(aaa);
       
        
        	
        });
        String resu="Rxcui_SCD;Label_SCD\n";
		for(String ae: resultatFinal) {
			resu=resu+ae;
		}
		
		 
	       try (PrintWriter out = new PrintWriter(error+".csv")) {
	     	    out.println(resu);
	     	}
        
        
	}
	public static Set<String> getRxCodesFile(OWLClass code) {

	    Set<String> codes = new HashSet<>();

	    JSONObject DoseFormForSCD = null;
	    //String RxNormCuiString = code.trim();

	    String RxNormCuiString = code.getIRI().getShortForm().replace("Rx", " ").trim();

	    try {
	    	String urltest =restUrl.getRestUrl() + "/REST/rxcui/"+RxNormCuiString+"/related.json?rela=has_dose_form";
	    	DoseFormForSCD = getresult(urltest, null);
	    
	        }

	    catch(Exception e) {

	           System.out.println("Unable to fetch dose form codes for Rxcui: " + RxNormCuiString);

	    }

	   

	    if( !DoseFormForSCD.isNull("relatedGroup") ) {

	           JSONObject propConceptGroup = (JSONObject) DoseFormForSCD.get("relatedGroup");

	           if( !propConceptGroup.isNull("conceptGroup") ) {

	        	   JSONArray rxnormConcepGroup = (JSONArray) propConceptGroup.get("conceptGroup");
	        	   for(int i=0; i < rxnormConcepGroup.length(); i++ ) {
	        		  JSONObject listelent= (JSONObject) rxnormConcepGroup.get(i);
	        		  if(!listelent.isNull("conceptProperties")) {
	        			  JSONArray conceptProperties = (JSONArray) listelent.get("conceptProperties");
	        			  for(int u=0; u < conceptProperties.length(); u++ ) {
	        				  JSONObject listDFdescription= (JSONObject) conceptProperties.get(u);
	        				  	if(!listDFdescription.isNull("rxcui")) {
	        				  		String rxString = listDFdescription.get("rxcui").toString();
	        				  		if(!listDFdescription.isNull("name")) {
	        				  			rxString=rxString+";"+listDFdescription.get("name").toString();
	        				  		}
	        				  	//	System.out.println("rxString "+rxString);
	        				  		codes.add(rxString);
	        				  	}
	        			  }
	        			  
	        		  }
	        	   }
             

	           }

	    }

	   

	    return codes;

	}
//	public static JSONObject getresult(String URLtoRead) throws IOException {
//
//	    URL url;
//
//	    HttpsURLConnection connexion;
//
//	    BufferedReader reader;
//
//	   
//
//	    String line;
//
//	    String result="";
//
//	    url= new URL(URLtoRead);
//
//	    
//	    connexion= (HttpsURLConnection) url.openConnection();
//	    connexion.setRequestMethod("GET");
//	     reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
//	    while ((line =reader.readLine())!=null) {
//
//	           result += line;
//
//	          
//
//	    }
//
//	  
//	    JSONObject json = new JSONObject(result);
//
//	    return json;
//
//	}
	
	public static JSONObject getresult(String URLtoRead, ExponentialBackoff b) throws IOException, InterruptedException {
		URL url;
		HttpURLConnection connexion;
		BufferedReader reader;
		 
		exponentialBackoff = b == null ? new ExponentialBackoff() : b; 
		
		String line;
		String result="";
		url= new URL(URLtoRead);

		connexion= (HttpURLConnection) url.openConnection();
		connexion.setRequestMethod("GET");
		try {
			reader= new BufferedReader(new InputStreamReader(connexion.getInputStream()));
		} catch(Exception e) {
			System.err.println("Network hiccup, retrying the call...");
			exponentialBackoff.multiply();
			if(exponentialBackoff.getTime() <= exponentialBackoff.getMax()) {
				Thread.sleep(exponentialBackoff.getTime());
				return getresult(URLtoRead, exponentialBackoff);
			} else {
				System.err.println("Tired of retrying the call. Throwing an error. (This may impact the final product. Is the API up? Waited more than" + exponentialBackoff.getTime() / 1000 + " seconds and haven't violated the ToS.)");
				throw e;
			}
		}
		while ((line =reader.readLine())!=null) {
			result += line;

		}

		JSONObject json = new JSONObject(result);
		return json;
	}	

}
