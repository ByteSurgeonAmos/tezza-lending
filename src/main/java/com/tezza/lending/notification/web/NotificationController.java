package com.tezza.lending.notification.web;

import com.tezza.lending.notification.api.dto.*;
import com.tezza.lending.notification.internal.entity.NotificationRule;
import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.repository.NotificationLogRepository;
import com.tezza.lending.notification.internal.repository.NotificationRuleRepository;
import com.tezza.lending.notification.internal.repository.NotificationTemplateRepository;
import com.tezza.lending.shared.ApiResponse;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class NotificationController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRuleRepository ruleRepository;
    private final NotificationLogRepository logRepository;

    public NotificationController(NotificationTemplateRepository templateRepository,
                                   NotificationRuleRepository ruleRepository,
                                   NotificationLogRepository logRepository) {
        this.templateRepository = templateRepository;
        this.ruleRepository = ruleRepository;
        this.logRepository = logRepository;
    }

    @PostMapping("/templates")
    @Operation(summary = "Create a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setEventType(request.getEventType());
        template.setChannel(request.getChannel());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created", TemplateResponse.from(templateRepository.save(template))));
    }

    @GetMapping("/templates")
    @Operation(summary = "List all notification templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> listTemplates() {
        List<TemplateResponse> templates = templateRepository.findAll().stream()
                .map(TemplateResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a notification template")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationTemplate", id.toString()));
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        return ResponseEntity.ok(ApiResponse.success("Template updated",
                TemplateResponse.from(templateRepository.save(template))));
    }

    @PostMapping("/rules")
    @Operation(summary = "Create a notification rule")
    public ResponseEntity<ApiResponse<NotificationRule>> createRule(
            @Valid @RequestBody NotificationRuleRequest request) {
        NotificationRule rule = new NotificationRule();
        rule.setProductId(request.getProductId());
        rule.setCustomerSegment(request.getCustomerSegment());
        rule.setEventType(request.getEventType());
        rule.setChannel(request.getChannel());
        rule.setEnabled(request.isEnabled());
        rule.setPriority(request.getPriority());
        rule.setDelayMinutes(request.getDelayMinutes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rule created", ruleRepository.save(rule)));
    }

    @GetMapping("/logs/{customerId}")
    @Operation(summary = "Get notification logs for a customer")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> getLogs(@PathVariable UUID customerId) {
        List<NotificationLogResponse> logs = logRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(NotificationLogResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
