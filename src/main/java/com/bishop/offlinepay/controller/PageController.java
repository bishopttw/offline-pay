package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.dto.AccountResponse;
import com.bishop.offlinepay.dto.LoginRequest;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Gender;
import com.bishop.offlinepay.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.hibernate.query.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    private final AccountService accountService;

    @Autowired
    public PageController(AccountService accountService){
        this.accountService = accountService;
    }

    @GetMapping("/signup")
    public String showSignupPage(){
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam Gender gender,
            @RequestParam String password,
            Model model) {

        try {
            SignupRequest request = new SignupRequest();
            request.setName(name);
            request.setEmail(email);
            request.setGender(gender);
            request.setPassword(password);

            accountService.signup(request);
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "signup";
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest httpRequest,
            Model model) {

        try {
            LoginRequest request = new LoginRequest();
            request.setEmail(email);
            request.setPassword(password);

            AccountResponse response = accountService.login(request);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("accountId", response.getId());

            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "login";
        }
    }

}
