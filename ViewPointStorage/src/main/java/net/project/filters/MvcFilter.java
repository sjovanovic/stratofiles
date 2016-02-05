package net.project.filters;

/**
 * Created by slobodanjovanovic on 2/5/16.
 */

import net.project.controller.IndexController;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MvcFilter implements Filter {

    private FilterConfig filterConfig;

    @Override
    public void init(FilterConfig fc) throws ServletException {
        this.filterConfig = fc;
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        String params[] = {};
        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if(path != ""){
            params = path.substring(1).split("/");
        }


        /**
         * Ignore URL-s that match pattern from "exclude" init param
         */
        String exc = this.filterConfig.getInitParameter("exclude");
        if(exc != null){
            if(path.matches(exc)){
                chain.doFilter(request, response);
                return;
            }
        }


        /**
         *  first param is a name of the controller
         *  second param is the name of the action
         *  invoke particular controller and action
         *   GET path is in form of /name/method where name is the name of the controller class + "Controller" and method is the name of the controller method + "Action"
         */

        String controllerName;
        if(params.length >=1 && params[0].length() > 1){
            controllerName = params[0].substring(0,1).toUpperCase() + params[0].substring(1).toLowerCase() + "Controller";
        }else{
            // instantiate IndexController
            controllerName = "IndexController";
        }
        String actionName;
        if(params.length >=2){
            actionName = params[1].substring(0,1).toUpperCase() + params[1].substring(1).toLowerCase() + "Action";
        }else{
            // call IndexAction method
            actionName = "IndexAction";
        }
        this.dispatchController(controllerName, actionName, params, (HttpServletRequest)request, (HttpServletResponse)response);

        //chain.doFilter(request, response);
    }

    /*
     * This method tries to instantiate a class given the class name
     * and then call a method given the method name
     * it defaults to IndexController class and IndexAction method
     */
    @SuppressWarnings("unchecked")
    private void dispatchController(String controllerName, String methodName, String[] params, HttpServletRequest request,HttpServletResponse response){
        try {
            Class myclass = Class.forName("net.project.controller."+controllerName);
            //Use reflection to list methods and invoke them
            Method[] methods = myclass.getMethods();
            Object object = myclass.newInstance();

            Boolean foundMethod = false;
            for (int i = methods.length - 1; i >= 0; i--) {
                if (methods[i].getName() == "init") {
                    methods[i].invoke(object, params, request, response, this.filterConfig);
                }
                if(methods[i].getName().contains(methodName)){
                    methods[i].invoke(object);
                    foundMethod = true;
                    return;
                }
            }
            if(!foundMethod){
                myclass.getMethod("IndexAction").invoke(object);
            }

        } catch (Exception ex) {
            //ex.printStackTrace();
            IndexController ict = new IndexController();
            ict.init(params, request, response, this.filterConfig);
            // check if given method exists
            methodName = controllerName.replace("Controller", "Action");
            try {
                Method me = ict.getClass().getMethod(methodName);
                try {
                    me.invoke((Object)ict);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                ict.IndexAction();
            }
        }
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }
}
