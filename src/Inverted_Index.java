import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

public class Inverted_Index {
	
	//term -> df map
	HashMap<String, Integer> vocabulary_map = new HashMap<String, Integer>();
	//sorted term -> df map
	Map<String, Integer> sorted_vocabulary_map;
	//term -> pointers to postingfile map
	HashMap<String, ArrayList<Long>> vocab_posting_ptrs_map = new HashMap<String, ArrayList<Long>>();
	//sorted term -> pointers to postingfile map
	Map<String, ArrayList<Long>> sorted_vocab_posting_ptrs_map;
	
	//term -> freq map
	HashMap<String, Integer> terms_freqs_map;
	
	TreeSet<String> stopwords_treeset;
	
	//document path/filename -> DocID
	HashMap<String, Long> doc_id_map = new HashMap<String, Long>();
	
	//term -> position list map
	HashMap<String, ArrayList<Long>> terms_pos_map;
	ArrayList<Long> pos_list;
	
	//term -> tf map
	HashMap<String, Double> terms_tf_map;
	
	
	//Document Collection folders (possible subfolders NOT included)
	public String docCol_bigger_path = "documentCollection"+File.separator+"bigger"+File.separator;
	public String docCol_greek_path = "documentCollection"+File.separator+"greek"+File.separator;
	public String docCol_novels_path = "documentCollection"+File.separator+"novels"+File.separator;
	public String docCol_scientific_path = "documentCollection"+File.separator+"scientific"+File.separator;
	public String docCol_testing_path = "documentCollection"+File.separator+"testing"+File.separator;
	
	//Folder where VocabularyFile.txt and DocumentsFile.txt are to be stored
	public String ColIndex_path = "CollectionIndex"+File.separator;
	
	//docID++ for every new document analyzed
	long doc_id = 0;
	
	//used for computing file position of a recurrent term in a line
	String line_for_fp = null;

	//file pointer of postingfile
	long posting_fp = 0;
	
	/**
	 * Loads stopwords from a given filename.
	 * @param stopwords_file
	 */
	public void load_StopWords(String stopwords_file) {
		FileReader readConnToFile = null;
		try {
			readConnToFile = new FileReader(stopwords_file);
		} catch (FileNotFoundException e) {
			System.err.println("File "+stopwords_file+" not found.");
			System.exit(1);
		}
		BufferedReader reader = new BufferedReader(readConnToFile);
		stopwords_treeset = new TreeSet<String>();
		String line;
		try {
			while((line = reader.readLine())!=null){stopwords_treeset.add(line.toLowerCase());}
		} catch (IOException e) {
			System.err.println("Error in reading file: " +stopwords_file);
			System.exit(1);
		}
	}	
	
