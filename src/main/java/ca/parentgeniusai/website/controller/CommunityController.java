package ca.parentgeniusai.website.controller;

import ca.parentgeniusai.website.model.CommunityPost;
import ca.parentgeniusai.website.model.Pillar;
import ca.parentgeniusai.website.service.PillarService;
import ca.parentgeniusai.website.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CommunityController {

    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);

    private final PillarService pillarService;
    private final PostService postService;

    public CommunityController(PillarService pillarService, PostService postService) {
        this.pillarService = pillarService;
        this.postService = postService;
    }

    @GetMapping("/community/pillar/{pillarId}")
    public String pillarPosts(@PathVariable Long pillarId, Model model) {
        Pillar pillar = pillarService.getPillarById(pillarId);
        if (pillar == null) {
            logger.warn("Community pillar {} not found; redirecting to /community", pillarId);
            return "redirect:/community";
        }

        List<CommunityPost> posts = postService.getPostsByPillarId(pillarId);
        model.addAttribute("pillar", pillar);
        model.addAttribute("posts", posts);
        logger.info("Serving community posts for pillar {} with {} posts", pillarId, posts.size());
        return "community/pillar-posts";
    }
}
