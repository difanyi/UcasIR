package com.news.search;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spell.TermFreqIterator;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.news.db.JDBCUtil;
import com.news.index.NewsIndexer;

public class AutoCompleter {
  
	public static final String FIELD = "title";
	public static final String INDEX_DIR = "G:\\MySQLData\\SuggestIndex";
	public static final int RESULTS_TO_DISPLAY = 10;
	private static Connection conn = null;     
    private static Statement stmt = null;     
    private static ResultSet rs = null; 
	  
	private FuzzySuggester suggestor = new FuzzySuggester(AutoCompleter.getAnalyzer());
	
	
	/**
	 * 
	 * @return
	 */
	public static Analyzer getAnalyzer() {
		//Defining a custom analyzer which will be used to index and suggest the data set
		//�Զ���һ��Analyzer�����ڼ����ͽ������ݼ�		
		Analyzer autosuggestAnalyzer = new Analyzer() {
			// ����ͣ�ôʱ�			
			final String [] stopWords =  {"��", "��", "��", "��", "�ȵ�", "at", "be", "but", "by",
			"for", "if", "in", "into", "is", "it",
			"no", "not", "of", "on", "or", "s", "such",
			"t", "that", "the", "their", "then", "there", "these",
			"they", "this", "to", "was", "will", "with"};
      
			@Override
			protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
				final Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_43, reader);
				TokenStream tok = new LowerCaseFilter(Version.LUCENE_43, tokenizer);
				tok = new StopFilter(Version.LUCENE_43, tok, StopFilter.makeStopSet(Version.LUCENE_43, stopWords, true));
				return new TokenStreamComponents(tokenizer, tok) {
					@Override
					protected void setReader(final Reader reader) throws IOException {
						super.setReader(reader);
					}
				};
			}
		};
		return autosuggestAnalyzer;
}
  
	/**
	 * ����Suggestor
	 * @param indexDir ����������Ŀ¼
	 * ʹ��Ĭ�ϵ�Field
	 * @return
	*/
	public boolean buildSuggestor(String indexDir) {
		try {
			Directory dir = new MMapDirectory(new File(indexDir));
			return buildSuggestor(dir, FIELD);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}  
	}
  
	/**
	 * 
	 * @param indexDir ����������Ŀ¼
	 * @param fieldName ����������Field
	 * @return
	*/
	public boolean buildSuggestor(String indexDir, String fieldName) {
		try {
			Directory dir = new MMapDirectory(new File(indexDir));
			return buildSuggestor(dir, fieldName);
		} catch (IOException e) {
			e.printStackTrace();
			return false; 
		}
	}

	public boolean buildSuggestor(Directory directory, String fieldName) {
	
		IndexReader reader;
		try {
		      
			reader = DirectoryReader.open(directory);
			AtomicReader aReader = SlowCompositeReaderWrapper.wrap(reader); // Should use reader.leaves instead ?
			Terms terms = aReader.terms(fieldName);
			      
			if (terms == null) 
				return false; 
			      
			TermsEnum termEnum = terms.iterator(null);
			TermFreqIterator wrapper = new TermFreqIterator.TermFreqIteratorWrapper(termEnum);
			      
			suggestor.build(wrapper);
		      
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
  
	public String[] suggest(String q) {
		List<LookupResult> results = suggestor.lookup(CharBuffer.wrap(q), true, AutoCompleter.RESULTS_TO_DISPLAY);
		String[] autosuggestResults = new String[results.size()];
		for(int i=0; i < results.size(); i++) {
			LookupResult result = results.get(i);
			autosuggestResults[i] = result.key.toString();
		}    
		return autosuggestResults;
	}
	
	/**   
	    * ��ȡ���ݿ�����   
	    * @return ResultSet   
	    * @throws Exception   
	    */    
    private void readDatabase() throws Exception {     
        conn = JDBCUtil.getConnection();     
        if(conn == null) {     
            throw new Exception("���ݿ�����ʧ�ܣ�");     
        }     
        String sql = "SELECT title, content, keyword FROM news";     
        try {     
            stmt = conn.createStatement();     
            rs = stmt.executeQuery(sql);     
            createSuggestIndex(rs);   //�����ݿⴴ������,�˴�ִ��һ�Σ���Ҫÿ�����ж������������Ժ������и��¿��Ժ�̨���ø�������     
                
        } catch(Exception e) {     
            e.printStackTrace();     
            throw new Exception("���ݿ��ѯsql���� sql : " + sql);     
        } finally {     
            if(rs != null) rs.close();     
            if(stmt != null) stmt.close();     
            if(conn != null) conn.close();     
        }     
    }   
    
    
    private void createSuggestIndex(ResultSet rs){
    	IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_43));
    	//�����Ĵ򿪷�ʽ��û�������ļ����½����оʹ�  	    	
    	iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); 
    	Directory directory = null;
    	IndexWriter indexWriter = null;
    	try {
    		
			directory = FSDirectory.open(new File(INDEX_DIR));
			
			//���������������״̬�������  
            if (IndexWriter.isLocked(directory)){  
                IndexWriter.unlock(directory);  
            }
			indexWriter = new IndexWriter(directory, iwConfig);
			
			Document document = null;
			while(rs.next()) {
				document = new Document();
				TextField title = new TextField("title", rs.getString("title"), Field.Store.YES);
				TextField content = new TextField("content", rs.getString("content"), Field.Store.YES);
				TextField keyword = new TextField("keyword", rs.getString("keyword"), Field.Store.YES);
				document.add(title);
				document.add(content);
				document.add(keyword);
				indexWriter.addDocument(document);
			}
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	//��indexWrite�����ύ��������ύ��֮ǰ�Ĳ��������ᱣ�浽Ӳ��  
        try {  
            //��һ��������ϵͳ��Դ������commit������Ҫ��һ���Ĳ���  
            indexWriter.commit();  
            //�ر���Դ  
            indexWriter.close();  
            directory.close();  
            System.out.println("�����������");
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }
	
	

	public static void main(String[] args) {
		
	    Directory dir;
		try {
			dir = FSDirectory.open(new File(INDEX_DIR));
			AutoCompleter suggestor = new AutoCompleter();
			suggestor.readDatabase();
		    boolean success = suggestor.buildSuggestor(dir, AutoCompleter.FIELD);
		    if(success) {
		    	String[] results = suggestor.suggest("ҽ");
		    	for (String string : results) {
					System.out.println(string);
		    	}
		    }
		    else {
		    	System.out.println("Failed to build suggestor");
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}