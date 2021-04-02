package cn.edu.hziee.cams.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by HuangChao on 2021/3/24
 * 神秘代码：URBBRGROUN
 * secret code:URBBRGROUN
 */
@Controller
public class LoginController {
    @RequestMapping("/login")
    public String login(){
        return "login";
    }
}
