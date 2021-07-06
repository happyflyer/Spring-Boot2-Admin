package org.example.spring_boot2.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author lifei
 */
@Slf4j
@Controller
public class FormController {
    @GetMapping("/form_layouts")
    public String formLayout() {
        return "form/form_layouts";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("email") String email,
                         @RequestParam("password") String password,
                         @RequestParam("one_file") MultipartFile oneFile,
                         @RequestParam("multi_file") MultipartFile[] multiFile)
            throws IOException {
        log.info("上传的数据：email={}, password={}, oneFile={}, multiFile={}",
                email, password, oneFile.getSize(), multiFile.length);
        if (!oneFile.isEmpty()) {
            String originalFilename = oneFile.getOriginalFilename();
            oneFile.transferTo(new File("D:/" + originalFilename));
        }
        if (multiFile.length > 0 && new File("D:/cache/").mkdirs()) {
            for (MultipartFile file : multiFile) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    file.transferTo(new File("D:/cache/" + originalFilename));
                }
            }
        }
        return "main";
    }
}
