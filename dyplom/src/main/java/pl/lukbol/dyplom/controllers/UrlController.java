package pl.lukbol.dyplom.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UrlController {
    @RequestMapping(value="/register")
    public ModelAndView getLogin() {
        return new ModelAndView("register");
    }

    @RequestMapping(value="/login")
    public ModelAndView getRegister() {
        return new ModelAndView("login");
    }
    @RequestMapping(value="/")
    public ModelAndView getIndex() {
        return new ModelAndView("index");
    }

}

