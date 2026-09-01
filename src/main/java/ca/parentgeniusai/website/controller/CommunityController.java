package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.CommunityPostPage;
import ca.parentgeniusai.website.model.Pillar;
import ca.parentgeniusai.website.service.PillarService;
import ca.parentgeniusai.website.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class CommunityController {

    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);
    static final String SESSION_ASK_QUESTION = "COMMUNITY_ASK_QUESTION";

    private final PillarService pillarService;
    private final PostService postService;

    public CommunityController(PillarService pillarService, PostService postService) {
        this.pillarService = pillarService;
        this.postService = postService;
    }

    @GetMapping("/community")
    public String community(@RequestParam(name = "askError", required = false) String askError,
                            HttpServletRequest request,
                            Model model) {
        model.addAttribute("askError", askError != null);
        addPendingQuestion(request, model);
        return "community";
    }

    @GetMapping("/community/pillar/{pillarId}")
    public String pillarPosts(@PathVariable Long pillarId, Model model) {
        Pillar pillar = pillarService.getPillarById(pillarId);
        if (pillar == null) {
            logger.warn("Community pillar {} not found; redirecting to /community", pillarId);
            return "redirect:/community";
        }

        model.addAttribute("pillar", pillar);
        logger.info("Serving community posts page for pillar {}", pillarId);
        return "community/pillar-posts";
    }

    @GetMapping(value = "/community/pillar/{pillarId}/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CommunityPostPage pillarPostsPage(@PathVariable Long pillarId,
                                             @RequestParam(name = "page", defaultValue = "1") int page) {
        if (pillarService.getPillarById(pillarId) == null) {
            return CommunityPostPage.empty(page, PostService.PAGE_SIZE);
        }
        return postService.getPostsByPillarId(pillarId, page);
    }

    @GetMapping("/community/pillar/{pillarId}/new")
    public String newPillarPost(@PathVariable Long pillarId, HttpServletRequest request, Model model) {
        Pillar pillar = pillarService.getPillarById(pillarId);
        if (pillar == null) {
            return "redirect:/community";
        }
        if (requireLogin(request, "/community/pillar/" + pillarId + "/new")) {
            return "redirect:/signin";
        }
        model.addAttribute("pillar", pillar);
        return "community/pillar-post-edit";
    }

    @PostMapping("/community/pillar/{pillarId}/new")
    public String savePillarPost(@PathVariable Long pillarId,
                                 @RequestParam(name = "content", required = false) String content,
                                 @RequestParam(name = "media", required = false) List<MultipartFile> media,
                                 HttpServletRequest request,
                                 Model model) {
        Pillar pillar = pillarService.getPillarById(pillarId);
        if (pillar == null) {
            return "redirect:/community";
        }
        if (requireLogin(request, "/community/pillar/" + pillarId + "/new")) {
            return "redirect:/signin";
        }

        String text = content == null ? "" : content.trim();
        boolean hasMedia = media != null && media.stream().anyMatch(file -> file != null && !file.isEmpty());
        if (text.isBlank() && !hasMedia) {
            model.addAttribute("pillar", pillar);
            model.addAttribute("content", content);
            model.addAttribute("postError", "empty");
            return "community/pillar-post-edit";
        }

        Long createdId = postService.createPost(text, pillarId, getJwtToken(request), media);
        if (createdId == null) {
            model.addAttribute("pillar", pillar);
            model.addAttribute("content", content);
            model.addAttribute("postError", "save");
            return "community/pillar-post-edit";
        }
        return "redirect:/community/pillar/" + pillarId;
    }

    @PostMapping("/community/ask")
    public String askAi(
            @RequestParam(name = "question", required = false) String question,
            HttpServletRequest request) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return "redirect:/community";
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_ASK_QUESTION, q);

        String jwtToken = getJwtToken(request);
        if (jwtToken == null) {
            session.setAttribute(CourseController.SESSION_LOGIN_REDIRECT, "/community");
            logger.info("Community Ask AI requires login; saving question and redirecting to signin");
            return "redirect:/signin";
        }

        Long matched = pillarService.classifyPillar(q, jwtToken);
        if (matched == null) {
            logger.warn("[AskAI] community: no pillar for question='{}'", q);
            return "redirect:/community?askError=1";
        }
        logger.info("[AskAI] community: question='{}' -> pillar {}", q, matched);
        session.removeAttribute(SESSION_ASK_QUESTION);
        return "redirect:/community/pillar/" + matched;
    }

    private void addPendingQuestion(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_ASK_QUESTION) instanceof String pending
                && !pending.isBlank()) {
            model.addAttribute("askQuestion", pending);
        }
    }

    private boolean requireLogin(HttpServletRequest request, String returnTo) {
        if (getJwtToken(request) != null) {
            return false;
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(CourseController.SESSION_LOGIN_REDIRECT, returnTo);
        logger.info("Community post compose requires login; redirecting to signin");
        return true;
    }

    private String getJwtToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String jwt = (session != null) ? (String) session.getAttribute("STRAPI_JWT") : null;
        return jwt == null || jwt.isBlank() ? null : jwt;
    }
}
