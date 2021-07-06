package org.example.spring_boot2.admin.controller;

import org.example.spring_boot2.admin.bean.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;

/**
 * @author lifei
 */
@Controller
@Slf4j
public class IndexController {
    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String main(User user, HttpSession session, Model model) {
        if (StringUtils.hasLength(user.getUsername())
                && StringUtils.hasLength(user.getPassword())
                && "zhangsan".equals(user.getUsername())
                && "123456".equals(user.getPassword())) {
            session.setAttribute("loginUser", user);
            // 防止表单重复提交，重定向
            return "redirect:/main.html";
        } else {
            model.addAttribute("msg", "账号密码错误");
            return "login";
        }
    }

    @GetMapping("/main.html")
    public String mainPage(HttpSession session, Model model) {
        // 验证是否登录，应该采用过滤器、拦截器
        // Object loginUser = session.getAttribute("loginUser");
        // if (loginUser != null) {
        //     return "main";
        // } else {
        //     model.addAttribute("msg", "请重新登录");
        //     return "login";
        // }
        log.info("执行控制器方法: {}.{}", "IndexController", "mainPage()");
        return "main";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("loginUser");
        return "redirect:/";
    }
}
