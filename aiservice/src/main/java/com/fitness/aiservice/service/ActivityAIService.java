package com.fitness.aiservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("RESPONSE FROM AI: {}", aiResponse);
        
        return processAiResponse(activity, aiResponse);
    }

    private Recommendation processAiResponse(Activity activity, String aiResponse){
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("candidates")
                                        .get(0)
                                        .path("content")
                                        .path("parts")
                                        .get(0)
                                        .path("text");
            String jsonContent = textNode.asText()
                                        .replaceAll("```json\\n", "")
                                        .replaceAll("\\n```", "")
                                        .trim();
             log.info("PARSED RESPONSE FROM AI: {}", jsonContent);

             JsonNode analysisJson = mapper.readTree(jsonContent);
             JsonNode analysisNode = analysisJson.path("analysis");
             StringBuilder fullAnalysis = new StringBuilder();
             addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall:");
             addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace:");
             addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate:");
             addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories:");

             List<String> improvements = extractImprovements(analysisJson.path("improvements"));
             List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety = extractSafetyGuidelines(analysisJson.path("saftey"));

            return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation(fullAnalysis.toString().trim())
                .improvements(improvements)
                .suggestions(suggestions)
                .safety(safety)
                .createdAt(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity){
        return Recommendation.builder()
            .activityId(activity.getId())
            .userId(activity.getUserId())
            .activityType(activity.getType())
            .recommendation("Unable to generate detailed analysis")
            .improvements(Collections.singletonList("Continue with your current routine"))
            .suggestions(Collections.singletonList("Consider consulting a fitness professional"))
            .safety(Arrays.asList(
                "Always warm up before exercise",
                "Stay hydrated",
                "Listen to your body"
            ))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix){
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                        .append(analysisNode.path(key).asText())
                        .append("\n\n");
        }
    }

    private List<String> extractImprovements(JsonNode improvementsNode){
        List<String> improvements = new ArrayList<>();
        if(improvementsNode.isArray()){
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, detail));
            });
        }
        return improvements.isEmpty() ? Collections.singletonList("No specific improvements provided") : improvements;
    }

    private List<String> extractSuggestions(JsonNode suggestionsNode){
        List<String> suggestions = new ArrayList<>();
         if(suggestionsNode.isArray()){
            suggestionsNode.forEach(suggestion -> {
                String area = suggestion.path("workout").asText();
                String detail = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", area, detail));
            });     
        }
         return suggestions.isEmpty() ? Collections.singletonList("No specific suggestions provided") : suggestions;
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode){
        List<String> safety = new ArrayList<>();
         if(safetyNode.isArray()){
            safetyNode.forEach(item -> safety.add(item.asText()));
        }
        return safety.isEmpty() ? Collections.singletonList("Follow general safety guidelines") : safety;
    }

    private String createPromptForActivity(Activity activity){
        return String.format("""
                Act as a professional fitness coach. Analyze the following activity:
                Activity Type: %s
                Duration: %d minutes
                Calories Burned: %d
                Additional Metrics: %s
                
                Based on this data, provide a detailed analysis including:
                1. A general recommendation/summary.
                2. Specific areas for improvement.
                3. Suggestions for future workouts.
                4. Safety advice relevant to this activity.
                
                Output the result strictly in the following JSON format:
                {
                  "recommendation": "String",
                  "improvements": ["String", "String"],
                  "suggestions": ["String", "String"],
                  "safety": ["String", "String"]
                }
                """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics()
        );
    }
}
