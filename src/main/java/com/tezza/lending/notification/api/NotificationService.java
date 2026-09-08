package com.tezza.lending.notification.api;

import com.tezza.lending.notification.api.dto.*;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    TemplateResponse createTemplate(TemplateRequest request);
    List<TemplateResponse> listTemplates();
    TemplateResponse updateTemplate(UUID id, TemplateRequest request);
    NotificationRuleRequest createRule(NotificationRuleRequest request);
    List<NotificationLogResponse> getCustomerLogs(UUID customerId);
}
