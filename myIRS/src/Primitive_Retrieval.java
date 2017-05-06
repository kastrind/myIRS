import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

public class Primitive_Retrieval {

	static Inverted_Index i_i;
	static int instance_number = 0;
	
	//term postingfile pointers list
	ArrayList<Long> term_posting_ptrs;
	//relevant docIDs for the query word
	ArrayList<Long> relevant_docID_list;
	//tf for every doc where query word appears
	ArrayList<Double> term_tf_list;
	//stores "docID docPATH"
	ArrayList<String> relevant_doc_paths_list;
	ArrayList<String> relevant_doc_pos_list;
	
	String query_word = null;

	public Primitive_Retrieval(){
		if(instance_number<1) {
		i_i = new Inverted_Index();
		System.out.print("Loading Vocabulary, please wait...");
		i_i.load_Vocabulary();
		System.out.println(" Done.");
		instance_number++;
		}
	}
	
	/**
	 * Searches the inverted index for the given word and prints the results to console.
	 * @param word
	 */
	public void Query(String word){
		word = word.toLowerCase();
		query_word = word;
		term_posting_ptrs = new ArrayList<Long>();
		
		//needed for Vector Space Retrieval
		relevant_docID_list = new ArrayList<Long>();
		term_tf_list = new ArrayList<Double>();
		
		//if word is not contained in the vocabulary, then there is no such word in the Collection
		if(i_i.sorted_vocab_posting_ptrs_map.containsKey(word)){
			term_posting_ptrs.addAll(i_i.sorted_vocab_posting_ptrs_map.get(word));
			search_PostingFile();
			search_DocumentsFile();
		//this is for Boolean_Retrieval logic to work
		}else if(!i_i.sorted_vocab_posting_ptrs_map.containsKey(word)) {
			relevant_doc_paths_list = new ArrayList<String>();
			relevant_doc_pos_list = new ArrayList<String>();
		}
	}
	
	private void search_PostingFile(){
		String line = null;
		long curr_docID = 0;
		String relev_doc_pos = null;
		double curr_tf = 0;
		
		try {
			RandomAccessFile raf_postingfile = new RandomAccessFile(i_i.ColIndex_path+"PostingFile.txt", "r");
			
			relevant_doc_pos_list = new ArrayList<String>();
			
			//for every postingfile pointer
			for(int i=0; i<term_posting_ptrs.size(); i++) {
				//locate the corresponding line in postingfile
				raf_postingfile.seek(Long.valueOf(term_posting_ptrs.get(i)));
				line = raf_postingfile.readLine();
				//extract the relevant docID
				curr_docID = Long.valueOf(line.substring(0, line.indexOf(" ")));
				relevant_docID_list.add(curr_docID);
				
				//extract term's tf for the current document
				curr_tf = Double.valueOf(line.substring(line.indexOf(" ")+1, line.indexOf("[")-1));
				term_tf_list.add(curr_tf);
				
				//store the file positions of the relevant doc to the relevant_doc_pos_list
				relev_doc_pos = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				relevant_doc_pos_list.add(relev_doc_pos);
			}
		}catch (FileNotFoundException e) {
		System.err.println("File PostingFile.txt not found.");
		System.exit(1);
		}
		catch (IOException e) {
		System.err.println("Error in reading file: PostingFile.txt");
		System.exit(1);
		}
	}
	
	private void search_DocumentsFile(){
		long line_num = 0;
		String line = null, relev_doc_path = null;
		relevant_doc_paths_list = new ArrayList<String>();
		
		try {
			RandomAccessFile raf_docsfile = new RandomAccessFile(i_i.ColIndex_path+"DocumentsFile.txt", "r");
			
			for(int i=0; i<relevant_docID_list.size(); i++) {
			
				while(line_num<relevant_docID_list.get(i)) {
					line = raf_docsfile.readLine();
					line_num++;
				}
				//store the relevant docID and document path to relevant_doc_paths_list
				relev_doc_path = line;
				relevant_doc_paths_list.add(relev_doc_path);	
			}
		}
		catch (FileNotFoundException e) {
		System.err.println("File DocumentsFile.txt not found.");
		System.exit(1);
		}
		catch (IOException e) {
		System.err.println("Error in reading file: DocumentsFile.txt");
		System.exit(1);
		}
	}
	
	public String printResults(){
		Iterator<String> doc_paths_i = relevant_doc_paths_list.iterator();
		Iterator<String> doc_pos_i = relevant_doc_pos_list.iterator();
		System.out.println("Search Results for word: '"+query_word+"'");
		String results = "";
		
		while((doc_paths_i.hasNext())&&(doc_pos_i.hasNext())) {
			System.out.println("Document:\n"+
								doc_paths_i.next()+"\n"+
								"Positions:"+"\n"+
								doc_pos_i.next()+"\n"+
								"________________________________");
			results.concat("Document:\n"+
						   doc_paths_i.next()+"\n"+
						   "Positions:"+"\n"+
						   doc_pos_i.next()+"\n"+
						   "________________________________");
		}
		return results;
	}
}
