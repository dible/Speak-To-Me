package org.cmi.wikisearch.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
// modified by Ning start 1
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import org.apache.lucene.analysis.CharArraySet;
// modified by Ning end 1

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.cmi.wikisearch.beans.SearchInfo;
import org.cmi.wikisearch.beans.SearchResult;

public class LuceneSearchService implements SearchService {

	private IndexSearcher isearcher;
	private String [] fields;
	private Analyzer analyzer;
	// modified by Ning start 2
	public static Set<?> MY_STOP_WORDS_SET;
	  
	  static {
	    List<String> stopWords = Arrays.asList(
	      "a", "an", "and", "are", "as", "at", "be", "but", "by",
	      "for", "if", "in", "into", "is", "it",
	      "no", "not", "of", "on", "or", "such",
	      "that", "the", "their", "then", "there", "these",
	      "they", "this", "to", "was", "will", "with",
	      "d", "t", "i", "like", "want"
	    );
	    CharArraySet stopSet = new CharArraySet(Version.LUCENE_31, 
	        stopWords.size(), false);
	    stopSet.addAll(stopWords);  
	    MY_STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet); 
	  }
	  // modified by Ning end 2
	
	protected final Log logger = LogFactory.getLog(getClass());
	
	public LuceneSearchService () {
		// modified by Ning start 3
		//analyzer = new StandardAnalyzer();
		analyzer = new StandardAnalyzer(MY_STOP_WORDS_SET);
		// modified by Ning end 3
	}
	
	public SearchInfo search(String searchQuery, int numResults) {
	    logger.debug("requesting " + numResults + " search results");
	    
		BooleanQuery wikiQuery = new BooleanQuery();
		try {
			this.addTermQuery(wikiQuery, "title",    searchQuery, 8, BooleanClause.Occur.SHOULD);
			this.addTermQuery(wikiQuery, "gist",     searchQuery, 4, BooleanClause.Occur.SHOULD);
			this.addTermQuery(wikiQuery, "contents", searchQuery, 1, BooleanClause.Occur.SHOULD);
		}
		catch (ParseException pe){
			logger.error("Error forming search query", pe);
			throw new RuntimeException("Error forming search query", pe);
		}
		
		Hits hits;
		try {
			hits = isearcher.search(wikiQuery);
		}
		catch (IOException io){
			logger.error("Error searching index, io");
			throw new RuntimeException(io);
		}
        
        int count = numResults;
        if (hits.length() < count)
           count = hits.length();

        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        try {
	        for (int i=0; i<count;i++){
	           Document hitDoc = hits.doc(i);
	           
	           SearchResult result = new SearchResult();
	           result.setScore(hits.score(i));
	           result.setDocID(URLEncoder.encode(hitDoc.getField("fileId").stringValue(),"UTF-8"));
	           result.setTitle(hitDoc.getField("title").stringValue().replace("'", ""));
	           String gist = hitDoc.getField("gist").stringValue();
	           result.setGist(gist.length() > 250 ? gist.substring(0,250) : gist);
	           results.add(result);
	        }
        }
        catch (IOException io){
        	logger.error("Error reading index", io);
        	throw new RuntimeException(io);
        }
        
        if (logger.isDebugEnabled()){
        	logger.debug("Search processed: " + searchQuery + "\tNumber of hits: " + hits.length());
        }
        
        SearchInfo info = new SearchInfo();
        info.setResults(results);
        info.setTotalHits(hits.length());
        
        return info;
	}

	protected void addTermQuery(BooleanQuery searchQuery, String field, String terms, float boost, BooleanClause.Occur occur) throws ParseException {
		QueryParser parser = new QueryParser("title", analyzer);
		if (terms != null && terms.trim().length() != 0) {
			String queryString = field + ":(" + terms + ")^"
					+ boost;
			Query query = parser.parse(queryString);
			searchQuery.add(query, occur);
		}
	}
	  
	public IndexSearcher getIsearcher() {
		return isearcher;
	}

	public void setIsearcher(IndexSearcher isearcher) {
		this.isearcher = isearcher;
	}

	public String[] getFields() {
		return fields;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}

}
