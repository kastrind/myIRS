
import java.io.IOException;

public class IR_main {
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Utils ut = new Utils();
		String user_choice = null, query = null;
		double k1 = 2, b = 0.75;
	
		System.out.println("HY-463(IRS) assignment3, spring semester 2009, CSD UoC. Student: kastrin, 1940\n"+
                       "To retrieve information, type a Retrieval Model of your preference: 'boolean', 'vector', 'okapi'.\n" +
						           "To exit, type 'exit'.\n" +
						           "To see this message again, type 'help'.\n");
			Primitive_Retrieval prim_ret = new Primitive_Retrieval();
			
			System.out.print("Load graphics? (y/n) ");
			user_choice = ut.getInput();
			if(user_choice.equalsIgnoreCase("y")) {GUI gui = new GUI(); gui.init();}
			else {
			while(true) {
				System.out.print("Command? ");
				user_choice = ut.getInput();
				
				if(user_choice.equalsIgnoreCase("boolean")) {
					System.out.print("Enter query: ");
					query = ut.getInput();
					Primitive_Retrieval bool_ret = new Boolean_Retrieval();
					bool_ret.Query(query);
					bool_ret.printResults();
				}
				else if(user_choice.equalsIgnoreCase("vector")) {
					Primitive_Retrieval vs_ret = new Vector_Space_Retrieval();
					System.out.print("Enter query: ");
					query = ut.getInput();
					if(!query.equals("")){vs_ret.Query(query);vs_ret.printResults();}
					else {System.err.println("No query!");continue;}
				}
				else if(user_choice.equalsIgnoreCase("okapi")) {
					try{System.out.print("Enter k1 value: ");
					k1 = Double.valueOf(ut.getInput());
					System.out.print("Enter b value: ");
					b = Double.valueOf(ut.getInput());
					Primitive_Retrieval okapi_ret = new Okapi_BM25(k1, b);
					System.out.print("Enter query: ");
					query = ut.getInput();
					okapi_ret.Query(query);
					okapi_ret.printResults();
					}catch(NumberFormatException e){System.err.println("Invalid value!"); continue;}
				}
				else if(user_choice.equalsIgnoreCase("help")){
					System.out.println("To retrieve information, type a Retrieval Model of your preference: 'boolean', 'vector', 'okapi'.\n" +
                             "To exit, type 'exit'.\n" +
                             "To see this message again, type 'help'.");
				}
				else if(user_choice.equalsIgnoreCase("exit")) System.exit(1);
			}
		}
	}
}
