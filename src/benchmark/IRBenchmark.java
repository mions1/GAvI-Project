package benchmark;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

//import org.apache.lucene.benchmark.byTask.Benchmark;

import index.Hit;
import index.Index;
import irModels.FuzzyModel;
import irModels.Model;
import irModels.VectorSpaceModel;
import plot.Plot;
import plot.Plot.Line;
//import sun.applet.Main;

public class IRBenchmark {

	//Attributi
	Model model;	//Model on which execute benchmark
	String fileDocumentsPaths;	//in this file, every line is a path of a document on which execute query
	String queryFile;	//File LISA.QUE
	String docExpected;	//File LISA.REL
	Index generalIndex; //Index
	LinkedList<String> ll; //List where to search (name, content)
	
	//Filled when executeBenchmark is called
	ArrayList<LinkedList<String>> expectedDocuments = new ArrayList<LinkedList<String>>();
	ArrayList<LinkedList<String>> retrivedDocuments = new ArrayList<LinkedList<String>>();
	ArrayList<LinkedList<String>> intersect = new ArrayList<LinkedList<String>>();
	ArrayList<Double> precision = new ArrayList<Double>();
	ArrayList<Double> recall = new ArrayList<Double>();

	/**
	 * 
	 * @param model model on which execute benchmark
	 * @param fileDocumentsPaths in this file, every line is a path of a document on which execute query
	 * @param queryFile this is lisa.que file
	 * @param docExpected in this file, for every query, there is a list of relevant documents for it
	 */
	public IRBenchmark(Model model, String fileDocumentsPaths, String queryFile, String docExpected) {
		this.model = model;
		this.fileDocumentsPaths = fileDocumentsPaths;
		this.queryFile = queryFile;
		this.docExpected = docExpected;

		this.generalIndex = Index.getIndex();
		this.generalIndex.setSimilarity(model.getSimilarity(), false);

		ll = new LinkedList<String>();
		ll.add("name");
		ll.add("content");
	}

	/**
	 * This method performs the LISA benchmark.
	 * Documents are added in the index.
	 * Array with expected documents is created. (Documents that should be retrieved)
 	 * Array with retrieved documents is created. (Documents that are retrieved)
 	 * Array with intersection between expected and retrieved documents is created. (For calculating precision and recall)
	 */
	public void executeBenchmark() {
		
		//Reading queries from file
		ArrayList<String> queries = readQueries();

		System.out.println("Loading index with " + fileDocumentsPaths);

		loadIndex();


		expectedDocuments = getExpectedDocuments();
		retrivedDocuments = retrieveDocuments(queries);
		intersect = getIntersection(expectedDocuments, retrivedDocuments);
		
		saveResults("resFuz.save", intersect);

		precision = getPrecision(intersect, retrivedDocuments);
		recall = getRecall(intersect, expectedDocuments);

		int i=0;
		for (Double rec: recall)
			System.out.println("Recall query "+(++i)+": " + rec);

		i=0;
		for (Double rec: precision)
			System.out.println("Precision query "+(++i)+": " + rec);
	}

