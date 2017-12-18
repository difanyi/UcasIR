package com.news.search;

import com.news.index.*;

import java.io.File;     
import java.util.ArrayList;     
import java.util.List;     
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

import org.apache.lucene.store.FSDirectory;  
import org.apache.lucene.util.Version;  
import org.wltea.analyzer.lucene.IKAnalyzer;  


/**     
* NewsSearcher.java   
* @version 1.0   
* @createTime Lucene ���ݿ����   
*/

public class NewsSearcher {     
        
         
    private static File indexFile = null;     
    private static IndexSearcher searcher = null; 
    private static IndexReader reader = null;
    private String prefixHTML = "<font color='red'>";
	private String suffixHTML = "</font>";
    /** ��ѯ������� */    
    private int maxBufferedDocs = 100;     
    
    
    /**
     * ������������б�
     * @param queryStr
     * @return
     * @throws Exception
     */
    public List<SearchBean> getResult(String queryStr) throws Exception{     
        List<SearchBean> result = null;  
        
        try {
        	result = search(queryStr);     
        }catch (Exception e) {
			e.printStackTrace();
		}
        
        return result;
    }    
    
    
    /**
     * ��������
     * @param queryStr
     * @return
     * @throws Exception
     */
    private List<SearchBean> search(String queryStr) throws Exception {
    	
    	// ����IndexReader    	
    	indexFile = new File(NewsIndexer.searchDir);
    	reader = DirectoryReader.open(FSDirectory.open(indexFile));
    	// ����IndexSearcher    	
    	searcher = new IndexSearcher(reader);
    	searcher.setSimilarity(new IKSimilarity());
    	// ������ѯ���    
    	String[] fields = {"title", "content", "keyword"};
    	MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(Version.LUCENE_43, fields, new IKAnalyzer()); 

    	// ִ�в�ѯ    	
    	Query query = multiFieldQueryParser.parse(queryStr);
    	
    	TopDocs topDocs = searcher.search(query, maxBufferedDocs);
    	ScoreDoc[] scoreDocs = topDocs.scoreDocs;
    	
    	// ���������ת��ΪSearchBean�б�
    	List<SearchBean> listBean = new ArrayList<SearchBean>();
    	SearchBean bean = null;
    	for(int i = 0; i < scoreDocs.length; i++) {
    		int docId = scoreDocs[i].doc;
    		Document document = searcher.doc(docId);
    		bean = new SearchBean();
    		bean.setTitle(document.get("title"));
    		bean.setContent(document.get("content"));
    		bean.setKeyword(document.get("keyword"));
    		bean.setSnippet(snippetGen(bean.getContent(), query));
    		listBean.add(bean);
    	}
    	return listBean;
    }   
    
    
    private String snippetGen(String content, Query query) {
    	SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter(prefixHTML, suffixHTML); 
    	Highlighter highlighter = new Highlighter(simpleHTMLFormatter, new QueryScorer(query));
    	String highLightText = "";
    	try {
    		highLightText = highlighter.getBestFragment(new IKAnalyzer(), "content", content);
//    		System.out.println(highLightText);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return highLightText;
    }
    
    
    public static void main(String[] args) {
    	NewsSearcher newsSearcher = new NewsSearcher();
    	try {
			Long startTime = System.currentTimeMillis();
			List<SearchBean> result = newsSearcher.getResult("�ѹ�");
			int i = 0;
			for(SearchBean bean : result) {
				if(i == 10)
					break;
				System.out.println("bean.title: " + bean.getTitle() + " bean.snippet: "+ bean.getSnippet() + " bean.content: " + bean.getContent() + " bean.keyword: " + bean.getKeyword());
				i++;
			}
			System.out.println("searchBean.result.size: " + result.size());
			Long endTime = System.currentTimeMillis();
			System.out.println("��ѯ������ʱ��Ϊ�� " + (endTime-startTime) + " ����");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
    }
}
  
