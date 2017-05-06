import java.util.ArrayList;
import java.util.Iterator;

public class Boolean_Retrieval extends Primitive_Retrieval {
	
	//result lists for word_1 of boolean query
	ArrayList<String> relevant_doc_paths_list_word_1;
	ArrayList<String> relevant_doc_pos_list_word_1;
	
	//result lists for word_2 of boolean_query
	ArrayList<String> relevant_doc_paths_list_word_2;
	ArrayList<String> relevant_doc_pos_list_word_2;
	
	//overall relevant doc paths and positions
	ArrayList<String> relevant_doc_paths_list_final;
	ArrayList<String> relevant_doc_pos_list_final;
	
	String query_sentence = null;
	
	public Boolean_Retrieval() {
		super();
		relevant_doc_paths_list_word_1 = new ArrayList<String>();
		relevant_doc_pos_list_word_1 = new ArrayList<String>();
		relevant_doc_paths_list_word_2 = new ArrayList<String>();
		relevant_doc_pos_list_word_2 = new ArrayList<String>();
		relevant_doc_paths_list_final = new ArrayList<String>();
		relevant_doc_pos_list_final = new ArrayList<String>();
	}
	
	/**
	 * Searches the inverted index for the given words and prints the results to console
	 * according to boolean logic between "term1 BOOLEAN_OPERATOR term2" 
	 * where BOOLEAN_OPERATOR = {AND, OR, BUT}.
	 * @param word
	 */
	public void Query(String sentence) {
		sentence = sentence.toLowerCase();
		query_sentence = sentence;
		String[] sentence_splitted = query_sentence.split(" ");
		try{
		String word_1 = sentence_splitted[0];
		String bool_op = sentence_splitted[1];
		String word_2 = sentence_splitted[2];
		
		super.Query(word_1);
		relevant_doc_paths_list_word_1.addAll(super.relevant_doc_paths_list);
		relevant_doc_pos_list_word_1.addAll(super.relevant_doc_pos_list);
		
		super.Query(word_2);
		relevant_doc_paths_list_word_2.addAll(super.relevant_doc_paths_list);
		relevant_doc_pos_list_word_2.addAll(super.relevant_doc_pos_list);
		
		execute_BooleanLogic(bool_op);
		}catch(ArrayIndexOutOfBoundsException e) {System.err.println("Invalid logic sentence!");}
	}
	
	private void execute_BooleanLogic(String bool_op) {
		if     (bool_op.equals("or"))  {execute_OR_logic();}
		else if(bool_op.equals("and")) {execute_AND_logic();}
		else if(bool_op.equals("but")) {execute_BUT_logic();}
		else {System.err.println("Invalid boolean operator!");}
	}
	
	/**
	 * {a, b, c} - {a, b} = {c}
	 * {a, b} - {a, c, d} = {b}
	 */
	private void execute_BUT_logic(){
		String curr_doc_path = null;
		int index_list_1 = 0;
		Iterator<String> doc_paths_i;
		
		relevant_doc_paths_list_final.addAll(relevant_doc_paths_list_word_1);
		//relevant_doc_pos_list_final.addAll(relevant_doc_pos_list_word_1);
		doc_paths_i = relevant_doc_paths_list_final.iterator();
		
		
		while(doc_paths_i.hasNext()) {
			curr_doc_path = doc_paths_i.next();
			
			if(relevant_doc_paths_list_word_2.contains(curr_doc_path)) {
				index_list_1 = relevant_doc_paths_list_word_1.indexOf(curr_doc_path);
				relevant_doc_paths_list_word_1.remove(curr_doc_path);
				relevant_doc_pos_list_word_1.remove(index_list_1);
			}
			
		}
		relevant_doc_paths_list_final.clear();
		//relevant_doc_pos_list_final.clear();
		relevant_doc_paths_list_final.addAll(relevant_doc_paths_list_word_1);
		relevant_doc_pos_list_final.addAll(relevant_doc_pos_list_word_1);
	}
	