	/**
	 * This method read query from lisa.que
	 * Queries are split with '#', so each query ends up with that terminator. 
	 * @return list of queries
	 */
	public ArrayList<String> readQueries() {

		//This will load queries in file queryFile
		ArrayList<String> queries = new ArrayList<String>();

		try {
			File queryF = new File(queryFile);
			BufferedReader br = new BufferedReader(new FileReader(queryF));

			String line = "";
			String query = "";

			while ( (line=br.readLine()) != null) {
				//So, if it isn't the line with query number
				if(line.length()>2) {
					query += line + " ";
				}

				//If is the last line of the query
				if(line.endsWith("#")) {
					//Add the line until '#'
					query = query.substring(0, query.length()-2);
					queries.add(query);
					query = "";
				}
			}
			br.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return queries;
	}

	/**
	 * Load documents to retrieve into the index.
	 * Read every line of file (a line = a document) and add it to the index
	 */
	private void loadIndex(){
		String docPath = "";

		try {
			File docF = new File(fileDocumentsPaths);
			BufferedReader br = new BufferedReader(new FileReader(docF));

			while ( (docPath = br.readLine()) != null) {
				generalIndex.addDocument(docPath);
			}
			br.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get list of expected documents for every query from LISA.REL
	 * @return list of expected documents.
	 */
	private ArrayList<LinkedList<String>> getExpectedDocuments() {
		ArrayList<LinkedList<String>> expectedDocuments = new ArrayList<LinkedList<String>>();
		LinkedList<String> rel = null;	
		String line = "";
		int query_num = 1;

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(docExpected)));

			while( (line=br.readLine()) != null) {
				rel = new LinkedList<String>();

				if (!line.contains("Refs") &&  !line.contains("Query") && line.length() > 0) {
					boolean terminator = false;
					while( !terminator ){ 
						String [] docs = line.split(" ");

						for (String doc : docs) {
							int num = Integer.parseInt(doc);
							if (num != -1) {
								rel.add(doc);
							} else {
								terminator = true;
								break;
							}
						}

						if(!terminator) {
							line = br.readLine();
						}
					}
					System.out.println("Documents expected for query " + query_num + ": " + rel.toString());
					query_num++;
					expectedDocuments.add(rel);
				}
			}
			br.close();
		} catch (Exception e) {
			System.err.println(e);
		}			

		return expectedDocuments;
	}

	/**
	 * This method retrieve documents, passing queries from benchmark.
	 * @param queries list of queries
	 * @return documents retrieved for each query
	 */
	private ArrayList<LinkedList<String>> retrieveDocuments(ArrayList<String> queries){
		ArrayList<LinkedList<String>> documentsRetrieved = new ArrayList<LinkedList<String>>();
		LinkedList<String> results = null;
		LinkedList<Hit> indexResults = null;
		int query_num = 1;

		for(String query : queries) {
			results = new LinkedList<String>();

			indexResults = generalIndex.submitQuery(query, ll, model, false);
			for(Hit indRes : indexResults) {
				results.add(indRes.getDocName().substring(0, indRes.getDocName().lastIndexOf(".")));
			}
			System.out.println("Results for query " + query_num + ": " + results.toString());
			System.out.print("******************************************\n");
			query_num++;
			documentsRetrieved.add(results);
		}
		return documentsRetrieved;
	}

	/**
	 * Do intersection between expected documents and retrieved documents.
	 * @param expectedDocuments
	 * @param retrievedDocuments
	 * @return list of intersection
	 */
	private ArrayList<LinkedList<String>> getIntersection(ArrayList<LinkedList<String>> expectedDocuments, ArrayList<LinkedList<String>> retrievedDocuments){
		ArrayList<LinkedList<String>> intersect = new ArrayList<LinkedList<String>>();

		LinkedList<String> intersection = null;

		for (int query = 0 ; query < expectedDocuments.size(); query++) {
			intersection = new LinkedList<String>();
			for (int i = 0; i < retrievedDocuments.get(query).size(); i++) {
				for (int j = 0; j < expectedDocuments.get(query).size(); j++) {
					if ( expectedDocuments.get(query).get(j).equals(retrievedDocuments.get(query).get(i)) ){
						if (!intersection.contains(expectedDocuments.get(query).get(j))){
							intersection.add(expectedDocuments.get(query).get(j));
						}
					}
				}
			}
			System.out.println("Intersection for query " + (query+1) + ": " + intersection.toString());
			intersect.add(intersection);
		}
		return intersect;
	}
	
