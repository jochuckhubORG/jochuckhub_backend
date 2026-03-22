package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.request.SignUpRequest;
import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.Role;
import com.guenbon.jochuckhub.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public String getMembers(Model model) {
        model.addAttribute("members", memberService.getMembers());
        return "members/list";
    }

    @GetMapping("/signup")
    public String signUpForm(Model model) {
        model.addAttribute("signUpRequest", new SignUpRequest());
        model.addAttribute("positions", Position.values());
        model.addAttribute("roles", Role.values());
        return "members/signup";
    }

    @PostMapping("/signup")
    public String signUp(@Valid @ModelAttribute SignUpRequest signUpRequest,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("positions", Position.values());
            model.addAttribute("roles", Role.values());
            return "members/signup";
        }

        try {
            memberService.register(signUpRequest);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("positions", Position.values());
            model.addAttribute("roles", Role.values());
            return "members/signup";
        }

        redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료됐습니다. 로그인해주세요.");
        return "redirect:/login";
    }
}
