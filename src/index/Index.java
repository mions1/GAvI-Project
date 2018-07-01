package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import irModels.Model;
import irModels.VectorSpaceModel;

/**
 * @author luca
 * 
 * This class implements an Index. This Index allows to being manipulated by user, 
 * who decide which Documents/Directories adding/removing.
 * 
 */
public class Index{

	/*
	 * A singleton to have an unique index, reachable by each part of the program and equals for 
	 * everybody
	 */
	private static Index uniqueIndex = null;
	
	private static StandardAnalyzer stdAnalyzer = null; 
	private static Directory dirIndex = null;
	private static IndexWriterConfig iwConfig = null; 
	private static IndexWriter inWriter = null; 
	private static IndexReader inReader = null;
	private static IndexSearcher inSearcher = null;
	private static Similarity simUsed = null;
	
	private Index() {
		startIndex();
	}
	
	/**
	 * This method makes the Index class a singleton, allocating uniqueIndex as the only instance of
	 * this class, and returning it. By default, VectorSpaceModel is the model applied to it.
	 * @return Index as a uniqueIndex
	 */
	public static Index getIndex() {
		if(uniqueIndex == null) {
			return getIndex(new VectorSpaceModel().getSimilarity());
		}
		return uniqueIndex;
	}
	
	/**
	 * This method allows to create index specifying what similarity has to be set. When constructor is called,
	 * simUsed define the model to use for similarity.
	 * @param sim is the similarity to set, not applied if uniqueIndex is yet created
	 * @return Index 
	 */
	public static Index getIndex(Similarity sim) {
		if(uniqueIndex == null) {
			simUsed = sim;
			uniqueIndex = new Index();
		}
		return uniqueIndex;
	}
	
