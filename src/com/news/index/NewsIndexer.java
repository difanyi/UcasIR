package com.news.index;

import com.news.db.*;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
	        String sql = "SELECT title, content, keyword FROM news";     
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
    	Directory directory = null;
    	IndexWriter indexWriter = null;
    	analyzer = new IKAnalyzer();
    	IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_43, analyzer);
    	//�����Ĵ򿪷�ʽ��û�������ļ����½����оʹ�  
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND); 
    	
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
				TextField title = new TextField("title", rs.getString("title"), Field.Store.YES);
				TextField content = new TextField("content", rs.getString("content"), Field.Store.YES);
				TextField keyword = new TextField("keyword", rs.getString("keyword"), Field.Store.YES);
				document.add(title);
				document.add(content);
				document.add(keyword);
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
            System.out.println("�����������");
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
