package net.project.controller;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

//import net.project.servlet.MvcFilter;


interface ControllerInterface{
	public void IndexAction();
}

public class Controller{
	public String[] params;
	public HttpServletRequest request;
	public HttpServletResponse response;
	private PrintWriter out;
	public FilterConfig config;
	public void init(String[] par, HttpServletRequest req, HttpServletResponse res, FilterConfig config){
		this.params = par;
		this.request = req;
		this.response = res;
		this.config = config;
	}
	public void echo(String str){
        if(this.out == null){
            try {
                this.out = response.getWriter();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		this.out.print(str);
	}
	public void render(String viewName){
		response.setContentType("text/html");
		RequestDispatcher rd = request.getRequestDispatcher("/"+viewName+".jsp");
		try {
			rd.include(request, response);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public String file(String path){
		InputStream is = config.getServletContext().getResourceAsStream(path);
		String out = "";
	    StringBuilder sb = new StringBuilder();
	    try {
	    	InputStreamReader isr = new InputStreamReader(is);
	    	BufferedReader br = new BufferedReader(isr);
	    	/*
	    	BufferedReader br = new BufferedReader(new FileReader(path));
	        */
	        while ((out = br.readLine()) != null) {
	        	if(sb.length() != 0){sb.append("\n");}
	        	sb.append(out);
	        }
	        
	    }catch (Exception e) {echo("Error: " + e);}
	    return sb.toString();
	}
	public void redirect(String path){
		try {
			response.sendRedirect(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
