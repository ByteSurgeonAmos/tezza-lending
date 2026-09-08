package com.tezza.lending.notification;

import com.tezza.lending.notification.internal.service.TemplateRendererService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererServiceTest {

    private final TemplateRendererService renderer = new TemplateRendererService();

    @Test
    void render_singleVariable_substituted() {
        String result = renderer.render("Hello {{name}}!", Map.of("name", "Alice"));
        assertThat(result).isEqualTo("Hello Alice!");
    }

    @Test
    void render_multipleVariables_allSubstituted() {
        String template = "Loan {{loanNumber}} of KES {{amount}} due {{dueDate}}";
        Map<String, String> vars = Map.of(
                "loanNumber", "TZ-2026-00000001",
                "amount", "10,000",
                "dueDate", "2026-10-07");
        String result = renderer.render(template, vars);
        assertThat(result).isEqualTo("Loan TZ-2026-00000001 of KES 10,000 due 2026-10-07");
    }

    @Test
    void render_missingVariable_leftAsIs() {
        String result = renderer.render("Hello {{name}} and {{unknown}}!", Map.of("name", "Bob"));
        assertThat(result).isEqualTo("Hello Bob and {{unknown}}!");
    }

    @Test
    void render_emptyTemplate_returnsEmpty() {
        assertThat(renderer.render("", Map.of("key", "value"))).isEmpty();
    }

    @Test
    void render_nullTemplate_returnsEmpty() {
        assertThat(renderer.render(null, Map.of())).isEmpty();
    }

    @Test
    void render_noPlaceholders_returnsTemplateUnchanged() {
        String template = "No placeholders here.";
        assertThat(renderer.render(template, Map.of("key", "value"))).isEqualTo(template);
    }
}
