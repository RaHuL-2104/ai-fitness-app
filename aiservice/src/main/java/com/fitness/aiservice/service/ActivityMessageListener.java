package com.fitness.aiservice.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService activityAIService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "${spring.rabbitmq.template.default-receive-queue}")
    public void processActivity(Activity activity){
        log.info("Recieved activity for processing: {}", activity.getId());
       // log.info("Generated Recommendation: {}", activityAIService.generateRecommendation(activity));
        Recommendation recommendation = activityAIService.generateRecommendation(activity);
        recommendationRepository.save(recommendation);
    }
}
