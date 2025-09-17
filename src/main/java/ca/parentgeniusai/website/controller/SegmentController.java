package ca.parentgeniusai.website.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class SegmentController {
    @GetMapping("/seg-mini-course")
    public String miniCourseSegment() {
        // This returns the Thymeleaf template located at templates/mini-courses.html
        return "segments/seg-mini-course";
    }
    
    @GetMapping("/seg-ai-chat")
    public String aiChatSegment() {
    	return "segments/seg-ai-chat";
    }
}
