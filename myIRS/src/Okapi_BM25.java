import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

public class Okapi_BM25 extends Primitive_Retrieval{
	
	//query term -> idf map
	HashMap<String, Double> query_terms_idf_map;
	//query term -> relevant documents
	HashMap<String, ArrayList<String>> query_terms_relev_docs_map;
	//query term -> relevant docIDs
	HashMap<String, ArrayList<Long>> query_terms_relev_docIDs_map;
	//query term -> tf list
	HashMap<String, ArrayList<Double>> query_terms_relev_docTF_map;
	HashMap<Long, String> overall_relev_docID_docpath_map;
	//docID -> score
	HashMap<Long, Double> docID_score;
	
	ArrayList<Long> arranged_docIDs;
	ArrayList<Double> sorted_scores;

	//number of documents in Document Collection
	long num_of_docs = 0;
	//total number of tokens in collection
	static long total_tokennum = 0;
	//average document length in tokens
	double avg_dl = 0;
	//free parameters of the Okapi_BM25 scoring function
	double k1 = 0, b = 0;
	String query_doc = null;
	
	static int instance_number = 0;
	
	/**
	 * 
	 * @param init_k1 free parameter (usually 2.0)
	 * @param init_b free parameter (usually 0.75)
	 */
	public Okapi_BM25(double init_k1, double init_b) {
		super();
		k1 = init_k1;
		b = init_b;
		num_of_docs = i_i.getNumOfDocs();
		if(instance_number<1){
			System.out.print("Counting docCollection token number, please wait...");
			total_tokennum = getTotalNumOfTokens();
			System.out.println(" Done.");
			instance_number++;
		}
		avg_dl = (double)total_tokennum/num_of_docs;
		query_terms_idf_map = new HashMap<String, Double>();
		query_terms_relev_docs_map = new HashMap<String, ArrayList<String>>();
		query_terms_relev_docIDs_map = new HashMap<String, ArrayList<Long>>();
		query_terms_relev_docTF_map = new HashMap<String, ArrayList<Double>>();
		overall_relev_docID_docpath_map = new HashMap<Long, String>();
		docID_score = new HashMap<Long, Double>();
		arranged_docIDs = new ArrayList<Long>();
		sorted_scores = new ArrayList<Double>();
	}
	
	/**
	 * Searches through the inverted index for relevant documents to the given query
	 * according to Okapi_BM25 ranking function.
	 */
	public void Query(String query){
		query_doc = query.toLowerCase();
		load_QueryTerms_compute_IDF();
		load_RelevantDocs_DocIDs_TFs();
		compute_RelevantDocsScore();
		//sort scores in descending order
		Utils ut = new Utils();
		sorted_scores.addAll(ut.sortValsDescending(docID_score));
		arranged_docIDs.addAll(ut.arrangeKeys(docID_score));
	}
	