	/**
	 * A method used to allocate all tools of the Index. To change similarity, index has to be re-initialized.
	 */
	private void startIndex() {
		stdAnalyzer = new StandardAnalyzer();
		dirIndex = new RAMDirectory();
		iwConfig = new IndexWriterConfig();
		iwConfig.setSimilarity(simUsed);
		
		try {
			inWriter = new IndexWriter(dirIndex, iwConfig);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			inReader = DirectoryReader.open(inWriter);
			inSearcher = new IndexSearcher(inReader);
			inSearcher.setSimilarity(simUsed);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * To change similarity used by an index, this has to be re-initialized, losing its content. To prevent this,
	 * reload can be set to true, saving content of index in a temporary Index (tempIndex) and reloading them after
	 * re-initializing. This is done only if similarity of index is different by the passed one.
	 * @param sim is the similarity to set
	 * @param reload is a boolean to save index in a temporary save and reload it's content after reset
	 */
	public void setSimilarity(Similarity sim, boolean reload) {
		if(simUsed.getClass() != sim.getClass()) {
			simUsed = sim;
			if(reload) {
				saveIndex("tempIndex.ser");
			}
			resetIndex();
			if(reload) {
				loadIndex("tempIndex.ser");
			}
		}
	}
	
	/**
	 * This method removes the previous index and closes its tools. 
	 * Then it makes a new Index, reallocating new tools.
	 * This is the fastest and easiest way to "clear" totally an index from its entries.
	 */	
	public void resetIndex() {
		closeIndex();
		startIndex();
	}
	
	/**
	 * This method close tools that are closable.
	 */
	private void closeIndex() {
		if(stdAnalyzer != null){
			stdAnalyzer.close();
		}
		if(inWriter != null) {
			try {
				inWriter.deleteAll();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(dirIndex != null) {
			try {
				dirIndex.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method write all documents path to the target file, as clear text.
	 * @param saveFile is the path of the saveFile (plain text) to load (each line is the path to the document)
	 */
	public void saveIndex(String saveFile) {
		if (getSize() == 0 && !saveFile.equals("tempIndex.ser")) {
			System.err.println("This index is empty, saving it is useless");
			return ;
		}
		
		PrintWriter fileWriter = null;
		
		try {
			fileWriter = new PrintWriter(saveFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("File " + saveFile + " doesn't exist");
		}
		
		for (int k = 0 ; k < this.getSize() ; k++) {
			fileWriter.println(getDocument(k).get("path") + getDocument(k).get("name"));
		}
		
		fileWriter.close();
		System.out.println("Saving successful to " + saveFile + "!");
	}
	
	/**
	 * This method, contrary to saveIndex, load documents in Index by a save file that contains a list of them.
	 * It doesn't overwrite index content, only adds documents to index.
	 * @param saveFile is the file containing documents to be loaded.
	 */
	public void loadIndex(String saveFile) {
		System.out.println("Loading from " + saveFile);
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new FileReader(new File(saveFile)));
		} catch (FileNotFoundException e) {
			if(!saveFile.equals("tempIndex.ser")) {
				e.printStackTrace();
			}
			System.err.println("File " + saveFile + " doesn't seem to exist, or some else error showed up. Loading aborted.");
			return ;
		}
		
		String line = "";
		try {
			while ( (line = reader.readLine()) != null) {
				addDocument(line);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			return ;
		}
		
		System.out.println("Loading successful from " + saveFile + "!");
	}
	
	
	/**
	 * This method is used to create and to add a document to the index.
	 * @param docPath is a concatenation of path and name of a document (for example "doc/Lucene.pdf")
	 */
	public void addDocument(String docPath) {
		Document doc = new Document();
		
		BufferedReader buffer = null;
		try{
			buffer = new BufferedReader(new FileReader(docPath));
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		/*
		 * Reading of document and storing it in content String, using line String to check 
		 * consistency of each line read by BufferedReader
		 */
		String content = "";
		String line;
		try {
			while((line = buffer.readLine()) != null) {
				content += line + "\n";
			}
		}catch(Exception e) {
			System.out.println("End of file");
		}
		
		int separatorIndex = docPath.lastIndexOf("/");
		
		String path = "";
		
		/*
		 * Relative path could be used in tests to add documents to index, so the "/" could be missing. Using
		 * gui, absolute path will be always declared, ignoring this problem
		 */
		if (separatorIndex != -1) {
			path = docPath.substring(0, separatorIndex+1);
		}
		
		String name = docPath.substring(separatorIndex+1, docPath.length());
		
		/*
		 * Document properties are stored into Document type.
		 * @warning path field is not intended to be used for queries
		 */
		doc.add(new TextField("path", path, Field.Store.YES));
		doc.add(new TextField("name", name, Field.Store.YES));
		doc.add(new TextField("content", content, Field.Store.YES));
		
		try {
			inWriter.addDocument(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		/*
		 * This updates indexReader because index has been modified (a new document has been added to it)
		 */
		try {
			inReader = DirectoryReader.openIfChanged((DirectoryReader) inReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a document giving corresponding index.
	 * @param index of the document to return
	 * @return document object from index
	 */
	public Document getDocument(int index) {
		Document doc = null;
		try {
			doc = inReader.document(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return doc;
	}
	
	/**
	 * This method removes a document from the index, given its position into the index
	 * @param index is index of document to remove
	 */
	public void removeDocument(int index) {
		try {
			inWriter.tryDeleteDocument(inReader, index);
			inReader = DirectoryReader.openIfChanged((DirectoryReader)inReader);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the number of documents stored in index.
	 * @return size of the index
	 */
	public int getSize() {
		return inWriter.numDocs();
	}
	
	/**
	 * This method requires a string representing user query, a LinkedList of Strings containing fields
	 * in which searching, the Model instance used to parse query, a boolean print to get query and results to be printed or not
	 * @param query is the query String
	 * @param fields are fields on which search
	 * @param m is the model to use for parsing query
	 * @param print allows query and results printing
	 * @return a list of "Hit", where Hit is a custom class that contains a document and its score for that query	 
	 */
	public LinkedList<Hit> submitQuery(String query, LinkedList<String> fields, Model m, boolean print) {
		
		LinkedList<Hit> queryResults = new LinkedList<Hit>();
		
		if(getSize() == 0) {
			System.err.println("No documents in index!");
			return null;
		}
		
		Query q = m.getQueryParsed(query, fields, stdAnalyzer);
		
		TopDocs results = null;
		ScoreDoc[] hits = null;
		
		if (print) {
			System.out.println("Printing query: " + q.toString() + "\n");
		}
		
		/* Updating of IndexSearcher. The only way to update a searcher is to
		 * create a new searcher on the current reader. This is cheap if we already have a reader
		 * available (as we have)
		 */		
		try {
			inSearcher = new IndexSearcher(inReader);
			inSearcher.setSimilarity(simUsed);
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		try {
			results = inSearcher.search(q, getSize());
			hits = results.scoreDocs;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("\nSomething goes wrong with your query... Quitting...");
			return null;
		}
		
		System.out.println(results.totalHits + " total matching documents");
		
		
		Document doc = null;
		try {
			for (int k=0 ; k < hits.length ; k++) {
					doc = inSearcher.doc(hits[k].doc);
					queryResults.add(new Hit(doc.get("path"), doc.get("name"), hits[k].score));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return queryResults;
	}
	
}