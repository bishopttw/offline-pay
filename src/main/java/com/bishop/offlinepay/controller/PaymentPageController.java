package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class PaymentPageController {

    private final PaymentService paymentService;

    public PaymentPageController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @GetMapping("/add-money")
    public String showAddMoney() {
        return "add-money";
    }

    @PostMapping("/add-money")
    public String startPayment(@RequestParam BigDecimal amount, HttpSession session, Model model){
        Long accountId = (Long) session.getAttribute("accountId");
        try{
            String checkoutUrl = paymentService.initialize(accountId, amount);
            return "redirect:" + checkoutUrl;
        } catch(RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "add-money";
        }
    }
}
