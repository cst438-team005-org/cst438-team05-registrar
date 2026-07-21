package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface CourseRepository extends CrudRepository<Course, String> {
    @Query("select c from Course c order by c.courseId")
    List<Course> findAllOrderByCourseId();
}
