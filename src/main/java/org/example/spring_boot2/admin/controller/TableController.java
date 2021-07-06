package org.example.spring_boot2.admin.controller;

import org.example.spring_boot2.admin.bean.User;
import org.example.spring_boot2.admin.exception.UserTooManyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * @author lifei
 */
@Controller
public class TableController {
    @GetMapping("/basic_table")
    public String basicTable(@RequestParam("a") String a) {
        int x = 10 / 0;
        return "table/basic_table";
    }

    @GetMapping("/dynamic_table")
    public String dynamicTable(Model model) {
        List<User> users = Arrays.asList(
                new User("user1", "pwd1"),
                new User("user2", "pwd2"),
                new User("user3", "pwd3"),
                new User("user4", "pwd4"),
                new User("user5", "pwd5"),
                new User("user6", "pwd6")
        );
        model.addAttribute("users", users);
        if (users.size() > 3) {
            throw new UserTooManyException();
        }
        return "table/dynamic_table";
    }

    @GetMapping("/editable_table")
    public String editableTable() {
        return "table/editable_table";
    }

    @GetMapping("/pricing_table")
    public String pricingTable() {
        return "table/pricing_table";
    }

    @GetMapping("/responsive_table")
    public String responsiveTable() {
        return "table/responsive_table";
    }
}
