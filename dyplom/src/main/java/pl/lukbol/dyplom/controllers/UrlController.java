package pl.lukbol.dyplom.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UrlController {
    @RequestMapping(value = "/register")
    public ModelAndView getRegister() {
        return new ModelAndView("register");
    }

    @RequestMapping(value = "/login")
    public ModelAndView getLogin() {
        return new ModelAndView("login");
    }

    @RequestMapping(value = "/")
    public ModelAndView getEmpty() {
        return new ModelAndView("login");
    }

    @RequestMapping(value = "/profile")
    public ModelAndView getProfile() {
        return new ModelAndView("profile");
    }

    @RequestMapping(value = "/price_list")
    public ModelAndView getPrices() {
        return new ModelAndView("price_list");
    }

    @RequestMapping(value = "/daily")
    public ModelAndView getDailies() {
        return new ModelAndView("daily");
    }

    @RequestMapping(value = "/locked")
    public ModelAndView getLocked() {
        return new ModelAndView("locked");
    }

    @RequestMapping(value = "/clientChat")
    public ModelAndView getClientChat() {
        return new ModelAndView("clientChat");
    }

    @RequestMapping(value = "/employeeChat")
    public ModelAndView getEmployeeChat() {
        return new ModelAndView("employeeChat");
    }

    @RequestMapping(value = "/materials")
    public ModelAndView getMaterialsList() {
        return new ModelAndView("materials");
    }

    @RequestMapping(value = "/ordersList")
    public ModelAndView getOrderByCode() {
        return new ModelAndView("orders");
    }


}

