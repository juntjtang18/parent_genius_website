package ca.parentgeniusai.website.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SegmentController {

    @GetMapping("/seg-mini-course")
    public String miniCourseSegment() {
        return "segments/seg-mini-course";
    }

    @GetMapping("/seg-ai-chat")
    public String aiChatSegment() {
        return "segments/seg-ai-chat";
    }

    @GetMapping("/seg-topics-and-tips")
    public String topicsAndTips() {
        return "segments/seg-topics-and-tips";
    }

    @GetMapping("/seg-emotion")
    public String emotionSegment() {
        return "segments/seg-emotion";
    }

    @GetMapping("/seg-join-community")
    public String communitySegment() {
        return "segments/seg-community"; // fixed typo
    }
}