	/**
	 * Save intersection on a file.
	 * @param fileName
	 * @param intersect
	 */
	private void saveResults(String fileName, ArrayList<LinkedList<String>> intersect) {
		try {
			FileWriter fw = new FileWriter(new File(fileName));
			for (int i = 0; i < intersect.size(); i++) {
				fw.append("Docs intersected for query "+(i+1)+":\n");
				for (int j = 0; j < intersect.get(i).size(); j++) {
					fw.append(intersect.get(i).get(j)+"\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculate recall
	 * @param intersect intersection beetwen expected and retrived documents
	 * @param expectedDocuments expected documents
	 * @return list of recall for every query
	 */
	public ArrayList<Double> getRecall(ArrayList<LinkedList<String>> intersect, ArrayList<LinkedList<String>> expectedDocuments) {
		ArrayList<Double> recall = new ArrayList<Double>();

		for (LinkedList<String> intersection: intersect)
			recall.add(intersection.size()+0.0);
		//For each query get intersection.size / relevants.size
		//recall = |intersect|/|relevants|
		for (int i = 0; i < expectedDocuments.size(); i++)
			if (expectedDocuments.get(i).size() != 0)
				recall.set(i, recall.get(i)/expectedDocuments.get(i).size());
			else
				recall.set(i, 0.0);

		return recall;
	}

	/**
	 * Calculate precision
	 * @param intersect intersection beetwen expected and retrived documents
	 * @param retrievedDocuments retrived documents
	 * @return list of precision for every query
	 */
	public ArrayList<Double> getPrecision(ArrayList<LinkedList<String>> intersect, ArrayList<LinkedList<String>> retrievedDocuments) {
		ArrayList<Double> precision = new ArrayList<Double>();

		for (LinkedList<String> intersection: intersect) {
			precision.add(intersection.size()+0.0);
		}
		//For each query get intersection.size / relevants.size
		//precision = |intersect|/|result|
		for (int i = 0; i < retrievedDocuments.size(); i++)
			if (retrievedDocuments.get(i).size() != 0)
				precision.set(i, precision.get(i)/retrievedDocuments.get(i).size());
			else
				precision.set(i, 0.0);
		return precision;
	}

	/**
	 * Get recall standard levels
	 * @return
	 */
	private ArrayList<Double> getRecallLevel() {
		ArrayList<Double> recall = new ArrayList<Double>();
		
		recall.add(0.33);
		recall.add(0.66);
		recall.add(1.0);
		
		return recall;
	}
	
	/**
	 * Get precision for standard levels
	 * @return Precision
	 */
	public ArrayList<ArrayList<Double>> getPrecision() {
		int lvl0;
		int lvl33;
		int lvl66;
		int lvl100;
		
		ArrayList<ArrayList<Double>> precision = new ArrayList<ArrayList<Double>>();
		
		ArrayList<Double> temp = new ArrayList<Double>();
		
		for (int i = 0; i < this.precision.size(); i++) {
			lvl0 = 0;
			lvl33 = retrivedDocuments.get(i).size()*33/100;
			lvl66 = retrivedDocuments.get(i).size()*66/100;
			lvl100 = retrivedDocuments.get(i).size();
			
			temp.add( ( (double) getIntersect(expectedDocuments.get(i), retrivedDocuments.get(i), lvl33).size() / (double) lvl33 ));
			temp.add( ((double)getIntersect(expectedDocuments.get(i), retrivedDocuments.get(i), lvl66).size() / (double) lvl66) );
			temp.add( ((double)getIntersect(expectedDocuments.get(i), retrivedDocuments.get(i), lvl100).size() / (double) lvl100 ) );
			
			precision.add(temp);
			temp = new ArrayList<Double>();
		}
		
		return precision;
	}

	/**
	 * Calculate R-Precision
	 * @param level R for R-Precision
	 * @return value of r-precision for every query
	 */
	public ArrayList<Double> getRPrecision(int level) {
		ArrayList<Double> rprec = new ArrayList<>();
		
		for (int i = 0; i < this.precision.size(); i++) {
			int intersect = getIntersect(expectedDocuments.get(i), retrivedDocuments.get(i), level).size();
			rprec.add( (double) intersect / (double) level );
		}
		
		return rprec;
	}
	
	/**
	 * Get Average precision
	 * @param precision precision by getPrecision()
	 * @return avgprecision for every standard level
	 */
	public ArrayList<Double> getAvgPrecision(ArrayList<ArrayList<Double>> precision) {
		ArrayList<Double> avgPrecision = new ArrayList<Double>();
		ArrayList<Double> recallLevel = getRecallLevel();
		double tmp = 0.0;
		
		for (int j = 0; j < recallLevel.size(); j++) {
			for (int i = 0; i < this.precision.size(); i++) {
				tmp += ( (double) precision.get(i).get(j) / (double) this.precision.size() );
			}
			avgPrecision.add(tmp);
			tmp = 0;
		}
		
		return avgPrecision;		
	}
	
	/**
	 * do intersection between two lists
	 * @param expectedDocuments
	 * @param retrievedDocuments
	 * @param much how much element of retrivedDocuments are read
	 * @return new list made by intersection
	 */
	private ArrayList<String> getIntersect(LinkedList<String> expectedDocuments, LinkedList<String> retrievedDocuments, int much){
		ArrayList<String> intersect = new ArrayList<String>();
	
		for (String s: expectedDocuments)
			for (int i = 0; i < much; i++)
				if (retrievedDocuments.get(i).equals(s))
					intersect.add(s);
		
		return intersect;
	}
	
	/**
	 * Calculate F-measure
	 * @param precision list of precision for every query
	 * @param recall list of recall for every query
	 * @return f-measure
	 */
	public ArrayList<Double> getFMeasure(ArrayList<Double> precision, ArrayList<Double> recall) {
		ArrayList<Double> fmeasure = new ArrayList<Double>();
		for (int i = 0; i < precision.size(); i++)
			if ( (precision.get(i)+recall.get(i)) != 0 )
				fmeasure.add(i, ( (2*precision.get(i)*recall.get(i))/(precision.get(i)+recall.get(i)) ) );
			else
				fmeasure.add(i,0.0);
		return fmeasure;
	}
	
	/**
	 * Calculate E-measure
	 * @param precision list of precision for every query
	 * @param recall list of recall for every query
	 * @param b parameter b for e-measure 
	 * @return emeasure
	 */
	public ArrayList<Double> getEMeasure(ArrayList<Double> precision, ArrayList<Double> recall, double b) {
		ArrayList<Double> emeasure = new ArrayList<Double>();
		
		for (int i = 0; i < precision.size(); i++)
			emeasure.add(i, ( 1-( (1+Math.pow(b, 2))/( (Math.pow(b, 2)/recall.get(i) + (1/precision.get(i)) ) ))));
		return emeasure;
	}
	
	/**
	 * Save measure (f or e) on a file. 
	 * @param measure measure to save
	 * @param fileName file name
	 */
	public void saveMeasure(ArrayList<Double> measure, String fileName) {
		try {
			File f = new File(fileName);
			FileWriter fw = new FileWriter(f);
			for (int i = measure.size()-1; i >= 0; i-- )
				fw.append( measure.get(i).toString()+"\n" );
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
	
	/**
	 * Procedure to do graph of benchmark
	 */
	public void doGraph() {
		
		ArrayList<Double> recallLevel = getRecallLevel(); //recallLevel (0.33, 0.66, 1.0)
		ArrayList<ArrayList<Double>> precision = getPrecision();
		
		//R-Precision for different values of R
		ArrayList<Double> rprecision5 = getRPrecision(5);
		ArrayList<Double> rprecision10 = getRPrecision(10);
		ArrayList<Double> rprecision15 = getRPrecision(15);
		
		//Array range(0,num_queries) for plot these numbers
		ArrayList<Double> num_queries = new ArrayList<Double>();
		
		ArrayList<Double> avgPrecision = getAvgPrecision(precision);
		
		for (int i = 0; i < this.precision.size(); i++) {
			num_queries.add(i + 0.0);
		}
		
		try {
			
			System.out.println("Plotting precision graph");
			//Precision
			Plot plot = Plot.plot(Plot.plotOpts().
					title("Precision graph").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Query #", Plot.axisOpts().
					format(Plot.AxisFormat.NUMBER_INT).
					range(0, num_queries.size())).
				yAxis("Precision", Plot.axisOpts().
					range(0, getMax(this.precision))).series(".", Plot.data().
							xy(num_queries, this.precision),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/precision", "png");
			
			System.out.println("Plotting recall graph");
			//Recall
			plot = Plot.plot(Plot.plotOpts().
					title("Recall graph").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Query #", Plot.axisOpts().
					format(Plot.AxisFormat.NUMBER_INT).
					range(0, num_queries.size())).
				yAxis("Recall", Plot.axisOpts().
					range(0, getMax(this.recall))).series(".", Plot.data().
							xy(num_queries, this.recall),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/recall", "png");
			
			System.out.println("Plotting R-Precision (5)");
			//R-Precision 5
			plot = Plot.plot(Plot.plotOpts().
					title("R-Precision 5").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Query #", Plot.axisOpts().
					format(Plot.AxisFormat.NUMBER_INT).
					range(0, num_queries.size())).
				yAxis("Precision", Plot.axisOpts().
					range(0, getMax(rprecision5))).series(".", Plot.data().
							xy(num_queries, rprecision5),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/rprecision5", "png");
			
			System.out.println("Plotting R-Precision (10)");
			//R-Precision 10
			plot = Plot.plot(Plot.plotOpts().
					title("R-Precision 10").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Query #", Plot.axisOpts().
					format(Plot.AxisFormat.NUMBER_INT).
					range(0, num_queries.size())).
				yAxis("Precision", Plot.axisOpts().
					range(0, getMax(rprecision10))).series(".", Plot.data().
							xy(num_queries, rprecision10),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/rprecision10", "png");
			
			System.out.println("Plotting R-Precision (15)");
			//R-Precision 15
			plot = Plot.plot(Plot.plotOpts().
					title("R-Precision 15").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Query #", Plot.axisOpts().
					format(Plot.AxisFormat.NUMBER_INT).
					range(0, num_queries.size())).
				yAxis("Precision", Plot.axisOpts().
					range(0, getMax(rprecision15))).series(".", Plot.data().
							xy(num_queries, rprecision15),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/rprecision15", "png");
			
			System.out.println("Plotting Avg Precision");
			//Avg Precision
			plot = Plot.plot(Plot.plotOpts().
					title("Avg Precision").
					width(1000).
					height(600).
					legend(Plot.LegendFormat.TOP)).	
				xAxis("Recall Level", Plot.axisOpts().
					range(0, 1)).
				yAxis("Avg Precision", Plot.axisOpts().
					range(0, getMax(avgPrecision))).series(".", Plot.data().
							xy(recallLevel, avgPrecision),
							Plot.seriesOpts().
								line(Line.NONE).
								marker(Plot.Marker.COLUMN).
								color(Color.BLUE).markerColor(Color.BLUE));
			plot.save("benchmark/lisa/results/avgprecision", "png");
			
			System.out.println("Plotting Recall/Precision for each query");
			//Recall Level/Precision
			for (int i = 0; i < this.recall.size(); i++) {
				
				plot = Plot.plot(Plot.plotOpts().
						title("Precision - Recall Query "+(i+1)).
						width(1000).
						height(600).
						legend(Plot.LegendFormat.NONE)).	
					xAxis("Recall", Plot.axisOpts().
						range(0.33, 1)).
					yAxis("Precision", Plot.axisOpts().
						range(0, getMax(precision.get(i)))).
					series(".", Plot.data().
						xy(recallLevel.get(2), precision.get(i).get(2)).
						xy(recallLevel.get(1), precision.get(i).get(1)).
						xy(recallLevel.get(0), precision.get(i).get(0)),
						Plot.seriesOpts().
							lineWidth(3).
							marker(Plot.Marker.CIRCLE).
							color(Color.BLUE));
				plot.save("benchmark/lisa/results/recall-precision"+(i+1), "png");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Plotting finished");
	}
	
	/**
	 * Get max value in a double array list
	 * @param list
	 * @return
	 */
	private static double getMax(ArrayList<Double> list) {
		double max = 0.0;
		if (list.size() > 0) 
			max = list.get(0);
		for (int i = 1; i < list.size(); i++)
			if (max < list.get(i))
				max = list.get(i);
		return max;
	}
	
	/**
	 * Get min value in a double array list
	 * @param list
	 * @return
	 */
	private static double getMin(ArrayList<Double> list) {
		double min = 0.0;
		if (list.size() > 0) 
			min = list.get(0);
		for (int i = 1; i < list.size(); i++)
			if (min > list.get(i))
				min = list.get(i);
		return min;
	}
			
}
