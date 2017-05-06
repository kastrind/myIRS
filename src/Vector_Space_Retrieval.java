import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class Vector_Space_Retrieval extends Primitive_Retrieval {
	Inverted_Index i_i;
	
	//query_term -> frequency map
	HashMap<String, Integer> query_terms_freqs_map;
	//query_term -> df map
	HashMap<String, Integer> query_terms_df_map;
	//query_term -> weight map
	HashMap<String, Double> query_terms_weights_map;
	//query_term -> relevant_doc list (doc = "docID docPATH") map
	HashMap<String, ArrayList<String>> query_terms_relev_docs_map;
	//query_term -> relevant_docID_list map
	HashMap<String, ArrayList<Long>> query_terms_relev_docIDs_map;
	//relevant_doc_path -> docID map
	HashMap<String, Long> overall_relev_doc_path_docID_map;
	
	//doc_term -> weights map
	HashMap<String, Double> doc_term_weights_map;
	//docID -> doc_terms -> weights map
	HashMap<Long, HashMap<String, Double>> docID_weightvectors_map;
	//docID -> weight_modulus_squared
	HashMap<Long, Double> docID_weight_modulus_squared;
	//docID -> score
	HashMap<Long, Double> docID_relevance_score;

	ArrayList<Long> arranged_docIDs;
	ArrayList<Double> sorted_scores;
	
	//user's query
	String query_doc= null;
	//used for computing idf (idf = docs_quantity/df)
	long docs_quantity = 0;
	//used for computing tf (tf = freq/Max{freq})
	int query_max_freq = 0;
	
	// |q| = x^2 + y^2 + z^2 ( where q = (x, y, z) )
	double query_vector_modulus_squared = 0;
	
	@SuppressWarnings("static-access")
	public Vector_Space_Retrieval(){
		super();
		i_i = new Inverted_Index();
		query_terms_freqs_map = new HashMap<String, Integer>();
		query_terms_df_map = new HashMap<String, Integer>();
		query_terms_weights_map = new HashMap<String, Double>();
		docs_quantity = super.i_i.getNumOfDocs();
		query_terms_relev_docs_map = new HashMap<String, ArrayList<String>>();
		query_terms_relev_docIDs_map = new HashMap<String, ArrayList<Long>>();
		overall_relev_doc_path_docID_map = new HashMap<String, Long>();
		doc_term_weights_map = new HashMap<String, Double>();
		docID_weightvectors_map = new HashMap<Long, HashMap<String, Double>>();
		docID_weight_modulus_squared = new HashMap<Long, Double>();
		docID_relevance_score = new HashMap<Long, Double>();
		arranged_docIDs = new ArrayList<Long>();
		sorted_scores = new ArrayList<Double>();
	}
	
	/**
	 * Searches through the inverted index for relevant documents to the given query
	 * according to the Vector Space Retrieval Model.
	 */
	public void Query(String query){
		query_doc = query.toLowerCase();
		loadQueryTerms_computeFreqs();
		query_max_freq = getQueryMaxFreq();
		load_QueryDf();
		compute_QueryTermWeights();
		compute_QueryVectorModulusSquared();
		load_RelevantDocs_DocIDs();
		compute_RelevantDocsWeightVectors();
		compute_RelevantDocsWeightsModulusSquared();
		compute_RelevantDocsScore();
		//sort scores in descending order
		Utils ut = new Utils();
		sorted_scores.addAll(ut.sortValsDescending(docID_relevance_score));
		arranged_docIDs.addAll(ut.arrangeKeys(docID_relevance_score));
	}
	
	/**
	 * Prints Results of user's query
	 * Prints {Doc ID, Doc path, Relevance Score} for each relevant document
	 * @return results as a String
	 */
	 public String printResults() {
		ArrayList<Long> docIDs = new ArrayList<Long>(overall_relev_doc_path_docID_map.values());
		ArrayList<String> docpaths = new ArrayList<String>(overall_relev_doc_path_docID_map.keySet());
		long curr_docID = 0;
		int index = 0;
		String curr_docpath = null;
		double curr_relevance = 0;
		System.out.println("Search Results:");
		Iterator<Long> docID_i = arranged_docIDs.iterator();
		String results = "";
		
		while(docID_i.hasNext()) {
			curr_docID = (Long)docID_i.next();
			index = docIDs.indexOf(curr_docID);
			curr_docpath = docpaths.get(index);
			curr_relevance = sorted_scores.get(arranged_docIDs.indexOf(curr_docID));
			System.out.println("Doc ID: "+curr_docID+"\n"+
							   "Doc path: "+curr_docpath+"\n"+
							   "Relevance Score: "+curr_relevance+"\n"+
							   "________________________________");
			results+="Doc ID: "+curr_docID+"\n"+"Doc path: "+curr_docpath+"\n"+"Relevance Score: "+curr_relevance+"\n"+"________________________________\n";
		}
		return results;
	}
	
	/**
	 * Computes CosSim Relevance for every relevant document,
	 * according to the Vector Space Retrieval Model
	 */
	private void compute_RelevantDocsScore() {
		long curr_docID = 0;
		double curr_weight_modulus_squared = 0;
		double curr_denominator = 0;
		HashMap<Long, Double> docID_R_denominator = new HashMap<Long, Double>();
		
		//compute denominator of CosSim Relevance formula for every relevant document
		Iterator<Long> docIDs_i = docID_weight_modulus_squared.keySet().iterator();
		while(docIDs_i.hasNext()) {
			curr_docID = (Long)docIDs_i.next();
			curr_weight_modulus_squared = docID_weight_modulus_squared.get(curr_docID);
			curr_denominator = Math.sqrt(curr_weight_modulus_squared*query_vector_modulus_squared);
			docID_R_denominator.put(curr_docID, curr_denominator);
		}
		//System.out.println("docID_R_denominator: "+docID_R_denominator.toString());
		
		String curr_query_term = null;
		double curr_query_term_w = 0, curr_doc_term_w = 0;
		double curr_numerator = 0;
		HashMap<Long, Double> docID_R_numerator = new HashMap<Long, Double>();
		
		//compute numerator of CosSim Relevance formula for every relevant document
		docIDs_i = docID_weightvectors_map.keySet().iterator();
		while(docIDs_i.hasNext()) {
			curr_docID = (Long)docIDs_i.next();
			curr_numerator = 0;
			Iterator<String> q_terms_i = query_terms_weights_map.keySet().iterator();
			while(q_terms_i.hasNext()) {
				curr_query_term = q_terms_i.next();
				curr_query_term_w = query_terms_weights_map.get(curr_query_term);
				if(docID_weightvectors_map.get(curr_docID).containsKey(curr_query_term)) {
					curr_doc_term_w = docID_weightvectors_map.get(curr_docID).get(curr_query_term);
					curr_numerator += curr_doc_term_w*curr_query_term_w;
				}
			}
			docID_R_numerator.put(curr_docID, curr_numerator);
		}
		//System.out.println("docID_R_numerator: "+docID_R_numerator.toString());
		
		//compute numerator/denominator of CosSim Relevance formula for every relevant document
		double curr_relevance = 0;
		docIDs_i = docID_R_numerator.keySet().iterator();
		while(docIDs_i.hasNext()) {
			curr_docID = (Long)docIDs_i.next();
			curr_relevance = docID_R_numerator.get(curr_docID)/docID_R_denominator.get(curr_docID);
			docID_relevance_score.put(curr_docID, curr_relevance);
		}
		//System.out.println("docID_relevance scores: "+docID_relevance_score.toString());
	} 
	
	/**
	 * Computes Weight Modulus Squared for every relevant docID
	 * and stores it to docID_weight_modulus_squared map.
	 */
	private void compute_RelevantDocsWeightsModulusSquared() {
		long curr_docID = 0;
		double curr_weight = 0;
		double weight_modulus_squared = 0;
		
		Iterator<Long> docIDs_i = docID_weightvectors_map.keySet().iterator();
		Iterator<Double> weights_i;
		while(docIDs_i.hasNext()) {
			curr_docID = (Long)docIDs_i.next();
			weight_modulus_squared = 0;
			weights_i = docID_weightvectors_map.get(curr_docID).values().iterator();
			while(weights_i.hasNext()) {
				curr_weight = (Double)weights_i.next();
				weight_modulus_squared += curr_weight*curr_weight;
			}
			docID_weight_modulus_squared.put(curr_docID, weight_modulus_squared);
		}
		//System.out.println("docID_weight_modulus_squared: "+docID_weight_modulus_squared.toString());
	}
	
	/**
	 * Generate weight vectors for every relevant document in the collection
	 * according to the Vector Space Retrieval Model. (w = tf*idf)
	 * Put each vector to docID_weightsvectors_map
	 */
	private void compute_RelevantDocsWeightVectors() {
		i_i.load_StopWords("stopWords.txt");
		String curr_doc_path = null;
		long curr_docID = 0;
		Iterator<String> i = overall_relev_doc_path_docID_map.keySet().iterator();
		//for every document in collection
		while(i.hasNext()) {
			curr_doc_path = i.next();
			curr_docID = overall_relev_doc_path_docID_map.get(curr_doc_path);
			doc_term_weights_map.clear();
			compute_DocTermsWeights(curr_doc_path);
			HashMap<String, Double> temp = new HashMap<String, Double>();
			temp.putAll(doc_term_weights_map);
			docID_weightvectors_map.put(curr_docID, temp);
		}
		//System.out.println("docID_weightvectors_map"+docID_weightvectors_map.toString());
	}
	
	/**
	 * Computes weights for every term in the given document file
	 * according to the Vector Space Retrieval Model. (w = tf*idf)
	 * Loads weights to map doc_term_weights_map.
	 */
	@SuppressWarnings("static-access")
	private void compute_DocTermsWeights(String document_file) {
		StringTokenizer tokenizer = null;
		String delimiter = "1234567890~!@#$%^&*+=.,:;?<>{}[]()-_/|\" \t\n\r\f";
		String line = null, curr_token = null;
		Long curr_docID = null;
		int index = 0;
		int curr_df = 0;
		double curr_tf = 0;
		double curr_idf = 0;
		double curr_weight = 0;
		
		try {
			RandomAccessFile raf_doc = new RandomAccessFile(document_file, "r");
			while ((line = raf_doc.readLine()) != null){
				line = line.toLowerCase();
				tokenizer = new StringTokenizer(line, delimiter);
				
				while(tokenizer.hasMoreTokens() ) {
					curr_token = tokenizer.nextToken();
					//if current token is NOT a stopword
					if(!i_i.stopwords_treeset.contains(curr_token)) {
						//and if current token exists in vocabulary
						if(super.i_i.sorted_vocabulary_map.containsKey(curr_token)) {
						//compute its weight
						curr_df = super.i_i.sorted_vocabulary_map.get(curr_token);
						super.Query(curr_token);
						curr_docID = overall_relev_doc_path_docID_map.get(document_file);
					
						if(super.relevant_docID_list.contains(curr_docID)) {
							index = super.relevant_docID_list.indexOf(curr_docID);
							curr_tf = super.term_tf_list.get(index);
							curr_idf = (double)docs_quantity/curr_df;
							curr_weight = curr_tf*curr_idf;
							doc_term_weights_map.put(curr_token, curr_weight);
						}
						}
					}
				}
			//System.out.println(doc_term_weights_map.toString());
			}
		}catch (FileNotFoundException e) {
			System.err.println("File "+document_file+" not found.");
			System.exit(1);
			}
			catch (IOException e) {
			System.err.println("Error in reading file: " +document_file);
			System.exit(1);
			}
	}
	
	/**
	 * Loads the query relevant docs, docIDs, to the query_terms_relev_docs_map,
	 * query_terms_relev_docIDs_map and overall_relev_docs_docID_map.
	 */
	private void load_RelevantDocs_DocIDs() {
		String curr_term = null;
		String curr_doc = null;
		long curr_docID = 0;
		
		Iterator<String> q_terms_i = query_terms_weights_map.keySet().iterator();
		while(q_terms_i.hasNext()) {
			curr_term = q_terms_i.next();
			//if current term appears in documents,
			if(query_terms_df_map.get(curr_term)!=0){
				super.Query(curr_term);
				//get its relevant docs and docIDs list
				query_terms_relev_docs_map.put(curr_term, super.relevant_doc_paths_list);
				query_terms_relev_docIDs_map.put(curr_term, super.relevant_docID_list);
				
				//and put the docs to the overall relevant doc_paths docID map
				Iterator<String> q_rev_docs_i = query_terms_relev_docs_map.get(curr_term).iterator();
				while(q_rev_docs_i.hasNext()) {
					curr_doc = q_rev_docs_i.next();
					curr_docID = Long.valueOf(curr_doc.substring(0, curr_doc.indexOf(" ")));
					curr_doc = curr_doc.substring(curr_doc.indexOf(" ")+1);
					overall_relev_doc_path_docID_map.put(curr_doc, curr_docID);
				}
			}
		}
		//System.out.println("overall_relev_doc_path_docID_map: "+overall_relev_doc_path_docID_map.toString());
	}	
	
	/**
	 * Loads query terms to query_terms_freqs map while computing their frequencies
	 */
	private void loadQueryTerms_computeFreqs() {
		int freq = 0;
		String curr_token = null;
		StringTokenizer tokenizer = null;
		String delimiter = "1234567890~!@#$%^&*+=.,:;?<>{}[]()-_/|\" \t\n\r\f";
		tokenizer = new StringTokenizer(query_doc, delimiter);
		
		while(tokenizer.hasMoreTokens() ) {
			curr_token = tokenizer.nextToken();
		
			//if query term is new, put it in the map
			if(!query_terms_freqs_map.containsKey(curr_token)) {
				query_terms_freqs_map.put(curr_token, 1);
			//else if query term has been seen before, increase its frequency by 1 
			}else if(query_terms_freqs_map.containsKey(curr_token)) {
				freq = query_terms_freqs_map.get(curr_token);
				query_terms_freqs_map.put(curr_token, freq+1);
			}
		}
		//System.out.println(query_terms_freqs_map.toString());
	}
	
	/**
	 * @return the maximum term frequency in the query
	 */
	private int getQueryMaxFreq() {
		return Collections.max(query_terms_freqs_map.values());
	}
	
	/**
	 * Loads df for every query term in VocabularyFile.txt to query_terms_df_map.
	 */
	@SuppressWarnings("static-access")
	private void load_QueryDf() {
		String curr_term = null;
		int curr_df = 0;
		
		Iterator<String> q_terms = query_terms_freqs_map.keySet().iterator();
		while(q_terms.hasNext()) {
			curr_term = q_terms.next();
			
			if(super.i_i.sorted_vocabulary_map.containsKey(curr_term)) {
				curr_df = super.i_i.sorted_vocabulary_map.get(curr_term);
				query_terms_df_map.put(curr_term, curr_df);
			}else if(!super.i_i.sorted_vocabulary_map.containsKey(curr_term)) {
				query_terms_df_map.put(curr_term, 0);
			}
		}
		//System.out.println(query_terms_df_map.toString());
	}
	
	/**
	 * Computes and loads query term weights to query_terms_weights_map
	 * according to the Vector Space Retrieval Model. (w = tf*idf)
	 */
	private void compute_QueryTermWeights(){
		int curr_freq = 0, curr_df = 0;
		double curr_tf = 0, curr_idf = 0;
		double curr_weight = 0;
		String curr_term = null;
		
		Iterator<String> q_terms_i = query_terms_freqs_map.keySet().iterator();
		while(q_terms_i.hasNext()) {
			curr_term = q_terms_i.next();
			curr_freq = query_terms_freqs_map.get(curr_term);
			curr_tf = (double)curr_freq/query_max_freq;
			curr_df = query_terms_df_map.get(curr_term);
			curr_idf = (double)docs_quantity/curr_df;
			
			if(curr_df!=0)curr_weight = curr_tf*curr_idf;
			else curr_weight = 0;
			query_terms_weights_map.put(curr_term, curr_weight);
		}
		//System.out.println(query_terms_weights_map.toString());
	}
	
	private void compute_QueryVectorModulusSquared() {
		double curr_weight = 0;
		String curr_term = null;
		
		Iterator<String> q_terms_i = query_terms_weights_map.keySet().iterator();
		while(q_terms_i.hasNext()) {
			curr_term = q_terms_i.next();
			curr_weight = query_terms_weights_map.get(curr_term);
			query_vector_modulus_squared += curr_weight*curr_weight;
		}
		//System.out.println("vector_modulus_squared = "+query_vector_modulus_squared);
	}
}
