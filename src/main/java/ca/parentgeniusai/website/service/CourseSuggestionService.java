package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseSuggestionService {
    private static final Logger logger = LoggerFactory.getLogger(CourseSuggestionService.class);

    private final CourseService courseService;

    public CourseSuggestionService(CourseService courseService) {
        this.courseService = courseService;
    }

    public List<Course> suggest(List<String> challengeIds, String ageRange) {
        List<Course> courses = courseService.getSuggestedCourses(challengeIds, ageRange);
        logger.info("Suggested {} courses from Strapi for challenges={} age={}",
            courses.size(), challengeIds, ageRange);
        return courses;
    }
}
