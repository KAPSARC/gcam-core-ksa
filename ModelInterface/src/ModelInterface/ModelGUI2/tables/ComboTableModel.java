package ModelInterface.ModelGUI2.tables;

import ModelInterface.ModelGUI2.queries.QueryGenerator;
import ModelInterface.ModelGUI2.DOMmodel;
import ModelInterface.ModelGUI2.XMLDB;
import ModelInterface.ModelGUI2.DbViewer;
import ModelInterface.ModelGUI2.Documentation;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
/*
import java.sql.Statement;
import java.sql.SQLException;
*/
import org.apache.poi.hssf.usermodel.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.*;
import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.table.TableCellRenderer;
import org.w3c.dom.xpath.*;

import com.sleepycat.dbxml.XmlValue;
import com.sleepycat.dbxml.XmlResults;
import com.sleepycat.dbxml.XmlException;


public class ComboTableModel extends BaseTableModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// new stuff
	Vector TreeMapVector = new Vector();
	Vector leftSideVector = new Vector();
	Vector leftHeaderVector;

	Vector indCol;
	Vector indRow;
	String ind1Name;
	String ind2Name;
	boolean flipped;
	TableCellRenderer documentationRenderer;

	//Vector tables;

	/**
	 * Constructor initializes data members, and calls buildTable to create the arrays of data maps,
	 * and the path vectors for each data map. Creates the headers for the table axis and path, , also
	 * creates the filterMaps based on the path information.
	 * @param tp the Tree Path which was selected from the tree, needed to build table
	 *        doc needed to run the XPath query against
	 *        parentFrame needed to create dialogs
	 *        tableTypeString to be able to display the type of table this is
	 */ 
	public ComboTableModel(TreePath tp, Document doc, JFrame parentFrame, String tableTypeString, Documentation documentationIn) {
		super(tp, doc, parentFrame, tableTypeString, documentationIn);
		leftHeaderVector = null;
		wild = chooseTableHeaders(tp/*, parentFrame*/);
		wild.set(0, ((DOMmodel.DOMNodeAdapter)wild.get(0)).getNode().getNodeName());
		wild.set(1, ((DOMmodel.DOMNodeAdapter)wild.get(1)).getNode().getNodeName());
		buildTable(treePathtoXPath(tp, doc.getDocumentElement(), 0));
		activeRows = new Vector( leftSideVector.size() * indRow.size() );
		for(int i = 0; i < (leftSideVector.size() * indRow.size() ); i++) {
			activeRows.add(new Integer(i));
		}
		indCol.add(0, ind1Name);
		documentationRenderer = getDocumentationRenderer();
	}

	/**
	 * Switches the row and column headers, and names. Also sets a boolean so we know 
	 * it has been flipped, since it makes a difference how we reference into the data maps
	 * @param row not used here
	 *        col not used here
	 */
	public void flip(int row, int col) {
		Vector tempArr = indCol;
		indCol = indRow;
		indRow = tempArr;
		indRow.remove(0);
		String tempStr = ind1Name;
		ind1Name = ind2Name;
		ind2Name= tempStr;
		indCol.add(0, ind1Name);
		flipped = !flipped;
		// to set active rows appropriatly
		doFilter( new Vector(tableFilterMaps.keySet()) );
		fireTableStructureChanged();
	}

	public TableCellRenderer getCellRenderer(int row, int col) {
		if( col <= leftHeaderVector.size() ){
			return null;
		} else {
			return documentationRenderer;
		}
	}

	/**
	 * Builds table and sets data maps, in a similar fashions as the MultiTableModel
	 * @see MultiTableModel#buildTable(XPathExpression)
	 */
	protected void buildTable(XPathExpression xpe) {
	  XPathResult res = (XPathResult)xpe.evaluate(doc.getDocumentElement(), XPathResult.ORDERED_NODE_ITERATOR_TYPE, null);
	  xpe = null;
	  Node tempNode;
	  Object[] regionAndYear;
	  TreeSet regions = new TreeSet();
	  TreeSet years = new TreeSet();
	  tableFilterMaps = new LinkedHashMap();
	  TreeMap dataTree = new TreeMap();
	  while ((tempNode = res.iterateNext()) != null) {
		regionAndYear = getRegionAndYearFromNode(tempNode.getParentNode(), tableFilterMaps);
		regions.add(regionAndYear[0]);
		years.add(regionAndYear[1]);
		addToDataTree(tempNode, dataTree).put((String)regionAndYear[0]+";"+(String)regionAndYear[1], tempNode);
	  }
	  recAddTables(dataTree, null, regions, years, "");
	  indCol = new Vector( regions );
	  indRow = new Vector( years );
	  ind1Name = (String)wild.get(0);
	  ind2Name = (String)wild.get(1);
	}

	/**
	 * Gets path information from a node in a similar fashions as the MultiTableModel
	 * @param n the node to get path info from
	 * @param filterMaps temporary filter map to update
	 * @see MultiTableModel#getRegionAndYearFromNode(Node, Map)
	 */
	private Object[] getRegionAndYearFromNode(Node n, Map filterMaps) {
	  Vector ret = new Vector(2,0);
	  do {
		  if(n.getNodeName().equals((String)wild.get(0)) || n.getNodeName().equals((String)wild.get(1))) {
			  //ret.add(n.getAttributes().getNamedItem("name").getNodeValue());
			  if(!n.hasAttributes()) {
				  ret.add(n.getNodeName());
			  } else {
				ret.add(getOneAttrVal(n));
			  }
				  /*
			  } else if(!getOneAttrVal(n).equals("fillout=1")) {
				ret.add(getOneAttrVal(n));
			  } else {
					ret.add(getOneAttrVal(n, 1));
			  }
			  */

		  } else if(n.hasAttributes()) {
			  HashMap tempFilter;
			  if (filterMaps.containsKey(n.getNodeName())) {
				  tempFilter = (HashMap)filterMaps.get(n.getNodeName());
			  } else {
				  tempFilter = new HashMap();
			  }
			  String attr = getOneAttrVal(n);
			  /*
			  if(attr.equals("fillout=1")) {
				  attr = getOneAttrVal(n, 1);
			  }
			  */
			  if (!tempFilter.containsKey(attr)) {
				tempFilter.put(attr, new Boolean(true));
				filterMaps.put(n.getNodeName(), tempFilter);
			  }
		  }
		  n = n.getParentNode();
	  } while(n.getNodeType() != Node.DOCUMENT_NODE /*&& (region == null || year == null)*/);
	  return ret.toArray();
	}

  /**
   * Sorts nodes in a map of maps creating a tree of data, same as in MulitTableModel.
   * @param currNode current level in tree being sorted
   * @param dataTree the entire data maps tree
   * @return the current map being used
   * @see MultiTableModel#addToDataTree(Node, TreeMap)
   */
  private TreeMap addToDataTree(Node currNode, TreeMap dataTree) {
	  if (currNode.getNodeType() == Node.DOCUMENT_NODE) {
		  return dataTree;
	  }
	  TreeMap tempMap = addToDataTree(currNode.getParentNode(), dataTree);
	  if( ((((String)wild.get(0)).matches(".*[Ss]ector") || ((String)wild.get(1)).matches(".*[Ss]ector"))) && currNode.getNodeName().matches(".*[Ss]ector") ) {
		  return tempMap;
	  }
	  if(currNode.hasAttributes() && !currNode.getNodeName().equals((String)wild.get(0)) && !currNode.getNodeName().equals((String)wild.get(1))) {
		String attr = getOneAttrVal(currNode);
		/*
		if(attr.equals("fillout=1")) {
			attr = getOneAttrVal(currNode, 1);
		}
		*/
		attr = currNode.getNodeName()+"@"+attr;
		if(!tempMap.containsKey(attr)) {
			tempMap.put(attr, new TreeMap());
		}
		return (TreeMap)tempMap.get(attr);
	  }
	  return tempMap;
  }

  /**
   * Similar to MultiTable model, except instead of creating a table adds the data to a vector of data maps, 
   * and splits the path string up into a vector for path info.
   * @param dataTree the mappings of attrubutes which will get us to the data
   * @param parent so that we can get the data map which is a level up once we hit the bottom
   * @param regions column axis attrubutes
   * @param years row axis attributes
   * @param title a string describing the path in which the data in the table is coming from
   * @see MultiTableModel#recAddTables(TreeMap, Map.Entry, TreeSet, TreeSet, String)
   */
  private void recAddTables(TreeMap dataTree, Map.Entry parent, TreeSet regions, TreeSet years, String titleStr) {
	Iterator it = dataTree.entrySet().iterator();
	while(it.hasNext()) {	
		Map.Entry me = (Map.Entry)it.next();
		if(me.getValue() instanceof Node || me.getValue() instanceof Double) {
			TreeMapVector.add( (TreeMap)parent.getValue() );
			
			// create a left side 2d vector, add it to LeftSideVector
			
			String lineToParse = titleStr+'/';
			
			// example:		/populationSGMRate@year=1985/gender:type=female/
	
			// get rid of begin and end '/'
			lineToParse = lineToParse.substring( 1, lineToParse.length()-1 );
			
			StringTokenizer st = new StringTokenizer( lineToParse, "/", false);
			int numberOfThem = st.countTokens();
			
			Vector onerow = new Vector( numberOfThem );
			Vector tempVector = new Vector();
			while( st.hasMoreTokens() ){
				//onerow = new Vector( numberOfThem );
				String allNodeInfo = st.nextToken(); // first one
				// 		populationSGMRate@year=1985
				StringTokenizer innerSt = new StringTokenizer( allNodeInfo, "@", false);
				if( innerSt.countTokens() != 2 ){
					System.out.println("BIG PROBLEM, COUNT TOKENS ISN'T 2!!!!!!!!!!");
					return;
				}
				String firstHalf = innerSt.nextToken(); //	populationSGMRate
				if(leftHeaderVector == null){
					tempVector.add( firstHalf );
				}
				String secHalf = innerSt.nextToken(); //	year=1985
				onerow.add( secHalf );
			}
			if(leftHeaderVector == null) {
				leftHeaderVector = tempVector;
			}
			if( ! onerow.isEmpty() ){
				leftSideVector.add( onerow );
			}
			return;
		}else{
			recAddTables((TreeMap)me.getValue(), me, regions, years, titleStr+'/'+(String)me.getKey());
		}
	}
  }
  
  /**
   * Gets the Key needed to reference into the data map, given a row and col position in the table
   * @param row the row for which we should get the row key
   * @param col the col for which we should get the col key
   * @return a string in format key1;key2
   */
  private String getKey (int row, int col) {
	  // if it is flipped the row needs to go first
	  // need to mod by the number of data blocks we have to get the correct key for the row
	  // and have to take into account the additional column headers for path
	  if(flipped) {
		  return (String)indRow.get(row % (indRow.size()))+";"+(String)indCol.get(col - leftHeaderVector.size());
	  }
	  return (String)indCol.get(col- leftHeaderVector.size())+";"+(String)indRow.get(row % (indRow.size()));
  }

  
        /**
	 * Returns the total number of column headers, which include the path headers
	 * one space for row axis, and column headers
	 * @return total number of column headers
	 */
	public int getColumnCount() {
		return leftHeaderVector.size() + indCol.size();
	}

	/**
	 * Total number of rows, does not include rows that have been filtered out
	 * @return length of acitveRows
	 */
	public int getRowCount() {
		return activeRows.size();
	}

	/**
	 * Returns the value to be displayed in the table at a certain position.
	 * @param row the row position in the table
	 *        col the col position in the table
	 * @return the data at the position requested
	 */
	public Object getValueAt(int row, int col) {
		try{
			// this is part of the path get info from leftHeaderVector
			if( col < leftHeaderVector.size() ){
				return ((Vector)leftSideVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( col );
			// this is the col for row axis
			}else if( col == leftHeaderVector.size() ){
				return indRow.get( ((Integer)activeRows.get( row )).intValue() % (indRow.size()) );
			// these columns represent data
			}else{
				Object temp = ((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) );
				if(temp instanceof Node) {
					return new Double(((Node)temp).getNodeValue());
				} else if(temp instanceof Double) {
					return (Double)temp;
				} else if(temp == null && doc == null) {
					return new Double(0.0);
				}
				//return new Double(((Node)((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) )).getNodeValue());
			}
		} catch(NullPointerException e) {
			return "";
		} catch(NumberFormatException nf) { // if the data is not numbers
			Object temp = ((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) );
			if(temp instanceof Node) {
				return ((Node)temp).getNodeValue();
			} else {
				nf.printStackTrace();
				/*
				try {
					return ((XmlValue)temp).getNodeValue();
				} catch(XmlException xe) {
					xe.printStackTrace();
				}
				*/
			}
			//return ((Node)((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) )).getNodeValue();
		}
		return "";
	}

	/**
	 * returns the actual Node that is contained at the position row, col in the table
	 * @param row position in table
	 * 	  col position in table
	 * @return the Node is the position, or null if it was an invalid positon
	 */
	protected Node getNodeAt(int row, int col) {
		if( col <= leftHeaderVector.size() ){
			return null;
		}
		Object temp = ((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() / (indRow.size()))).get( getKey( row, col ) );
		if(temp instanceof Node) {
			return (Node)temp;
		} else {
			// annotate shouldn't be enabled
			return null;
		}
	}

	/**
	 * returns the attr value which defines the column/path by number passed in
	 * @param column an integer position to define which column
	 * @return the header value in the column index, or path index  at the position passed in
	 */
	public String getColumnName(int col) {
		if( col < leftHeaderVector.size() ){
			return (String)leftHeaderVector.get( col );
		}else{
			return (String)indCol.get( col - leftHeaderVector.size() );
		}
	}

	/**
	 * Used to tell which cells are editable, which are all but the path columns and 
	 * the column for row headers.
	 * @param row the row position being queryed
	 *        col the column position being queryed
	 * @return true or false depeneding on if the cell is editable
	 */
	public boolean isCellEditable(int row, int col) {
		if(doc == null) {
			return false;
		}
		if( col <= leftHeaderVector.size() ){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * Update the activeRows vector by going through all of the path information, and
	 * the filterMaps and determining if any of the rows correspond the the qualifying
	 * path info.
	 * @param possibleFilters the list of nodeNames in the filterMap that wil be fillered
	 */
	protected void doFilter(Vector possibleFilters) {
		        // reset the activeRows
			activeRows = new Vector();
			for (int i = 0; i < (leftSideVector.size() * indRow.size()); i++) {
				activeRows.addElement(new Integer(i));
			}
			Integer rowPos = new Integer(-1);

			// Should be able to make this more efficient, but just need it to work right now
			// goes through all of the nodeNames in the filterMaps
			// then goes through each of its different attrubutes that are filtered out 
			// and then goes through all of activeRows to see if they have that attrubutes 
			// in the pathVector, 
			for (int i = 0; i < possibleFilters.size(); i++) {
				if (((String)possibleFilters.get(i)).equals("")) {
					continue;
				}
				currKeys = (String[])((Map)tableFilterMaps.get((String)possibleFilters.get(i))).keySet().toArray(new String[0]);
				//for (Iterator it = activeRows.iterator(); it.hasNext(); rowPos = (Integer)it.next()) {
				Iterator it = activeRows.iterator();
				while (it.hasNext()) {
					rowPos = (Integer)it.next();
					for (int j = 0; j < currKeys.length; j++) {
						if (!((Boolean)((Map)tableFilterMaps.get((String)possibleFilters.get(i))).get(currKeys[j])).booleanValue() ){
							if (((String)((Vector)leftSideVector.get( rowPos.intValue() / (indRow.size()) )).get( possibleFilters.size()-i-1 )).equals(currKeys[j])){
								it.remove();
								break;
	
							}
						}
					}
				}
			}
	}
	
	/**
	 * Update the value of a cell in the table, same as in NewDataTableModel 
	 * @param val new value the cell should be changed to
	 * @param row the row of the cell being edited
	 * @param col the col of the cell being edited
	 * @see NewDataTableModel#setValueAt(Object, int, int)
	 */
	public void setValueAt(Object val, int row, int col) {
		
		TreeMap data = ((TreeMap)TreeMapVector.get( row / (indRow.size())));

		Node n = (Node)data.get(getKey(row,col));
		if( n != null ){
			n.setNodeValue(val.toString());
		}else{
			n = doc.createTextNode( val.toString() );
			Node updown = null;
			Node side = null;

			// Try to look in table for value in this column
			for(int i = 0; i < getRowCount() && ( updown = ((Node)((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( i )).intValue() / (indRow.size()))).get( getKey( i, col ) ))) /*(Node)data.get(getKey(i, col))*/ == null; ++i) {
			}
			// Try to look in this row to see if there is a value
			for(int i = leftHeaderVector.size()+1; i < getColumnCount() && 
					( side = (Node)data.get(getKey(row, i))) == null; ++i) {
			}
			// If there weren't values in the same column and row won't be 
			// able to figure out the path down the tree to put the data
			if( updown == null || side == null ) {
				// throw some exception
				System.out.println("Couldn't gather enough info to create Node");
				JOptionPane.showMessageDialog(parentFrame, 
						"Couldn't gather enough information to \ncreate the data",
						"Set Value Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			ArrayList nodePath = new ArrayList();
			Node parent = ((Node)side.getParentNode());
			
			
			String headerone = ind1Name; // ex. region
			String headertwo = ind2Name; // ex. populationSGM
			
			String attributesLine = getKey( row, col );
			String[] attributesLineArray = attributesLine.split(";", 2);
			if(flipped) {
				String temp; //= attributesLineArray[0];
				/*
				attributesLineArray[0] = attributesLineArray[1];
				attributesLineArray[1] = temp;
				*/
				temp = headerone;
				headerone = headertwo;
				headertwo = temp;
			}

			StringTokenizer st = new StringTokenizer( attributesLineArray[ 1 ], "=", false);
			
			String attrFrom1 = st.nextToken();
			String attrTo1 = st.nextToken();
			
			st = new StringTokenizer( attributesLineArray[0], "=", false);
			String attrFrom2 = st.nextToken();
			String attrTo2 = st.nextToken();

			// Work our way up the until we find the tag for corrent
			// column header which by the way the axis are chosen should 
			// always be higher in the path
			while( !parent.getNodeName().equals( headerone ) ) {
				nodePath.add(parent);
				parent = parent.getParentNode();
			}

			// Go down the path back to where the value should be
			// if there needs to be nodes created they will be using info 
			// from the row header, or the path info from the same row
			parent = parent.getParentNode();
			parent = checkPath(parent, headerone, attrFrom1, attrTo1);
			for(int i = nodePath.size()-1; i >= 0; --i) {
				Element temp = (Element)nodePath.get(i);
				if(temp.getNodeName().equals(headertwo)) {
					parent = checkPath(parent, headertwo, attrFrom2, attrTo2);
				} else {
					Node attrTemp = temp.getAttributes().item(0);
					if(attrTemp == null) {
						parent = checkPath(parent, temp.getNodeName(), null, null);
					} else {
						parent = checkPath(parent, temp.getNodeName(), attrTemp.getNodeName(), 
								attrTemp.getNodeValue());
					}
				}
			}

			parent.appendChild( n );
			data.put( getKey(row,col), n );
		}
		
		fireTableCellUpdated(row, col);

		// fireOffSomeListeners?

	}

	/** 
	 * Used to follow the path down a tree where parent is the current parent and want to 
	 * go under the node with the passed in node name and attributes will create the node
	 * if it does not exsit
	 * @param parent current node were are at in the path
	 * @param nodeName name of the node that we want to follow
	 * @param attrKey attribute name of the node that we want to follow
	 * @param attrVal attribute value of the node that we want to follow
	 * @return the pointer to the node we wanted to follow
	 */
	private Node checkPath(Node parent, String nodeName, String attrKey, String attrVal) {
		NodeList nl = parent.getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i) {
			Element temp = (Element)nl.item(i);
			if(temp.getNodeName().equals(nodeName) && attrKey == null) {
				return temp;
			} else if(temp.getNodeName().equals(nodeName) && temp.getAttribute(attrKey).equals(attrVal)) {
				return temp;
			}
		}
		Element newElement = doc.createElement(nodeName);
		if(attrKey != null) {
			newElement.setAttribute(attrKey, attrVal);
		}
		parent.appendChild(newElement);
		return newElement;
	}


	public JFreeChart createChart(int rowAt, int colAt) {
		//throw new UnsupportedOperationException();
		// Start by creating an XYSeriesSet to contain the series.
		XYSeriesCollection chartData = new XYSeriesCollection();
		// Loop through the rows and create a data series for each.
		for( int row = 0; row < getRowCount(); ++row ){
			// Row name is at element zero.
			//String rowNameFull = (String)getValueAt(row,0);
			String rowNameFull = (String)indRow.get( ((Integer)activeRows.get( row )).intValue() % (indRow.size()) );
			
			// Split out the name attribute if it contains it.
			String rowName;
			if( rowNameFull.indexOf('=') != -1 ){
				rowName = rowNameFull.split("=")[ 1 ];
			}
			else {
				rowName = rowNameFull;
			}
			XYSeries currSeries = new XYSeries(rowName);
			// Skip column 1 because it contained the label.
			for( int col = leftHeaderVector.size() + 1; col < getColumnCount(); ++col ){
				double yValue = ( (Double)getValueAt(row, col) ).doubleValue();
				String fullColumn = getColumnName(col);
				// Get the year part of it.
				if(fullColumn.indexOf("=") != -1) {
					fullColumn = fullColumn.split("=")[1];
				}
				double year = Double.parseDouble( fullColumn );
				if(yValue != 0 || ((TreeMap)TreeMapVector.get( ((Integer)activeRows.get( row )).intValue() 
								/ (indRow.size()))).get( getKey( row, col ) ) != null) {
					currSeries.add( year, yValue);
				}
				/*
				int year = Integer.parseInt( fullColumn );
				currSeries.add( year, yValue);
				*/
			}
			// Add the series to the set.
			chartData.addSeries(currSeries);
		}
		// Done adding series, create the chart.
		// Create the domain axis label.
		// TODO: Improve naming.
		NumberAxis xAxis = new NumberAxis("Year");
		
		// Use the parent element name as the name of the axis.
		NumberAxis yAxis = new NumberAxis(ind2Name);
		
		// This turns off always including zero in the domain.
		xAxis.setAutoRangeIncludesZero(false);
		
		// This turns on automatic resizing of the domain..
		xAxis.setAutoRange(true);
		
		// This makes the X axis use integer tick units.
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		
		// This turns on automatic resizing of the range.
		yAxis.setAutoRange(true);
		
		// Create the plot.
		XYPlot xyPlot = new XYPlot( chartData, xAxis, yAxis, new XYLineAndShapeRenderer());
		
		// Draw the zero line.
		//xyPlot.setZeroRangeBaselineVisible(true);
		
		// Create the chart.
		JFreeChart chart = new JFreeChart( xyPlot );
		
		// Create a title for the chart.
		TextTitle ttitle = new TextTitle(title);
		chart.setTitle(ttitle);
		return chart;
	}

	protected QueryGenerator qg;
	public ComboTableModel(QueryGenerator qgIn, String filterQuery, Object[] regions, JFrame parentFrameIn) {
		qg = qgIn;
		parentFrame = parentFrameIn;
		//title = qgIn.getVariable();
		title = qgIn.toString();
		buildTable(DbViewer.xmlDB.createQuery(qgIn.getCompleteXPath(regions), filterQuery, null), qgIn.isSumAll(), qgIn.getLevelValues());
		//DbViewer.xmlDB.setQueryFilter("");
		ind2Name = qgIn.getVariable();
		activeRows = new Vector( leftSideVector.size() * indRow.size() );
		for(int i = 0; i < (leftSideVector.size() * indRow.size() ); i++) {
			activeRows.add(new Integer(i));
		}
		indCol.add(0, ind1Name);
	}
	private void buildTable(XmlResults res, boolean sumAll, Object[] levelValues) {
	  try {
		  if(!res.hasNext()) {
			  System.out.println("Query didn't get any results");
			  JOptionPane.showMessageDialog(parentFrame, "Query didn't get any results", "Build Table Error",
					  JOptionPane.ERROR_MESSAGE);
			  // display an error on the screen
			  return;
		  }
	  } catch(XmlException e) {
		  e.printStackTrace();

	  }
	  XmlValue tempNode;
	  Object[] regionAndYear;
	  TreeSet regions = new TreeSet();
	  TreeSet years = new TreeSet();
	  tableFilterMaps = new LinkedHashMap();
	  TreeMap dataTree = new TreeMap();
	  try {
		  while(res.hasNext()) {
			  tempNode = res.next();
			  //regionAndYear = getRegionAndYearFromNode(tempNode.getParentNode(), tableFilterMaps);
			  regionAndYear = qg.extractAxisInfo(tempNode.getParentNode(), tableFilterMaps);
			  if(sumAll) {
				  //regionAndYear[1] = "All "+(String)wild.get(0);
				  regionAndYear[1] = "All "+qg.getNodeLevel();
			  }
			  regions.add(regionAndYear[0]);
			  years.add(regionAndYear[1]);
			  Map retMap = qg.addToDataTree(new XmlValue(tempNode), dataTree); //.put((String)regionAndYear[0]+";"+(String)regionAndYear[1], tempNode);
			  //Map retMap = addToDataTree(new XmlValue(tempNode), dataTree); //.put((String)regionAndYear[0]+";"+(String)regionAndYear[1], tempNode);
			  DbViewer.xmlDB.printLockStats("addToDataTree");
			  Double ret = (Double)retMap.get((String)regionAndYear[0]+";"+(String)regionAndYear[1]);
			  if(ret == null) {
				  retMap.put((String)regionAndYear[0]+";"+(String)regionAndYear[1], new Double(tempNode.asNumber()));
			  } else {
				  //ret += tempNode.asNumber();
				  retMap.put((String)regionAndYear[0]+";"+(String)regionAndYear[1], 
						  new Double(ret.doubleValue() + tempNode.asNumber()));
			  }
			  tempNode.delete();
		  }
		  res.delete();
		  DbViewer.xmlDB.printLockStats("buildTable");
	  } catch(Exception e) {
		  e.printStackTrace();
	  }
	  recAddTables(dataTree, null, regions, years, "");
	  System.out.println("Level Selected: "+levelValues);
	  if(!sumAll && levelValues != null && years.size() != levelValues.length) {
		  //indRow = new Vector(levelValues);
		  indRow = new Vector(levelValues.length, 0);
		  for(int i =0; i < levelValues.length; ++i) {
			  System.out.println(levelValues[i]);
			  indRow.add(levelValues[i]);
		  }
	  } else {
		  indRow = new Vector( years );
	  }
	  indCol = new Vector( regions );
	  ind1Name = qg.getNodeLevel();
	  //ind2Name = (String)wild.get(1);
	}

  	private Object[] getRegionAndYearFromNode(XmlValue n, Map filterMaps) throws Exception {
	  Vector ret = new Vector(2,0);
	  XmlValue nBefore;
	  do {
		  if(n.getNodeName().equals((String)wild.get(0))) {
			  ret.add(XMLDB.getAttr(n));
		  } 
		  if(n.getNodeName().equals((String)wild.get(1))) {
			  ret.add(XMLDB.getAttr(n, "year"));
			  /*
			  //ret.add(n.getAttributes().getNamedItem("name").getNodeValue());
			  if(!getOneAttrVal(n).equals("fillout=1")) {
			  	ret.add(getOneAttrVal(n));
			  } else {
			        ret.add(getOneAttrVal(n, 1));
			  }
			  */

		  } else if(XMLDB.hasAttr(n)) {
			  HashMap tempFilter;
	           	  if (filterMaps.containsKey(n.getNodeName())) {
	                          tempFilter = (HashMap)filterMaps.get(n.getNodeName());
                          } else {
                                  tempFilter = new HashMap();
                          }
			  String attr = XMLDB.getAttr(n);
			  if (!tempFilter.containsKey(attr)) {
                          	tempFilter.put(attr, new Boolean(true));
                          	filterMaps.put(n.getNodeName(), tempFilter);
			  }
		  }
		  nBefore = n;
		  n = n.getParentNode();
		  nBefore.delete();
	  } while(n.getNodeType() != XmlValue.DOCUMENT_NODE); 
	  //} while(!n.isType(XmlValue.DOCUMENT_NODE)); // isType doesn't seem to work at least not how I thought it would
	  //} while(n.getNodeType() != Node.DOCUMENT_NODE /*&& (region == null || year == null)*/);
	  n.delete();
	  DbViewer.xmlDB.printLockStats("getRegionAndYearFromNode");
	  return ret.toArray();
  	}

  private TreeMap addToDataTree(XmlValue currNode, TreeMap dataTree) throws Exception {
	  if (currNode.getNodeType() == XmlValue.DOCUMENT_NODE) {
		  currNode.delete();
		  return dataTree;
	  }
	  TreeMap tempMap = addToDataTree(currNode.getParentNode(), dataTree);
	  // used to combine sectors and subsectors when possible to avoid large amounts of sparse tables
	  String w = (String)wild.get(0);
	  //if(currNode.getNodeName().matches(".*sector") || currNode.getNodeName().equals("technology")) {
	  if((w.equals("supplysector") && currNode.getNodeName().equals("subsector")) || (w.matches(".*sector") && currNode.getNodeName().equals("technology"))) {
	  //if( ((((String)wild.get(0)).matches(".*[Ss]ector") || ((String)wild.get(1)).matches(".*[Ss]ector"))) && currNode.getNodeName().equals(".*[Ss]ector") ) {
		  currNode.delete();
		  return tempMap;
	  }
	  if(XMLDB.hasAttr(currNode) && !currNode.getNodeName().equals((String)wild.get(0)) && !currNode.getNodeName().equals((String)wild.get(1))) {
		String attr = XMLDB.getAllAttr(currNode);
		attr = currNode.getNodeName()+"@"+attr;
		if(!tempMap.containsKey(attr)) {
			tempMap.put(attr, new TreeMap());
		}
		currNode.delete();
		return (TreeMap)tempMap.get(attr);
	  } 
	  currNode.delete();
	  return tempMap;
  }
  public void exportToExcel(HSSFSheet sheet, HSSFWorkbook wb, HSSFPatriarch dp) {
	  HSSFRow row = sheet.createRow(sheet.getLastRowNum()+1);
	  row.createCell((short)0).setCellValue(title);
	  row = sheet.createRow(sheet.getLastRowNum()+1);
	  for(int i = 0; i < getColumnCount(); ++i) {
		  row.createCell((short)i).setCellValue(getColumnName(i));
	  }
	  for(int rowN = 0; rowN < getRowCount(); ++rowN) {
		  row = sheet.createRow(sheet.getLastRowNum()+1);
		  for(int col = 0; col < getColumnCount(); ++col) {
			  Object obj = getValueAt(rowN, col);
			  if(obj instanceof Double) {
				  row.createCell((short)col).setCellValue(((Double)obj).doubleValue());
			  } else {
				  row.createCell((short)col).setCellValue(getValueAt(rowN,col).toString());
			  }
		  }
	  }
	  try {
		  java.awt.image.BufferedImage chartImage = createChart(0,0).createBufferedImage(350,350);
		  int where = wb.addPicture(org.jfree.chart.ChartUtilities.encodeAsPNG(chartImage), HSSFWorkbook.PICTURE_TYPE_PNG);
		  dp.createPicture(new HSSFClientAnchor(0,0,50,50,(short)(getColumnCount()+1),
					  1,(short)(getColumnCount()+4),getRowCount()+5), where);
	  } catch(java.io.IOException ioe) {
		  ioe.printStackTrace();
	  }
  }

  /*
  public void exportToExcel(Statement excelStatement) {
	  try {
		  StringBuffer sqlStr = new StringBuffer("Insert into 'Sheet1$' values(");
		  for(int i = 0; i < getColumnCount(); ++i) {
			  sqlStr.append(getColumnName(i));
			  sqlStr.append(",");
		  }
		  sqlStr.replace(sqlStr.length()-1, sqlStr.length(), ")");
		  //sqlStr.append(")");
		  System.out.println(sqlStr.toString());
		  excelStatement.executeUpdate(sqlStr.toString());
		  sqlStr.delete(29, sqlStr.length());
		  for(int row = 0; row < getRowCount(); ++row) {
			  for(int col = 0; col > getColumnCount(); ++col) {
				  sqlStr.append(getValueAt(row, col));
				  sqlStr.append(",");
			  }
		  }
		  sqlStr.replace(sqlStr.length()-1, sqlStr.length(), ")");
		  //sqlStr.append(")");
		  System.out.println(sqlStr.toString());
		  excelStatement.executeUpdate(sqlStr.toString());
	  } catch(SQLException se) {
		  se.printStackTrace();
	  }
  }
	  */
  public void annotate(int[] rows, int[] cols, Documentation documentation) {
	  Vector<Node> selectedNodes = new Vector<Node>(rows.length*cols.length, 0);
	  for(int i = 0; i < rows.length; ++i) {
		  for(int j = 0; j < cols.length; ++j) {
			  selectedNodes.add(getNodeAt(rows[i], cols[j]));
		  }
	  }
	  documentation.getDocumentation(selectedNodes, rows, cols);
  }
}