	/**
	 * Creates a terms and frequencies map from a given document file while checking and removing possible stopwords.
	 * Also maintains a map of terms and file positions(arraylist) of terms.
	 * @param document_file
	 */
	public void loadTerms_removeStopWords(String document_file) {
		StringTokenizer tokenizer = null;
		String delimiter = "1234567890~!@#$%^&*+=.,:;?<>{}[]()-_/|\" \t\n\r\f";
		String line = null, curr_token = null;
		terms_freqs_map = new HashMap<String, Integer>();
		int freq = 0;
		terms_pos_map = new HashMap<String, ArrayList<Long>>();
		long fp = 0;
		long file_pos;
		
		try {
		RandomAccessFile raf_doc = new RandomAccessFile(document_file, "r");
		while ((line = raf_doc.readLine()) != null){
			line = line.toLowerCase();
			line_for_fp = line;
			tokenizer = new StringTokenizer(line, delimiter);
			
			while(tokenizer.hasMoreTokens() ) {
				curr_token = tokenizer.nextToken();
				
				fp = raf_doc.getFilePointer();
				//compute and get the file position of the term in document
				file_pos = getFilePosition(curr_token, fp);
				pos_list = new ArrayList<Long>();
				
				//if this token is NOT a stopword (always lower case)
				if(!stopwords_treeset.contains(curr_token)) {
					
					//if term does not exist, we add it to our term_freqs map (always lower case)
					if(!terms_freqs_map.containsKey(curr_token)) {
						terms_freqs_map.put(curr_token, 1);
						
						//also register the file position of the new term
						pos_list.add(file_pos);
						terms_pos_map.put(curr_token, pos_list);
					}
					//if it exists, we increase its frequency by 1
					else if(terms_freqs_map.containsKey(curr_token)) {
						freq = terms_freqs_map.get(curr_token);
						terms_freqs_map.put(curr_token, freq+1);
						
						//also add the file position of the term to its positions list
						pos_list.addAll(terms_pos_map.get(curr_token));
						pos_list.add(file_pos);
						terms_pos_map.put(curr_token, pos_list);
					}
				}
			}
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
	
	public void build_InvertedIndex(){
		String[] Files;
		File documentCollection = null;
		File ColIndex_folder = new File("CollectionIndex");
		if (!ColIndex_folder.exists()) ColIndex_folder.mkdir();
		
		documentCollection = new File(docCol_bigger_path);
		Files = documentCollection.list();
		
		for(int i=0; i<Files.length; i++){
			refresh_Documents2DocID_map(docCol_bigger_path+Files[i]);
			save_PostingPositionsOfDocsFile(doc_id);
			save_DocumentsFile(docCol_bigger_path+Files[i]);
			loadTerms_removeStopWords(docCol_bigger_path+Files[i]);
			compute_tf();
			refresh_Vocabulary();	
			save_PostingFile(docCol_bigger_path+Files[i]);	
		}
		
		documentCollection = new File(docCol_greek_path);
		Files = documentCollection.list();
		
		for(int i=0; i<Files.length; i++){
			refresh_Documents2DocID_map(docCol_greek_path+Files[i]);
			save_PostingPositionsOfDocsFile(doc_id);
			save_DocumentsFile(docCol_greek_path+Files[i]);
			loadTerms_removeStopWords(docCol_greek_path+Files[i]);
			compute_tf();
			refresh_Vocabulary();	
			save_PostingFile(docCol_greek_path+Files[i]);	
		}
		
		documentCollection = new File(docCol_novels_path);
		Files = documentCollection.list();
		
		for(int i=0; i<Files.length; i++){
			refresh_Documents2DocID_map(docCol_novels_path+Files[i]);
			save_PostingPositionsOfDocsFile(doc_id);
			save_DocumentsFile(docCol_novels_path+Files[i]);
			loadTerms_removeStopWords(docCol_novels_path+Files[i]);
			compute_tf();
			refresh_Vocabulary();	
			save_PostingFile(docCol_novels_path+Files[i]);	
		}
		
		documentCollection = new File(docCol_scientific_path);
		Files = documentCollection.list();
		
		for(int i=0; i<Files.length; i++){
			refresh_Documents2DocID_map(docCol_scientific_path+Files[i]);
			save_PostingPositionsOfDocsFile(doc_id);
			save_DocumentsFile(docCol_scientific_path+Files[i]);
			loadTerms_removeStopWords(docCol_scientific_path+Files[i]);
			compute_tf();
			refresh_Vocabulary();	
			save_PostingFile(docCol_scientific_path+Files[i]);	
		}
		
		documentCollection = new File(docCol_testing_path);
		Files = documentCollection.list();
		
		for(int i=0; i<Files.length; i++){
			refresh_Documents2DocID_map(docCol_testing_path+Files[i]);
			save_PostingPositionsOfDocsFile(doc_id);
			save_DocumentsFile(docCol_testing_path+Files[i]);
			loadTerms_removeStopWords(docCol_testing_path+Files[i]);
			compute_tf();
			refresh_Vocabulary();	
			save_PostingFile(docCol_testing_path+Files[i]);	
		}
		sort_Vocabulary();
		save_Vocabulary();
	}
	
	/**
	 * Loads VocabularyFile.txt to main memory. 
	 * (vocabulary_map, sorted_vocabulary_map, vocab_posting_ptrs_map, sorted_vocab_posting_ptrs_map)
	 */
	public void load_Vocabulary(){
		String line = null;
		String curr_term = null;
		String curr_df_str = null;
		int curr_df = 0;
		ArrayList<Long> postptrs_al;
		
		try {
			RandomAccessFile vocab_doc = new RandomAccessFile(ColIndex_path+"VocabularyFile.txt", "r");
			while ((line = vocab_doc.readLine()) != null){
				curr_term = line.substring(0, line.indexOf(" "));
				
				line = line.substring(line.indexOf(" ")+1);
				curr_df_str = line.substring(0, line.indexOf(" "));
				curr_df = Integer.valueOf(curr_df_str);
				
				vocabulary_map.put(curr_term, curr_df);
				postptrs_al = get_PostingPointers(line);
				vocab_posting_ptrs_map.put(curr_term, postptrs_al);
			}
			sort_Vocabulary();
		}catch (FileNotFoundException e) {
			System.err.println("File VocabularyFile.txt not found.");
			System.exit(1);
		}
		catch (IOException e) {
		System.err.println("Error in reading file: VocabularyFile.txt");
		System.exit(1);
		}
	}
	
	/**
	 * @return number of documents in document collection
	 */
	public long getNumOfDocs() {
		String line = null;
		long length = 0;
		long move = 2;
		long num_of_docs = 0;
	
		try {
			RandomAccessFile documents_doc = new RandomAccessFile(ColIndex_path+"DocumentsFile.txt", "r");
			length = documents_doc.length();
			documents_doc.seek(length - move);
			while (documents_doc.read()!= '\n')documents_doc.seek(length - ++move); 
			line = documents_doc.readLine();
			line = line.substring(0, line.indexOf(" "));
			num_of_docs = Long.valueOf(line);
		
		}catch (FileNotFoundException e) {
			System.err.println("File DocumentsFile.txt not found.");
			System.exit(1);
		}
		catch (IOException e) {
			System.err.println("Error in reading file: DocumentsFile.txt");
			System.exit(1);
		}
		return num_of_docs;
	}
		
	/**
	 * Extracts postingfile pointers from a given line from VocabularyFile.txt.
	 * @param line
	 * @return ArrayList<Long> of postingfile pointers
	 */
	private ArrayList<Long> get_PostingPointers(String line){
		line = line.substring(line.indexOf("[")+1, line.indexOf("]"));
		
		ArrayList<Long> postptrs_al = new ArrayList<Long>();
		String[] postptrs;
		postptrs = line.split(", ");
		for(int i=0; i<postptrs.length; i++) postptrs_al.add(Long.valueOf(postptrs[i]));
		return postptrs_al;
	}
	
	/**
	 * Computes and returns the position of current token in current line of current document file.
	 * File must end with a new line!
	 * @param curr_token
	 * @param fp
	 * @return
	 */
	private long getFilePosition(String curr_token,  long fp){
		int token_pos = line_for_fp.indexOf(curr_token);
		char[] line_for_fp_2char = line_for_fp.toCharArray();
		
		//mark the token so we won't compute it again
		line_for_fp_2char[token_pos] = '$';
		line_for_fp = new String(line_for_fp_2char);
		fp = fp - line_for_fp.length() -2 + token_pos + 1;
		return fp;	
	}
	
	/**
	 * For the given term, refresh its posting pointers.
	 * @param curr_term
	 */
	private void refresh_PostingPointers(String curr_term){
		ArrayList<Long> ptrs = new ArrayList<Long>();
		
		if (!vocab_posting_ptrs_map.containsKey(curr_term)) {
			ptrs.add(posting_fp);
			vocab_posting_ptrs_map.put(curr_term, ptrs);
			
		}else if(vocab_posting_ptrs_map.containsKey(curr_term)) {
			ptrs.addAll(vocab_posting_ptrs_map.get(curr_term));
			ptrs.add(posting_fp);
			vocab_posting_ptrs_map.put(curr_term, ptrs);
		}
	}
	
	/**
	 * Stores PostingFile to CollectionIndex/PostingFile.txt.
	 * Also refreshes pointers to postingfile(line nums) for every term.
	 * Info stored: {DocID tf positions}.
	 * It must be called after analyzing a new document file. (loadTerms_removeStopWords)
	 * @param document_path
	 */
	private void save_PostingFile(String document_path) {
		FileWriter writeConnToFile = null;
		try {
			writeConnToFile = new FileWriter(ColIndex_path+"PostingFile.txt", true);
		} catch (IOException e) {
			System.err.println("Error writing to file: PostingFile.txt");
			System.exit(1);
		}
		PrintWriter printer = new PrintWriter(new BufferedWriter(writeConnToFile));
		
		Iterator<Double> tf_i = terms_tf_map.values().iterator();
		Iterator<String> terms_i = terms_tf_map.keySet().iterator();
		double curr_tf = 0;
		String curr_term;
		String line = null;
		long line_length = 0;
		
		while(tf_i.hasNext()&&(terms_i.hasNext())){
			curr_tf = tf_i.next();
			curr_term = terms_i.next();
			line = doc_id_map.get(document_path)+" "+curr_tf+" "+terms_pos_map.get(curr_term).toString();
			line_length = line.length();
			printer.println(line);
			refresh_PostingPointers(curr_term);
			posting_fp += line_length + 2;//+2 = '\n'
		}
		printer.close();
	}
	
	/**
	 * Saves the exact file pointer at which each document (docID) begins in PostingFile.txt
	 * at file PostingPositionsOfDocsFile.txt
	 * @param DocID
	 */
	private void save_PostingPositionsOfDocsFile(long DocID){
		FileWriter writeConnToFile = null;
		try {
			writeConnToFile = new FileWriter(ColIndex_path+"PostingPositionsOfDocsFile.txt", true);
		} catch (IOException e) {
			System.err.println("Error writing to file: PostingPositionsOfDocsFile.txt");
			System.exit(1);
		}
		PrintWriter printer = new PrintWriter(new BufferedWriter(writeConnToFile));
		printer.println(DocID+" "+posting_fp);
		printer.close();
	}
	/**
	 * Sorts the vocabulary in lexicographically ascending order of keys.
	 */
	private void sort_Vocabulary(){
		sorted_vocabulary_map = new TreeMap<String, Integer>(vocabulary_map);
		sorted_vocab_posting_ptrs_map = new TreeMap<String, ArrayList<Long>>(vocab_posting_ptrs_map);
		//System.out.println(sorted_vocabulary_map.toString());
		//System.out.println(sorted_vocab_posting_ptrs_map.toString());
	}
	
	/**
	 * Saves sorted vocabulary to CollectionIndex/VocabularyFile.txt.
	 * Info stored: {Term df [posting_ptrs]}
	 * Sort first, save afterwards!
	 */
	private void save_Vocabulary(){
		FileWriter writeConnToFile = null;
		try {
			writeConnToFile = new FileWriter(ColIndex_path+"VocabularyFile.txt", false);
		} catch (IOException e) {
			System.err.println("Error writing to file: VocabularyFile.txt");
			System.exit(1);
		}
		PrintWriter printer = new PrintWriter(new BufferedWriter(writeConnToFile));
		Iterator<String> keys_i = sorted_vocabulary_map.keySet().iterator();
		String curr_vocab_term = null;
		int curr_df = 0;
		
		while(keys_i.hasNext()){
			curr_vocab_term = keys_i.next();
			curr_df = sorted_vocabulary_map.get(curr_vocab_term);
			printer.println(curr_vocab_term+" "+curr_df+" "+sorted_vocab_posting_ptrs_map.get(curr_vocab_term).toString());	
		}
		printer.close();
	}

	/**
	 * Refreshes the vocabulary, adding a new term. If term already exists, increases its df by 1
	 * It must be called after analyzing a new document file. (loadTerms_removeStopWords)
	 * It must be called after computing tf.
	 */
	private void refresh_Vocabulary(){
		String curr_term;
		int curr_df;
		Iterator<String> keys_i = terms_freqs_map.keySet().iterator();
		while(keys_i.hasNext()) {
			curr_term = keys_i.next();
			if(!vocabulary_map.containsKey(curr_term)) {
				vocabulary_map.put(curr_term, 1);
			}else if(vocabulary_map.containsKey(curr_term)) {
				curr_df =  (Integer)vocabulary_map.get(curr_term);
				vocabulary_map.put(curr_term, curr_df+1);
			}
		}
	}
	
	/**
	 * Refreshes doc_id map with the path/filename of current document being analyzed.
	 * It must be called before save_DocumentsFile()
	 * @param document_path
	 */
	private void refresh_Documents2DocID_map(String document_path){
		doc_id++;
		doc_id_map.put(document_path, doc_id);
	}
	
	/**
	 * Saves doc_id and path of current document to CollectionIndex/DocumentsFile.txt
	 * Info stored: {doc_id document_path}
	 * It must be called after analyzing a new document file. (loadTerms_removeStopWords)
	 * It must be called after refreshing documents.
	 * @param document_path
	 */
	private void save_DocumentsFile(String document_path){
		FileWriter writeConnToFile = null;
		try {
			writeConnToFile = new FileWriter(ColIndex_path+"DocumentsFile.txt", true);
		} catch (IOException e) {
			System.err.println("Error writing to file: DocumentsFile.txt");
			System.exit(1);
		}
		PrintWriter printer = new PrintWriter(new BufferedWriter(writeConnToFile));
		printer.println(doc_id+" "+document_path);
		printer.close();
	}

	/**
	 * Computes tf for every term stored in the terms_freqs_map, after reading current document file.
	 * Stores tf in HashMap<String, Double> terms_tf_map for every document file.
	 * It must be called after analyzing a new document file. (loadTerms_removeStopWords)
	 * It must be called before refreshing vocabulary.
	 */
	private void compute_tf(){
		terms_tf_map = new HashMap<String, Double>();
		Iterator<String> keys_i = terms_freqs_map.keySet().iterator();
		String curr_term=null;
		int curr_freq = 0;
		int max_freq = Collections.max(terms_freqs_map.values());
		double tf = 0;
		
		while(keys_i.hasNext()) {
			curr_term = keys_i.next();
			curr_freq = terms_freqs_map.get(curr_term);
			
			tf = (double)curr_freq/max_freq;
			
			terms_tf_map.put(curr_term, tf);
		}
	}
}
