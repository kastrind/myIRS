import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class Utils {

	/**Reads user data from keyboard and returns it as a string
	 * @return User input data as a string
	 * */
	public String getInput(){
	Scanner in = new Scanner(System.in);
	String str = in.nextLine();
	return str;
	}
	
	/**
	 * Prints an ArrayList to console.
	 * @param al: the ArrayList
	 */
	public void printArrayList(ArrayList<Double> al) {
	for(int i = 0; i<al.size(); i++) System.out.println(al.get(i));
	}
	
	/**
	 * 
	 * Sorts double values of a HashMap in descending order
	 * @param map
	 * @return ArrayList of the values
	 */
	public ArrayList<Double> sortValsDescending(HashMap<Long, Double> map) {
		ArrayList<Double> vals = new ArrayList<Double>(map.values());
		Comparator<Double> comparator = Collections.reverseOrder();
		Collections.sort(vals, comparator);
		return vals;
	}
	
	/**
	 * Arranges the keys of a HashMap according to the descending order of its values
	 * @param map
	 * @return ArrayList of the keys
	 */
	public ArrayList<Long> arrangeKeys(HashMap<Long, Double> map) {
		
		ArrayList<Double> sorted_vals_desc = sortValsDescending(map);
		ArrayList<Long> keys = new ArrayList<Long>(map.keySet());
		Collections.sort(keys);
		ArrayList<Long> keys_new = new ArrayList<Long>();
		
		Iterator<Double> vals_i = sorted_vals_desc.iterator();
		Iterator<Long> keys_i;
		double curr_val = 0;
		double curr_val2 = 0;
		long curr_docID = 0;
		
		while(vals_i.hasNext()){
			curr_val = (Double)vals_i.next();
			keys_i = keys.iterator();
			while (keys_i.hasNext()) {
				curr_docID = keys_i.next();
				curr_val2 = map.get(curr_docID);
				if(curr_val == curr_val2) {
					keys.remove(curr_docID);
					keys_new.add(curr_docID);
					break;
				}
			}
		}
	return keys_new;
	}	
}
