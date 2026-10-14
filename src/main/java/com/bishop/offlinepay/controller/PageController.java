package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.dto.AccountResponse;
import com.bishop.offlinepay.dto.LoginRequest;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.TransactionRepository;
import com.bishop.offlinepay.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.List;

@Controller
public class PageController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public PageController(AccountService accountService, AccountRepository accountRepository, TransactionRepository transactionRepository){
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/signup")
    public String showSignupPage(){
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(
            @Valid @ModelAttribute SignupRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().get(0).getDefaultMessage();
            model.addAttribute("error", message);
            return "signup";
        }

        try {
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

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("accountId");
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) {
            session.invalidate();
            return "redirect:/login";
        }

        List<Transaction> recent = transactionRepository
                .findBySenderIdOrReceiverIdOrderByTimestampDesc(accountId, accountId)
                .stream()
                .limit(5)
                .toList();

        String firstName = account.getName().trim().split("\\s+")[0];

        model.addAttribute("account", account);
        model.addAttribute("transactions", recent);
        model.addAttribute("firstName", firstName);
        return "dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }

}
