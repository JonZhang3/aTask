package com.atask;

import com.atask.util.Utils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class StatViewServlet extends HttpServlet {

    private static final String USER_KEY = "a-task-user";
    private static final String PARAM_NAME_USERNAME = "username";
    private static final String PARAM_NAME_PASSWORD = "password";
    private static final String RESOURCE_PATH = "support";

    private String username;
    private String password;



    @Override
    public void init() throws ServletException {
        this.username = getInitParameter(PARAM_NAME_USERNAME);
        this.password = getInitParameter(PARAM_NAME_PASSWORD);
        if(username == null) {
            username = "";
        }
        if(password == null) {
            password = "";
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String requestURI = req.getRequestURI();
        resp.setCharacterEncoding("UTF-8");
        if(contextPath == null) {
            contextPath = "";
        }
        String uri = contextPath + servletPath;
        String path = requestURI.substring(contextPath.length() + servletPath.length());
        // 登录接口，不需要验证
        if("/toLogin".equals(path)) {
            String reqUsername = req.getParameter(PARAM_NAME_USERNAME);
            String reqPassword = req.getParameter(PARAM_NAME_PASSWORD);
            if(this.username.equals(reqUsername) && this.password.equals(reqPassword)) {
                if(requireAuth()) {
                    Cookie cookie = new Cookie(USER_KEY, encodeCookieValue());
                    cookie.setMaxAge(12 * 60 * 60);
                    resp.addCookie(cookie);
                }
                resp.getWriter().print("success");
            } else {
                resp.getWriter().print("error");
            }
            return;
        }
        if (requireAuth() //
            && !checkUser(req)//
            && !("/login.html".equals(path) //
            || path.startsWith("/css")//
            || path.startsWith("/js"))) {
            if (contextPath.equals("") || contextPath.equals("/")) {
                toLogin(resp, "/atask/login.html");
            } else {
                if ("".equals(path)) {
                    toLogin(resp, "atask/login.html");
                } else {
                    toLogin(resp, "login.html");
                }
            }
            return;
        }
        if ("".equals(path)) {
            if (contextPath.equals("") || contextPath.equals("/")) {
                resp.sendRedirect("/atask/index.html");
            } else {
                resp.sendRedirect("atask/index.html");
            }
            return;
        }

        if ("/".equals(path)) {
            resp.sendRedirect("index.html");
            return;
        }
        if(path.contains(".json")) {
            String fullUrl = path;
            if (req.getQueryString() != null && req.getQueryString().length() > 0) {
                fullUrl += "?" + req.getQueryString();
            }
            resp.getWriter().print(process(fullUrl));
            return;
        }
        returnFile(path, uri, resp);
    }

    private boolean requireAuth() {
        return Utils.isNotEmpty(this.username);
    }

    private boolean checkUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if(USER_KEY.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if(Utils.isNotEmpty(value)) {
                        try {
                            byte[] bytes = Utils.aesDecode(Base64.getDecoder().decode(value), password);
                            value = new String(bytes, StandardCharsets.UTF_8);
                            return username.equals(value);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void returnFile(String fileName, String uri, HttpServletResponse response) throws IOException {
        String filePath = RESOURCE_PATH + fileName;
        if(fileName.endsWith(".html")) {
            response.setContentType("text/html; charset=utf-8");
        } else if(fileName.endsWith(".css")) {
            response.setContentType("text/css;charset=utf-8");
        } else if(fileName.endsWith(".js")) {
            response.setContentType("text/javascript;charset=utf-8");
        }
        String text = Utils.readFromResource(filePath);
        if(text == null) {
            response.sendRedirect(uri + "/index.html");
            return;
        }
        response.getWriter().write(text);
    }

    private String encodeCookieValue() {
        byte[] bytes = Utils.aesEncode(username, password);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String process(String url) {
        if (url.equals("/basic.json")) {
            return TaskStatService.INSTANCE.getBasicInfo();
        }
        if(url.equals("/tasks.json")) {
            return TaskStatService.INSTANCE.getTaskInfo();
        }
        return "";
    }

    private void toLogin(HttpServletResponse resp, String url) throws IOException {
        Cookie cookie = new Cookie(USER_KEY, "");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
        resp.sendRedirect(url);
    }

}
