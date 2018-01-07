package com.news.index;

import com.news.db.*;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class NewsIndexer {
	
	public static final String searchDir = "G:\\MySQLData\\Index";
	private static Analyzer analyzer = null; 
	private static Connection conn = null;     
    private static Statement stmt = null;     
    private static ResultSet rs = null; 
    private static File indexFile = null;     

	
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
	        String sql = "SELECT news_id, title, content, keyword, release_time, category, source, join_num FROM news where release_time > '2017-12-10'";     
	        try {     
	            stmt = conn.createStatement();     
	            rs = stmt.executeQuery(sql);     
	            this.createIndex(rs);   //�����ݿⴴ������,�˴�ִ��һ�Σ���Ҫÿ�����ж������������Ժ������и��¿��Ժ�̨���ø�������     
	                
	        } catch(Exception e) {     
	            e.printStackTrace();     
	            throw new Exception("���ݿ��ѯsql���� sql : " + sql);     
	        } finally {     
	            if(rs != null) rs.close();     
	            if(stmt != null) stmt.close();     
	            if(conn != null) conn.close();     
	        }              
	    }     
	
	
	/**
     * Ϊ���ݿⴴ������
     * @param rs
     */
    private void createIndex(ResultSet rs) throws Exception {
    	System.out.println("���ڴ������� ... ...");
    	long t0 = System.currentTimeMillis();
    	Directory directory = null;
    	IndexWriter indexWriter = null;
    	analyzer = new IKAnalyzer();
    	IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_43, analyzer);
    	//�����Ĵ򿪷�ʽ��û�������ļ����½����оʹ�  
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE); 
    	
    	try {
    		
			indexFile = new File(searchDir);
			directory = FSDirectory.open(indexFile);
			
			//���������������״̬�������  
            if (IndexWriter.isLocked(directory)){  
                IndexWriter.unlock(directory);  
            }
			indexWriter = new IndexWriter(directory, indexWriterConfig);
			
			Document document = null;
			while(rs.next()) {
				document = new Document();
//				StringField��������TextField����
				TextField title = new TextField("title", rs.getString("title"), Field.Store.YES);
				TextField content = new TextField("content", rs.getString("content"), Field.Store.YES);
				TextField keyword = new TextField("keyword", rs.getString("keyword"), Field.Store.YES);
				Field release_time = new StringField("release_time", rs.getString("release_time"), Field.Store.YES);
				Field join_num = new IntField("join_num", rs.getInt("join_num"), Field.Store.YES);
				Field category = new StringField("category", rs.getString("category"), Field.Store.YES);
				Field newsId = new StringField("newsId", rs.getString("news_id"), Field.Store.YES);
				Field source = new StringField("source", rs.getString("source"), Field.Store.YES);
				title.setBoost(100);
				keyword.setBoost(100);
				content.setBoost(50);
				document.add(title);
				document.add(content);
				document.add(keyword);
				document.add(release_time);
				document.add(join_num);
				document.add(category);
				document.add(newsId);
				document.add(source);
				indexWriter.addDocument(document);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	//��indexWrite�����ύ��������ύ��֮ǰ�Ĳ��������ᱣ�浽Ӳ��  
        try {  
            //��һ��������ϵͳ��Դ������commit������Ҫ��һ���Ĳ���  
            indexWriter.commit();  
            //�ر���Դ  
            indexWriter.close();  
            directory.close();  
            System.out.println("�����������, �ܼ���ʱ " + (System.currentTimeMillis()-t0)/1000 + " s");
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }
    
    public static void main(String[] args) {
    	NewsIndexer newsIndexer = new NewsIndexer();
    	try {
    		newsIndexer.readDatabase();
    	}catch (Exception e) {
			e.getSuppressed();
		}
    }
}