	/**
	 * {a, b} and {b} = {b}
	 */
	private void execute_AND_logic(){
		String curr_doc_path = null;
		String new_doc_pos = null;
		int index_list_1, index_list_2 = 0;
		
		Iterator<String> doc_paths_i;
		
		relevant_doc_paths_list_final.addAll(relevant_doc_paths_list_word_1);
		relevant_doc_pos_list_final.addAll(relevant_doc_pos_list_word_1);
		doc_paths_i = relevant_doc_paths_list_word_1.iterator();
			
		while(doc_paths_i.hasNext()) {
			curr_doc_path = doc_paths_i.next();
			
			//if document list don't have a certain document in common, remove it and its positions
			if(!relevant_doc_paths_list_word_2.contains(curr_doc_path)) {
				index_list_1 = relevant_doc_paths_list_word_1.indexOf(curr_doc_path);
				relevant_doc_paths_list_final.remove(curr_doc_path);
				relevant_doc_pos_list_final.remove(index_list_1);	
			}
		}
	
		doc_paths_i = relevant_doc_paths_list_final.iterator();
		while(doc_paths_i.hasNext()) {
			curr_doc_path = doc_paths_i.next();
				
			//if the document lists contain a common document, concatenate the document positions
			if(relevant_doc_paths_list_word_2.contains(curr_doc_path)) {
				index_list_2 = relevant_doc_paths_list_word_2.indexOf(curr_doc_path);
				index_list_1 = relevant_doc_paths_list_final.indexOf(curr_doc_path);
				new_doc_pos = relevant_doc_pos_list_word_2.get(index_list_2)+", "+relevant_doc_pos_list_final.get(index_list_1);
				relevant_doc_pos_list_final.set(index_list_1, new_doc_pos);	
			}
		}
	}
	
	/**
	 * {a} or {b} = {a, b}
	 */
	private void execute_OR_logic(){
		int index_list_1 = 0, index_list_2 = 0;
		String curr_doc_path = null;
		String new_doc_pos = null;
		
		 relevant_doc_paths_list_final = new ArrayList<String>();
		 relevant_doc_paths_list_final.addAll(relevant_doc_paths_list_word_1);
		 relevant_doc_pos_list_final = new ArrayList<String>();
		 relevant_doc_pos_list_final.addAll(relevant_doc_pos_list_word_1);
		 
		Iterator<String> doc_paths_i = relevant_doc_paths_list_word_2.iterator();
		while(doc_paths_i.hasNext()) {
			curr_doc_path = doc_paths_i.next();
			
			//if the document lists contain a common document, concatenate the document positions
			if(relevant_doc_paths_list_word_1.contains(curr_doc_path)) {
				index_list_1 = relevant_doc_paths_list_word_1.indexOf(curr_doc_path);
				index_list_2 = relevant_doc_paths_list_word_2.indexOf(curr_doc_path);
				new_doc_pos = relevant_doc_pos_list_word_1.get(index_list_1)+", "+relevant_doc_pos_list_word_2.get(index_list_2);
				relevant_doc_pos_list_final.set(index_list_1, new_doc_pos);
			
			//if document lists don't have a certain document in common, add the document and its positions. 
			}else if(!relevant_doc_paths_list_word_1.contains(curr_doc_path)) {
				relevant_doc_paths_list_final.add(curr_doc_path);
				index_list_2 = relevant_doc_paths_list_word_2.indexOf(curr_doc_path);
				new_doc_pos = relevant_doc_pos_list_word_2.get(index_list_2);
				relevant_doc_pos_list_final.add(new_doc_pos);
			}
		}
	}
	
	/**
	 * Prints results of user's query
	 * @return results as a String
	 */
	public String printResults(){
		Iterator<String> doc_paths_i = relevant_doc_paths_list_final.iterator();
		Iterator<String> doc_pos_i = relevant_doc_pos_list_final.iterator();
		System.out.println("Search results for sentence: '"+query_sentence+"'");
		String results = "";
		String curr_docpath = null;
		String curr_docpos = null;
		
		while((doc_paths_i.hasNext())&&(doc_pos_i.hasNext())) {
			curr_docpath = doc_paths_i.next();
			curr_docpos = doc_pos_i.next();
			System.out.println("Document:\n"+
								curr_docpath+"\n"+
								"Positions:"+"\n"+
								curr_docpos+"\n"+
								"________________________________");
			
			results+="Document:\n"+curr_docpath+"\n"+"Positions:"+"\n"+curr_docpos+"\n"+"________________________________\n";
		}
		return results;
	}
}
