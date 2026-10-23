package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.model.DepositStatus;
import com.bishop.offlinepay.service.DepositService;
import com.bishop.offlinepay.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class PaymentPageController {

    private final PaymentService paymentService;
    private final DepositService depositService;

    public PaymentPageController(PaymentService paymentService, DepositService depositService){
        this.paymentService = paymentService;
        this.depositService = depositService;
    }

    @GetMapping("/add-money")
    public String showAddMoney() {
        return "add-money";
    }

    @GetMapping("/payments/callback")
    public String callback(@RequestParam("reference") String reference,
                           RedirectAttributes redirect) {
        try {
            PaymentService.VerifiedPayment payment = paymentService.verify(reference);

            if (payment == null) {
                // we could not reach Paystack or got an error: don't touch the deposit
                redirect.addFlashAttribute("payError",
                        "We could not confirm your payment right now. If you paid, it will be added shortly.");
            } else if ("success".equals(payment.status())) {
                DepositStatus result = depositService.creditDeposit(
                        reference, payment.amountKobo(), payment.currency()
                );
                if (result == DepositStatus.SUCCESS) {
                    redirect.addFlashAttribute("payMessage", "Payment successful. Your money has been added.");
                } else {
                    redirect.addFlashAttribute("payError", "Payment could not be confirmed.");
                }
            } else {
                depositService.markFailed(reference);
                redirect.addFlashAttribute("payError", "Payment was not completed. Nothing was added to your wallet.");
            }
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("payError", "Something went wrong confirming your payment.");
        }
        return "redirect:/dashboard";
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
