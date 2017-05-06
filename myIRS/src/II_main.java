
public class II_main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Inverted_Index i_i = new Inverted_Index();
		i_i.load_StopWords("stopWords.txt");
		System.out.print("Building Inverted Index...");
		i_i.build_InvertedIndex();
		System.out.println(" Done.");
	}
}
