package ca.parentgeniusai.website.service;

import ca.parentgeniusai.website.model.Course;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourseSuggestionServiceTest {

    @Test
    void suggestDelegatesToStrapiBoundedQuery() {
        Course course = new Course();
        course.setTitle("Sleeping");
        CourseService courseService = new CourseService() {
            @Override
            public List<Course> getSuggestedCourses(List<String> challenges, String age) {
                assertEquals(List.of("emotions", "dailylife"), challenges);
                assertEquals("0-2", age);
                return List.of(course);
            }
        };

        List<Course> suggested = new CourseSuggestionService(courseService)
            .suggest(List.of("emotions", "dailylife"), "0-2");

        assertEquals(1, suggested.size());
        assertEquals("Sleeping", suggested.get(0).getTitle());
    }
}
