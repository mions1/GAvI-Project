package index;

/**
 * @author luca
 * 
 * This class represent an "hit", a document retrieved by index. It is made by path of the document, name of
 * the document, and its score. Index returns a list of Hit. This class is needed only to link a document to its
 * score, for that query
 */
public class Hit {
	private String docPath;
	private String docName;  
    private float score;  
    
    Hit(String docPath, String docName, float score){ 
    	this.docPath = docPath;
        this.docName = docName;
        this.score = score;
    }  
    
    public String getDocPath() {
    	return docPath;
    }
    
    public String getDocName() {
    	return docName;
    }
    
    public float getScore() {
    	return score;
    }
}
