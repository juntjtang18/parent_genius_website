package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.service.MembershipService;
import ca.parentgeniusai.website.service.MembershipService.RegisterResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class MembershipController {

    private static final Logger logger = LoggerFactory.getLogger(MembershipController.class);
    private static final String FROM_COURSE = "MEMBERSHIP_FROM_COURSE";

    private final MembershipService membershipService;
    private final SecurityContextRepository securityContextRepository;

    public MembershipController(MembershipService membershipService,
                                SecurityContextRepository securityContextRepository) {
        this.membershipService = membershipService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/membership")
    public String membership(@RequestParam(name = "fromCourse", required = false) String fromCourse,
                             Model model) {
        String returnPath = membershipService.safeReturnPath(fromCourse);
        model.addAttribute("signupUrl", membershipService.getSignupUrl());
        model.addAttribute("fromCourse", returnPath);
        logger.info("Serving membership page{}", returnPath == null ? "" : " from course " + returnPath);
        return "membership";
    }

    @GetMapping("/membership/register")
    public String createAccount(@RequestParam(name = "fromCourse", required = false) String fromCourse,
                                HttpServletRequest request,
                                Model model) {
        rememberReturnPath(request, fromCourse);
        model.addAttribute("step", 1);
        return "membership/register";
    }

    @PostMapping("/membership/register")
    public String createAccountSubmit(@RequestParam String email,
                                      @RequestParam String password,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      Model model) {
        RegisterResult result = membershipService.register(email, password);
        if (!result.ok()) {
            model.addAttribute("step", 1);
            model.addAttribute("email", email);
            model.addAttribute("signupError", result.error());
            return "membership/register";
        }
        login(request, response, result.username(), result.jwt());
        return "redirect:/membership/register/personalize";
    }

    @GetMapping("/membership/register/personalize")
    public String personalize(HttpServletRequest request, Model model) {
        if (jwt(request) == null) {
            return "redirect:/membership/register";
        }
        model.addAttribute("step", 2);
        model.addAttribute("childAges", MembershipService.CHILD_AGES);
        model.addAttribute("hobbies", MembershipService.HOBBIES);
        return "membership/register";
    }

    @PostMapping("/membership/register/personalize")
    public String personalizeSubmit(@RequestParam(name = "childAge", required = false) String childAge,
                                    @RequestParam(name = "hobby", required = false) String hobby,
                                    @RequestParam(name = "skip", required = false) String skip,
                                    HttpServletRequest request) {
        String token = jwt(request);
        if (token == null) {
            return "redirect:/membership/register";
        }
        if (skip == null) {
            membershipService.updatePersonalization(token, childAge, hobby);
        }
        return "redirect:/membership/register/welcome";
    }

    @GetMapping("/membership/register/welcome")
    public String welcome(HttpServletRequest request, Model model) {
        if (jwt(request) == null) {
            return "redirect:/membership/register";
        }
        HttpSession session = request.getSession(false);
        String next = session != null ? membershipService.safeReturnPath((String) session.getAttribute(FROM_COURSE)) : null;
        model.addAttribute("step", 3);
        model.addAttribute("nextUrl", next != null ? next : "/pillars");
        return "membership/register";
    }

    private void rememberReturnPath(HttpServletRequest request, String fromCourse) {
        String path = membershipService.safeReturnPath(fromCourse);
        if (path != null) {
            request.getSession(true).setAttribute(FROM_COURSE, path);
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response, String username, String jwt) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            username,
            jwt,
            List.of(
                new SimpleGrantedAuthority("ROLE_AUTHENTICATED"),
                new SimpleGrantedAuthority("ROLE_USER")
            )
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute("STRAPI_JWT", jwt);
        securityContextRepository.saveContext(context, request, response);
        logger.info("Membership signup signed in as {}", username);
    }

    private static String jwt(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object token = session != null ? session.getAttribute("STRAPI_JWT") : null;
        return token instanceof String s && !s.isBlank() ? s : null;
    }
}
