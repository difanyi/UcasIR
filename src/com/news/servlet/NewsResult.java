package com.news.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.news.search.CommentBean;
import com.news.search.ContentSearcher;
import com.news.search.NewsSearcher;
import com.news.search.SearchBean;

public class NewsResult extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * ����get����
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("doGet����ִ��");
		// ������Ӧ��������
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
		try {
            String queryStr = new String(request.getParameter("newsId").getBytes("ISO-8859-1"),"utf-8");
            System.out.println(queryStr);
            ContentSearcher contentSearcher = new ContentSearcher();
            SearchBean news = contentSearcher.getContent(queryStr);
			System.out.println(news.getContent());
            
			List<CommentBean> comments = contentSearcher.getComments(queryStr);
			System.out.println("��������:"+comments.size());
			HttpSession session = request.getSession();
			
			session.setAttribute("news",news);
			session.setAttribute("comments",comments);
			
			String url="/UcasIR/news.jsp";
			response.sendRedirect(url);
			

		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			out.close();
		}
	}
	
	/**
	 * ����POST����
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
