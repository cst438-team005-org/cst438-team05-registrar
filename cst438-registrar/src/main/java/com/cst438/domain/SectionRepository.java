package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SectionRepository extends CrudRepository<Section, Integer> {
	
    @Query("select s from Section s where s.course.courseId like :courseId and s.term.year=:year and s.term.semester=:semester order by s.course.courseId, s.sectionId asc")
    List<Section> findByLikeCourseIdAndYearAndSemester(String courseId, int year, String semester);

    @Query("select s from Section s where current_date between s.term.addDate and s.term.addDeadline " +
            " order by s.course.courseId, s.sectionId")
    List<Section> findByOpenOrderByCourseIdSectionId();

}