	/**
	 * Prints Results of user's query
	 * Prints {Doc ID, Doc path, Relevance Score} for each relevant document
	 * @return results as a String
	 */
	public String printResults() {
		ArrayList<Long> docIDs = new ArrayList<Long>(overall_relev_docID_docpath_map.keySet());
		ArrayList<String> docpaths = new ArrayList<String>(overall_relev_docID_docpath_map.values());
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
	 * Computes score of every relevant document in Document Collection,
	 * according to the Okapi_BM25 ranking function.
	 */
	private void compute_RelevantDocsScore() {
		long curr_docID = 0;
		long curr_docID_tokkenum = 0;
		String curr_query_term = null;
		double curr_doc_score = 0;
		double curr_idf = 0;
		double curr_tf = 0;
		int index = 0;
		double score_numerator = 0;
		double score_denominator = 0;
		
		Iterator<Long> docIDs_i = overall_relev_docID_docpath_map.keySet().iterator();
		while(docIDs_i.hasNext()) {
		curr_docID = docIDs_i.next();
		curr_doc_score = 0;
			Iterator<String> query_term_i = query_terms_idf_map.keySet().iterator();
			while(query_term_i.hasNext()) {
				curr_query_term = query_term_i.next();
				curr_idf = query_terms_idf_map.get(curr_query_term);
				//if current query term appears in current document, extract its TF
				if(query_terms_relev_docIDs_map.get(curr_query_term).contains(curr_docID)) {
					index = query_terms_relev_docIDs_map.get(curr_query_term).indexOf(curr_docID);
					curr_tf = query_terms_relev_docTF_map.get(curr_query_term).get(index);
					curr_docID_tokkenum = getNumOfTokens(curr_docID);
					//and compute the score based on Okapi_BM25 ranking function
					score_numerator = curr_tf*(k1+1);
					score_denominator = curr_tf + k1*(1-b + b*(curr_docID_tokkenum/avg_dl));
					curr_doc_score += curr_idf*(score_numerator/score_denominator);
				}
			}
			docID_score.put(curr_docID, curr_doc_score);
			//System.out.println("docID "+curr_docID+" curr_docID_tokkenum "+ curr_docID_tokkenum);
		}
		//System.out.println(docID_score.toString());
	}
	
	/**
	 * Loads relevant docs, docIDS and TFs for every query term to the 
	 * query_terms_relev_docs_map, query_terms_relev_docIDs_map, query_terms_relev_docTF_map
	 * respectively. Also loads whole query relevant documents to overall_relev_docID_docpath_map.
	 */
	private void load_RelevantDocs_DocIDs_TFs() {
		String curr_term = null;
		String curr_doc = null;
		long curr_docID = 0;
		
		Iterator<String> q_terms_i = query_terms_idf_map.keySet().iterator();
		while(q_terms_i.hasNext()) {
			curr_term = q_terms_i.next();
			//if current term appears in documents,
			if(query_terms_idf_map.containsKey(curr_term)){
				super.Query(curr_term);
				//get its relevant docs and docIDs list
				query_terms_relev_docs_map.put(curr_term, super.relevant_doc_paths_list);
				query_terms_relev_docIDs_map.put(curr_term, super.relevant_docID_list);
				query_terms_relev_docTF_map.put(curr_term, super.term_tf_list);
				
				//and put the docs to the overall relevant doc_paths docID map
				Iterator<String> q_rev_docs_i = query_terms_relev_docs_map.get(curr_term).iterator();
				while(q_rev_docs_i.hasNext()) {
					curr_doc = q_rev_docs_i.next();
					curr_docID = Long.valueOf(curr_doc.substring(0, curr_doc.indexOf(" ")));
					curr_doc = curr_doc.substring(curr_doc.indexOf(" ")+1);
					overall_relev_docID_docpath_map.put(curr_docID, curr_doc);
				}
			}
		}
	//System.out.println("overall_relev_docID_docpath_map: "+overall_relev_docID_docpath_map.toString());
	//System.out.println("query_terms_relev_docs_map: "+query_terms_relev_docs_map.toString());
	//System.out.println("query_terms_relev_docIDs_map: "+query_terms_relev_docIDs_map.toString());
	//System.out.println("query_terms_relev_docTF_map: "+query_terms_relev_docTF_map.toString());
	}	
	
	/**
	 * Loads query terms from VocabularyFile.txt and their Df.
	 * Computes IDF for every query term and stores it to query_terms_idf_map
	 * CAUTION: Now, IDF = (|N|-df+0.5)/(df+0.5) for each query term
	 */
	@SuppressWarnings("static-access")
	private void load_QueryTerms_compute_IDF() {
		String curr_token = null;
		int curr_df = 0;
		double curr_idf = 0;
		StringTokenizer tokenizer = null;
		String delimiter = "1234567890~!@#$%^&*+=.,:;?<>{}[]()-_/|\" \t\n\r\f";
		tokenizer = new StringTokenizer(query_doc, delimiter);
		
		while(tokenizer.hasMoreTokens() ) {
			curr_token = tokenizer.nextToken();
			if(super.i_i.sorted_vocabulary_map.containsKey(curr_token)) {
				curr_df = super.i_i.sorted_vocabulary_map.get(curr_token);
				curr_idf = (double)(num_of_docs - curr_df +0.5)/(curr_df + 0.5);
				query_terms_idf_map.put(curr_token, curr_idf);
			}
		}
		//System.out.println(query_terms_idf_map.toString());
	}
	
	/**
	 * @return total number of tokens in Document Collection
	 */
	private long getTotalNumOfTokens() {
		long total_tokennum = 0;
		String line = null;
		
		try {
			RandomAccessFile raf_postingfile = new RandomAccessFile(i_i.ColIndex_path+"PostingFile.txt", "r");
			while((line = raf_postingfile.readLine())!= null) {
				line = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				String temp[] = line.split(", ");
				total_tokennum += temp.length;
			}
		}catch (FileNotFoundException e) {
		System.err.println("File PostingFile.txt not found.");
		System.exit(1);
		}
		catch (IOException e) {
		System.err.println("Error in reading file: PostingFile.txt");
		System.exit(1);
		}
	return total_tokennum;
	}
	
	/**
	 * @param docID
	 * @return number of tokens in the given docID of a document
	 */
	private long getNumOfTokens(long docID) {
	String line = null;
	long curr_docID = 0;
	long posting_fp = 0;
	long num_of_tokens = 0;
	//first we read the PostingPositionsOfDocsFile.txt to get the PostingFile file pointer at which the current
	//docID starts, in order to count its number of tokens
	try {
		RandomAccessFile raf_posting_doc_ptrs_file = new RandomAccessFile(i_i.ColIndex_path+"PostingPositionsOfDocsFile.txt", "r");
		while((line = raf_posting_doc_ptrs_file.readLine())!= null) {
			curr_docID = Long.valueOf(line.substring(0, line.indexOf(" ")));
			
			if(curr_docID == docID) {
				line = line.substring(line.indexOf(" ")+1, line.length());
				posting_fp = Long.valueOf(line);
			}
			
		}
	}catch (FileNotFoundException e) {
	System.err.println("File PostingPositionsOfDocsFile.txt not found.");
	System.exit(1);
	}
	catch (IOException e) {
	System.err.println("Error in reading file: PostingPositionsOfDocsFile.txt");
	System.exit(1);
	}
	//then we read the PostingFile.txt and start counting the number of tokens of the current docID
		try {
			RandomAccessFile raf_postingfile = new RandomAccessFile(i_i.ColIndex_path+"PostingFile.txt", "r");
			raf_postingfile.seek(posting_fp);
			line = raf_postingfile.readLine();
			curr_docID = docID;
			while(curr_docID == docID) {
				line = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				String temp[] = line.split(", ");
				num_of_tokens += temp.length;
				line = raf_postingfile.readLine();
				curr_docID = Long.valueOf(line.substring(0, line.indexOf(" ")));
			}	
		}catch (FileNotFoundException e) {
		System.err.println("File PostingFile.txt not found.");
		System.exit(1);
		}
		catch (IOException e) {
		System.err.println("Error in reading file: PostingFile.txt");
		System.exit(1);
		}
	return num_of_tokens;
	}
}
