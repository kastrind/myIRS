import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.SoftBevelBorder;

public class GUI extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private LayoutManager layout;
	private JFrame window;
	private JButton search_button;
	private JTextField query_field;
	private JLabel query_prompt;
	private JLabel query_res;
	private JTextArea results;
	private JScrollPane scrollpane;
	private JRadioButton boolean_model;
	private JRadioButton vector_model;
	private JRadioButton okapi_model;
	JPanel panel_up;
	JPanel panel_middle;
	
	private String chosen_model = "Boolean";
	private Primitive_Retrieval bool_ret;
	private Primitive_Retrieval vec_ret;
	private Primitive_Retrieval okapi_ret;
	
	public void init(){
		layout = new GridLayout(3,1);
		window = new JFrame("HY-463(IRS) assignment3, spring semester 2009, CSD UoC. Student: kastrin, 1940");
		window.setLayout(layout);
		
		query_prompt = new JLabel("Enter query: ");
		query_field = new JTextField(32);
		search_button = new JButton("Search");
		search_button.addActionListener(this);
		query_res = new JLabel();
		boolean_model = new JRadioButton("Boolean",true);
		vector_model = new JRadioButton("Vector Space",false);
		okapi_model = new JRadioButton("Okapi_BM25",false);
		SelectionPanel models = new SelectionPanel();
		results = new JTextArea();
		scrollpane = new JScrollPane(results);
		results.setEditable(false);
		results.setLineWrap(true);
		results.setBackground(Color.LIGHT_GRAY);
		results.setText("no results yet...");
		
		panel_up = new JPanel();
		panel_up.setLayout(new FlowLayout());
		panel_up.add(query_prompt);
		panel_up.add(query_field);
		panel_up.add(search_button);
		panel_up.setSize(600,100);
		window.add(panel_up);
		panel_middle = new JPanel();
		panel_middle.setLayout(new GridLayout(2,1));
		panel_middle.add(models);
		panel_middle.add(query_res);
		window.add(panel_middle);
		window.add(scrollpane);
		window.pack();
		window.setSize(600, 400);
		window.setVisible(true);
		
	}
	
	private class SelectionPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public SelectionPanel(){
			ButtonGroup group = new ButtonGroup();
			group.add(boolean_model);
			group.add(vector_model);
			group.add(okapi_model);
			add(boolean_model);
			add(vector_model);
			add(okapi_model);
			RadioButtonListener listener = new RadioButtonListener();
			boolean_model.addActionListener(listener);
			vector_model.addActionListener(listener);
			okapi_model.addActionListener(listener);
			SoftBevelBorder border = new SoftBevelBorder(BevelBorder.RAISED);
			Border groupBox = BorderFactory.createTitledBorder(border, "Choose retrieval model: ");
			setBorder(groupBox);
		}
	}
	
	private class RadioButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent event) {
			chosen_model = event.getActionCommand();
		}
	}
	
	public void actionPerformed(ActionEvent event) {
		String query = query_field.getText();
		query_res.setText("Results of query: "+query);
		
		if(chosen_model.equals("Boolean")) {
			bool_ret = new Boolean_Retrieval();
			bool_ret.Query(query);
			results.setText(bool_ret.printResults());
			
		}
		else if(chosen_model.equals("Vector Space")) {
			vec_ret = new Vector_Space_Retrieval();
			vec_ret.Query(query);
			results.setText(vec_ret.printResults());
		}
		else if(chosen_model.equals("Okapi_BM25")) {
			okapi_ret = new Okapi_BM25(2, 0.75);
			okapi_ret.Query(query);
			results.setText(okapi_ret.printResults());
		}
	}
}
