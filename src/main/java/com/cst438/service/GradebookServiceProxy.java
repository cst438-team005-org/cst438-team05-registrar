package com.cst438.service;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class GradebookServiceProxy {

    private final EnrollmentRepository enrollmentRepository;
    private final RabbitTemplate rabbitTemplate;

    public GradebookServiceProxy(EnrollmentRepository enrollmentRepository, RabbitTemplate rabbitTemplate) {
        this.enrollmentRepository = enrollmentRepository;
        this.rabbitTemplate = rabbitTemplate;
    }
    Queue gradebookServiceQueue = new Queue("gradebook_service", true);

    @Bean
    public Queue createQueue() {
        return new Queue("registrar_service", true);
    }

    @RabbitListener(queues = "registrar_service")
    public void receiveFromGradebook(String message)  {
        try {
            System.out.println("registrar received " + message);
            // process updateEnrollment messages from GradeBook
            // only updatable field is grade
            String[] parts = message.split(" ", 2);
            if (parts[0].equals("updateEnrollment")) {
                EnrollmentDTO dto = fromJsonString(parts[1], EnrollmentDTO.class);
                Enrollment e = enrollmentRepository.findById(dto.enrollmentId()).orElse(null);
                if (e==null) {
                    System.out.println("Error. enrollment id not found " + dto.enrollmentId());
                } else {
                    e.setGrade(dto.grade());
                    enrollmentRepository.save(e);
                    System.out.println("Ok. Enrollment updated");
                }
            } else {
                System.out.println("Error. unknown message type");
            }

        } catch (Exception e) {
            System.out.println("Exception "+e.getMessage());
        }
    }
    public void sendMessage(String cmd, Object obj)  {
        String msg = cmd +" "+ asJsonString(obj);
        System.out.println("sending "+msg);
        rabbitTemplate.convertAndSend(gradebookServiceQueue.getName(), msg);
    }

    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T  fromJsonString(String str, Class<T> valueType ) {
        try {
            return new ObjectMapper().readValue(str, valueType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}