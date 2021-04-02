package cn.edu.hziee.cams.intercepter;

/**
 * Created by HuangChao on 2021/3/24
 */
import cn.edu.hziee.cams.entity.Admin;
import cn.edu.hziee.cams.entity.User;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Component
public class LoginIntercepter implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求的URL
        String url = request.getRequestURI();
        if (url.matches("/user")){
            request.getSession().setAttribute("admin",null);
            User user = (User)request.getSession().getAttribute("user");
            if (null==user){
                response.sendRedirect("/login");
                return false;
            }else {
                return true;
            }
        }else if (url.matches("/admin")){
            request.getSession().setAttribute("user",null);
            Admin admin = (Admin) request.getSession().getAttribute("admin");
            if (null==admin){
                response.sendRedirect("/login");
                return false;
            }else {
                return true;
            }
        }else {
            return true;
        }
//        //获取Session
//        User user = (User)request.getSession().getAttribute("user");
//        Admin admin = (Admin) request.getSession().getAttribute("admin");
//        if (null == user&&null==admin) {
//            response.sendRedirect("/login");
//            return false;
//        }else {
//            return true;
//        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {

    }
}
